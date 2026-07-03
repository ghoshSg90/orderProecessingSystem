package com.test.orderProcessingSystem.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationLifecycleLogger {

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        String[] profiles = environment.getActiveProfiles();
        String profile = profiles.length == 0 ? "default" : String.join(",", profiles);
        log.info("Application started successfully profile={} javaVersion={} database={} cache=Caffeine jwtEnabled=true",
                profile, System.getProperty("java.version"), databaseVendor());
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.info("Application shutting down");
    }

    private String databaseVendor() {
        String url = environment.getProperty("spring.datasource.url", "");
        // jdbc:postgresql://host:port/db -> postgresql
        String[] parts = url.split(":");
        return parts.length >= 2 ? parts[1] : "unknown";
    }
}
