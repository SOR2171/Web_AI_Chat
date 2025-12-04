package com.github.sor2171.backend.config

import com.github.sor2171.backend.utils.Const
import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfiguration {

    @Bean
    fun stJsonMessageConverter() = Jackson2JsonMessageConverter()

    @Bean
    fun stQueue(): Queue = Queue(Const.ST_QUEUE_NAME, true)

    @Bean
    fun stExchange(): TopicExchange = TopicExchange(Const.ST_EXCHANGE_NAME)

    @Bean
    fun stBinding(stQueue: Queue, stExchange: TopicExchange): Binding =
        BindingBuilder.bind(stQueue).to(stExchange).with(Const.ST_ROUTING_KEY)

    @Bean
    fun mailQueue(): Queue = Queue(Const.MAIL_QUEUE_NAME, true)

    @Bean
    fun mailExchange(): TopicExchange = TopicExchange(Const.MAIL_EXCHANGE_NAME)

    @Bean
    fun mailBinding(mailQueue: Queue, mailExchange: TopicExchange): Binding =
        BindingBuilder.bind(mailQueue).to(mailExchange).with(Const.MAIL_ROUTING_KEY)
}