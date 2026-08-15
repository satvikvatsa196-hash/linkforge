package com.linkforge.util

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class MetricsTracker(private val registry: MeterRegistry) {
    fun recordRedirectRequest() = registry.counter("linkforge.redirects.total").increment()
    fun recordRedirectSuccess() = registry.counter("linkforge.redirects.success").increment()
    fun recordRedirectNotFound() = registry.counter("linkforge.redirects.not_found").increment()
    fun recordRedirectExpired() = registry.counter("linkforge.redirects.expired").increment()
    
    fun recordCacheHit() = registry.counter("linkforge.cache.hits").increment()
    fun recordCacheMiss() = registry.counter("linkforge.cache.misses").increment()
    
    fun recordUrlCreated() = registry.counter("linkforge.urls.created").increment()
    
    fun recordRateLimitRejection() = registry.counter("linkforge.ratelimit.rejected").increment()
    
    fun recordRabbitMqFailure() = registry.counter("linkforge.rabbitmq.failures").increment()
    
    fun recordAnalyticsEventProcessed() = registry.counter("linkforge.analytics.processed").increment()
}
