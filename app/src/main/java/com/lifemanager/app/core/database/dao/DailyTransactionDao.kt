package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 日常交易DAO接口
 */
@Dao
interface DailyTransactionDao {

    /**
     * 获取指定日期范围的交易记录
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun getByDateRange(startDate: Int, endDate: Int): Flow<List<DailyTransactionEntity>>

    /**
     * 获取指定日期的交易记录
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date = :date
        ORDER BY createdAt DESC
    """)
    fun getByDate(date: Int): Flow<List<DailyTransactionEntity>>

    /**
     * 获取指定日期范围内指定类型的总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate AND type = :type
    """)
    suspend fun getTotalByTypeInRange(startDate: Int, endDate: Int, type: String): Double

    /**
     * 获取指定日期范围内各分类的汇总
     */
    @Query("""
        SELECT categoryId as fieldId, SUM(amount) as total
        FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate AND type = :type
        GROUP BY categoryId
    """)
    fun getCategoryTotalsInRange(startDate: Int, endDate: Int, type: String): Flow<List<FieldTotal>>

    /**
     * 根据ID获取交易
     */
    @Query("SELECT * FROM daily_transactions WHERE id = :id")
    suspend fun getById(id: Long): DailyTransactionEntity?

    /**
     * 获取最近的交易记录
     */
    @Query("""
        SELECT * FROM daily_transactions
        ORDER BY date DESC, createdAt DESC
        LIMIT :limit
    """)
    fun getRecentTransactions(limit: Int = 50): Flow<List<DailyTransactionEntity>>

    /**
     * 搜索交易（按备注）
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE note LIKE '%' || :keyword || '%'
        ORDER BY date DESC
        LIMIT :limit
    """)
    fun searchByNote(keyword: String, limit: Int = 100): Flow<List<DailyTransactionEntity>>

    /**
     * 获取各日期的支出总额（用于日历视图）
     */
    @Query("""
        SELECT date, SUM(amount) as total
        FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate AND type = 'EXPENSE'
        GROUP BY date
    """)
    fun getDailyExpenseTotals(startDate: Int, endDate: Int): Flow<List<DateTotal>>

    /**
     * 插入交易
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: DailyTransactionEntity): Long

    /**
     * 批量插入交易
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<DailyTransactionEntity>)

    /**
     * 更新交易
     */
    @Update
    suspend fun update(transaction: DailyTransactionEntity)

    /**
     * 删除交易
     */
    @Query("DELETE FROM daily_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 批量删除交易
     */
    @Query("DELETE FROM daily_transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * 统计指定日期范围的交易数量
     */
    @Query("""
        SELECT COUNT(*) FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun countInRange(startDate: Int, endDate: Int): Int

    /**
     * 获取今日支出总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM daily_transactions
        WHERE date = :today AND type = 'EXPENSE'
    """)
    suspend fun getTodayExpense(today: Int): Double

    /**
     * 获取指定日期的交易记录（别名）
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date = :date
        ORDER BY createdAt DESC
    """)
    fun getTransactionsByDate(date: Int): Flow<List<DailyTransactionEntity>>

    /**
     * 获取指定日期范围的交易记录（别名）
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsInRange(startDate: Int, endDate: Int): Flow<List<DailyTransactionEntity>>

    /**
     * 获取各日期的收支汇总（用于日历视图）
     */
    @Query("""
        SELECT date,
               SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as income,
               SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as expense
        FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
    """)
    fun getDailyIncomeExpenseTotals(startDate: Int, endDate: Int): Flow<List<DailyIncomeExpense>>

    /**
     * 获取指定日期范围内指定分类的支出总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
        AND categoryId = :categoryId
        AND type = 'EXPENSE'
    """)
    suspend fun getTotalByCategoryInRange(startDate: Int, endDate: Int, categoryId: Long): Double

    /**
     * 获取所有交易记录用于导出
     */
    @Query("""
        SELECT * FROM daily_transactions
        ORDER BY date DESC, createdAt DESC
    """)
    suspend fun getAllForExport(): List<DailyTransactionEntity>

    /**
     * 获取指定日期范围的交易记录用于导出
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    suspend fun getByDateRangeForExport(startDate: Int, endDate: Int): List<DailyTransactionEntity>
}

/**
 * 日期收支数据类
 */
data class DailyIncomeExpense(
    val date: Int,
    val income: Double,
    val expense: Double
)

/**
 * 日期总额数据类
 */
data class DateTotal(
    val date: Int,
    val total: Double
)
