package com.lifemanager.app.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 设置数据类
 */
data class AppSettings(
    val isDarkMode: Boolean = false,
    val enableNotification: Boolean = true,
    val reminderTime: String = "09:00",
    val autoBackup: Boolean = false,
    val language: String = "简体中文"
)

/**
 * 设置仓库
 * 使用DataStore持久化设置
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val ENABLE_NOTIFICATION = booleanPreferencesKey("enable_notification")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val LANGUAGE = stringPreferencesKey("language")
    }

    /**
     * 获取设置流
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: false,
                enableNotification = preferences[PreferencesKeys.ENABLE_NOTIFICATION] ?: true,
                reminderTime = preferences[PreferencesKeys.REMINDER_TIME] ?: "09:00",
                autoBackup = preferences[PreferencesKeys.AUTO_BACKUP] ?: false,
                language = preferences[PreferencesKeys.LANGUAGE] ?: "简体中文"
            )
        }

    /**
     * 设置深色模式
     */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = enabled
        }
    }

    /**
     * 设置通知开关
     */
    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_NOTIFICATION] = enabled
        }
    }

    /**
     * 设置提醒时间
     */
    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_TIME] = time
        }
    }

    /**
     * 设置自动备份
     */
    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP] = enabled
        }
    }

    /**
     * 设置语言
     */
    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
}
