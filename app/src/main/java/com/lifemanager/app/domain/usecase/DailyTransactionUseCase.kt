package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.domain.repository.CustomFieldRepository
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * 日常记账用例
 */
class DailyTransactionUseCase @Inject constructor(
    private val transactionRepository: DailyTransactionRepository,
    private val fieldRepository: CustomFieldRepository
) {

    /**
     * 获取指定日期的交易（带分类信息）
     */
    fun getTransactionsByDate(date: Int): Flow<List<DailyTransactionWithCategory>> {
        return combine(
            transactionRepository.getByDate(date),
            fieldRepository.getFieldsByModule("DAILY_EXPENSE")
        ) { transactions, fields ->
            val fieldMap = fields.associateBy { it.id }
            transactions.map { transaction ->
                DailyTransactionWithCategory(
                    transaction = transaction,
                    category = transaction.categoryId?.let { fieldMap[it] }
                )
            }
        }
    }

    /**
     * 获取指定日期范围的交易（按日期分组）
     */
    fun getTransactionGroupsByDateRange(startDate: Int, endDate: Int): Flow<List<DailyTransactionGroup>> {
        return combine(
            transactionRepository.getByDateRange(startDate, endDate),
            fieldRepository.getFieldsByModule("DAILY_EXPENSE")
        ) { transactions, fields ->
            val fieldMap = fields.associateBy { it.id }

            transactions
                .groupBy { it.date }
                .map { (date, dayTransactions) ->
                    val localDate = LocalDate.ofEpochDay(date.toLong())
                    val withCategory = dayTransactions.map { transaction ->
                        DailyTransactionWithCategory(
                            transaction = transaction,
                            category = transaction.categoryId?.let { fieldMap[it] }
                        )
                    }

                    DailyTransactionGroup(
                        date = date,
                        dateText = formatDate(localDate),
                        dayOfWeek = localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA),
                        transactions = withCategory,
                        totalIncome = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        totalExpense = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    /**
     * 获取最近交易（按日期分组）
     */
    fun getRecentTransactionGroups(limit: Int = 50): Flow<List<DailyTransactionGroup>> {
        return combine(
            transactionRepository.getRecentTransactions(limit),
            fieldRepository.getFieldsByModule("DAILY_EXPENSE")
        ) { transactions, fields ->
            val fieldMap = fields.associateBy { it.id }

            transactions
                .groupBy { it.date }
                .map { (date, dayTransactions) ->
                    val localDate = LocalDate.ofEpochDay(date.toLong())
                    val withCategory = dayTransactions.map { transaction ->
                        DailyTransactionWithCategory(
                            transaction = transaction,
                            category = transaction.categoryId?.let { fieldMap[it] }
                        )
                    }

                    DailyTransactionGroup(
                        date = date,
                        dateText = formatDate(localDate),
                        dayOfWeek = localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA),
                        transactions = withCategory,
                        totalIncome = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        totalExpense = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    /**
     * 获取日期范围内的统计
     */
    suspend fun getPeriodStats(startDate: Int, endDate: Int): PeriodStats {
        val income = transactionRepository.getTotalByTypeInRange(startDate, endDate, TransactionType.INCOME)
        val expense = transactionRepository.getTotalByTypeInRange(startDate, endDate, TransactionType.EXPENSE)
        val count = transactionRepository.countInRange(startDate, endDate)
        val days = endDate - startDate + 1

        return PeriodStats(
            startDate = startDate,
            endDate = endDate,
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            transactionCount = count,
            avgDailyExpense = if (days > 0) expense / days else 0.0
        )
    }

    /**
     * 获取本月统计
     */
    suspend fun getCurrentMonthStats(): PeriodStats {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())

        return getPeriodStats(
            startOfMonth.toEpochDay().toInt(),
            endOfMonth.toEpochDay().toInt()
        )
    }

    /**
     * 获取今日统计
     */
    suspend fun getTodayStats(): DailyStats {
        val today = LocalDate.now().toEpochDay().toInt()
        val income = transactionRepository.getTotalByTypeInRange(today, today, TransactionType.INCOME)
        val expense = transactionRepository.getTotalByTypeInRange(today, today, TransactionType.EXPENSE)
        val count = transactionRepository.countInRange(today, today)

        return DailyStats(
            date = today,
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            transactionCount = count
        )
    }

    /**
     * 获取分类支出统计
     */
    fun getCategoryExpenseStats(startDate: Int, endDate: Int): Flow<List<CategoryExpenseStats>> {
        return combine(
            transactionRepository.getCategoryTotalsInRange(startDate, endDate, TransactionType.EXPENSE),
            fieldRepository.getFieldsByModule("DAILY_EXPENSE")
        ) { totals, fields ->
            val fieldMap = fields.associateBy { it.id }
            val totalExpense = totals.sumOf { it.total }

            totals.map { total ->
                val field = total.fieldId?.let { fieldMap[it] }
                CategoryExpenseStats(
                    categoryId = total.fieldId,
                    categoryName = field?.name ?: "未分类",
                    categoryColor = field?.color ?: "#9E9E9E",
                    totalAmount = total.total,
                    percentage = if (totalExpense > 0) total.total / totalExpense * 100 else 0.0,
                    transactionCount = 0
                )
            }.sortedByDescending { it.totalAmount }
        }
    }

    /**
     * 获取日历视图的每日支出数据
     */
    fun getCalendarExpenseData(yearMonth: Int): Flow<Map<Int, Double>> {
        val year = yearMonth / 100
        val month = yearMonth % 100
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

        return transactionRepository.getDailyExpenseTotals(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        ).map { totals ->
            totals.associate { it.date to it.total }
        }
    }

    /**
     * 添加交易
     */
    suspend fun addTransaction(
        type: String,
        amount: Double,
        categoryId: Long?,
        date: Int,
        time: String = "",
        note: String = "",
        source: String = "MANUAL"
    ): Long {
        val transaction = DailyTransactionEntity(
            type = type,
            amount = amount,
            categoryId = categoryId,
            date = date,
            time = time,
            note = note,
            source = source
        )
        return transactionRepository.insert(transaction)
    }

    /**
     * 更新交易
     */
    suspend fun updateTransaction(
        id: Long,
        type: String,
        amount: Double,
        categoryId: Long?,
        date: Int,
        time: String = "",
        note: String = ""
    ) {
        val existing = transactionRepository.getById(id) ?: return
        val updated = existing.copy(
            type = type,
            amount = amount,
            categoryId = categoryId,
            date = date,
            time = time,
            note = note
        )
        transactionRepository.update(updated)
    }

    /**
     * 删除交易
     */
    suspend fun deleteTransaction(id: Long) {
        transactionRepository.deleteById(id)
    }

    /**
     * 批量删除交易
     */
    suspend fun deleteTransactions(ids: List<Long>) {
        transactionRepository.deleteByIds(ids)
    }

    /**
     * 获取交易详情
     */
    suspend fun getTransactionById(id: Long): DailyTransactionWithCategory? {
        val transaction = transactionRepository.getById(id) ?: return null
        val fields = fieldRepository.getFieldsByModule("DAILY_EXPENSE").first()
        val category = transaction.categoryId?.let { catId ->
            fields.find { it.id == catId }
        }
        return DailyTransactionWithCategory(transaction, category)
    }

    /**
     * 搜索交易
     */
    fun searchTransactions(keyword: String): Flow<List<DailyTransactionWithCategory>> {
        return combine(
            transactionRepository.searchByNote(keyword),
            fieldRepository.getFieldsByModule("DAILY_EXPENSE")
        ) { transactions, fields ->
            val fieldMap = fields.associateBy { it.id }
            transactions.map { transaction ->
                DailyTransactionWithCategory(
                    transaction = transaction,
                    category = transaction.categoryId?.let { fieldMap[it] }
                )
            }
        }
    }

    /**
     * 格式化日期
     */
    private fun formatDate(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date == today -> "今天"
            date == today.minusDays(1) -> "昨天"
            date == today.minusDays(2) -> "前天"
            date.year == today.year -> "${date.monthValue}月${date.dayOfMonth}日"
            else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
        }
    }
}
