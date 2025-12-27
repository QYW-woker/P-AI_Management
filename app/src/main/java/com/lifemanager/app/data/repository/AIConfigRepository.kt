package com.lifemanager.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.lifemanager.app.core.ai.model.AIConfig
import com.lifemanager.app.core.ai.model.AIFeatureConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_config"
)

/**
 * AI配置仓库
 */
@Singleton
class AIConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.aiConfigDataStore

    // AI配置键
    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val ENABLED = booleanPreferencesKey("enabled")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = doublePreferencesKey("temperature")

        // 功能开关
        val VOICE_INPUT_ENABLED = booleanPreferencesKey("voice_input_enabled")
        val SMART_CLASSIFY_ENABLED = booleanPreferencesKey("smart_classify_enabled")
        val IMAGE_RECOGNITION_ENABLED = booleanPreferencesKey("image_recognition_enabled")
        val FLOATING_BALL_ENABLED = booleanPreferencesKey("floating_ball_enabled")
        val NOTIFICATION_LISTENER_ENABLED = booleanPreferencesKey("notification_listener_enabled")
        val AUTO_CONFIRM_ENABLED = booleanPreferencesKey("auto_confirm_enabled")
    }

    /**
     * 获取AI配置Flow
     */
    fun getConfigFlow(): Flow<AIConfig> = dataStore.data.map { prefs ->
        AIConfig(
            apiKey = prefs[Keys.API_KEY] ?: "",
            baseUrl = prefs[Keys.BASE_URL] ?: "https://api.deepseek.com/",
            model = prefs[Keys.MODEL] ?: "deepseek-chat",
            enabled = prefs[Keys.ENABLED] ?: false,
            maxTokens = prefs[Keys.MAX_TOKENS] ?: 500,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.3
        )
    }

    /**
     * 获取AI配置（挂起函数）
     */
    suspend fun getConfig(): AIConfig = getConfigFlow().first()

    /**
     * 同步获取配置（仅用于快速检查）
     */
    fun getConfigSync(): AIConfig = runBlocking { getConfig() }

    /**
     * 保存AI配置
     */
    suspend fun saveConfig(config: AIConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.BASE_URL] = config.baseUrl
            prefs[Keys.MODEL] = config.model
            prefs[Keys.ENABLED] = config.enabled
            prefs[Keys.MAX_TOKENS] = config.maxTokens
            prefs[Keys.TEMPERATURE] = config.temperature
        }
    }

    /**
     * 保存API Key
     */
    suspend fun saveApiKey(apiKey: String) {
        dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = apiKey
        }
    }

    /**
     * 获取功能配置Flow
     */
    fun getFeatureConfigFlow(): Flow<AIFeatureConfig> = dataStore.data.map { prefs ->
        AIFeatureConfig(
            voiceInputEnabled = prefs[Keys.VOICE_INPUT_ENABLED] ?: true,
            smartClassifyEnabled = prefs[Keys.SMART_CLASSIFY_ENABLED] ?: true,
            imageRecognitionEnabled = prefs[Keys.IMAGE_RECOGNITION_ENABLED] ?: true,
            floatingBallEnabled = prefs[Keys.FLOATING_BALL_ENABLED] ?: false,
            notificationListenerEnabled = prefs[Keys.NOTIFICATION_LISTENER_ENABLED] ?: false,
            autoConfirmEnabled = prefs[Keys.AUTO_CONFIRM_ENABLED] ?: false
        )
    }

    /**
     * 获取功能配置
     */
    suspend fun getFeatureConfig(): AIFeatureConfig = getFeatureConfigFlow().first()

    /**
     * 保存功能配置
     */
    suspend fun saveFeatureConfig(config: AIFeatureConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.VOICE_INPUT_ENABLED] = config.voiceInputEnabled
            prefs[Keys.SMART_CLASSIFY_ENABLED] = config.smartClassifyEnabled
            prefs[Keys.IMAGE_RECOGNITION_ENABLED] = config.imageRecognitionEnabled
            prefs[Keys.FLOATING_BALL_ENABLED] = config.floatingBallEnabled
            prefs[Keys.NOTIFICATION_LISTENER_ENABLED] = config.notificationListenerEnabled
            prefs[Keys.AUTO_CONFIRM_ENABLED] = config.autoConfirmEnabled
        }
    }

    /**
     * 设置悬浮球开关
     */
    suspend fun setFloatingBallEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.FLOATING_BALL_ENABLED] = enabled
        }
    }

    /**
     * 设置通知监听开关
     */
    suspend fun setNotificationListenerEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_LISTENER_ENABLED] = enabled
        }
    }

    /**
     * 清除所有配置
     */
    suspend fun clearConfig() {
        dataStore.edit { it.clear() }
    }
}
