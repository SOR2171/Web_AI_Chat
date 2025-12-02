package com.github.sor2171.backend.controller

import com.github.sor2171.backend.entity.vo.request.ChatRequestVO
import com.github.sor2171.backend.service.ChatHistoryService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/st")
class StChatController(
    private val chatService: ChatHistoryService
) {
    @PostMapping("/chat")
    fun chatRequest(
        @RequestBody @Valid vo: ChatRequestVO,
        request: HttpServletRequest
    ): String {
        val token = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7)
        return chatService.getNewChatRequest(vo, request.remoteAddr, token)
    }
}