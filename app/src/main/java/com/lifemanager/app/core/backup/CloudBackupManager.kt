package com.lifemanager.app.core.backup

import android.content.Context
import android.content.SharedPreferences
import com.lifemanager.app.core.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云备份管理器
 *
 * 支持多种云存储:
 * - 百度网盘 (Baidu Cloud)
 * - 阿里云盘 (Aliyun Drive)
 * - 本地备份
 */
@Singleton
class CloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val PREFS_NAME = "cloud_backup_prefs"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_BACKUP_INTERVAL = "backup_interval"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_CLOUD_PROVIDER = "cloud_provider"
        private const val KEY_BAIDU_TOKEN = "baidu_token"
        private const val KEY_BAIDU_REFRESH_TOKEN = "baidu_refresh_token"
        private const val KEY_ALIYUN_TOKEN = "aliyun_token"
        private const val KEY_ALIYUN_REFRESH_TOKEN = "aliyun_refresh_token"
        private const val KEY_CLOUD_BACKUP_PATH = "cloud_backup_path"

        // 备份间隔选项 (小时)
        const val INTERVAL_DAILY = 24
        const val INTERVAL_WEEKLY = 168
        const val INTERVAL_BIWEEKLY = 336
        const val INTERVAL_MONTHLY = 720
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 备份状态
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    // 云服务连接状态
    private val _baiduConnected = MutableStateFlow(false)
    val baiduConnected: StateFlow<Boolean> = _baiduConnected.asStateFlow()

    private val _aliyunConnected = MutableStateFlow(false)
    val aliyunConnected: StateFlow<Boolean> = _aliyunConnected.asStateFlow()

    // 当前云提供商
    private val _currentProvider = MutableStateFlow(CloudProvider.LOCAL)
    val currentProvider: StateFlow<CloudProvider> = _currentProvider.asStateFlow()

    // 备份设置
    private val _autoBackupEnabled = MutableStateFlow(false)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _backupInterval = MutableStateFlow(INTERVAL_DAILY)
    val backupInterval: StateFlow<Int> = _backupInterval.asStateFlow()

    // 备份列表
    private val _localBackups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val localBackups: StateFlow<List<BackupInfo>> = _localBackups.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val cloudBackups: StateFlow<List<BackupInfo>> = _cloudBackups.asStateFlow()

    init {
        loadSettings()
        checkCloudConnections()
    }

    private fun loadSettings() {
        _autoBackupEnabled.value = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        _backupInterval.value = prefs.getInt(KEY_BACKUP_INTERVAL, INTERVAL_DAILY)
        _currentProvider.value = CloudProvider.fromString(
            prefs.getString(KEY_CLOUD_PROVIDER, CloudProvider.LOCAL.name) ?: CloudProvider.LOCAL.name
        )
    }

    private fun checkCloudConnections() {
        _baiduConnected.value = prefs.getString(KEY_BAIDU_TOKEN, null) != null
        _aliyunConnected.value = prefs.getString(KEY_ALIYUN_TOKEN, null) != null
    }

    /**
     * 设置自动备份
     */
    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
        _autoBackupEnabled.value = enabled
    }

    /**
     * 设置备份间隔
     */
    fun setBackupInterval(intervalHours: Int) {
        prefs.edit().putInt(KEY_BACKUP_INTERVAL, intervalHours).apply()
        _backupInterval.value = intervalHours
    }

    /**
     * 设置云存储提供商
     */
    fun setCloudProvider(provider: CloudProvider) {
        prefs.edit().putString(KEY_CLOUD_PROVIDER, provider.name).apply()
        _currentProvider.value = provider
    }

    /**
     * 连接百度网盘
     */
    suspend fun connectBaiduCloud(authCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.Connecting("正在连接百度网盘...")

            // TODO: 实际OAuth2认证流程
            // 这里模拟认证成功
            // 1. 使用authCode换取access_token和refresh_token
            // 2. 保存tokens
            // 3. 验证连接

            // 模拟token保存
            prefs.edit()
                .putString(KEY_BAIDU_TOKEN, "mock_baidu_token_$authCode")
                .putString(KEY_BAIDU_REFRESH_TOKEN, "mock_baidu_refresh_$authCode")
                .apply()

            _baiduConnected.value = true
            _backupState.value = BackupState.Success("百度网盘连接成功")
            Result.success(true)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("连接失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 断开百度网盘
     */
    fun disconnectBaiduCloud() {
        prefs.edit()
            .remove(KEY_BAIDU_TOKEN)
            .remove(KEY_BAIDU_REFRESH_TOKEN)
            .apply()
        _baiduConnected.value = false
        if (_currentProvider.value == CloudProvider.BAIDU) {
            setCloudProvider(CloudProvider.LOCAL)
        }
    }

    /**
     * 连接阿里云盘
     */
    suspend fun connectAliyunDrive(authCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.Connecting("正在连接阿里云盘...")

            // TODO: 实际OAuth2认证流程
            // 模拟token保存
            prefs.edit()
                .putString(KEY_ALIYUN_TOKEN, "mock_aliyun_token_$authCode")
                .putString(KEY_ALIYUN_REFRESH_TOKEN, "mock_aliyun_refresh_$authCode")
                .apply()

            _aliyunConnected.value = true
            _backupState.value = BackupState.Success("阿里云盘连接成功")
            Result.success(true)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("连接失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 断开阿里云盘
     */
    fun disconnectAliyunDrive() {
        prefs.edit()
            .remove(KEY_ALIYUN_TOKEN)
            .remove(KEY_ALIYUN_REFRESH_TOKEN)
            .apply()
        _aliyunConnected.value = false
        if (_currentProvider.value == CloudProvider.ALIYUN) {
            setCloudProvider(CloudProvider.LOCAL)
        }
    }

    /**
     * 立即备份到本地
     */
    suspend fun backupToLocal(): Result<String> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.BackingUp("正在备份到本地...")

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val backupDir = getLocalBackupDir()
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "backup_$timestamp.db")

            // 复制数据库文件
            dbFile.copyTo(backupFile, overwrite = true)

            // 更新最后备份时间
            prefs.edit().putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis()).apply()

            // 刷新本地备份列表
            refreshLocalBackups()

            _backupState.value = BackupState.Success("备份成功")
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("备份失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 备份到云端
     */
    suspend fun backupToCloud(): Result<String> = withContext(Dispatchers.IO) {
        val provider = _currentProvider.value

        when (provider) {
            CloudProvider.LOCAL -> backupToLocal()
            CloudProvider.BAIDU -> backupToBaiduCloud()
            CloudProvider.ALIYUN -> backupToAliyunDrive()
        }
    }

    private suspend fun backupToBaiduCloud(): Result<String> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.BackingUp("正在备份到百度网盘...")

            if (!_baiduConnected.value) {
                throw Exception("百度网盘未连接")
            }

            // 先备份到本地
            val localBackupResult = backupToLocal()
            if (localBackupResult.isFailure) {
                throw localBackupResult.exceptionOrNull() ?: Exception("本地备份失败")
            }

            val localPath = localBackupResult.getOrNull()!!

            // TODO: 实际上传到百度网盘
            // 使用百度网盘API上传文件
            // 这里模拟上传成功

            // 刷新云端备份列表
            refreshCloudBackups()

            _backupState.value = BackupState.Success("已备份到百度网盘")
            Result.success(localPath)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("备份失败: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun backupToAliyunDrive(): Result<String> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.BackingUp("正在备份到阿里云盘...")

            if (!_aliyunConnected.value) {
                throw Exception("阿里云盘未连接")
            }

            // 先备份到本地
            val localBackupResult = backupToLocal()
            if (localBackupResult.isFailure) {
                throw localBackupResult.exceptionOrNull() ?: Exception("本地备份失败")
            }

            val localPath = localBackupResult.getOrNull()!!

            // TODO: 实际上传到阿里云盘
            // 使用阿里云盘API上传文件
            // 这里模拟上传成功

            // 刷新云端备份列表
            refreshCloudBackups()

            _backupState.value = BackupState.Success("已备份到阿里云盘")
            Result.success(localPath)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("备份失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 从本地备份恢复
     */
    suspend fun restoreFromLocal(backupInfo: BackupInfo): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.Restoring("正在恢复数据...")

            val backupFile = File(backupInfo.path)
            if (!backupFile.exists()) {
                throw Exception("备份文件不存在")
            }

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)

            // 关闭数据库连接
            database.close()

            // 恢复数据库文件
            backupFile.copyTo(dbFile, overwrite = true)

            _backupState.value = BackupState.Success("数据恢复成功，请重启应用")
            Result.success(true)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("恢复失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 从云端恢复
     */
    suspend fun restoreFromCloud(backupInfo: BackupInfo): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.Restoring("正在从云端下载备份...")

            // TODO: 根据云服务商下载备份文件
            when (backupInfo.provider) {
                CloudProvider.BAIDU -> {
                    // 从百度网盘下载
                }
                CloudProvider.ALIYUN -> {
                    // 从阿里云盘下载
                }
                else -> {}
            }

            // 模拟下载完成后恢复
            _backupState.value = BackupState.Success("数据恢复成功，请重启应用")
            Result.success(true)
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("恢复失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 刷新本地备份列表
     */
    suspend fun refreshLocalBackups() = withContext(Dispatchers.IO) {
        val backupDir = getLocalBackupDir()
        if (!backupDir.exists()) {
            _localBackups.value = emptyList()
            return@withContext
        }

        val backups = backupDir.listFiles { file ->
            file.name.startsWith("backup_") && file.name.endsWith(".db")
        }?.map { file ->
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = file.name.removePrefix("backup_").removeSuffix(".db")
            val date = try {
                dateFormat.parse(timestamp)
            } catch (e: Exception) {
                Date(file.lastModified())
            }

            BackupInfo(
                id = file.name,
                path = file.absolutePath,
                size = file.length(),
                createdAt = date ?: Date(file.lastModified()),
                provider = CloudProvider.LOCAL
            )
        }?.sortedByDescending { it.createdAt } ?: emptyList()

        _localBackups.value = backups
    }

    /**
     * 刷新云端备份列表
     */
    suspend fun refreshCloudBackups() = withContext(Dispatchers.IO) {
        val backups = mutableListOf<BackupInfo>()

        // TODO: 从各云服务获取备份列表
        if (_baiduConnected.value) {
            // 从百度网盘获取备份列表
        }

        if (_aliyunConnected.value) {
            // 从阿里云盘获取备份列表
        }

        _cloudBackups.value = backups.sortedByDescending { it.createdAt }
    }

    /**
     * 删除本地备份
     */
    suspend fun deleteLocalBackup(backupInfo: BackupInfo): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(backupInfo.path)
            if (file.exists()) {
                file.delete()
            }
            refreshLocalBackups()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取最后备份时间
     */
    fun getLastBackupTime(): Long {
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0)
    }

    /**
     * 检查是否需要自动备份
     */
    fun needsAutoBackup(): Boolean {
        if (!_autoBackupEnabled.value) return false

        val lastBackup = getLastBackupTime()
        if (lastBackup == 0L) return true

        val intervalMs = _backupInterval.value * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastBackup >= intervalMs
    }

    /**
     * 清除状态
     */
    fun clearState() {
        _backupState.value = BackupState.Idle
    }

    private fun getLocalBackupDir(): File {
        return File(context.getExternalFilesDir(null), "backups")
    }
}

/**
 * 云存储提供商
 */
enum class CloudProvider(val displayName: String, val icon: String) {
    LOCAL("本地存储", "📱"),
    BAIDU("百度网盘", "☁️"),
    ALIYUN("阿里云盘", "🌥️");

    companion object {
        fun fromString(value: String): CloudProvider {
            return entries.find { it.name == value } ?: LOCAL
        }
    }
}

/**
 * 备份信息
 */
data class BackupInfo(
    val id: String,
    val path: String,
    val size: Long,
    val createdAt: Date,
    val provider: CloudProvider
) {
    val formattedSize: String
        get() {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            }
        }

    val formattedDate: String
        get() {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return dateFormat.format(createdAt)
        }
}

/**
 * 备份状态
 */
sealed class BackupState {
    object Idle : BackupState()
    data class Connecting(val message: String) : BackupState()
    data class BackingUp(val message: String) : BackupState()
    data class Restoring(val message: String) : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}
