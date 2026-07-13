package com.tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for Redis cache operations with graceful fallback.
 * All operations are wrapped in try-catch to handle Redis unavailability.
 * When Redis is unavailable, a warning is logged and the caller proceeds with a direct DB query.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final long DEFAULT_TTL_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public CacheService(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        if (this.redisTemplate == null) {
            log.warn("RedisTemplate is not available. Caching is disabled.");
        }
    }

    /**
     * Retrieve a cached value by key.
     *
     * @param key the cache key
     * @return the cached value, or null if not found or Redis is unavailable
     */
    public Object get(String key) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable during cache get for key '{}'. Falling back to direct DB query.", key, e);
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error during cache get for key '{}'. Falling back to direct DB query.", key, e);
            return null;
        }
    }

    /**
     * Store a value in the cache with the default TTL of 5 minutes.
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Store a value in the cache with a custom TTL.
     *
     * @param key      the cache key
     * @param value    the value to cache
     * @param timeout  the TTL duration
     * @param timeUnit the time unit for the TTL
     */
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable during cache set for key '{}'. Continuing without caching.", key, e);
        } catch (Exception e) {
            log.warn("Unexpected error during cache set for key '{}'. Continuing without caching.", key, e);
        }
    }

    /**
     * Invalidate (delete) a cached entry by key.
     *
     * @param key the cache key to invalidate
     */
    public void invalidate(String key) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable during cache invalidation for key '{}'. Continuing without invalidation.", key, e);
        } catch (Exception e) {
            log.warn("Unexpected error during cache invalidation for key '{}'. Continuing without invalidation.", key, e);
        }
    }
}
