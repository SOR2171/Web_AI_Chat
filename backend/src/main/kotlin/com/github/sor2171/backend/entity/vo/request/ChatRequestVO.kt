package com.github.sor2171.backend.entity.vo.request

import com.github.sor2171.backend.entity.DataCopy

data class ChatRequestVO(
    val modelId: Int,
    val character: Int,
    val input: String,
    var uuid: String
) : DataCopy