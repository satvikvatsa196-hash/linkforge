package com.linkforge.service.generator

interface ShortCodeGenerator {
    /**
     * Generates a short code for the given original URL.
     * Implementations can use the provided originalUrl, or ignore it.
     */
    fun generate(originalUrl: String): String
}
