package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsRecordEntity

/**
 * 存钱计划模块数据模型
 */

/**
 * 存钱计划及其详细信息
 */
data class SavingsPlanWithDetails(
    val plan: SavingsPlanEntity,
    val records: List<SavingsRecordEntity> = emptyList(),
    val progress: Float = 0f,           // 进度百分比 0-1
    val daysRemaining: Int = 0,         // 剩余天数
    val daysElapsed: Int = 0,           // 已过天数
    val dailyTarget: Double = 0.0,      // 每日目标金额
    val expectedAmount: Double = 0.0,   // 预期应存金额
    val isOnTrack: Boolean = true       // 是否符合预期进度
)

/**
 * 存钱统计数据
 */
data class SavingsStats(
    val activePlans: Int = 0,
    val totalTarget: Double = 0.0,
    val totalCurrent: Double = 0.0,
    val overallProgress: Float = 0f,
    val totalRecords: Int = 0,
    val thisMonthDeposit: Double = 0.0
)

/**
 * 存钱计划UI状态
 */
sealed class SavingsUiState {
    object Loading : SavingsUiState()
    data class Success(val message: String? = null) : SavingsUiState()
    data class Error(val message: String) : SavingsUiState()
}

/**
 * 计划编辑状态
 */
data class PlanEditState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val targetAmount: Double = 0.0,
    val startDate: Int = 0,
    val targetDate: Int = 0,
    val strategy: String = "FIXED_MONTHLY",
    val periodAmount: Double? = null,
    val color: String = "#4CAF50",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 存款记录编辑状态
 */
data class RecordEditState(
    val id: Long = 0,
    val planId: Long = 0,
    val amount: Double = 0.0,
    val date: Int = 0,
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 预定义的计划颜色
 */
val savingsColors = listOf(
    "#4CAF50" to "绿色",
    "#2196F3" to "蓝色",
    "#9C27B0" to "紫色",
    "#FF9800" to "橙色",
    "#F44336" to "红色",
    "#00BCD4" to "青色",
    "#E91E63" to "粉色",
    "#795548" to "棕色"
)

/**
 * 存钱策略选项
 */
val strategyOptions = listOf(
    "FIXED_DAILY" to "每天固定",
    "FIXED_WEEKLY" to "每周固定",
    "FIXED_MONTHLY" to "每月固定",
    "INCREASING" to "递增存钱",
    "CUSTOM" to "自定义"
)

/**
 * 获取策略显示文本
 */
fun getStrategyDisplayText(strategy: String): String {
    return when (strategy) {
        "FIXED_DAILY" -> "每天固定"
        "FIXED_WEEKLY" -> "每周固定"
        "FIXED_MONTHLY" -> "每月固定"
        "INCREASING" -> "递增存钱"
        "CUSTOM" -> "自定义"
        else -> "每月固定"
    }
}

/**
 * 获取状态显示文本
 */
fun getStatusDisplayText(status: String): String {
    return when (status) {
        "ACTIVE" -> "进行中"
        "COMPLETED" -> "已完成"
        "PAUSED" -> "已暂停"
        "CANCELLED" -> "已取消"
        else -> "进行中"
    }
}
