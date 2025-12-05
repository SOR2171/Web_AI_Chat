package com.github.sor2171.backend.entity.dto

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import lombok.AllArgsConstructor
import java.util.Date

@TableName("chat_history")
@AllArgsConstructor
data class ChatHistory(
    @TableId(type = IdType.AUTO)
    var id: Int? = null,
    val userId: Int = 0,
    val characterId: Int = 0,
    val modelId: Int = 0,
    val input: String = "",
    var output: String? = null,
    val begin: Date = Date(),
    var finish: Date? = null,
)
            