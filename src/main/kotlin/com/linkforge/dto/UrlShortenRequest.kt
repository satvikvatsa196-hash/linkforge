package com.linkforge.dto

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.URL

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class UrlShortenRequest(
    @field:NotBlank(message = "URL cannot be blank")
    @field:URL(message = "Invalid URL format")
    @Schema(description = "The original long URL to shorten", example = "https://example.com/very-long-path")
    val originalUrl: String,

    @field:Future(message = "Expiration must be in the future")
    @Schema(description = "Optional expiration date in ISO-8601 format", example = "2027-12-31T23:59:59Z")
    val expiresAt: OffsetDateTime? = null,

    @field:Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Alias can only contain letters, numbers, hyphens, and underscores")
    @field:Size(min = 3, max = 50, message = "Alias must be between 3 and 50 characters")
    @Schema(description = "Optional custom alias (3-50 chars, alphanumeric/hyphens/underscores)", example = "my-link")
    val alias: String? = null,

    @field:Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}${'$'}", message = "Invalid domain format")
    @Schema(description = "Optional custom domain", example = "go.example.com")
    val domain: String? = null
)
