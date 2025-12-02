package com.github.sor2171.backend.service.impl

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.sor2171.backend.controller.exception.ValidationController
import com.github.sor2171.backend.entity.RestBean
import com.github.sor2171.backend.entity.bo.StOutput
import com.github.sor2171.backend.entity.dto.ChatHistory
import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.mapper.ChatHistoryMapper
import com.github.sor2171.backend.service.ChatHistoryService
import com.github.sor2171.backend.utils.Const
import com.github.sor2171.backend.utils.FlowUtils
import com.github.sor2171.backend.utils.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.util.Date

@Service
class ChatHistoryServiceImpl(
    private val utilsF: FlowUtils,
    private val utilsJ: JwtUtils,
    private val rabbitTemplate: RabbitTemplate,
    private val stWebClient: WebClient,
    private val objectMapper: ObjectMapper,

    @param:Value("\${spring.st.limitTime}")
    private val chatLimitSeconds: Int

) : ServiceImpl<ChatHistoryMapper, ChatHistory>(), ChatHistoryService {

    private val log = LoggerFactory.getLogger(ValidationController::class.java)

    override fun getNewChatRequest(vo: ChatRequestVO, ip: String, token: String): String {
        synchronized(token.intern()) {
            if (this.verifyLimit(token)) {
                val decodedToken = utilsJ.resolveJwt(token)
                val userId = decodedToken?.let { utilsJ.toId(it) }
                    ?: return RestBean
                        .unauthenticated("Invalid token. Please log in again.")
                        .toJsonString()

                log.info(
                    "Get message from user " +
                            "(${utilsJ.toUser(decodedToken).username}): " +
                            "${vo.input.take(10)}..."
                )

                val chatHistory = vo.toAnotherObject(
                    ChatHistory::class,
                    mapOf(
                        "id" to null,
                        "userId" to userId,
                        "output" to null,
                        "begin" to Date(),
                        "finish" to null,
                    ) as Map<String, Any?>
                )

                this.save(chatHistory)
                vo.uuid = chatHistory.id.toString()

//                rabbitTemplate.convertAndSend(
//                    "st_chat",
//                    vo.uuid,
//                    vo
//                )

                return RestBean.success(
                    mapOf("sessionId" to vo.uuid)
                ).toJsonString()
            }
            return RestBean
                .forbidden("Request limit exceeded. Please try again later.")
                .toJsonString()
        }
    }

    override fun sendRequestAndHandleStream(vo: ChatRequestVO): Flux<String> {
        // 构造发送给 ST 的标准 OpenAI 兼容请求体
        val stRequestBody = mapOf(
            "model" to vo.modelId,
            "messages" to listOf(mapOf("role" to "User", "content" to vo.input)),
            "stream" to true
        )

        return stWebClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(stRequestBody)
            .retrieve()
            .bodyToFlux(String::class.java) // 接收原始字符串流 (包含 data: [JSON])
            .flatMap { rawChunk ->
                // 解析 SSE 格式，提取 JSON 字符串
                rawChunk.split("\n")
                    .map { it.trim() }
                    .filter { it != "[DONE]" } // 过滤结束标记
                    .map { jsonString ->
                        try {
                            // 尝试反序列化 JSON
                            val chunk = objectMapper.readValue(
                                jsonString,
                                StOutput::class.java
                            )
                            // 提取核心内容并返回
                            chunk.choices.firstOrNull()?.delta?.content ?: ""
                        } catch (e: Exception) {
                            log.error("Error parsing JSON chunk for session ${vo.uuid}: $jsonString", e)
                            ""
                        }
                    }
                    .filter { it.isNotEmpty() } // 过滤掉空字符串（如只有 role 变化的 chunk）
                    .let { Flux.fromIterable(it) } // 转换为 Flux<String>
            }
            .doOnNext { content ->
                // 4. LOG: 打印处理后的流式内容
                log.info("Session ${vo.uuid} - Stream received: ${content.take(20)}...")
            }
            .doOnError { e ->
                log.error("ST API stream failed for session ${vo.uuid}", e)
            }
    }

    private fun findChatHistoryByChatId(id: Int) {}
    private fun findChatHistoryByUserId(id: Int) {}
    private fun existChatHistoryByUserId(id: Int) {}

    private fun verifyLimit(jwtToken: String): Boolean {
        val key = Const.VERIFY_CHAT_LIMIT + jwtToken
        return utilsF.limitOnceCheck(key, chatLimitSeconds)
    }
}