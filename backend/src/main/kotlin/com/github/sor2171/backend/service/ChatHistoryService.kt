package com.github.sor2171.backend.service

import com.baomidou.mybatisplus.extension.service.IService
import com.github.sor2171.backend.entity.dto.ChatHistory
import com.github.sor2171.backend.entity.vo.request.ChatHistoryRequestVO
import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import jakarta.servlet.http.HttpServletRequest
import reactor.core.publisher.Flux

interface ChatHistoryService : IService<ChatHistory> {
    fun getChatHistory(vo: ChatHistoryRequestVO, request: HttpServletRequest): String
    fun getNewChatRequest(vo: ChatRequestVO, request: HttpServletRequest): String
    fun sendRequestAndHandleStream(vo: ChatRequestVO): Flux<String>
}