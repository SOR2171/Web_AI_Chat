package com.github.sor2171.backend.entity.bo

data class StOutput(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val system_fingerprint: String?,
    val choices: List<Choice>
) {
    data class Choice(
        val index: Int,
        val delta: Delta,
        val finish_reason: String?
    )

    data class Delta(
        val role: String?,
        val content: String?,
        val reasoning: String? = null
    )
}