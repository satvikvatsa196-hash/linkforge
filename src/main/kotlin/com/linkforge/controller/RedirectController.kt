package com.linkforge.controller

import com.linkforge.service.UrlService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@Tag(name = "URL Redirection", description = "Handles redirection from short code to original URL")
class RedirectController(private val urlService: UrlService) {

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to original URL", description = "Looks up the short code and returns a 302 Found redirect to the original URL")
    fun redirect(@PathVariable shortCode: String): ResponseEntity<Void> {
        val originalUrl = urlService.getOriginalUrl(shortCode)
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build()
    }
}
