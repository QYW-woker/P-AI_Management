package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsRecordEntity

/**
 * 存钱计划模块数据模型
 */

/**
 * 里程碑类型
 */
enum class Milestone(val percentage: Int, val icon: String, val label: String) {
    START(0, "🚀", "起步"),
    QUARTER(25, "🌟", "四分之一"),
    HALF(50, "⭐", "一半"),
    THREE_QUARTERS(75, "🔥", "四分之三"),
    COMPLETE(100, "🎉", "完成")
}

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
    val isOnTrack: Boolean = true,      // 是否符合预期进度
    val totalDeposits: Double = 0.0,    // 总存款金额
    val totalWithdrawals: Double = 0.0, // 总取款金额
    val depositCount: Int = 0,          // 存款次数
    val withdrawalCount: Int = 0,       // 取款次数
    val currentMilestone: Milestone = Milestone.START,  // 当前里程碑
    val nextMilestone: Milestone? = Milestone.QUARTER   // 下一个里程碑
) {
    /**
     * 获取当前已达成的里程碑
     */
    fun getReachedMilestone(): Milestone {
        val progressPercent = (progress * 100).toInt()
        return when {
            progressPercent >= 100 -> Milestone.COMPLETE
            progressPercent >= 75 -> Milestone.THREE_QUARTERS
            progressPercent >= 50 -> Milestone.HALF
            progressPercent >= 25 -> Milestone.QUARTER
            else -> Milestone.START
        }
    }

    /**
     * 计算下一个待达成的里程碑
     */
    fun calculateNextMilestone(): Milestone? {
        val progressPercent = (progress * 100).toInt()
        return when {
            progressPercent >= 100 -> null
            progressPercent >= 75 -> Milestone.COMPLETE
            progressPercent >= 50 -> Milestone.THREE_QUARTERS
            progressPercent >= 25 -> Milestone.HALF
            else -> Milestone.QUARTER
        }
    }

    /**
     * 距离下一个里程碑还需存多少
     */
    fun getAmountToNextMilestone(): Double {
        val next = calculateNextMilestone() ?: return 0.0
        val targetForNext = plan.targetAmount * next.percentage / 100
        return maxOf(0.0, targetForNext - plan.currentAmount)
    }
}

/**
 * 存钱统计数据
 */
data class SavingsStats(
    val activePlans: Int = 0,
    val totalTarget: Double = 0.0,
    val totalCurrent: Double = 0.0,
    val overallProgress: Float = 0f,
    val totalRecords: Int = 0,
    val thisMonthDeposit: Double = 0.0,
    val lastMonthDeposit: Double = 0.0,    // 上月存款总额
    val monthlyChange: Double = 0.0,        // 月度变化百分比
    val totalDeposits: Double = 0.0,        // 总存款金额
    val totalWithdrawals: Double = 0.0,     // 总取款金额
    val savingsStreak: Int = 0              // 连续存款天数
) {
    /**
     * 月度变化是否为正
     */
    fun isPositiveChange(): Boolean = monthlyChange >= 0

    /**
     * 获取月度变化显示文本
     */
    fun getMonthlyChangeText(): String {
        return if (monthlyChange >= 0) {
            "+${String.format("%.1f", monthlyChange)}%"
        } else {
            "${String.format("%.1f", monthlyChange)}%"
        }
    }
}

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
 * 存款/取款记录编辑状态
 */
data class RecordEditState(
    val id: Long = 0,
    val planId: Long = 0,
    val amount: Double = 0.0,
    val date: Int = 0,
    val note: String = "",
    val isWithdrawal: Boolean = false,  // 是否为取款
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 快速存款预设金额
 */
val quickDepositAmounts = listOf(
    10.0 to "¥10",
    50.0 to "¥50",
    100.0 to "¥100",
    200.0 to "¥200",
    500.0 to "¥500",
    1000.0 to "¥1000"
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

/**
 * 存钱计划模板
 */
data class SavingsPlanTemplate(
    val name: String,
    val description: String,
    val icon: String,
    val suggestedAmount: Double,
    val suggestedMonths: Int,
    val color: String,
    val strategy: String = "FIXED_MONTHLY"
)

/**
 * 预设存钱计划模板
 */
val savingsPlanTemplates = listOf(
    SavingsPlanTemplate(
        name = "应急基金",
        description = "建立3-6个月生活费的应急储备",
        icon = "🛡️",
        suggestedAmount = 30000.0,
        suggestedMonths = 12,
        color = "#2196F3",
        strategy = "FIXED_MONTHLY"
    ),
    SavingsPlanTemplate(
        name = "旅行基金",
        description = "为下一次旅行攒钱",
        icon = "✈️",
        suggestedAmount = 10000.0,
        suggestedMonths = 6,
        color = "#00BCD4",
        strategy = "FIXED_MONTHLY"
    ),
    SavingsPlanTemplate(
        name = "数码产品",
        description = "新手机、电脑等电子产品",
        icon = "📱",
        suggestedAmount = 8000.0,
        suggestedMonths = 4,
        color = "#9C27B0",
        strategy = "FIXED_MONTHLY"
    ),
    SavingsPlanTemplate(
        name = "节日礼物",
        description = "春节/生日/纪念日礼物预算",
        icon = "🎁",
        suggestedAmount = 3000.0,
        suggestedMonths = 3,
        color = "#E91E63",
        strategy = "FIXED_MONTHLY"
    ),
    SavingsPlanTemplate(
        name = "学习提升",
        description = "课程培训、书籍资料费用",
        icon = "📚",
        suggestedAmount = 5000.0,
        suggestedMonths = 6,
        color = "#4CAF50",
        strategy = "FIXED_MONTHLY"
    ),
    SavingsPlanTemplate(
        name = "购车首付",
        description = "汽车首付款储蓄计划",
        icon = "🚗",
        suggestedAmount = 50000.0,
        suggestedMonths = 24,
        color = "#FF9800",
        strategy = "FIXED_MONTHLY"
    ),
    SavingsPlanTemplate(
        name = "装修基金",
        description = "家居装修或家具更新",
        icon = "🏠",
        suggestedAmount = 30000.0,
        suggestedMonths = 12,
        color = "#795548",
        strategy = "FIXED_MONTHLY"
    ),
    SavingsPlanTemplate(
        name = "婚礼基金",
        description = "婚礼筹备费用",
        icon = "💍",
        suggestedAmount = 100000.0,
        suggestedMonths = 24,
        color = "#F44336",
        strategy = "FIXED_MONTHLY"
    )
)
