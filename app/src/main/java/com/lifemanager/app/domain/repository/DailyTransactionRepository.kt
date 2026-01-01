package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.dao.DateTotal
import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 日常记账仓库接口
 */
interface DailyTransactionRepository {

    /**
     * 获取指定日期范围的交易记录
     */
    fun getByDateRange(startDate: Int, endDate: Int): Flow<List<DailyTransactionEntity>>

    /**
     * 获取指定日期的交易记录
     */
    fun getByDate(date: Int): Flow<List<DailyTransactionEntity>>

    /**
     * 获取最近的交易记录
     */
    fun getRecentTransactions(limit: Int = 50): Flow<List<DailyTransactionEntity>>

    /**
     * 获取指定日期范围内指定类型的总额
     */
    suspend fun getTotalByTypeInRange(startDate: Int, endDate: Int, type: String): Double

    /**
     * 获取指定日期范围内各分类的汇总
     */
    fun getCategoryTotalsInRange(startDate: Int, endDate: Int, type: String): Flow<List<FieldTotal>>

    /**
     * 获取各日期的支出总额（用于日历视图）
     */
    fun getDailyExpenseTotals(startDate: Int, endDate: Int): Flow<List<DateTotal>>

    /**
     * 根据ID获取交易
     */
    suspend fun getById(id: Long): DailyTransactionEntity?

    /**
     * 搜索交易（按备注）
     */
    fun searchByNote(keyword: String, limit: Int = 100): Flow<List<DailyTransactionEntity>>

    /**
     * 插入交易
     */
    suspend fun insert(transaction: DailyTransactionEntity): Long

    /**
     * 批量插入交易
     */
    suspend fun insertAll(transactions: List<DailyTransactionEntity>)

    /**
     * 更新交易
     */
    suspend fun update(transaction: DailyTransactionEntity)

    /**
     * 删除交易
     */
    suspend fun deleteById(id: Long)

    /**
     * 批量删除交易
     */
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * 获取今日支出总额
     */
    suspend fun getTodayExpense(today: Int): Double

    /**
     * 统计指定日期范围的交易数量
     */
    suspend fun countInRange(startDate: Int, endDate: Int): Int

    /**
     * 获取指定日期范围内指定分类的支出总额
     */
    suspend fun getTotalByCategoryInRange(startDate: Int, endDate: Int, categoryId: Long): Double

    /**
     * 查找潜在的重复交易
     */
    suspend fun findPotentialDuplicates(
        date: Int,
        type: String,
        amount: Double,
        categoryId: Long?
    ): List<DailyTransactionEntity>

    /**
     * 查找时间窗口内的重复交易
     */
    suspend fun findDuplicatesInTimeWindow(
        date: Int,
        type: String,
        amount: Double,
        timeWindowMinutes: Int = 5
    ): List<DailyTransactionEntity>

    /**
     * 获取指定账户的交易记录
     */
    suspend fun getByAccountId(accountId: Long, startDate: Int, endDate: Int): List<DailyTransactionEntity>
}
