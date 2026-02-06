package com.github.sor2171.backend.service.impl

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.sor2171.backend.entity.RestBean
import com.github.sor2171.backend.entity.bo.StOutput
import com.github.sor2171.backend.entity.dto.ChatHistory
import com.github.sor2171.backend.entity.vo.request.ChatHistoryRequestVO
import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.entity.vo.response.ChatHistoryResponseVO
import com.github.sor2171.backend.mapper.ChatHistoryMapper
import com.github.sor2171.backend.service.ChatHistoryService
import com.github.sor2171.backend.service.SseEmitterBrokerService
import com.github.sor2171.backend.utils.Const
import com.github.sor2171.backend.utils.FlowUtils
import com.github.sor2171.backend.utils.JwtUtils
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux
import java.util.*

@Service
class ChatHistoryServiceImpl(
    private val utilsF: FlowUtils,
    private val utilsJ: JwtUtils,
    private val rabbitTemplate: RabbitTemplate,
    private val stWebClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val brokerService: SseEmitterBrokerService,

    @param:Value("\${spring.st.limitTime}")
    private val chatLimitSeconds: Int

) : ServiceImpl<ChatHistoryMapper, ChatHistory>(), ChatHistoryService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getChatHistory(
        vo: ChatHistoryRequestVO,
        request: HttpServletRequest
    ): String {
        val userId = utilsJ.requestToId(request)
            ?: return RestBean
                .unauthenticated("Invalid token. Please log in again.")
                .toJsonString()

        var limit = vo.limit
        logger.info("Received history request by $userId, limit: $limit")

        if (limit <= 0) {
            return RestBean
                .success(emptyArray<ChatHistoryResponseVO>())
                .toJsonString()
        }
        if (limit % 2 == 1) limit += 1
        if (limit > 200) limit = 200
        limit /= 2

        val results = ktQuery()
            .eq(ChatHistory::userId, userId)
            .isNotNull(ChatHistory::output)
            .orderByDesc(ChatHistory::begin)
            .last("LIMIT $limit")
            .list() // 直接调用 .list() 获取结果
            .asReversed()
        logger.info("User: $userId, get history size: ${results.size}")

        val chatHistoryList = results.flatMap { res ->
            listOf(
                ChatHistoryResponseVO("user", res.input, res.id.toString()),
                ChatHistoryResponseVO("assistant", res.output!!, res.id.toString())
            )
        }

        return RestBean
            .success(chatHistoryList)
            .toJsonString()
    }

    override fun getNewChatRequest(
        vo: ChatRequestVO,
        request: HttpServletRequest
    ): String {
        val headerToken = utilsJ.requestToHeader(request)!!

        synchronized(headerToken.intern()) {
            if (!this.verifyLimit(headerToken)) {
                return RestBean
                    .forbidden("Request limit exceeded. Please try again later.")
                    .toJsonString()
            }

            val userId = utilsJ.tokenToIdOrNull(headerToken)
                ?: return RestBean
                    .unauthenticated("Invalid token. Please log in again.")
                    .toJsonString()

            logger.info(
                "Get message from user " +
                        "(${utilsJ.toUser(utilsJ.resolveJwt(headerToken)!!).username}): " +
                        "${vo.messages.take(10)}..."
            )

            val chatHistory = vo.toAnotherObject(
                ChatHistory::class,
                mapOf(
                    "id" to null,
                    "userId" to userId,
                    "input" to vo.messages[vo.messages.size - 1].content,
                    "output" to null,
                    "begin" to Date(),
                    "finish" to null,
                ) as Map<String, Any?>
            )

            this.save(chatHistory)
            vo.uuid = chatHistory.id.toString()
            brokerService.registerSessionId(vo.uuid)

            rabbitTemplate.convertAndSend(
                Const.ST_EXCHANGE_NAME,
                Const.ST_ROUTING_KEY,
                vo
            )

            return RestBean
                .success(mapOf("sessionId" to vo.uuid))
                .toJsonString()
        }
    }

    override fun sendRequestAndHandleStream(vo: ChatRequestVO): Flux<String> {
        // 构造发送给 ST 的标准 OpenAI 兼容请求体
        val stRequestBody = mapOf(
            "model" to when (vo.modelId) {
                1 -> "qwen3:0.6b"
                else -> "qwen3:0.6b"
            },
            "messages" to vo.messages.map {
                mapOf(
                    "role" to it.role,
                    "content" to it.content
                )
            },
            "stream" to true
        )

        val fullContent = StringBuilder()

        return stWebClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(stRequestBody)
            .retrieve()
            .bodyToFlux<String>()
            .flatMap { rawChunk ->
                // 解析 SSE 格式，提取 JSON 字符串
                rawChunk.split("\n")
                    .map { it.trim() }
                    .filter { it != "[DONE]" }
                    .map { jsonString ->
                        return@map try {
                            val chunk = objectMapper.readValue(
                                jsonString,
                                StOutput::class.java
                            )
                            // 提取核心内容并返回
                            val originalChunk = chunk.choices.firstOrNull()?.delta?.content ?: ""
                            if (originalChunk.isNotEmpty()) {
                                "|$originalChunk"
                            } else {
                                ""
                            }
                        } catch (e: Exception) {
                            logger.error("Error parsing JSON chunk for session ${vo.uuid}: $jsonString", e)
                            ""
                        }
                    }
                    .filter { it.isNotEmpty() }
                    .let { Flux.fromIterable(it) }
            }
            .doOnNext { content ->
                fullContent.append(content.substring(1))
//                logger.info("Session ${vo.uuid} - Chunk: [${content.replace("\n", "\\n")}]")
            }
            .doOnError { e ->
                logger.error("ST API stream failed for session ${vo.uuid}", e)
            }
            .doOnComplete {
                // 流结束后，更新数据库中的聊天记录
                this.getById(vo.uuid.toInt())?.let {
                    it.output = fullContent.toString()
                    it.finish = Date()
                    this.updateById(it)
                    logger.info("Session ${vo.uuid} - Chat finished: ${fullContent.toString().take(32)}...")
                } ?: logger.warn("Session ${vo.uuid} - Chat history not found in DB.")
            }
    }

    private fun verifyLimit(jwtToken: String): Boolean {
        val key = Const.VERIFY_CHAT_LIMIT + jwtToken
        return utilsF.limitOnceCheck(key, chatLimitSeconds)
    }
}