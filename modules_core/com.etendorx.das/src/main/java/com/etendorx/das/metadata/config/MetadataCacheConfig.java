package com.etendorx.das.metadata.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Caffeine-based metadata caching.
 * This cache stores projection metadata loaded from the database to avoid repeated queries.
 *
 * Cache names:
 * - "projections": Cache by projection ID
 * - "projectionsByName": Cache by projection name (primary lookup method)
 *
 * Cache settings:
 * - Maximum 500 entries
 * - Expire after 24 hours of being written
 * - Statistics recording enabled for monitoring
 */
@Configuration
@EnableCaching
public class MetadataCacheConfig {

    /**
     * Creates the Caffeine-based cache manager for metadata caching.
     *
     * @return configured CacheManager with projection caches
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("projections", "projectionsByName");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofHours(24))
            .recordStats());
        return cacheManager;
    }
}
