package com.github.sor2171.backend.controller

import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.service.ChatHistoryService
import com.github.sor2171.backend.service.SseEmitterBrokerService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/chat")
class StChatController(
    private val chatService: ChatHistoryService,
    private val brokerService: SseEmitterBrokerService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // 1. 请求入口：接收请求并加入队列
    @PostMapping("/request")
    fun sendChatRequest(@RequestBody vo: ChatRequestVO, request: HttpServletRequest): String {
        logger.info("Received request for session ${vo.uuid}")

        return chatService.getNewChatRequest(
            vo,
            request.remoteAddr,
            request
                .getHeader(HttpHeaders.AUTHORIZATION)
                .substring("Bearer ".length)
        )
    }

    // 2. 响应出口：建立 SSE 连接
    @GetMapping("/stream/{sessionId}")
    fun streamResponse(@PathVariable sessionId: String): SseEmitter {

        // 注册 SseEmitter 并返回。Spring MVC 将保持此 HTTP 连接打开。
        val emitter = brokerService.registerEmitter(sessionId)
            ?: return brokerService.invalidateEmitter()
        
        logger.info("SSE Stream requested for session $sessionId")

        // 首次发送一个空消息或状态消息，以确保连接立即建立
        try {
            emitter.send(
                SseEmitter
                    .event()
                    .name("CONNECT")
                    .data("Stream established for $sessionId")
            )
        } catch (e: Exception) {
            // 首次发送失败，说明连接有问题，直接关闭
            logger.error("Failed to send initial SSE message.", e)
            emitter.completeWithError(e)
        }

        return emitter
    }
}