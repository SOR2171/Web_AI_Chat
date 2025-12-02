package com.github.sor2171.backend.service

import com.baomidou.mybatisplus.extension.service.IService
import com.github.sor2171.backend.entity.dto.ChatHistory
import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.entity.vo.response.ChatStreamVO
import reactor.core.publisher.Flux

interface ChatHistoryService : IService<ChatHistory> {
    fun getNewChatRequest(vo: ChatRequestVO, ip: String, token: String): String
    fun sendRequestAndHandleStream(vo: ChatRequestVO): Flux<String>
}