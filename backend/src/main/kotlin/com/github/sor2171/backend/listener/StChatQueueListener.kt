package com.github.sor2171.backend.listener

import com.github.sor2171.backend.controller.exception.ValidationController
import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.service.ChatHistoryService
import com.github.sor2171.backend.service.WebSocketBrokerService
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
@RabbitListener(queues = ["st_chat"])
class StChatQueueListener(
    private val chatService: ChatHistoryService,
    private val brokerService: WebSocketBrokerService
) {
    private val log = LoggerFactory.getLogger(ValidationController::class.java)

    @RabbitHandler
    fun stChatRequest(request: ChatRequestVO) {
        log.info("Listener processing request for session ${request.uuid}")

        // 1. 调用 Service，获取流式响应 Flux<String>
        val stream = chatService.sendRequestAndHandleStream(request)

        // 2. 订阅流，并实时推送
        stream
            .doOnNext { content ->
                // 每收到一个文本块，就推送到前端
                brokerService.pushMessage(request.uuid, content)
            }
            .doOnComplete {
                // 流结束，发送结束标记
                brokerService.pushMessage(request.uuid, "[DONE]")
                log.info("Session ${request.uuid} completed and stream closed.")
            }
            .doOnError { e ->
                // 错误发生，发送错误信息
                log.error("ST API stream failed for session ${request.uuid}", e)
                brokerService.pushMessage(request.uuid, "[ERROR]: ${e.message}")
            }
            // 3. 关键：使用 block() 或 blockLast() 来确保 Listener 线程不会立即退出，等待 Flux 完成
            // 注意：在 Listener 中 block() 会占用线程池资源，但对于这种异步转同步的场景是必要的妥协。
            .blockLast()
    }
}