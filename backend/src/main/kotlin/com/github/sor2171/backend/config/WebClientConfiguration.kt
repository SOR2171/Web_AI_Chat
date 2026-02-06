package com.github.sor2171.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(

    @param:Value("\${spring.st.baseUrl}")
    private val baseUrl: String,

    @param:Value("\${spring.st.apiKey}")
    private val apiKey: String
) {
    @Bean
    fun stWebClient(builder: WebClient.Builder): WebClient {
        return builder
            // 指向 SillyTavern 的代理 API 地址
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer $apiKey")
            .build()
    }
}