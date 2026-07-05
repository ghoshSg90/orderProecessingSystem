package com.test.orderProcessingSystem.health;

import com.test.orderProcessingSystem.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the in-process Caffeine cache. Unlike a networked cache (e.g. Redis), Caffeine
 * lives in the JVM heap, so "health" means the cache manager initialised correctly and the expected
 * cache is present and usable. Registered as a {@link HealthIndicator} bean, it also contributes a
 * "caffeine" entry to /actuator/health automatically.
 */
@Component
public class CaffeineHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;

    private static final Logger logger = LoggerFactory.getLogger(CaffeineHealthIndicator.class);

    public CaffeineHealthIndicator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Health health() {
        try {
            Cache cache = cacheManager.getCache(CacheConfig.USER_DETAILS_CACHE);
            if (cache == null) {
                return Health.down()
                        .withDetail("message", "Cache '" + CacheConfig.USER_DETAILS_CACHE + "' is not configured")
                        .build();
            }

            long estimatedSize = -1;
            if (cache instanceof CaffeineCache caffeineCache) {
                estimatedSize = caffeineCache.getNativeCache().estimatedSize();
            }

            if (logger.isInfoEnabled()) {
                logger.info("health() - Caffeine cache healthCheck. caches: {}, estimatedSize: {}",
                        cacheManager.getCacheNames(), estimatedSize);
            }

            return Health.up()
                    .withDetail("cacheManager", cacheManager.getClass().getSimpleName())
                    .withDetail("caches", cacheManager.getCacheNames())
                    .withDetail("estimatedSize", estimatedSize)
                    .withDetail("message", "Caffeine cache is up and running")
                    .build();

        } catch (Exception e) {
            return Health.down().withException(e).withDetail("message", "Caffeine health check failed").build();
        }
    }
}
