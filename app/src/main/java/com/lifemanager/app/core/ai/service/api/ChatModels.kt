package com.lifemanager.app.core.ai.service.api

import com.google.gson.annotations.SerializedName

/**
 * DeepSeek Chat API 请求模型
 */
data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
    @SerializedName("max_tokens")
    val maxTokens: Int = 500,
    val stream: Boolean = false
)

/**
 * 聊天消息
 */
data class ChatMessage(
    val role: String,      // system, user, assistant
    val content: String
)

/**
 * DeepSeek Chat API 响应模型
 */
data class ChatResponse(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<Choice>?,
    val usage: Usage?
)

/**
 * 选择项
 */
data class Choice(
    val index: Int?,
    val message: ChatMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

/**
 * Token使用情况
 */
data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)

/**
 * API错误响应
 */
data class ApiError(
    val error: ErrorDetail?
)

data class ErrorDetail(
    val message: String?,
    val type: String?,
    val code: String?
)
