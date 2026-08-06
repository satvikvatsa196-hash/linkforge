package com.linkforge.service

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UrlService(private val urlRepository: UrlRepository) {
    
    private val log = LoggerFactory.getLogger(UrlService::class.java)

    // TODO: Implement URL shortening logic
}
