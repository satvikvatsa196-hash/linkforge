package com.linkforge.util

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestId = request.getHeader("X-Request-ID") ?: UUID.randomUUID().toString()
        MDC.put("requestId", requestId)

        val startTime = System.currentTimeMillis()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val status = response.status
            val endpoint = request.requestURI
            val method = request.method
            
            if (!endpoint.startsWith("/actuator") && !endpoint.startsWith("/swagger") && !endpoint.startsWith("/v3/api-docs")) {
                log.info("Request completed: method={} endpoint={} status={} duration={}ms", method, endpoint, status, duration)
            }
            MDC.remove("requestId")
        }
    }
}
