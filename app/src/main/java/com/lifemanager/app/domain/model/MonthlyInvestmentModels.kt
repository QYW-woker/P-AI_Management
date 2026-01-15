package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.MonthlyInvestmentEntity

/**
 * 月度定投记录模型（带字段详情）
 *
 * 包含定投记录及其关联的自定义字段信息
 */
data class MonthlyInvestmentWithField(
    val record: MonthlyInvestmentEntity,
    val field: CustomFieldEntity?
)

/**
 * 月度定投统计模型
 *
 * 汇总某月的定投预算和实际投入
 */
data class InvestmentMonthlyStats(
    val yearMonth: Int,
    val totalBudget: Double,
    val totalActual: Double
) {
    /**
     * 计算预算完成率（预算为0时返回0）
     */
    val completionRate: Double get() = if (totalBudget > 0) {
        (totalActual / totalBudget) * 100
    } else {
        0.0
    }

    /**
     * 计算预算差额（实际 - 预算）
     */
    val budgetDiff: Double get() = totalActual - totalBudget

    /**
     * 是否超出预算
     */
    val isOverBudget: Boolean get() = totalActual > totalBudget
}

/**
 * 定投字段统计模型
 *
 * 某个字段的预算和实际金额合计
 */
data class InvestmentFieldStats(
    val fieldId: Long,
    val fieldName: String,
    val fieldColor: String,
    val fieldIcon: String,
    val budgetAmount: Double,
    val actualAmount: Double,
    val percentage: Double
) {
    /**
     * 完成率
     */
    val completionRate: Double get() = if (budgetAmount > 0) {
        (actualAmount / budgetAmount) * 100
    } else {
        0.0
    }
}

/**
 * 定投记录UI状态
 */
sealed class InvestmentUiState {
    /** 加载中 */
    data object Loading : InvestmentUiState()

    /** 加载成功 */
    data class Success(
        val records: List<MonthlyInvestmentWithField>,
        val stats: InvestmentMonthlyStats,
        val fieldStats: List<InvestmentFieldStats>
    ) : InvestmentUiState()

    /** 加载失败 */
    data class Error(val message: String) : InvestmentUiState()
}

/**
 * 添加/编辑定投记录UI状态
 */
data class EditInvestmentState(
    val id: Long = 0,
    val yearMonth: Int = 0,
    val fieldId: Long = 0,
    val budgetAmount: Double = 0.0,
    val actualAmount: Double = 0.0,
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
