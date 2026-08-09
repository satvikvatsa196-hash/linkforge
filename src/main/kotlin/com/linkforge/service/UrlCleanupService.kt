package com.linkforge.service

import com.linkforge.repository.UrlRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class UrlCleanupService(
    private val urlRepository: UrlRepository,
    private val urlService: UrlService
) {
    private val log = LoggerFactory.getLogger(UrlCleanupService::class.java)

    @Scheduled(cron = "\${app.cleanup.cron:0 * * * * *}") // Run every minute by default
    @Transactional
    fun cleanupExpiredUrls() {
        log.info("Starting expired URLs cleanup task")
        val now = OffsetDateTime.now()
        
        val expiredUrls = urlRepository.findByInactiveFalseAndExpiresAtBefore(now)
        
        if (expiredUrls.isEmpty()) {
            log.info("No expired URLs found to clean up")
            return
        }

        expiredUrls.forEach { url ->
            url.inactive = true
            urlService.invalidateCache(url.shortCode)
        }
        
        urlRepository.saveAll(expiredUrls)
        
        log.info("Cleaned up \${expiredUrls.size} expired URLs")
    }
}
