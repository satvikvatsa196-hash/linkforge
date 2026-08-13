package com.linkforge.service

import com.linkforge.dto.ClickEventMessage
import com.linkforge.repository.ClickEventRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.OffsetDateTime
import org.mockito.ArgumentMatchers.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

@ExtendWith(MockitoExtension::class)
class ClickEventConsumerTest {

    @Mock
    private lateinit var clickEventRepository: ClickEventRepository

    @InjectMocks
    private lateinit var clickEventConsumer: ClickEventConsumer

    private fun anyClickEvent(): com.linkforge.model.ClickEvent {
        org.mockito.ArgumentMatchers.any(com.linkforge.model.ClickEvent::class.java)
        return com.linkforge.model.ClickEvent(urlId = 0L, ipHash = "")
    }

    @Test
    fun `processClickEvent should save event to repository`() {
        // Arrange
        val message = ClickEventMessage(
            urlId = 1L,
            shortCode = "test",
            timestamp = OffsetDateTime.now(),
            ipHash = "hash123",
            userAgent = "test-agent",
            referrer = "test-referer"
        )

        // Act
        clickEventConsumer.processClickEvent(message)

        // Assert
        Mockito.verify(clickEventRepository).save(anyClickEvent())
    }

    @Test
    fun `processClickEvent should propagate exception on db failure`() {
        // Arrange
        val message = ClickEventMessage(
            urlId = 1L,
            shortCode = "test",
            timestamp = OffsetDateTime.now(),
            ipHash = "hash123",
            userAgent = null,
            referrer = null
        )

        Mockito.`when`(clickEventRepository.save(anyClickEvent())).thenThrow(RuntimeException("DB failure"))

        // Act & Assert
        val exception = assertThrows<RuntimeException> {
            clickEventConsumer.processClickEvent(message)
        }
        
        assertThat(exception.message).isEqualTo("DB failure")
        Mockito.verify(clickEventRepository).save(anyClickEvent())
    }
}
