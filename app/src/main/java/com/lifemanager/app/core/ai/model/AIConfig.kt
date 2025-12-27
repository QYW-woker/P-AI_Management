package com.lifemanager.app.core.ai.model

/**
 * AI服务配置
 */
data class AIConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/",
    val model: String = "deepseek-chat",
    val enabled: Boolean = false,
    val maxTokens: Int = 500,
    val temperature: Double = 0.3
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    companion object {
        val DEFAULT = AIConfig()
    }
}

/**
 * AI功能开关配置
 */
data class AIFeatureConfig(
    val voiceInputEnabled: Boolean = true,
    val smartClassifyEnabled: Boolean = true,
    val imageRecognitionEnabled: Boolean = true,
    val floatingBallEnabled: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val autoConfirmEnabled: Boolean = false  // 是否自动确认（不需要二次确认）
)

/**
 * 语音识别状态
 */
sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    object Listening : VoiceRecognitionState()
    object Processing : VoiceRecognitionState()
    data class Result(val text: String) : VoiceRecognitionState()
    data class Error(val message: String) : VoiceRecognitionState()
}

/**
 * AI处理状态
 */
sealed class AIProcessState {
    object Idle : AIProcessState()
    object Processing : AIProcessState()
    data class Success(val result: Any) : AIProcessState()
    data class Error(val message: String) : AIProcessState()
}
