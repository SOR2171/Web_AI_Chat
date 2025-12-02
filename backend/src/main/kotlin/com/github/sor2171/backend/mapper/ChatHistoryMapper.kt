package com.github.sor2171.backend.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.github.sor2171.backend.entity.dto.ChatHistory
import org.apache.ibatis.annotations.Mapper

@Mapper
interface ChatHistoryMapper : BaseMapper<ChatHistory>