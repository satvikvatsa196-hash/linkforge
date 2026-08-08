package com.linkforge.service.generator

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.SecureRandom

import org.springframework.dao.DataIntegrityViolationException

@Component
@ConditionalOnProperty(name = ["app.shortener.strategy"], havingValue = "random")
class RandomShortCodeGenerator(
    private val urlRepository: UrlRepository
) : ShortCodeGenerator {

    private val random = SecureRandom()
    private val alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val length = 7

    override fun generate(originalUrl: String): Url {
        var attempts = 0
        val maxAttempts = 10

        while (attempts < maxAttempts) {
            val shortCode = generateRandomString(length)
            
            // Fast check for collision
            if (urlRepository.findByShortCode(shortCode) == null) {
                val url = Url(
                    originalUrl = originalUrl,
                    shortCode = shortCode
                )
                try {
                    return urlRepository.saveAndFlush(url)
                } catch (e: DataIntegrityViolationException) {
                    // Collision caught by DB unique constraint
                }
            }
            attempts++
        }

        throw RuntimeException("Could not generate a unique short code after $maxAttempts attempts")
    }

    private fun generateRandomString(len: Int): String {
        val sb = java.lang.StringBuilder(len)
        for (i in 0 until len) {
            sb.append(alphabet[random.nextInt(alphabet.length)])
        }
        return sb.toString()
    }
}
