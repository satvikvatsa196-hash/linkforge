package com.linkforge.service

import com.linkforge.dto.ClickEventMessage
import com.linkforge.config.RabbitMQConfig
import com.linkforge.util.IpHashUtil
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ClickTrackingService(
    private val rabbitTemplate: RabbitTemplate,
    private val ipHashUtil: IpHashUtil
) {
    private val log = LoggerFactory.getLogger(ClickTrackingService::class.java)

    fun recordClick(urlId: Long, shortCode: String, request: HttpServletRequest) {
        try {
            val ipAddress = getClientIp(request)
            val ipHash = ipHashUtil.hashIp(ipAddress)
            val userAgent = request.getHeader("User-Agent")?.take(512)
            val referrer = request.getHeader("Referer")?.take(512)

            val event = ClickEventMessage(
                urlId = urlId,
                shortCode = shortCode,
                timestamp = OffsetDateTime.now(),
                ipHash = ipHash,
                userAgent = userAgent,
                referrer = referrer
            )
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event)
            log.debug("Published click event for shortCode: {}", shortCode)
        } catch (e: Exception) {
            log.error("Failed to publish click event for shortCode: $shortCode", e)
        }
    }

    private fun getClientIp(request: HttpServletRequest): String {
        var ip = request.getHeader("X-Forwarded-For")
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        // X-Forwarded-For can contain a comma-separated list of IPs. 
        // The first one is the client IP.
        if (!ip.isNullOrEmpty() && ip.contains(",")) {
            ip = ip.split(",")[0].trim()
        }
        return ip ?: "unknown"
    }
}
