package com.github.sor2171.backend.config

import com.github.sor2171.backend.service.WebSocketBrokerService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Mono

@Configuration
class WebSocketConfiguration {

    @Bean
    fun webSocketHandler(brokerService: WebSocketBrokerService): WebSocketHandler {
        return WebSocketHandler { session ->
            // 1. 从 URI 或消息中获取 sessionId (这里假设前端连接 URI 为 /ws/{sessionId})
            val sessionId = session.attributes["sessionId"] as? String
                ?: session.handshakeInfo.uri.path.substringAfterLast('/')

            // 2. 注册会话
            brokerService.registerSession(sessionId, session)

            // 3. 处理传入消息 (前端通常只发送注册/心跳消息)
            session.receive()
                // 收到消息后，可以处理心跳或关闭请求
                .doOnNext { msg ->
                    println("Received message from $sessionId: ${msg.payloadAsText}")
                }
                .then()
                // 4. 关闭时注销
                .doFinally { brokerService.unregisterSession(sessionId) }
                .then(Mono.empty())
        }
    }

    @Bean
    fun handlerMapping(webSocketHandler: WebSocketHandler): HandlerMapping {
        return SimpleUrlHandlerMapping().apply {
            // 映射到 /ws/your-session-id 路径
            urlMap = mapOf("/ws/{sessionId}" to webSocketHandler)
            order = -1 // 确保它比其他 handler 先执行
        }
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}