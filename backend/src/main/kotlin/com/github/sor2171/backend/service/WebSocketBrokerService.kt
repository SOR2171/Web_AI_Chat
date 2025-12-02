package com.github.sor2171.backend.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class WebSocketBrokerService {
    // 存储 Session ID -> WebSocketSession 映射
    private val sessions: MutableMap<String, WebSocketSession> = ConcurrentHashMap()

    fun registerSession(sessionId: String, session: WebSocketSession) {
        sessions[sessionId] = session
        println("WebSocket registered: $sessionId")
    }

    fun unregisterSession(sessionId: String) {
        sessions.remove(sessionId)
        println("WebSocket unregistered: $sessionId")
    }

    fun pushMessage(sessionId: String, message: String): Boolean {
        val session = sessions[sessionId]
        return if (session != null && session.isOpen) {
            // 使用 session.send 发送消息给前端
            session.send(reactor.core.publisher.Flux.just(session.textMessage(message))).subscribe()
            true
        } else {
            println("Session $sessionId not found or closed.")
            false
        }
    }
}