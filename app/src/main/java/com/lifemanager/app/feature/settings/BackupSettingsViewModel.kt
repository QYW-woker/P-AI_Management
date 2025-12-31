package com.lifemanager.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.backup.BackupInfo
import com.lifemanager.app.core.backup.BackupState
import com.lifemanager.app.core.backup.CloudBackupManager
import com.lifemanager.app.core.backup.CloudProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 备份设置ViewModel
 */
@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val backupManager: CloudBackupManager
) : ViewModel() {

    // 从BackupManager获取状态
    val backupState: StateFlow<BackupState> = backupManager.backupState
    val autoBackupEnabled: StateFlow<Boolean> = backupManager.autoBackupEnabled
    val backupInterval: StateFlow<Int> = backupManager.backupInterval
    val currentProvider: StateFlow<CloudProvider> = backupManager.currentProvider
    val baiduConnected: StateFlow<Boolean> = backupManager.baiduConnected
    val aliyunConnected: StateFlow<Boolean> = backupManager.aliyunConnected
    val localBackups: StateFlow<List<BackupInfo>> = backupManager.localBackups
    val cloudBackups: StateFlow<List<BackupInfo>> = backupManager.cloudBackups

    // 最后备份时间
    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    // 对话框状态
    private val _showIntervalPicker = MutableStateFlow(false)
    val showIntervalPicker: StateFlow<Boolean> = _showIntervalPicker.asStateFlow()

    private val _showProviderPicker = MutableStateFlow(false)
    val showProviderPicker: StateFlow<Boolean> = _showProviderPicker.asStateFlow()

    private val _showRestoreDialog = MutableStateFlow(false)
    val showRestoreDialog: StateFlow<Boolean> = _showRestoreDialog.asStateFlow()

    private val _showCloudConnectDialog = MutableStateFlow(false)
    val showCloudConnectDialog: StateFlow<Boolean> = _showCloudConnectDialog.asStateFlow()

    private val _connectingProvider = MutableStateFlow<CloudProvider?>(null)
    val connectingProvider: StateFlow<CloudProvider?> = _connectingProvider.asStateFlow()

    var isRestoringFromCloud: Boolean = false
        private set

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _lastBackupTime.value = backupManager.getLastBackupTime()
            backupManager.refreshLocalBackups()
            backupManager.refreshCloudBackups()
        }
    }

    // 设置方法
    fun setAutoBackupEnabled(enabled: Boolean) {
        backupManager.setAutoBackupEnabled(enabled)
    }

    fun setBackupInterval(intervalHours: Int) {
        backupManager.setBackupInterval(intervalHours)
        hideIntervalPickerDialog()
    }

    fun setCloudProvider(provider: CloudProvider) {
        backupManager.setCloudProvider(provider)
        hideProviderPickerDialog()
    }

    // 备份操作
    fun backupNow() {
        viewModelScope.launch {
            backupManager.backupToLocal()
            _lastBackupTime.value = backupManager.getLastBackupTime()
        }
    }

    fun backupToCloud() {
        viewModelScope.launch {
            backupManager.backupToCloud()
            _lastBackupTime.value = backupManager.getLastBackupTime()
        }
    }

    // 恢复操作
    fun restoreFromBackup(backup: BackupInfo) {
        viewModelScope.launch {
            hideRestoreDialog()
            if (backup.provider == CloudProvider.LOCAL) {
                backupManager.restoreFromLocal(backup)
            } else {
                backupManager.restoreFromCloud(backup)
            }
        }
    }

    // 删除备份
    fun deleteBackup(backup: BackupInfo) {
        viewModelScope.launch {
            backupManager.deleteLocalBackup(backup)
        }
    }

    // 云服务连接
    fun connectCloud(authCode: String) {
        viewModelScope.launch {
            val provider = _connectingProvider.value ?: return@launch
            hideConnectDialog()

            when (provider) {
                CloudProvider.BAIDU -> backupManager.connectBaiduCloud(authCode)
                CloudProvider.ALIYUN -> backupManager.connectAliyunDrive(authCode)
                else -> {}
            }
        }
    }

    fun disconnectBaidu() {
        backupManager.disconnectBaiduCloud()
    }

    fun disconnectAliyun() {
        backupManager.disconnectAliyunDrive()
    }

    // 清除状态
    fun clearState() {
        backupManager.clearState()
    }

    // 对话框控制
    fun showIntervalPickerDialog() {
        _showIntervalPicker.value = true
    }

    fun hideIntervalPickerDialog() {
        _showIntervalPicker.value = false
    }

    fun showProviderPickerDialog() {
        _showProviderPicker.value = true
    }

    fun hideProviderPickerDialog() {
        _showProviderPicker.value = false
    }

    fun showRestoreDialogFromLocal() {
        isRestoringFromCloud = false
        _showRestoreDialog.value = true
    }

    fun showRestoreDialogFromCloud() {
        isRestoringFromCloud = true
        _showRestoreDialog.value = true
    }

    fun hideRestoreDialog() {
        _showRestoreDialog.value = false
    }

    fun showConnectDialog(provider: CloudProvider) {
        _connectingProvider.value = provider
        _showCloudConnectDialog.value = true
    }

    fun hideConnectDialog() {
        _showCloudConnectDialog.value = false
        _connectingProvider.value = null
    }
}
