package com.test.orderProcessingSystem.controller;


import java.util.List;

import com.test.orderProcessingSystem.dto.HealthCheckResponse;
import com.test.orderProcessingSystem.health.CaffeineHealthIndicator;
import com.test.orderProcessingSystem.repository.HealthCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;


@RestController("HealthCheckController")
@RequestMapping("/v1/healthCheck")
@Tag(name = "Health Check Services", description=" - API's related to Application Health Check")
public class HealthCheckController {

    @Autowired
    HealthCheckRepository healthCheckRepository;
    @Autowired
    CaffeineHealthIndicator caffeineHealthIndicator;

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);


    /**
     * Startup probe: the app has finished initialising and every critical subsystem (database + cache)
     * is reachable. Returns 503 until everything is ready.
     */
    @GetMapping(value = "/startup", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<HealthCheckResponse> startupCheck() {
        try {
            dbCheck();
            Status cacheStatus = caffeineHealthIndicator.health().getStatus();
            if (!Status.UP.equals(cacheStatus)) {
                logger.warn("startup healthCheck DOWN - cache status={}", cacheStatus);
                return down();
            }
            return up();
        } catch (Exception e) {
            logger.warn("startup healthCheck DOWN - {}", e.getMessage());
            return down();
        }
    }

    /**
     * Liveness probe: is the application process itself responsive? Deliberately does NOT touch the
     * database or cache — restarting the pod (which a failed liveness triggers) cannot fix an external
     * outage, so liveness must not depend on external systems.
     */
    @GetMapping(value = "/liveliness", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<HealthCheckResponse> livelinessCheck() {
        logger.debug("liveliness healthCheck");
        return up();
    }

    /**
     * Readiness probe: can the app currently serve traffic? Checks the database, since requests cannot
     * be served without it. Returns 503 when the DB is unreachable so traffic is diverted away from
     * this instance (without restarting it).
     */
    @GetMapping(value = "/readiness", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<HealthCheckResponse> readinessCheck() {
        try {
            dbCheck();
            return up();
        } catch (Exception e) {
            logger.warn("readiness healthCheck DOWN - {}", e.getMessage());
            return down();
        }
    }

    /**
     * Cache health: reports the Caffeine cache status, returning 503 when it is DOWN.
     */
    @GetMapping(value = "/caffeine", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Health> caffeineCheck() {
        Health health = caffeineHealthIndicator.health();
        HttpStatus httpStatus = Status.UP.equals(health.getStatus())
                ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }

    private void dbCheck() {
        List<Object[]> response = healthCheckRepository.dbConnectionCheck();
        if (logger.isDebugEnabled()) {
            logger.debug("dbCheck success rows={}", response.size());
        }
    }

    private ResponseEntity<HealthCheckResponse> up() {
        return ResponseEntity.ok(new HealthCheckResponse(HealthCheckResponse.STATUS.UP));
    }

    private ResponseEntity<HealthCheckResponse> down() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HealthCheckResponse(HealthCheckResponse.STATUS.DOWN));
    }
}
