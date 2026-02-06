package com.github.sor2171.backend.service

import com.github.sor2171.backend.utils.Const
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Service
class SseEmitterBrokerService(
    private val redisTemplate: StringRedisTemplate
) {
    // 存储 Session ID -> SseEmitter 映射
    private val emitters: MutableMap<String, SseEmitter> = ConcurrentHashMap()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun registerSessionId(uuid: String) {
        if (!existSessionId(uuid)) {
            redisTemplate.opsForValue().set(
                Const.VERIFY_CHAT_SESSION + uuid,
                "1",
                Const.EMITTER_TIMEOUT / 60 / 1000,
                java.util.concurrent.TimeUnit.MINUTES
            )
        }
    }

    fun existSessionId(uuid: String): Boolean = redisTemplate
        .hasKey(Const.VERIFY_CHAT_SESSION + uuid) ?: false

    fun invalidateEmitter(): SseEmitter {
        return SseEmitter().apply {
            try {
                this.send(
                    SseEmitter
                        .event()
                        .name("ERROR")
                        .data("Invalid or expired session ID.")
                )
            } catch (e: Exception) {
                logger.error("Failed to send error SSE message.", e)
            } finally {
                this.complete()
            }
        }
    }

    fun registerEmitter(sessionId: String): SseEmitter? {
        if (!existSessionId(sessionId)) {
            return null
        }

        val emitter = SseEmitter(Const.EMITTER_TIMEOUT)

        // 注册回调：完成、超时、错误时移除
        emitter.onCompletion {
            emitters.remove(sessionId)
            logger.info("Emitter completed: $sessionId")
        }
        emitter.onTimeout {
            emitter.complete()
            emitters.remove(sessionId)
            logger.info("Emitter timed out: $sessionId")
        }
        emitter.onError { e ->
            logger.warn("Emitter error for $sessionId: ${e.message}")
            emitters.remove(sessionId)
        }

        emitters[sessionId] = emitter
        return emitter
    }

    fun pushMessage(sessionId: String, message: String): Boolean {
        val emitter = emitters[sessionId]
        return if (emitter != null) {
            try {
                // 使用 SseEmitter.send 推送数据。
                // SseEmitter.event() 用于格式化成标准的 SSE 格式 (data: ...)
                emitter.send(SseEmitter.event().data(message))
                true
            } catch (e: IOException) {
                // 客户端连接断开时，通常会抛出IOException
                println("Client connection broken for $sessionId. Removing emitter.")
                emitter.completeWithError(e)
                emitters.remove(sessionId)
                false
            }
        } else {
            false
        }
    }

    fun completeSession(sessionId: String) {
        emitters[sessionId]?.complete()
        // complete() 会触发 onCompletion 回调，自动移除
        redisTemplate.opsForValue().decrement(Const.VERIFY_CHAT_SESSION + sessionId)
    }
}