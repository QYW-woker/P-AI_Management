package com.lifemanager.app.core.floatingball

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * 悬浮球重启接收器
 * 在用户解锁屏幕或应用升级后检查并重启悬浮球服务
 */
class FloatingBallRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // 检查是否应该启动悬浮球
                if (shouldRestartFloatingBall(context)) {
                    startFloatingBallService(context)
                }
            }
        }
    }

    /**
     * 检查是否应该重启悬浮球
     */
    private fun shouldRestartFloatingBall(context: Context): Boolean {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                return false
            }
        }

        // 从SharedPreferences检查用户是否启用了悬浮球
        val prefs = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE)
        return prefs.getBoolean("floating_ball_enabled", false)
    }

    /**
     * 启动悬浮球服务
     */
    private fun startFloatingBallService(context: Context) {
        try {
            FloatingBallService.start(context)
        } catch (e: Exception) {
            // 启动失败，忽略
        }
    }
}
