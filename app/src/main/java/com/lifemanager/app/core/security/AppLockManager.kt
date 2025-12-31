package com.lifemanager.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用锁管理器
 *
 * 管理应用锁定状态和设置
 */
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val biometricAuthService: BiometricAuthService
) {

    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_AUTH_ON_LAUNCH = "auth_on_launch"
        private const val KEY_AUTH_ON_RESUME = "auth_on_resume"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout"
        private const val KEY_SENSITIVE_PROTECTION = "sensitive_protection"
        private const val KEY_LAST_UNLOCK_TIME = "last_unlock_time"
        private const val KEY_PROTECTED_MODULES = "protected_modules"

        // 默认锁定超时时间
        const val TIMEOUT_IMMEDIATE = 0L
        const val TIMEOUT_30_SECONDS = 30 * 1000L
        const val TIMEOUT_1_MINUTE = 60 * 1000L
        const val TIMEOUT_5_MINUTES = 5 * 60 * 1000L
        const val TIMEOUT_15_MINUTES = 15 * 60 * 1000L
        const val TIMEOUT_30_MINUTES = 30 * 60 * 1000L

        // 可保护的模块
        val PROTECTABLE_MODULES = listOf(
            ProtectableModule("FINANCE", "财务", "记账、预算、资产等财务数据"),
            ProtectableModule("DIARY", "日记", "私人日记内容"),
            ProtectableModule("HEALTH", "健康", "健康记录和目标"),
            ProtectableModule("AI_CHAT", "AI对话", "AI助手对话记录"),
            ProtectableModule("SAVINGS", "存钱计划", "存钱计划详情"),
            ProtectableModule("ALL", "全部数据", "应用所有数据")
        )
    }

    private val _lockState = MutableStateFlow<LockState>(LockState.Unknown)
    val lockState: Flow<LockState> = _lockState.asStateFlow()

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 加密存储创建失败时使用普通存储
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * 初始化锁状态
     */
    fun initialize() {
        val isEnabled = isLockEnabled()
        _lockState.value = if (isEnabled) {
            if (shouldRequireAuth()) {
                LockState.Locked
            } else {
                LockState.Unlocked
            }
        } else {
            LockState.Disabled
        }
    }

    /**
     * 检查是否启用了应用锁
     */
    fun isLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCK_ENABLED, false)
    }

    /**
     * 启用应用锁
     */
    fun enableLock() {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, true).apply()
        _lockState.value = LockState.Locked
    }

    /**
     * 禁用应用锁
     */
    fun disableLock() {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, false).apply()
        _lockState.value = LockState.Disabled
    }

    /**
     * 检查是否需要认证
     */
    fun shouldRequireAuth(): Boolean {
        if (!isLockEnabled()) return false

        val lastUnlockTime = prefs.getLong(KEY_LAST_UNLOCK_TIME, 0)
        val timeout = getLockTimeout()

        if (timeout == TIMEOUT_IMMEDIATE) return true

        val elapsed = System.currentTimeMillis() - lastUnlockTime
        return elapsed > timeout
    }

    /**
     * 记录解锁时间
     */
    fun recordUnlock() {
        prefs.edit().putLong(KEY_LAST_UNLOCK_TIME, System.currentTimeMillis()).apply()
        _lockState.value = LockState.Unlocked
    }

    /**
     * 锁定应用
     */
    fun lock() {
        if (isLockEnabled()) {
            _lockState.value = LockState.Locked
        }
    }

    /**
     * 获取锁定超时时间
     */
    fun getLockTimeout(): Long {
        return prefs.getLong(KEY_LOCK_TIMEOUT, TIMEOUT_5_MINUTES)
    }

    /**
     * 设置锁定超时时间
     */
    fun setLockTimeout(timeout: Long) {
        prefs.edit().putLong(KEY_LOCK_TIMEOUT, timeout).apply()
    }

    /**
     * 是否在启动时认证
     */
    fun isAuthOnLaunchEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTH_ON_LAUNCH, true)
    }

    /**
     * 设置启动时认证
     */
    fun setAuthOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_ON_LAUNCH, enabled).apply()
    }

    /**
     * 是否在返回时认证
     */
    fun isAuthOnResumeEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTH_ON_RESUME, false)
    }

    /**
     * 设置返回时认证
     */
    fun setAuthOnResume(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_ON_RESUME, enabled).apply()
    }

    /**
     * 是否启用敏感数据保护
     */
    fun isSensitiveProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_SENSITIVE_PROTECTION, true)
    }

    /**
     * 设置敏感数据保护
     */
    fun setSensitiveProtection(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SENSITIVE_PROTECTION, enabled).apply()
    }

    /**
     * 获取受保护的模块
     */
    fun getProtectedModules(): Set<String> {
        return prefs.getStringSet(KEY_PROTECTED_MODULES, setOf("ALL")) ?: setOf("ALL")
    }

    /**
     * 设置受保护的模块
     */
    fun setProtectedModules(modules: Set<String>) {
        prefs.edit().putStringSet(KEY_PROTECTED_MODULES, modules).apply()
    }

    /**
     * 检查模块是否需要保护
     */
    fun isModuleProtected(moduleId: String): Boolean {
        if (!isLockEnabled()) return false
        if (!isSensitiveProtectionEnabled()) return false

        val protectedModules = getProtectedModules()
        return protectedModules.contains("ALL") || protectedModules.contains(moduleId)
    }

    /**
     * 获取当前锁设置
     */
    fun getLockSettings(): AppLockSettings {
        return AppLockSettings(
            isEnabled = isLockEnabled(),
            authOnLaunch = isAuthOnLaunchEnabled(),
            authOnResume = isAuthOnResumeEnabled(),
            lockTimeout = getLockTimeout(),
            sensitiveDataProtection = isSensitiveProtectionEnabled()
        )
    }

    /**
     * 保存锁设置
     */
    fun saveLockSettings(settings: AppLockSettings) {
        prefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, settings.isEnabled)
            .putBoolean(KEY_AUTH_ON_LAUNCH, settings.authOnLaunch)
            .putBoolean(KEY_AUTH_ON_RESUME, settings.authOnResume)
            .putLong(KEY_LOCK_TIMEOUT, settings.lockTimeout)
            .putBoolean(KEY_SENSITIVE_PROTECTION, settings.sensitiveDataProtection)
            .apply()

        _lockState.value = if (settings.isEnabled) LockState.Locked else LockState.Disabled
    }

    /**
     * 获取超时选项列表
     */
    fun getTimeoutOptions(): List<TimeoutOption> = listOf(
        TimeoutOption(TIMEOUT_IMMEDIATE, "立即"),
        TimeoutOption(TIMEOUT_30_SECONDS, "30秒"),
        TimeoutOption(TIMEOUT_1_MINUTE, "1分钟"),
        TimeoutOption(TIMEOUT_5_MINUTES, "5分钟"),
        TimeoutOption(TIMEOUT_15_MINUTES, "15分钟"),
        TimeoutOption(TIMEOUT_30_MINUTES, "30分钟")
    )

    /**
     * 检查生物识别可用性
     */
    fun checkBiometricStatus(): BiometricAvailability {
        return biometricAuthService.checkBiometricAvailability()
    }

    /**
     * 获取认证方式描述
     */
    fun getAuthMethodDescription(): String {
        return biometricAuthService.getRecommendedAuthDescription()
    }
}

/**
 * 锁状态
 */
sealed class LockState {
    object Unknown : LockState()
    object Disabled : LockState()
    object Locked : LockState()
    object Unlocked : LockState()
}

/**
 * 可保护模块
 */
data class ProtectableModule(
    val id: String,
    val name: String,
    val description: String
)

/**
 * 超时选项
 */
data class TimeoutOption(
    val timeout: Long,
    val label: String
)
