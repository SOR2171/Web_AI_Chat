package com.github.sor2171.backend.entity.vo.response

import java.util.Date

data class AuthorizeVO(
    val username: String = "",
    val role: String = "",
    val token: String = "",
    val expire: Date = Date(),
)