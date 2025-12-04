package com.github.sor2171.backend.controller

import com.github.sor2171.backend.entity.RestBean
import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.service.SseEmitterBrokerService
import com.github.sor2171.backend.utils.Const
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/chat")
class StChatController(
    private val rabbitTemplate: RabbitTemplate,
    private val brokerService: SseEmitterBrokerService // 注入 SseEmitter Broker
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // 1. 请求入口：接收请求并加入队列
    @PostMapping("/request")
    fun sendChatRequest(@RequestBody request: ChatRequestVO): String {
        logger.info("Received request for session ${request.uuid}")

        // 理想情况下，前端应先请求 stream 接口

        rabbitTemplate.convertAndSend(
            Const.ST_EXCHANGE_NAME,
            Const.ST_ROUTING_KEY,
            request
        )

        return RestBean.accept().toJsonString()
    }

    // 2. 响应出口：建立 SSE 连接
    @GetMapping("/stream/{sessionId}")
    fun streamResponse(@PathVariable sessionId: String): SseEmitter {
        logger.info("SSE Stream requested for session $sessionId")

        // 注册 SseEmitter 并返回。Spring MVC 将保持此 HTTP 连接打开。
        val emitter = brokerService.registerEmitter(sessionId)

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