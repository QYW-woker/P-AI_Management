package com.lifemanager.app.core.ai.service.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * DeepSeek API 接口
 */
interface DeepSeekApi {

    /**
     * 发送聊天请求
     */
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}
