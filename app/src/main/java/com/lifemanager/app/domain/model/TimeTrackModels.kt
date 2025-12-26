package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.TimeCategoryEntity
import com.lifemanager.app.core.database.entity.TimeRecordEntity

/**
 * 时间统计UI状态
 */
sealed class TimeTrackUiState {
    object Loading : TimeTrackUiState()
    object Success : TimeTrackUiState()
    data class Error(val message: String) : TimeTrackUiState()
}

/**
 * 带分类的时间记录
 */
data class TimeRecordWithCategory(
    val record: TimeRecordEntity,
    val category: TimeCategoryEntity?
)

/**
 * 分类时长统计
 */
data class CategoryDuration(
    val categoryId: Long?,
    val categoryName: String,
    val categoryColor: String,
    val durationMinutes: Int,
    val percentage: Double
)

/**
 * 今日统计
 */
data class TodayTimeStats(
    val totalMinutes: Int = 0,
    val recordCount: Int = 0,
    val topCategory: String? = null,
    val isTracking: Boolean = false
)

/**
 * 周统计
 */
data class WeeklyTimeStats(
    val totalMinutes: Int = 0,
    val avgDailyMinutes: Int = 0,
    val topCategory: String? = null
)

/**
 * 计时器状态
 */
data class TimerState(
    val isRunning: Boolean = false,
    val currentRecordId: Long? = null,
    val categoryId: Long? = null,
    val categoryName: String = "",
    val startTime: Long = 0,
    val elapsedSeconds: Long = 0
)

/**
 * 时间记录编辑状态
 */
data class TimeRecordEditState(
    val id: Long = 0,
    val isEditing: Boolean = false,
    val categoryId: Long? = null,
    val date: Int = 0,
    val durationMinutes: Int = 0,
    val note: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 时间分类编辑状态
 */
data class CategoryEditState(
    val id: Long = 0,
    val isEditing: Boolean = false,
    val name: String = "",
    val color: String = "#2196F3",
    val iconName: String = "schedule",
    val isSaving: Boolean = false,
    val error: String? = null
)
