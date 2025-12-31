package com.lifemanager.app.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI设置ViewModel
 */
@HiltViewModel
class AISettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "ai_settings"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_PERSONALITY = "personality"
        private const val KEY_VOICE_STYLE = "voice_style"
        private const val KEY_FLOATING_BALL_ENABLED = "floating_ball_enabled"
        private const val KEY_AUTO_HIDE_FLOATING_BALL = "auto_hide_floating_ball"
        private const val KEY_MOOD_SYNC = "mood_sync"
        private const val KEY_FLOATING_BALL_OPACITY = "floating_ball_opacity"
        private const val KEY_VOICE_ACCOUNTING_ENABLED = "voice_accounting_enabled"
        private const val KEY_VOICE_FEEDBACK = "voice_feedback"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_EXPENSE_ANALYSIS = "expense_analysis"
        private const val KEY_BUDGET_WARNING = "budget_warning"
        private const val KEY_SAVING_TIPS = "saving_tips"
        private const val KEY_EMOTION_INSIGHT = "emotion_insight"
        private const val KEY_SCREENSHOT_RECOGNITION = "screenshot_recognition"
        private const val KEY_INVOICE_RECOGNITION = "invoice_recognition"
        private const val KEY_BANK_CARD_RECOGNITION = "bank_card_recognition"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CLOUD_AI = "cloud_ai"
        private const val KEY_SAVE_HISTORY = "save_history"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AISettings> = _settings.asStateFlow()

    // 对话框状态
    private val _showVoiceSelector = MutableStateFlow(false)
    val showVoiceSelector: StateFlow<Boolean> = _showVoiceSelector.asStateFlow()

    private val _showPersonalitySelector = MutableStateFlow(false)
    val showPersonalitySelector: StateFlow<Boolean> = _showPersonalitySelector.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private fun loadSettings(): AISettings {
        return AISettings(
            aiEnabled = prefs.getBoolean(KEY_AI_ENABLED, true),
            personality = AIPersonality.entries.find {
                it.name == prefs.getString(KEY_PERSONALITY, AIPersonality.FRIENDLY.name)
            } ?: AIPersonality.FRIENDLY,
            voiceStyle = VoiceStyle.entries.find {
                it.name == prefs.getString(KEY_VOICE_STYLE, VoiceStyle.SWEET.name)
            } ?: VoiceStyle.SWEET,
            floatingBallEnabled = prefs.getBoolean(KEY_FLOATING_BALL_ENABLED, true),
            autoHideFloatingBall = prefs.getBoolean(KEY_AUTO_HIDE_FLOATING_BALL, false),
            moodSync = prefs.getBoolean(KEY_MOOD_SYNC, true),
            floatingBallOpacity = prefs.getFloat(KEY_FLOATING_BALL_OPACITY, 1f),
            voiceAccountingEnabled = prefs.getBoolean(KEY_VOICE_ACCOUNTING_ENABLED, true),
            voiceFeedback = prefs.getBoolean(KEY_VOICE_FEEDBACK, true),
            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false),
            expenseAnalysis = prefs.getBoolean(KEY_EXPENSE_ANALYSIS, true),
            budgetWarning = prefs.getBoolean(KEY_BUDGET_WARNING, true),
            savingTips = prefs.getBoolean(KEY_SAVING_TIPS, true),
            emotionInsight = prefs.getBoolean(KEY_EMOTION_INSIGHT, true),
            screenshotRecognition = prefs.getBoolean(KEY_SCREENSHOT_RECOGNITION, true),
            invoiceRecognition = prefs.getBoolean(KEY_INVOICE_RECOGNITION, true),
            bankCardRecognition = prefs.getBoolean(KEY_BANK_CARD_RECOGNITION, true),
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            cloudAI = prefs.getBoolean(KEY_CLOUD_AI, false),
            saveHistory = prefs.getBoolean(KEY_SAVE_HISTORY, true)
        )
    }

    private fun saveAndUpdate(update: (AISettings) -> AISettings) {
        val newSettings = update(_settings.value)
        _settings.value = newSettings
    }

    // AI助手设置
    fun setAIEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
        saveAndUpdate { it.copy(aiEnabled = enabled) }
    }

    fun setPersonality(personality: AIPersonality) {
        prefs.edit().putString(KEY_PERSONALITY, personality.name).apply()
        saveAndUpdate { it.copy(personality = personality) }
        hidePersonalitySelectorDialog()
    }

    fun setVoiceStyle(style: VoiceStyle) {
        prefs.edit().putString(KEY_VOICE_STYLE, style.name).apply()
        saveAndUpdate { it.copy(voiceStyle = style) }
        hideVoiceSelectorDialog()
    }

    // 悬浮球设置
    fun setFloatingBallEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLOATING_BALL_ENABLED, enabled).apply()
        saveAndUpdate { it.copy(floatingBallEnabled = enabled) }
    }

    fun setAutoHideFloatingBall(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_HIDE_FLOATING_BALL, enabled).apply()
        saveAndUpdate { it.copy(autoHideFloatingBall = enabled) }
    }

    fun setMoodSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOOD_SYNC, enabled).apply()
        saveAndUpdate { it.copy(moodSync = enabled) }
    }

    fun setFloatingBallOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_FLOATING_BALL_OPACITY, opacity).apply()
        saveAndUpdate { it.copy(floatingBallOpacity = opacity) }
    }

    // 语音识别设置
    fun setVoiceAccountingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_ACCOUNTING_ENABLED, enabled).apply()
        saveAndUpdate { it.copy(voiceAccountingEnabled = enabled) }
    }

    fun setVoiceFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_FEEDBACK, enabled).apply()
        saveAndUpdate { it.copy(voiceFeedback = enabled) }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        saveAndUpdate { it.copy(wakeWordEnabled = enabled) }
    }

    // 智能分析设置
    fun setExpenseAnalysis(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EXPENSE_ANALYSIS, enabled).apply()
        saveAndUpdate { it.copy(expenseAnalysis = enabled) }
    }

    fun setBudgetWarning(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUDGET_WARNING, enabled).apply()
        saveAndUpdate { it.copy(budgetWarning = enabled) }
    }

    fun setSavingTips(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVING_TIPS, enabled).apply()
        saveAndUpdate { it.copy(savingTips = enabled) }
    }

    fun setEmotionInsight(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMOTION_INSIGHT, enabled).apply()
        saveAndUpdate { it.copy(emotionInsight = enabled) }
    }

    // 图像识别设置
    fun setScreenshotRecognition(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_RECOGNITION, enabled).apply()
        saveAndUpdate { it.copy(screenshotRecognition = enabled) }
    }

    fun setInvoiceRecognition(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INVOICE_RECOGNITION, enabled).apply()
        saveAndUpdate { it.copy(invoiceRecognition = enabled) }
    }

    fun setBankCardRecognition(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BANK_CARD_RECOGNITION, enabled).apply()
        saveAndUpdate { it.copy(bankCardRecognition = enabled) }
    }

    // 高级设置
    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
        saveAndUpdate { it.copy(apiKey = key) }
    }

    fun setCloudAI(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_AI, enabled).apply()
        saveAndUpdate { it.copy(cloudAI = enabled) }
    }

    fun setSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
        saveAndUpdate { it.copy(saveHistory = enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            // TODO: 清除对话历史
        }
    }

    // 对话框控制
    fun showVoiceSelectorDialog() {
        _showVoiceSelector.value = true
    }

    fun hideVoiceSelectorDialog() {
        _showVoiceSelector.value = false
    }

    fun showPersonalitySelectorDialog() {
        _showPersonalitySelector.value = true
    }

    fun hidePersonalitySelectorDialog() {
        _showPersonalitySelector.value = false
    }

    fun showApiKeyDialog() {
        _showApiKeyDialog.value = true
    }

    fun hideApiKeyDialog() {
        _showApiKeyDialog.value = false
    }
}

/**
 * AI设置数据类
 */
data class AISettings(
    val aiEnabled: Boolean = true,
    val personality: AIPersonality = AIPersonality.FRIENDLY,
    val voiceStyle: VoiceStyle = VoiceStyle.SWEET,
    val floatingBallEnabled: Boolean = true,
    val autoHideFloatingBall: Boolean = false,
    val moodSync: Boolean = true,
    val floatingBallOpacity: Float = 1f,
    val voiceAccountingEnabled: Boolean = true,
    val voiceFeedback: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val expenseAnalysis: Boolean = true,
    val budgetWarning: Boolean = true,
    val savingTips: Boolean = true,
    val emotionInsight: Boolean = true,
    val screenshotRecognition: Boolean = true,
    val invoiceRecognition: Boolean = true,
    val bankCardRecognition: Boolean = true,
    val apiKey: String = "",
    val cloudAI: Boolean = false,
    val saveHistory: Boolean = true
) {
    val hasApiKey: Boolean get() = apiKey.isNotBlank()
}
