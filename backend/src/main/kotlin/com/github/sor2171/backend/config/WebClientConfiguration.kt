package com.github.sor2171.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(

    @param:Value("\${spring.st.apiKey}")
    val apiKey: String,
) {
    @Bean
    fun stWebClient(builder: WebClient.Builder): WebClient {
        return builder
            // 指向 SillyTavern 的代理 API 地址 (例如: 8002 或 8003 端口)
            .baseUrl("http://127.0.0.1:8003/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .build()
    }
}