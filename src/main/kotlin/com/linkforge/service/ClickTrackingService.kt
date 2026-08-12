package com.linkforge.service

import com.linkforge.model.ClickEvent
import com.linkforge.repository.ClickEventRepository
import com.linkforge.util.IpHashUtil
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClickTrackingService(
    private val clickEventRepository: ClickEventRepository,
    private val ipHashUtil: IpHashUtil
) {
    private val log = LoggerFactory.getLogger(ClickTrackingService::class.java)

    @Transactional
    fun recordClick(urlId: Long, request: HttpServletRequest) {
        try {
            val ipAddress = getClientIp(request)
            val ipHash = ipHashUtil.hashIp(ipAddress)
            val userAgent = request.getHeader("User-Agent")?.take(512)
            val referrer = request.getHeader("Referer")?.take(512)

            val clickEvent = ClickEvent(
                urlId = urlId,
                ipHash = ipHash,
                userAgent = userAgent,
                referrer = referrer
            )
            
            clickEventRepository.save(clickEvent)
        } catch (e: Exception) {
            log.error("Failed to record click for urlId: $urlId", e)
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
