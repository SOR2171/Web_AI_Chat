package com.github.sor2171.backend.listener

import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.service.ChatHistoryService
import com.github.sor2171.backend.service.SseEmitterBrokerService
import com.github.sor2171.backend.utils.Const
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component


@Component
class STRequestListener(
    private val chatService: ChatHistoryService,
    private val brokerService: SseEmitterBrokerService // 注入 SseEmitter Broker
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // Listener 线程现在可以是非阻塞的，因为它只负责启动 Flux
    @RabbitListener(queues = [Const.ST_QUEUE_NAME])
    fun handleChatRequest(request: ChatRequestVO) {
        logger.info("Listener starting async stream for session ${request.uuid}")

        val stream = chatService.sendRequestAndHandleStream(request)

        stream
            .doOnNext { content ->
                brokerService.pushMessage(request.uuid, content)
            }
            .doOnComplete {
                brokerService.pushMessage(request.uuid, "[DONE]")
                brokerService.completeSession(request.uuid)
                logger.info("Session ${request.uuid} completed and SSE closed.")
            }
            .doOnError { e ->
                logger.error("ST API stream failed for session ${request.uuid}", e)
                brokerService.pushMessage(request.uuid, "[ERROR]: ${e.message}")
                brokerService.completeSession(request.uuid)
            }
            .subscribe()
    }
}