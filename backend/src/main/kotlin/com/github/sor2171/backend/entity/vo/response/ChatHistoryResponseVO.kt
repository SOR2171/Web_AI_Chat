package com.github.sor2171.backend.entity.vo.response

import com.github.sor2171.backend.entity.DataCopy

data class ChatHistoryResponseVO(
    val role: String = "",
    val content: String = "",
    val id: String? = ""
): DataCopy