package com.lifemanager.app.core.floatingball

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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

    /**
     * 检查电池优化是否已禁用（允许后台运行）
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /**
     * 请求禁用电池优化
     * 返回跳转到设置页面的Intent
     */
    @SuppressLint("BatteryLife")
    fun requestDisableBatteryOptimization(): Intent? {
        if (isBatteryOptimizationDisabled()) {
            return null
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

    /**
     * 获取打开自启动设置的Intent（针对国产ROM）
     * 不同厂商有不同的自启动设置页面
     */
    fun getAutoStartSettingsIntent(): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intents = mutableListOf<Intent>()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                // 小米/红米
                intents.add(Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                })
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                // 华为/荣耀
                intents.add(Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                })
                intents.add(Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                })
            }
            manufacturer.contains("oppo") -> {
                // OPPO
                intents.add(Intent().apply {
                    component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity"
                    )
                })
            }
            manufacturer.contains("vivo") -> {
                // vivo
                intents.add(Intent().apply {
                    component = ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                })
            }
            manufacturer.contains("samsung") -> {
                // 三星
                intents.add(Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                })
            }
            manufacturer.contains("meizu") -> {
                // 魅族
                intents.add(Intent().apply {
                    component = ComponentName(
                        "com.meizu.safe",
                        "com.meizu.safe.permission.SmartBGActivity"
                    )
                })
            }
        }

        // 通用设置页面
        intents.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        })

        // 尝试找到可用的Intent
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        }

        return null
    }

    /**
     * 获取所有需要的权限状态
     */
    fun getPermissionStatus(): FloatingBallPermissionStatus {
        return FloatingBallPermissionStatus(
            hasOverlayPermission = checkPermission(),
            hasBatteryOptimizationExemption = isBatteryOptimizationDisabled()
        )
    }
}

/**
 * 悬浮球权限状态
 */
data class FloatingBallPermissionStatus(
    val hasOverlayPermission: Boolean,
    val hasBatteryOptimizationExemption: Boolean
) {
    val isFullyConfigured: Boolean get() = hasOverlayPermission && hasBatteryOptimizationExemption
}
