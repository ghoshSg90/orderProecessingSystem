package com.test.orderProcessingSystem.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /** Cache of UserDetails keyed by username, populated on every authenticated request. */
    public static final String USER_DETAILS_CACHE = "userDetails";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(USER_DETAILS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Safety net: a cached user goes stale after 15 min even if an eviction is missed
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(1_000));
        return cacheManager;
    }
}
