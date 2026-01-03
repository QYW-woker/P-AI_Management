package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.DiaryEntity

/**
 * 日记UI状态
 */
sealed class DiaryUiState {
    object Loading : DiaryUiState()
    object Success : DiaryUiState()
    data class Error(val message: String) : DiaryUiState()
}

/**
 * 日记编辑状态
 */
data class DiaryEditState(
    val id: Long = 0,
    val isEditing: Boolean = false,
    val date: Int = 0,
    val title: String = "",
    val content: String = "",
    val moodScore: Int? = null,
    val weather: String? = null,

    // 位置信息
    val locationName: String? = null,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val poiName: String? = null,

    // 旧字段保留兼容
    val location: String? = null,
    val sleepMinutes: Int? = null,

    val attachments: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = false,
    val error: String? = null
) {
    /**
     * 是否有位置
     */
    fun hasLocation(): Boolean = latitude != null && longitude != null

    /**
     * 获取位置显示名称
     */
    fun getLocationDisplayName(): String? = poiName ?: locationName
}

/**
 * 睡眠时长信息
 */
data class SleepDuration(
    val hours: Int,
    val minutes: Int
) {
    val totalMinutes: Int get() = hours * 60 + minutes

    fun formatDisplay(): String {
        return if (minutes == 0) {
            "${hours}小时"
        } else {
            "${hours}小时${minutes}分钟"
        }
    }

    companion object {
        fun fromMinutes(totalMinutes: Int?): SleepDuration? {
            if (totalMinutes == null || totalMinutes <= 0) return null
            return SleepDuration(totalMinutes / 60, totalMinutes % 60)
        }
    }
}

/**
 * 预定义快捷睡眠时长（小时）
 */
val quickSleepOptions = listOf(5, 6, 7, 8, 9)

/**
 * 日记统计
 */
data class DiaryStatistics(
    val totalCount: Int = 0,
    val currentStreak: Int = 0,
    val moodDistribution: Map<Int, Int> = emptyMap(),
    val averageMood: Double = 0.0
)

/**
 * 日历项（用于日历视图）
 */
data class DiaryCalendarItem(
    val date: Int,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasDiary: Boolean,
    val moodScore: Int? = null
)

/**
 * 心情信息
 */
data class MoodInfo(
    val score: Int,
    val name: String,
    val emoji: String,
    val color: Long
)

/**
 * 预定义心情列表
 */
val moodList = listOf(
    MoodInfo(1, "很差", "😞", 0xFF9E9E9E),
    MoodInfo(2, "较差", "😔", 0xFFFF9800),
    MoodInfo(3, "一般", "😐", 0xFFFFC107),
    MoodInfo(4, "较好", "😊", 0xFF8BC34A),
    MoodInfo(5, "很好", "😄", 0xFF4CAF50)
)

/**
 * 天气信息
 */
data class WeatherInfo(
    val code: String,
    val name: String,
    val emoji: String
)

/**
 * 预定义天气列表
 */
val weatherList = listOf(
    WeatherInfo("SUNNY", "晴天", "☀️"),
    WeatherInfo("CLOUDY", "多云", "⛅"),
    WeatherInfo("OVERCAST", "阴天", "☁️"),
    WeatherInfo("RAINY", "雨天", "🌧️"),
    WeatherInfo("SNOWY", "雪天", "❄️"),
    WeatherInfo("WINDY", "大风", "💨"),
    WeatherInfo("FOGGY", "雾天", "🌫️")
)

/**
 * 月度日记摘要
 */
data class MonthlyDiarySummary(
    val yearMonth: Int,
    val diaryCount: Int,
    val averageMood: Double?,
    val dominantMood: Int?
)
