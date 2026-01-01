package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.IncomeExpenseType
import com.lifemanager.app.core.database.entity.MonthlyIncomeExpenseEntity

/**
 * 月度收支记录模型（带字段详情）
 *
 * 包含收支记录及其关联的自定义字段信息
 */
data class MonthlyIncomeExpenseWithField(
    val record: MonthlyIncomeExpenseEntity,
    val field: CustomFieldEntity?
)

/**
 * 月度收支统计模型
 *
 * 汇总某月的收入和支出总额
 */
data class IncomeExpenseMonthlyStats(
    val yearMonth: Int,
    val totalIncome: Double,
    val totalExpense: Double
) {
    /**
     * 计算净收入（收入 - 支出）
     */
    val netIncome: Double get() = totalIncome - totalExpense

    /**
     * 计算储蓄率（收入为0时返回0）
     */
    val savingsRate: Double get() = if (totalIncome > 0) {
        (netIncome / totalIncome) * 100
    } else {
        0.0
    }
}

/**
 * 字段统计模型
 *
 * 某个字段的金额合计
 */
data class FieldStats(
    val fieldId: Long,
    val fieldName: String,
    val fieldColor: String,
    val fieldIcon: String,
    val amount: Double,
    val percentage: Double
)

/**
 * 月度趋势数据点
 */
data class MonthlyTrendPoint(
    val yearMonth: Int,
    val amount: Double
) {
    /**
     * 获取年份
     */
    val year: Int get() = yearMonth / 100

    /**
     * 获取月份
     */
    val month: Int get() = yearMonth % 100

    /**
     * 格式化显示月份
     */
    fun formatMonth(): String = "${month}月"
}

/**
 * 收支记录UI状态
 */
sealed class IncomeExpenseUiState {
    /** 加载中 */
    data object Loading : IncomeExpenseUiState()

    /** 加载成功 */
    data class Success(
        val records: List<MonthlyIncomeExpenseWithField>,
        val stats: IncomeExpenseMonthlyStats,
        val incomeByField: List<FieldStats>,
        val expenseByField: List<FieldStats>
    ) : IncomeExpenseUiState()

    /** 加载失败 */
    data class Error(val message: String) : IncomeExpenseUiState()
}

/**
 * 添加/编辑记录UI状态
 */
data class EditRecordState(
    val id: Long = 0,
    val yearMonth: Int = 0,
    val type: String = IncomeExpenseType.INCOME,
    val fieldId: Long = 0,
    val amount: Double = 0.0,
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
