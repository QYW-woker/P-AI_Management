package com.lifemanager.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.data.repository.AppSettings
import com.lifemanager.app.core.data.repository.SettingsRepository
import com.lifemanager.app.core.database.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 设置ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 设置状态
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // UI状态
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // 显示时间选择器
    private val _showTimePicker = MutableStateFlow(false)
    val showTimePicker: StateFlow<Boolean> = _showTimePicker.asStateFlow()

    // 显示语言选择
    private val _showLanguagePicker = MutableStateFlow(false)
    val showLanguagePicker: StateFlow<Boolean> = _showLanguagePicker.asStateFlow()

    // 显示清除数据确认
    private val _showClearDataDialog = MutableStateFlow(false)
    val showClearDataDialog: StateFlow<Boolean> = _showClearDataDialog.asStateFlow()

    // 显示备份成功对话框
    private val _showBackupSuccessDialog = MutableStateFlow<String?>(null)
    val showBackupSuccessDialog: StateFlow<String?> = _showBackupSuccessDialog.asStateFlow()

    /**
     * 切换深色模式
     */
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    /**
     * 切换通知开关
     */
    fun toggleNotification(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationEnabled(enabled)
        }
    }

    /**
     * 切换自动备份
     */
    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackup(enabled)
        }
    }

    /**
     * 显示时间选择器
     */
    fun showTimePickerDialog() {
        _showTimePicker.value = true
    }

    fun hideTimePickerDialog() {
        _showTimePicker.value = false
    }

    /**
     * 设置提醒时间
     */
    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(time)
            hideTimePickerDialog()
        }
    }

    /**
     * 显示语言选择
     */
    fun showLanguagePickerDialog() {
        _showLanguagePicker.value = true
    }

    fun hideLanguagePickerDialog() {
        _showLanguagePicker.value = false
    }

    /**
     * 设置语言
     */
    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            hideLanguagePickerDialog()
            _uiState.value = SettingsUiState.Success("语言已更改，重启应用后生效")
        }
    }

    /**
     * 显示清除数据确认
     */
    fun showClearDataConfirmation() {
        _showClearDataDialog.value = true
    }

    fun hideClearDataConfirmation() {
        _showClearDataDialog.value = false
    }

    /**
     * 立即备份
     */
    fun backupNow() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("正在备份...")
            try {
                val backupPath = withContext(Dispatchers.IO) {
                    performBackup()
                }
                _uiState.value = SettingsUiState.Idle
                _showBackupSuccessDialog.value = backupPath
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("备份失败: ${e.message}")
            }
        }
    }

    /**
     * 执行备份
     */
    private fun performBackup(): String {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val backupFile = File(backupDir, "backup_$timestamp.db")

        // 复制数据库文件
        dbFile.copyTo(backupFile, overwrite = true)

        return backupFile.absolutePath
    }

    /**
     * 关闭备份成功对话框
     */
    fun hideBackupSuccessDialog() {
        _showBackupSuccessDialog.value = null
    }

    /**
     * 恢复数据
     */
    fun restoreData() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("正在查找备份...")
            try {
                val result = withContext(Dispatchers.IO) {
                    performRestore()
                }
                if (result) {
                    _uiState.value = SettingsUiState.Success("数据恢复成功，请重启应用")
                } else {
                    _uiState.value = SettingsUiState.Error("未找到备份文件")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("恢复失败: ${e.message}")
            }
        }
    }

    /**
     * 执行恢复
     */
    private fun performRestore(): Boolean {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) {
            return false
        }

        // 找到最新的备份文件
        val backupFiles = backupDir.listFiles { file ->
            file.name.startsWith("backup_") && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() }

        val latestBackup = backupFiles?.firstOrNull() ?: return false

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)

        // 恢复数据库文件
        latestBackup.copyTo(dbFile, overwrite = true)

        return true
    }

    /**
     * 清除所有数据
     */
    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("正在清除数据...")
            try {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }
                hideClearDataConfirmation()
                _uiState.value = SettingsUiState.Success("所有数据已清除")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("清除失败: ${e.message}")
            }
        }
    }

    /**
     * 清除UI状态
     */
    fun clearUiState() {
        _uiState.value = SettingsUiState.Idle
    }
}

/**
 * 设置UI状态
 */
sealed class SettingsUiState {
    object Idle : SettingsUiState()
    data class Loading(val message: String) : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
