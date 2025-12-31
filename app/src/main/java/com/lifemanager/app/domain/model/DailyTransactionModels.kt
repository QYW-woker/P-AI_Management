package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.DailyTransactionEntity

/**
 * 带分类信息的日常交易
 */
data class DailyTransactionWithCategory(
    val transaction: DailyTransactionEntity,
    val category: CustomFieldEntity?
)

/**
 * 日常记账UI状态
 */
sealed class TransactionUiState {
    object Loading : TransactionUiState()
    object Success : TransactionUiState()
    data class Error(val message: String) : TransactionUiState()
}

/**
 * 日期分组的交易列表
 */
data class DailyTransactionGroup(
    val date: Int,
    val dateText: String,
    val dayOfWeek: String,
    val transactions: List<DailyTransactionWithCategory>,
    val totalIncome: Double,
    val totalExpense: Double
)

/**
 * 日统计数据
 */
data class DailyStats(
    val date: Int,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0
)

/**
 * 周/月统计数据
 */
data class PeriodStats(
    val startDate: Int,
    val endDate: Int,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0,
    val avgDailyExpense: Double = 0.0
)

/**
 * 分类支出统计
 */
data class CategoryExpenseStats(
    val categoryId: Long?,
    val categoryName: String,
    val categoryColor: String,
    val totalAmount: Double,
    val percentage: Double,
    val transactionCount: Int
)

/**
 * 交易编辑状态
 */
data class TransactionEditState(
    val id: Long = 0,
    val isEditing: Boolean = false,
    val type: String = "EXPENSE",
    val amount: Double = 0.0,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val date: Int = 0,
    val time: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 交易类型
 */
object TransactionType {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
}

/**
 * 日历日期项
 */
data class CalendarDayItem(
    val date: Int,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasTransactions: Boolean,
    val totalExpense: Double = 0.0
)

/**
 * 快捷记账模板
 */
data class QuickInputTemplate(
    val id: Long = 0,
    val name: String,
    val type: String,
    val amount: Double,
    val categoryId: Long?,
    val note: String = "",
    val usageCount: Int = 0
)
