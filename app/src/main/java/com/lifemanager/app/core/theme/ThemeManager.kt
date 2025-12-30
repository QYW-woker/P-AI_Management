package com.lifemanager.app.core.theme

import com.lifemanager.app.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主题管理器
 * 管理应用的深色/浅色模式
 */
@Singleton
class ThemeManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * 是否深色模式
     */
    val isDarkMode: Flow<Boolean> = settingsRepository.settingsFlow.map { it.isDarkMode }

    /**
     * 切换深色模式
     */
    suspend fun setDarkMode(enabled: Boolean) {
        settingsRepository.setDarkMode(enabled)
    }
}
