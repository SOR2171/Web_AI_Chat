package com.github.sor2171.backend.service

import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Service
class SseEmitterBrokerService {
    // 存储 Session ID -> SseEmitter 映射
    private val emitters: MutableMap<String, SseEmitter> = ConcurrentHashMap()

    // 设置超时时间，通常需要长一些以容纳聊天时间 (例如 30分钟)
    private val EMITTER_TIMEOUT: Long = 30 * 60 * 1000L

    fun registerEmitter(sessionId: String): SseEmitter {
        val emitter = SseEmitter(EMITTER_TIMEOUT)

        // 注册回调：完成、超时、错误时移除
        emitter.onCompletion {
            emitters.remove(sessionId)
            println("Emitter completed: $sessionId")
        }
        emitter.onTimeout {
            emitter.complete()
            emitters.remove(sessionId)
            println("Emitter timed out: $sessionId")
        }
        emitter.onError { e ->
            println("Emitter error for $sessionId: ${e.message}")
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
    }
}