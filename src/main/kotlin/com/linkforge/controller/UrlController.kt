package com.linkforge.controller

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.service.UrlService
import com.linkforge.service.QrCodeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URL Management", description = "Endpoints for shortening and managing URLs")
class UrlController(
    private val urlService: UrlService,
    private val qrCodeService: QrCodeService
) {

    @PostMapping
    @Operation(summary = "Shorten a URL", description = "Creates a short code for a given URL, or returns existing if already shortened")
    fun shortenUrl(@Valid @RequestBody request: UrlShortenRequest): ResponseEntity<UrlShortenResponse> {
        val response = urlService.shortenUrl(request)
        // Could return 201 Created or 200 OK. Returning 200 for simplicity and idempotency.
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{shortCode}/qr")
    @Operation(summary = "Get QR code", description = "Generates a QR code for the shortened URL")
    fun getQrCode(@PathVariable shortCode: String): ResponseEntity<ByteArray> {
        val fullUrl = urlService.getUrlForQr(shortCode)
        val qrCodeImage = qrCodeService.generateQrCode(fullUrl)
        
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.IMAGE_PNG_VALUE)
            .body(qrCodeImage)
    }
}
