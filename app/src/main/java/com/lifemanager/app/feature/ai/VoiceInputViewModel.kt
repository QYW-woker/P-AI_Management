package com.lifemanager.app.feature.ai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.ExecutionResult
import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.core.ai.model.TransactionType
import com.lifemanager.app.core.ai.service.AIService
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.core.database.entity.TransactionSource
import com.lifemanager.app.core.floatingball.FloatingBallManager
import com.lifemanager.app.core.ocr.OcrManager
import com.lifemanager.app.core.voice.CommandProcessState
import com.lifemanager.app.core.voice.VoiceCommandExecutor
import com.lifemanager.app.core.voice.VoiceCommandProcessor
import com.lifemanager.app.core.voice.VoiceRecognitionManager
import com.lifemanager.app.core.voice.VoiceRecognitionState
import com.lifemanager.app.data.repository.AIConfigRepository
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 语音输入ViewModel
 * 管理语音识别和命令处理的状态
 */
@HiltViewModel
class VoiceInputViewModel @Inject constructor(
    private val voiceRecognitionManager: VoiceRecognitionManager,
    private val voiceCommandProcessor: VoiceCommandProcessor,
    private val voiceCommandExecutor: VoiceCommandExecutor,
    private val aiConfigRepository: AIConfigRepository,
    private val aiService: AIService,
    private val transactionRepository: DailyTransactionRepository,
    private val floatingBallManager: FloatingBallManager,
    private val ocrManager: OcrManager
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
                    is CommandProcessState.Parsed -> {
                        // 自动确认模式下，直接执行命令
                        executeIntent(state.intent)
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

        // 实际执行命令
        intent?.let { executeIntent(it) }

        return intent
    }

    /**
     * 执行命令意图
     */
    private fun executeIntent(intent: CommandIntent) {
        viewModelScope.launch {
            try {
                val result = voiceCommandExecutor.execute(intent)
                markExecuted(result)
            } catch (e: Exception) {
                markExecuted(ExecutionResult.Failure(e.message ?: "执行失败"))
            }
        }
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

    /**
     * 处理图片进行识别 - 使用ML Kit OCR + AI解析
     */
    fun processImageForRecognition(context: Context, uri: Uri, onResult: (PaymentInfo?) -> Unit) {
        viewModelScope.launch {
            try {
                _resultMessage.value = Pair("正在识别图片文字...", true)

                // 使用ML Kit OCR识别图片中的文字
                val ocrResult = ocrManager.recognizeFromUri(uri)

                ocrResult.fold(
                    onSuccess = { result ->
                        val ocrText = result.rawText
                        if (ocrText.isBlank()) {
                            _resultMessage.value = Pair("未能识别到图片中的文字", false)
                            onResult(null)
                            return@fold
                        }

                        _resultMessage.value = Pair("正在分析支付信息...", true)

                        // 使用AI解析OCR文本
                        val parseResult = aiService.parsePaymentScreenshot(ocrText)
                        parseResult.fold(
                            onSuccess = { paymentInfo ->
                                if (paymentInfo.amount > 0) {
                                    _resultMessage.value = Pair("识别成功！", true)
                                    onResult(paymentInfo)
                                } else {
                                    _resultMessage.value = Pair("未能识别到有效的支付信息", false)
                                    onResult(null)
                                }
                            },
                            onFailure = { e ->
                                _resultMessage.value = Pair("解析支付信息失败: ${e.message}", false)
                                onResult(null)
                            }
                        )
                    },
                    onFailure = { e ->
                        _resultMessage.value = Pair("图片文字识别失败: ${e.message}", false)
                        onResult(null)
                    }
                )
            } catch (e: Exception) {
                _resultMessage.value = Pair("图片识别失败: ${e.message}", false)
                onResult(null)
            }
        }
    }

    /**
     * 确认支付记录
     */
    fun confirmPaymentRecord(payment: PaymentInfo) {
        viewModelScope.launch {
            try {
                val date = LocalDate.now()
                val now = LocalTime.now()

                val entity = DailyTransactionEntity(
                    id = 0,
                    type = if (payment.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME",
                    amount = payment.amount,
                    categoryId = null,
                    date = date.toEpochDay().toInt(),
                    time = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    note = payment.payee ?: "图片识别记账",
                    source = TransactionSource.SCREENSHOT
                )

                transactionRepository.insert(entity)

                val typeStr = if (payment.type == TransactionType.EXPENSE) "支出" else "收入"
                _resultMessage.value = Pair(
                    "已记录${typeStr}: ${payment.payee ?: ""}，金额 ¥${String.format("%.2f", payment.amount)}",
                    true
                )
            } catch (e: Exception) {
                _resultMessage.value = Pair("记录失败: ${e.message}", false)
            }
        }
    }

    /**
     * 切换悬浮球状态
     */
    fun toggleFloatingBall(context: Context) {
        viewModelScope.launch {
            val currentConfig = featureConfig.value
            val isEnabled = currentConfig?.floatingBallEnabled ?: false

            if (!isEnabled) {
                // 检查悬浮窗权限
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    _resultMessage.value = Pair("请授予悬浮窗权限", false)
                    return@launch
                }

                val success = floatingBallManager.enable()
                if (success) {
                    aiConfigRepository.setFloatingBallEnabled(true)
                    _resultMessage.value = Pair("悬浮球已开启", true)
                } else {
                    _resultMessage.value = Pair("悬浮球启动失败", false)
                }
            } else {
                floatingBallManager.disable()
                aiConfigRepository.setFloatingBallEnabled(false)
                _resultMessage.value = Pair("悬浮球已关闭", true)
            }
        }
    }
}
