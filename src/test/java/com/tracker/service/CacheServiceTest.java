package com.tracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @SuppressWarnings("unchecked")
    @Mock
    private ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        cacheService = new CacheService(redisTemplateProvider);
    }

    @Test
    void get_shouldReturnCachedValue_whenRedisAvailable() {
        String key = "dashboard:1";
        String cachedValue = "{\"income\": 1000}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(cachedValue);

        Object result = cacheService.get(key);

        assertThat(result).isEqualTo(cachedValue);
        verify(redisTemplate).opsForValue();
    }

    @Test
    void get_shouldReturnNull_whenKeyNotFound() {
        String key = "dashboard:999";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        Object result = cacheService.get(key);

        assertThat(result).isNull();
    }

    @Test
    void get_shouldReturnNull_whenRedisUnavailable() {
        String key = "dashboard:1";

        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Connection refused"));

        Object result = cacheService.get(key);

        assertThat(result).isNull();
    }

    @Test
    void set_shouldStoreValueWithDefaultTtl() {
        String key = "dashboard:1";
        Object value = "{\"income\": 1000}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.set(key, value);

        verify(valueOperations).set(key, value, 5, TimeUnit.MINUTES);
    }

    @Test
    void set_shouldStoreValueWithCustomTtl() {
        String key = "dashboard:1";
        Object value = "{\"income\": 1000}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.set(key, value, 10, TimeUnit.MINUTES);

        verify(valueOperations).set(key, value, 10, TimeUnit.MINUTES);
    }

    @Test
    void set_shouldNotThrow_whenRedisUnavailable() {
        String key = "dashboard:1";
        Object value = "{\"income\": 1000}";

        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Connection refused"));

        // Should not throw - logs warning and continues
        cacheService.set(key, value);
    }

    @Test
    void invalidate_shouldDeleteKey_whenRedisAvailable() {
        String key = "dashboard:1";

        when(redisTemplate.delete(key)).thenReturn(true);

        cacheService.invalidate(key);

        verify(redisTemplate).delete(key);
    }

    @Test
    void invalidate_shouldNotThrow_whenRedisUnavailable() {
        String key = "dashboard:1";

        when(redisTemplate.delete(key)).thenThrow(new RedisConnectionFailureException("Connection refused"));

        // Should not throw - logs warning and continues
        cacheService.invalidate(key);
    }

    @Test
    void get_shouldReturnNull_whenUnexpectedExceptionOccurs() {
        String key = "dashboard:1";

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Unexpected error"));

        Object result = cacheService.get(key);

        assertThat(result).isNull();
    }

    @Test
    void set_shouldNotThrow_whenUnexpectedExceptionOccurs() {
        String key = "dashboard:1";
        Object value = "test";

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Unexpected error"));

        // Should not throw
        cacheService.set(key, value);
    }

    @Test
    void invalidate_shouldNotThrow_whenUnexpectedExceptionOccurs() {
        String key = "dashboard:1";

        when(redisTemplate.delete(key)).thenThrow(new RuntimeException("Unexpected error"));

        // Should not throw
        cacheService.invalidate(key);
    }
}
