package com.lifemanager.app.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.ai.model.AIConfig
import com.lifemanager.app.core.ai.model.AIFeatureConfig
import com.lifemanager.app.core.ai.service.AIService
import com.lifemanager.app.data.repository.AIConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI设置ViewModel
 */
@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val configRepository: AIConfigRepository,
    private val aiService: AIService
) : ViewModel() {

    // AI配置
    val aiConfig: StateFlow<AIConfig> = configRepository.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AIConfig.DEFAULT)

    // 功能配置
    val featureConfig: StateFlow<AIFeatureConfig> = configRepository.getFeatureConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AIFeatureConfig())

    // UI状态
    private val _uiState = MutableStateFlow<AISettingsUiState>(AISettingsUiState.Idle)
    val uiState: StateFlow<AISettingsUiState> = _uiState.asStateFlow()

    // 编辑中的API Key
    private val _editingApiKey = MutableStateFlow("")
    val editingApiKey: StateFlow<String> = _editingApiKey.asStateFlow()

    // 显示API Key输入对话框
    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    // 测试连接结果
    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _editingApiKey.value = configRepository.getConfig().apiKey
        }
    }

    /**
     * 显示API Key输入对话框
     */
    fun showApiKeyDialog() {
        viewModelScope.launch {
            _editingApiKey.value = configRepository.getConfig().apiKey
            _showApiKeyDialog.value = true
        }
    }

    /**
     * 隐藏API Key输入对话框
     */
    fun hideApiKeyDialog() {
        _showApiKeyDialog.value = false
        _testResult.value = null
    }

    /**
     * 更新编辑中的API Key
     */
    fun updateApiKey(key: String) {
        _editingApiKey.value = key
    }

    /**
     * 保存API Key
     */
    fun saveApiKey() {
        viewModelScope.launch {
            val key = _editingApiKey.value.trim()
            configRepository.saveApiKey(key)
            _showApiKeyDialog.value = false
            _uiState.value = AISettingsUiState.Success("API Key已保存")
        }
    }

    /**
     * 测试API连接
     */
    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = AISettingsUiState.Testing
            _testResult.value = "正在测试连接..."

            // 临时保存以测试
            val key = _editingApiKey.value.trim()
            if (key.isBlank()) {
                _testResult.value = "请输入API Key"
                _uiState.value = AISettingsUiState.Idle
                return@launch
            }

            // 先保存再测试
            configRepository.saveApiKey(key)

            val result = aiService.testConnection()
            result.fold(
                onSuccess = { message ->
                    _testResult.value = message
                    _uiState.value = AISettingsUiState.Success("连接成功")
                },
                onFailure = { error ->
                    _testResult.value = "连接失败: ${error.message}"
                    _uiState.value = AISettingsUiState.Error(error.message ?: "连接失败")
                }
            )
        }
    }

    /**
     * 设置悬浮球开关
     */
    fun setFloatingBallEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setFloatingBallEnabled(enabled)
        }
    }

    /**
     * 设置通知监听开关
     */
    fun setNotificationListenerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setNotificationListenerEnabled(enabled)
        }
    }

    /**
     * 更新功能配置
     */
    fun updateFeatureConfig(config: AIFeatureConfig) {
        viewModelScope.launch {
            configRepository.saveFeatureConfig(config)
        }
    }

    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.value = AISettingsUiState.Idle
        _testResult.value = null
    }
}

/**
 * AI设置UI状态
 */
sealed class AISettingsUiState {
    object Idle : AISettingsUiState()
    object Testing : AISettingsUiState()
    data class Success(val message: String) : AISettingsUiState()
    data class Error(val message: String) : AISettingsUiState()
}
