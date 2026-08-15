package com.linkforge.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RateLimitService(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${app.rate-limit.anonymous.requests:60}") private val maxRequests: Int,
    @Value("\${app.rate-limit.anonymous.window-ms:60000}") private val windowMs: Long,
    private val metricsTracker: com.linkforge.util.MetricsTracker
) {
    private val logger = LoggerFactory.getLogger(RateLimitService::class.java)

    private val rateLimitScript = DefaultRedisScript(
        """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window_size_ms = tonumber(ARGV[2])
        local max_requests = tonumber(ARGV[3])
        local request_id = ARGV[4]

        redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window_size_ms)
        local current_requests = redis.call('ZCARD', key)

        if current_requests < max_requests then
            redis.call('ZADD', key, now, request_id)
            redis.call('PEXPIRE', key, window_size_ms)
            return 0
        else
            local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
            if oldest and oldest[2] then
                local oldest_time = tonumber(oldest[2])
                local wait_time = oldest_time + window_size_ms - now
                return wait_time
            else
                return window_size_ms
            end
        end
        """.trimIndent(),
        Long::class.java
    )

    fun isAllowed(operation: String, clientId: String): RateLimitResult {
        val key = "rate:$operation:$clientId"
        val now = Instant.now().toEpochMilli()
        val requestId = UUID.randomUUID().toString()

        return try {
            val result = redisTemplate.execute(
                rateLimitScript,
                listOf(key),
                now.toString(),
                windowMs.toString(),
                maxRequests.toString(),
                requestId
            )

            if (result != null && result > 0L) {
                metricsTracker.recordRateLimitRejection()
                RateLimitResult(allowed = false, retryAfterMs = result)
            } else {
                RateLimitResult(allowed = true, retryAfterMs = 0)
            }
        } catch (e: Exception) {
            logger.warn("Redis rate limiting failed. Failing open for {}", key, e)
            RateLimitResult(allowed = true, retryAfterMs = 0)
        }
    }
}

data class RateLimitResult(
    val allowed: Boolean,
    val retryAfterMs: Long
)
