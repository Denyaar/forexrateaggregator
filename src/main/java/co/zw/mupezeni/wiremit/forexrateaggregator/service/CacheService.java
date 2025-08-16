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


    public void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cache '{}' cleared", cacheName);
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








    }


