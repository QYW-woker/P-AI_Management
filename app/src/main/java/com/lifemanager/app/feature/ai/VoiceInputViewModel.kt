package com.lifemanager.app.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.ExecutionResult
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.voice.CommandProcessState
import com.lifemanager.app.core.voice.VoiceCommandProcessor
import com.lifemanager.app.core.voice.VoiceRecognitionManager
import com.lifemanager.app.core.voice.VoiceRecognitionState
import com.lifemanager.app.data.repository.AIConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 语音输入ViewModel
 * 管理语音识别和命令处理的状态
 */
@HiltViewModel
class VoiceInputViewModel @Inject constructor(
    private val voiceRecognitionManager: VoiceRecognitionManager,
    private val voiceCommandProcessor: VoiceCommandProcessor,
    private val aiConfigRepository: AIConfigRepository
) : ViewModel() {

    // 语音识别状态
    val recognitionState: StateFlow<VoiceRecognitionState> = voiceRecognitionManager.state

    // 音量级别
    val volumeLevel: StateFlow<Float> = voiceRecognitionManager.volumeLevel

    // 命令处理状态
    val commandState: StateFlow<CommandProcessState> = voiceCommandProcessor.state

    // 语音识别是否可用
    val isVoiceAvailable: StateFlow<Boolean> = voiceRecognitionManager.isAvailable

    // 功能配置
    val featureConfig = aiConfigRepository.getFeatureConfigFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 可用分类列表（从外部注入）
    private val _categories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val categories: StateFlow<List<CustomFieldEntity>> = _categories.asStateFlow()

    // 最近的识别结果
    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    // 显示确认对话框
    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog: StateFlow<Boolean> = _showConfirmDialog.asStateFlow()

    // 待确认的命令意图
    private val _pendingIntent = MutableStateFlow<CommandIntent?>(null)
    val pendingIntent: StateFlow<CommandIntent?> = _pendingIntent.asStateFlow()

    // 待确认的描述
    private val _pendingDescription = MutableStateFlow<String?>(null)
    val pendingDescription: StateFlow<String?> = _pendingDescription.asStateFlow()

    // 执行结果消息
    private val _resultMessage = MutableStateFlow<Pair<String, Boolean>?>(null)
    val resultMessage: StateFlow<Pair<String, Boolean>?> = _resultMessage.asStateFlow()

    init {
        // 监听语音识别结果
        viewModelScope.launch {
            recognitionState.collect { state ->
                when (state) {
                    is VoiceRecognitionState.Result -> {
                        _lastRecognizedText.value = state.text
                        processRecognizedText(state.text)
                    }
                    else -> {}
                }
            }
        }

        // 监听命令处理状态
        viewModelScope.launch {
            commandState.collect { state ->
                when (state) {
                    is CommandProcessState.NeedConfirmation -> {
                        _pendingIntent.value = state.intent
                        _pendingDescription.value = state.description
                        _showConfirmDialog.value = true
                    }
                    is CommandProcessState.Error -> {
                        _resultMessage.value = Pair(state.message, false)
                    }
                    else -> {}
                }
            }
        }

        // 加载自动确认配置
        viewModelScope.launch {
            featureConfig.collect { config ->
                config?.let {
                    voiceCommandProcessor.setAutoConfirm(it.autoConfirmEnabled)
                }
            }
        }
    }

    /**
     * 设置可用分类列表
     */
    fun setCategories(categories: List<CustomFieldEntity>) {
        _categories.value = categories
    }

    /**
     * 开始语音识别
     */
    fun startListening() {
        voiceCommandProcessor.reset()
        _lastRecognizedText.value = null
        voiceRecognitionManager.startListening()
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        voiceRecognitionManager.stopListening()
    }

    /**
     * 取消语音识别
     */
    fun cancelListening() {
        voiceRecognitionManager.cancel()
        voiceCommandProcessor.reset()
        _lastRecognizedText.value = null
    }

    /**
     * 处理识别到的文本
     */
    private suspend fun processRecognizedText(text: String) {
        voiceCommandProcessor.processVoiceText(text, _categories.value)
    }

    /**
     * 手动处理文本输入
     */
    fun processTextInput(text: String) {
        viewModelScope.launch {
            _lastRecognizedText.value = text
            processRecognizedText(text)
        }
    }

    /**
     * 确认执行命令
     */
    fun confirmExecution(): CommandIntent? {
        _showConfirmDialog.value = false
        val intent = voiceCommandProcessor.confirmExecution()
        _pendingIntent.value = null
        _pendingDescription.value = null
        return intent
    }

    /**
     * 取消执行命令
     */
    fun cancelExecution() {
        _showConfirmDialog.value = false
        voiceCommandProcessor.cancelExecution()
        _pendingIntent.value = null
        _pendingDescription.value = null
    }

    /**
     * 标记命令执行完成
     */
    fun markExecuted(result: ExecutionResult) {
        voiceCommandProcessor.markExecuted(result)

        // 显示执行结果消息
        val message = when (result) {
            is ExecutionResult.Success -> result.message
            is ExecutionResult.Failure -> result.message
            is ExecutionResult.NotRecognized -> result.originalText
            is ExecutionResult.NeedConfirmation -> result.previewMessage
            is ExecutionResult.NeedMoreInfo -> result.prompt
        }

        _resultMessage.value = Pair(message, result is ExecutionResult.Success)
    }

    /**
     * 清除结果消息
     */
    fun clearResultMessage() {
        _resultMessage.value = null
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        voiceRecognitionManager.resetState()
        voiceCommandProcessor.reset()
        _lastRecognizedText.value = null
        _showConfirmDialog.value = false
        _pendingIntent.value = null
        _pendingDescription.value = null
        _resultMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognitionManager.destroy()
    }
}
