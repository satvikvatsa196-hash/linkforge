package com.linkforge.service

import com.linkforge.config.RabbitMQConfig
import com.linkforge.dto.ClickEventMessage
import com.linkforge.model.ClickEvent
import com.linkforge.repository.ClickEventRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClickEventConsumer(
    private val clickEventRepository: ClickEventRepository,
    private val metricsTracker: com.linkforge.util.MetricsTracker
) {
    private val log = LoggerFactory.getLogger(ClickEventConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.QUEUE_NAME])
    @Transactional
    fun processClickEvent(message: ClickEventMessage) {
        log.debug("Received click event for shortCode: {}", message.shortCode)
        
        val clickEvent = ClickEvent(
            urlId = message.urlId,
            clickedAt = message.timestamp,
            ipHash = message.ipHash,
            userAgent = message.userAgent,
            referrer = message.referrer
        )
        
        clickEventRepository.save(clickEvent)
        metricsTracker.recordAnalyticsEventProcessed()
        log.debug("Successfully saved click event for shortCode: {}", message.shortCode)
    }
}
