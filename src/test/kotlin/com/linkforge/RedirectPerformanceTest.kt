package com.linkforge

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.service.UrlService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.nanoseconds

@SpringBootTest
@ActiveProfiles("test")
class RedirectPerformanceTest {

    @Autowired
    private lateinit var urlService: UrlService

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Test
    fun benchmarkRedirectLookup() {
        val originalUrl = "https://example.com/benchmark-test-url-very-long"
        
        // Setup: create a short URL
        val request = UrlShortenRequest(originalUrl = originalUrl, alias = "bench123")
        
        // Ignore alias exists exception if run multiple times
        try {
            urlService.shortenUrl(request)
        } catch (e: Exception) {
            // Already exists
        }
        
        // 1. Measure DB lookup (cache bypassed)
        urlService.invalidateCache("bench123")
        
        // Warmup DB
        urlService.getOriginalUrl("bench123")
        urlService.invalidateCache("bench123")
        
        val dbLookupTimes = mutableListOf<Long>()
        for (i in 1..100) {
            urlService.invalidateCache("bench123")
            val time = measureNanoTime {
                urlService.getOriginalUrl("bench123")
            }
            dbLookupTimes.add(time)
        }
        
        val avgDbTime = dbLookupTimes.average().nanoseconds.inWholeMicroseconds
        
        // 2. Measure Cache hit
        // Ensure it's in cache
        urlService.getOriginalUrl("bench123")
        
        // Warmup Cache
        urlService.getOriginalUrl("bench123")
        
        val cacheLookupTimes = mutableListOf<Long>()
        for (i in 1..1000) {
            val time = measureNanoTime {
                urlService.getOriginalUrl("bench123")
            }
            cacheLookupTimes.add(time)
        }
        
        val avgCacheTime = cacheLookupTimes.average().nanoseconds.inWholeMicroseconds
        
        println("==================================================")
        println("PERFORMANCE BENCHMARK RESULTS (Microseconds)")
        println("==================================================")
        println("Average DB Lookup Time:   ${avgDbTime} µs")
        println("Average Cache Hit Time:   ${avgCacheTime} µs")
        if (avgCacheTime > 0) {
            println("Cache is ${avgDbTime / avgCacheTime}x faster")
        }
        println("==================================================")
    }
}
