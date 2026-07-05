package com.test.orderProcessingSystem.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs very early (before the logging system starts) and, when {@code app.log.path} is configured,
 * computes the absolute log file path and the file-only log level so {@code logback-spring.xml} can
 * write to  <logPath>/<env>_<appName>_<startTimestamp>.log .
 *
 * <p>If {@code app.log.path} is NOT set, it prints a warning and leaves file logging disabled — the
 * application still starts and console logging continues normally.
 */
public class LoggingFileEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Activated only when file logging is enabled; matched by <springProfile> in logback-spring.xml. */
    private static final String FILE_LOGGING_PROFILE = "file-logging";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String logPath = environment.getProperty("app.log.path");

        if (!StringUtils.hasText(logPath)) {
            // Logging system isn't up yet, so warn on stderr. Do not fail — the app must still start.
            System.err.println("WARNING: 'app.log.path' is not configured - file logging is disabled. "
                    + "Set 'app.log.path' in application.yaml (or via env) to enable it.");
            return;
        }

        String env = resolveEnvironment(environment);
        String appName = environment.getProperty("spring.application.name", "application");
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        String fileName = env + "_" + appName + "_" + timestamp + ".log";
        String logFile = logPath.replaceAll("[/\\\\]+$", "") + "/" + fileName;

        Map<String, Object> props = new HashMap<>();
        props.put("LOG_FILE", logFile);
        props.put("FILE_LOG_LEVEL", fileLevelFor(env));
        environment.getPropertySources().addFirst(new MapPropertySource("computedLogFile", props));

        environment.addActiveProfile(FILE_LOGGING_PROFILE);
    }

    private String resolveEnvironment(ConfigurableEnvironment environment) {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            active = StringUtils.commaDelimitedListToStringArray(
                    environment.getProperty("spring.profiles.active", ""));
        }
        for (String p : active) {
            if (p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("qa") || p.equalsIgnoreCase("prod")) {
                return p.toLowerCase();
            }
        }
        return active.length > 0 ? active[0].toLowerCase() : "app";
    }

    /** File-only threshold: dev = all levels, qa = info/warn/error, prod = warn/error. */
    private String fileLevelFor(String env) {
        return switch (env) {
            case "dev" -> "TRACE";
            case "qa" -> "INFO";
            case "prod" -> "WARN";
            default -> "INFO";
        };
    }

    @Override
    public int getOrder() {
        // Run after ConfigData (application.yaml already loaded) but before the logging system starts.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
