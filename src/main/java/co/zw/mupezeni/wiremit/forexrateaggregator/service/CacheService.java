/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 8:07 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Redis cache operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ALL_RATES_CACHE = "allRates";
    private static final String CURRENCY_RATE_CACHE = "currencyRate";
    private static final String HISTORICAL_RATES_CACHE = "historicalRates";

    /**
     * Evict all rates caches
     */
    public void evictAllRatesCaches() {
        log.info("Evicting all rates caches");

        evictCache(ALL_RATES_CACHE);
        evictCache(CURRENCY_RATE_CACHE);
        evictCache(HISTORICAL_RATES_CACHE);

        log.info("All rates caches evicted successfully");
    }


    public void evictCurrencyRateCache(String currency) {
        log.debug("Evicting currency rate cache for: {}", currency);

        Cache cache = cacheManager.getCache(CURRENCY_RATE_CACHE);
        if (cache != null) {
            cache.evict(currency);
            log.debug("Currency rate cache evicted for: {}", currency);
        }
    }


    public void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cache '{}' cleared", cacheName);
        }
    }


    public void putWithTtl(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            log.debug("Cached value with key: {} and TTL: {} {}", key, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Error caching value with key: {}", key, e);
        }
    }


    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error retrieving value with key: {}", key, e);
            return null;
        }
    }


    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking key existence: {}", key, e);
            return false;
        }
    }


    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted key from cache: {}", key);
        } catch (Exception e) {
            log.error("Error deleting key: {}", key, e);
        }
    }


    public Set<String> getKeys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Error getting keys with pattern: {}", pattern, e);
            return Set.of();
        }
    }


    public void logCacheStatistics() {
        try {
            Set<String> allKeys = redisTemplate.keys("*");
            log.info("Total cache keys: {}", allKeys != null ? allKeys.size() : 0);

            // Log cache sizes for our main caches
            logCacheSize(ALL_RATES_CACHE);
            logCacheSize(CURRENCY_RATE_CACHE);
            logCacheSize(HISTORICAL_RATES_CACHE);

        } catch (Exception e) {
            log.error("Error retrieving cache statistics", e);
        }
    }

    private void logCacheSize(String cachePattern) {
        Set<String> keys = getKeys(cachePattern + "*");
        log.debug("Cache '{}' contains {} keys", cachePattern, keys.size());
    }
}