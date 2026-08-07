package com.linkforge.service.generator

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
@ConditionalOnProperty(name = ["app.shortener.strategy"], havingValue = "random")
class RandomShortCodeGenerator(
    private val urlRepository: UrlRepository
) : ShortCodeGenerator {

    private val random = SecureRandom()
    private val alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val length = 7

    override fun generate(originalUrl: String): String {
        var shortCode: String
        var isUnique = false
        var attempts = 0
        val maxAttempts = 10

        do {
            shortCode = generateRandomString(length)
            // Check for collision
            if (urlRepository.findByShortCode(shortCode) == null) {
                isUnique = true
            }
            attempts++
        } while (!isUnique && attempts < maxAttempts)

        if (!isUnique) {
            throw RuntimeException("Could not generate a unique short code after $maxAttempts attempts")
        }

        val url = Url(
            originalUrl = originalUrl,
            shortCode = shortCode
        )
        urlRepository.save(url)
        
        return shortCode
    }

    private fun generateRandomString(len: Int): String {
        val sb = java.lang.StringBuilder(len)
        for (i in 0 until len) {
            sb.append(alphabet[random.nextInt(alphabet.length)])
        }
        return sb.toString()
    }
}
