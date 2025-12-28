package com.coderace.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caching configuration using Caffeine
 * Caches user objects to reduce database queries on every authenticated request
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager
     * TTL set to 24 hours to match JWT expiration
     * Max size to prevent memory issues
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache configuration
     * - TTL: 24 hours (matches JWT expiration)
     * - Max size: 10,000 users (adjust based on expected concurrent users)
     * - Eviction: LRU (Least Recently Used)
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(10_000)
                .recordStats(); // Enable cache statistics for monitoring
    }
}
