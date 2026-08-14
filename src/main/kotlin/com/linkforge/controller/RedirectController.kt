package com.linkforge.controller

import com.linkforge.service.UrlService
import com.linkforge.service.ClickTrackingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@Tag(name = "URL Redirection", description = "Handles redirection from short code to original URL")
class RedirectController(
    private val urlService: UrlService,
    private val clickTrackingService: ClickTrackingService
) {

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to original URL", description = "Looks up the short code and returns a 302 Found redirect to the original URL")
    fun redirect(@PathVariable shortCode: String, request: HttpServletRequest): ResponseEntity<Void> {
        val domain = request.serverName
        val redirectInfo = urlService.getOriginalUrl(shortCode, domain)
        
        clickTrackingService.recordClick(redirectInfo.id, shortCode, request)
        
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(redirectInfo.originalUrl))
            .build()
    }
}
