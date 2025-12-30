package com.lifemanager.app.core.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音识别状态
 */
sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    object Listening : VoiceRecognitionState()
    object Processing : VoiceRecognitionState()
    data class Result(val text: String, val confidence: Float) : VoiceRecognitionState()
    data class PartialResult(val text: String) : VoiceRecognitionState()
    data class Error(val code: Int, val message: String) : VoiceRecognitionState()
}

/**
 * 语音识别管理器
 * 封装Android SpeechRecognizer，提供简洁的语音识别API
 */
@Singleton
class VoiceRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    // 默认设为true，尝试启动时才判断
    private val _isAvailable = MutableStateFlow(true)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    init {
        // 使用更宽松的检查方式
        checkAvailability()
    }

    /**
     * 检查语音识别是否可用
     * 使用多种方式检查，避免误判
     */
    fun checkAvailability(): Boolean {
        // 方式1: 系统API检查
        val systemAvailable = SpeechRecognizer.isRecognitionAvailable(context)

        // 方式2: 检查是否有可响应语音识别Intent的应用
        val intentAvailable = checkVoiceRecognitionIntent()

        // 任一方式可用即认为可用
        val available = systemAvailable || intentAvailable
        _isAvailable.value = available
        return available
    }

    /**
     * 检查是否有应用可以处理语音识别Intent
     */
    private fun checkVoiceRecognitionIntent(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val activities = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return activities.isNotEmpty()
    }

    /**
     * 初始化语音识别器
     */
    private fun initRecognizer(): Boolean {
        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)?.apply {
                    setRecognitionListener(createRecognitionListener())
                }
                return speechRecognizer != null
            } catch (e: Exception) {
                return false
            }
        }
        return true
    }

    /**
     * 开始语音识别
     */
    fun startListening() {
        // 尝试初始化，即使checkAvailability返回false也尝试
        if (!initRecognizer()) {
            _isAvailable.value = false
            _state.value = VoiceRecognitionState.Error(
                code = -1,
                message = "语音识别服务初始化失败，请检查是否安装了语音输入法"
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // 设置更长的静音超时时间
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }

        try {
            speechRecognizer?.startListening(intent)
            _state.value = VoiceRecognitionState.Listening
        } catch (e: Exception) {
            _isAvailable.value = false
            _state.value = VoiceRecognitionState.Error(
                code = -2,
                message = "启动语音识别失败: ${e.message}"
            )
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // 忽略停止时的异常
        }
    }

    /**
     * 取消语音识别
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _state.value = VoiceRecognitionState.Idle
        } catch (e: Exception) {
            // 忽略取消时的异常
        }
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _state.value = VoiceRecognitionState.Idle
        _volumeLevel.value = 0f
    }

    /**
     * 释放资源
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            _state.value = VoiceRecognitionState.Idle
        } catch (e: Exception) {
            // 忽略销毁时的异常
        }
    }

    /**
     * 创建识别监听器
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                // 用户开始说话
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 音量变化，用于显示动画
                // rmsdB范围通常是-2到12
                val normalizedLevel = ((rmsdB + 2) / 14f).coerceIn(0f, 1f)
                _volumeLevel.value = normalizedLevel
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 接收到语音数据
            }

            override fun onEndOfSpeech() {
                _state.value = VoiceRecognitionState.Processing
                _volumeLevel.value = 0f
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音输入超时"
                    else -> "未知错误 ($error)"
                }
                _state.value = VoiceRecognitionState.Error(error, errorMessage)
                _volumeLevel.value = 0f
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val confidence = confidences?.getOrNull(0) ?: 0.8f
                    _state.value = VoiceRecognitionState.Result(text, confidence)
                } else {
                    _state.value = VoiceRecognitionState.Error(
                        SpeechRecognizer.ERROR_NO_MATCH,
                        "未识别到语音内容"
                    )
                }
                _volumeLevel.value = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _state.value = VoiceRecognitionState.PartialResult(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 其他事件
            }
        }
    }

    companion object {
        /**
         * 获取错误描述
         */
        fun getErrorDescription(errorCode: Int): String {
            return when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> "音频录制错误，请检查麦克风"
                SpeechRecognizer.ERROR_CLIENT -> "客户端错误，请重试"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "请授予录音权限"
                SpeechRecognizer.ERROR_NETWORK -> "网络错误，请检查网络连接"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时，请重试"
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音，请重新说话"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙，请稍后重试"
                SpeechRecognizer.ERROR_SERVER -> "服务器错误，请稍后重试"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音输入"
                else -> "发生错误，请重试"
            }
        }
    }
}
