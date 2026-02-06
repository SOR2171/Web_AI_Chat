package com.github.sor2171.backend.entity.vo.request

import com.github.sor2171.backend.entity.DataCopy
import com.github.sor2171.backend.entity.bo.StMessage

data class ChatRequestVO(
    val modelId: Int,
    val characterId: Int,
    val messages: List<StMessage>,
    var uuid: String
) : DataCopy