package com.lifemanager.app.core.floatingball

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 悬浮球管理器
 * 管理悬浮球的开关和权限
 */
@Singleton
class FloatingBallManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    init {
        checkPermission()
    }

    /**
     * 检查悬浮窗权限
     */
    fun checkPermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        _hasPermission.value = hasPermission
        return hasPermission
    }

    /**
     * 请求悬浮窗权限
     * 返回是否需要跳转设置页面
     */
    fun requestPermission(): Intent? {
        if (checkPermission()) {
            return null
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

    /**
     * 启用悬浮球
     */
    fun enable(): Boolean {
        if (!checkPermission()) {
            return false
        }

        FloatingBallService.start(context)
        _isEnabled.value = true
        return true
    }

    /**
     * 禁用悬浮球
     */
    fun disable() {
        FloatingBallService.stop(context)
        _isEnabled.value = false
    }

    /**
     * 切换悬浮球状态
     */
    fun toggle(): Boolean {
        return if (_isEnabled.value) {
            disable()
            false
        } else {
            enable()
        }
    }

    /**
     * 悬浮球是否正在运行
     */
    fun isRunning(): Boolean {
        return _isEnabled.value && checkPermission()
    }
}
