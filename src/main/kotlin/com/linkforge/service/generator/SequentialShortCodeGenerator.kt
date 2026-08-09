package com.linkforge.service.generator

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import com.linkforge.util.Base62Encoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.data.repository.findByIdOrNull

@Component
@ConditionalOnProperty(name = ["app.shortener.strategy"], havingValue = "sequential", matchIfMissing = true)
class SequentialShortCodeGenerator(
    private val urlRepository: UrlRepository
) : ShortCodeGenerator {

    override fun generate(originalUrl: String, expiresAt: java.time.OffsetDateTime?): Url {
        val nextId = urlRepository.getNextSequenceValue()
        val actualShortCode = Base62Encoder.encode(nextId)
        
        urlRepository.insertUrlWithId(nextId, originalUrl, actualShortCode, expiresAt)
        
        return urlRepository.findByIdOrNull(nextId) 
            ?: throw IllegalStateException("Failed to retrieve inserted URL with id: $nextId")
    }
}
