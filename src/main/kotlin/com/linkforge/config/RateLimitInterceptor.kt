package com.linkforge.config

import com.linkforge.service.RateLimitService
import com.linkforge.util.IpHashUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RateLimitInterceptor(
    private val rateLimitService: RateLimitService,
    private val ipHashUtil: IpHashUtil
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val uri = request.requestURI
        val method = request.method
        
        val operation = when {
            uri == "/api/v1/urls" && method == "POST" -> "create_url"
            uri.startsWith("/api/v1/analytics") -> "analytics"
            uri.matches(Regex("^/api/v1/urls/[^/]+/analytics$")) -> "analytics"
            else -> return true 
        }

        val ip = getClientIp(request)
        val clientId = ipHashUtil.hashIp(ip)

        val result = rateLimitService.isAllowed(operation, clientId)

        if (!result.allowed) {
            response.status = 429
            response.contentType = "application/json"
            val retryAfterSeconds = Math.max(1, (result.retryAfterMs + 999) / 1000)
            response.setHeader("Retry-After", retryAfterSeconds.toString())
            response.writer.write("""{"error": "Too Many Requests", "message": "Rate limit exceeded. Try again later."}""")
            return false
        }

        return true
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
        return ip?.split(",")?.firstOrNull()?.trim() ?: "unknown"
    }
}
