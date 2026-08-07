package com.linkforge.service.generator

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import com.linkforge.util.Base62Encoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.shortener.strategy"], havingValue = "sequential", matchIfMissing = true)
class SequentialShortCodeGenerator(
    private val urlRepository: UrlRepository
) : ShortCodeGenerator {

    override fun generate(originalUrl: String): String {
        // Save first with a temporary short code to get the ID
        val tempShortCode = UUID.randomUUID().toString()
        val url = Url(
            originalUrl = originalUrl,
            shortCode = tempShortCode
        )
        val savedUrl = urlRepository.save(url)

        // Generate actual short code using the generated ID
        val actualShortCode = Base62Encoder.encode(savedUrl.id)
        savedUrl.shortCode = actualShortCode
        urlRepository.save(savedUrl)
        
        return actualShortCode
    }
}
