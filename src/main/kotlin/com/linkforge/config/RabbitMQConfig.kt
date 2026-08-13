package com.linkforge.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val EXCHANGE_NAME = "analytics.exchange"
        const val QUEUE_NAME = "analytics.click.queue"
        const val ROUTING_KEY = "analytics.click.routingKey"

        const val DLX_NAME = "analytics.dlx"
        const val DLQ_NAME = "analytics.click.dlq"
        const val DLQ_ROUTING_KEY = "analytics.click.dlq.routingKey"
    }

    @Bean
    fun dlq(): Queue {
        return Queue(DLQ_NAME, true)
    }

    @Bean
    fun dlx(): DirectExchange {
        return DirectExchange(DLX_NAME)
    }

    @Bean
    fun dlqBinding(dlq: Queue, dlx: DirectExchange): Binding {
        return BindingBuilder.bind(dlq).to(dlx).with(DLQ_ROUTING_KEY)
    }

    @Bean
    fun clickQueue(): Queue {
        return QueueBuilder.durable(QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build()
    }

    @Bean
    fun exchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NAME)
    }

    @Bean
    fun binding(clickQueue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder.bind(clickQueue).to(exchange).with(ROUTING_KEY)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: MessageConverter): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }
}
