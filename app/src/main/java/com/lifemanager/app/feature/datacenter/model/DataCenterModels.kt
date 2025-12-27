package com.lifemanager.app.feature.datacenter.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

/**
 * 日期范围类型枚举
 */
enum class DateRangeType(val displayName: String) {
    WEEK("本周"),
    MONTH("本月"),
    YEAR("本年"),
    CUSTOM("自定义"),
    ALL("全部")
}

/**
 * 图表类型枚举
 */
enum class ChartType(val displayName: String) {
    PIE("饼图"),
    BAR("柱状图"),
    LINE("折线图")
}

/**
 * 数据中心筛选状态
 */
data class DataCenterFilterState(
    val dateRangeType: DateRangeType = DateRangeType.MONTH,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val selectedCategoryIds: Set<Long> = emptySet(),  // 空表示全选
    val chartType: ChartType = ChartType.PIE
)

/**
 * 分类图表数据项
 */
data class CategoryChartItem(
    val fieldId: Long,
    val name: String,
    val value: Double,
    val percentage: Float,
    val color: Color,
    val iconName: String
)

/**
 * 每日财务趋势数据
 */
data class DailyFinanceTrend(
    val date: Int,  // epochDay
    val income: Double,
    val expense: Double
)

/**
 * 每月财务趋势数据
 */
data class MonthlyFinanceTrend(
    val yearMonth: Int,
    val income: Double,
    val expense: Double
)

/**
 * 财务图表数据
 */
data class FinanceChartData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val incomeByCategory: List<CategoryChartItem> = emptyList(),
    val expenseByCategory: List<CategoryChartItem> = emptyList(),
    val dailyTrend: List<DailyFinanceTrend> = emptyList(),
    val monthlyTrend: List<MonthlyFinanceTrend> = emptyList()
)

/**
 * 每日计数数据
 */
data class DailyCount(
    val date: Int,  // epochDay
    val count: Int
)

/**
 * 待办图表数据
 */
data class TodoChartData(
    val completed: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    val completionRate: Float = 0f,
    val dailyCompletions: List<DailyCount> = emptyList()
)

/**
 * 习惯完成项
 */
data class HabitCompletionItem(
    val habitId: Long,
    val name: String,
    val completionRate: Float,
    val color: Color
)

/**
 * 习惯图表数据
 */
data class HabitChartData(
    val activeHabits: Int = 0,
    val todayCheckedIn: Int = 0,
    val overallRate: Float = 0f,
    val habitCompletionRates: List<HabitCompletionItem> = emptyList(),
    val weeklyCheckins: List<DailyCount> = emptyList()
)

/**
 * 每日时长数据
 */
data class DailyDuration(
    val date: Int,  // epochDay
    val minutes: Int
)

/**
 * 时间统计图表数据
 */
data class TimeChartData(
    val totalMinutes: Int = 0,
    val categoryBreakdown: List<CategoryChartItem> = emptyList(),
    val dailyDurations: List<DailyDuration> = emptyList()
)

/**
 * 效率统计图表数据
 */
data class ProductivityChartData(
    val todoStats: TodoChartData = TodoChartData(),
    val habitStats: HabitChartData = HabitChartData(),
    val timeStats: TimeChartData = TimeChartData()
)

/**
 * 心情分布项
 */
data class MoodDistributionItem(
    val moodScore: Int,
    val count: Int,
    val percentage: Float,
    val color: Color
)

/**
 * 每日心情数据
 */
data class DailyMood(
    val date: Int,  // epochDay
    val moodScore: Float
)

/**
 * 日记图表数据
 */
data class DiaryChartData(
    val totalEntries: Int = 0,
    val averageMood: Float = 0f,
    val moodDistribution: List<MoodDistributionItem> = emptyList(),
    val dailyMoodTrend: List<DailyMood> = emptyList()
)

/**
 * 存钱计划进度
 */
data class SavingsPlanProgress(
    val planId: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val progress: Float,
    val color: Color
)

/**
 * 存钱统计图表数据
 */
data class SavingsChartData(
    val activePlans: Int = 0,
    val totalTarget: Double = 0.0,
    val totalCurrent: Double = 0.0,
    val overallProgress: Float = 0f,
    val planProgress: List<SavingsPlanProgress> = emptyList()
)

/**
 * 生活方式图表数据
 */
data class LifestyleChartData(
    val diaryStats: DiaryChartData = DiaryChartData(),
    val savingsStats: SavingsChartData = SavingsChartData()
)
