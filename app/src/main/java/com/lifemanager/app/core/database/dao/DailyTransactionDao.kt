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
     * 获取指定日期范围的交易记录（同步版本，用于导出）
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    suspend fun getTransactionsBetweenDatesSync(startDate: Int, endDate: Int): List<DailyTransactionEntity>

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
     * 同步获取指定日期的交易记录（用于AI服务）
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date = :date
        ORDER BY createdAt DESC
    """)
    fun getByDateSync(date: Int): List<DailyTransactionEntity>

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

    // ==================== 高级搜索 ====================

    /**
     * 高级搜索 - 多条件过滤
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:accountId IS NULL OR accountId = :accountId)
        AND (:keyword IS NULL OR note LIKE '%' || :keyword || '%')
        AND (:source IS NULL OR source = :source)
        AND (:hasAttachments IS NULL OR
            (CASE WHEN :hasAttachments = 1 THEN attachments != '[]' ELSE attachments = '[]' END))
        ORDER BY
            CASE WHEN :sortBy = 'DATE_DESC' THEN date END DESC,
            CASE WHEN :sortBy = 'DATE_ASC' THEN date END ASC,
            CASE WHEN :sortBy = 'AMOUNT_DESC' THEN amount END DESC,
            CASE WHEN :sortBy = 'AMOUNT_ASC' THEN amount END ASC,
            createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun advancedSearch(
        startDate: Int?,
        endDate: Int?,
        minAmount: Double?,
        maxAmount: Double?,
        type: String?,
        categoryId: Long?,
        accountId: Long?,
        keyword: String?,
        source: String?,
        hasAttachments: Boolean?,
        sortBy: String = "DATE_DESC",
        limit: Int = 50,
        offset: Int = 0
    ): List<DailyTransactionEntity>

    /**
     * 高级搜索计数
     */
    @Query("""
        SELECT COUNT(*) FROM daily_transactions
        WHERE (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:accountId IS NULL OR accountId = :accountId)
        AND (:keyword IS NULL OR note LIKE '%' || :keyword || '%')
        AND (:source IS NULL OR source = :source)
    """)
    suspend fun advancedSearchCount(
        startDate: Int?,
        endDate: Int?,
        minAmount: Double?,
        maxAmount: Double?,
        type: String?,
        categoryId: Long?,
        accountId: Long?,
        keyword: String?,
        source: String?
    ): Int

    /**
     * 按分类ID列表筛选
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE categoryId IN (:categoryIds)
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    suspend fun getByCategoryIds(categoryIds: List<Long>, startDate: Int, endDate: Int): List<DailyTransactionEntity>

    /**
     * 按账户ID列表筛选
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE accountId IN (:accountIds)
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    suspend fun getByAccountIds(accountIds: List<Long>, startDate: Int, endDate: Int): List<DailyTransactionEntity>

    /**
     * 按金额范围筛选
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE amount BETWEEN :minAmount AND :maxAmount
        AND date BETWEEN :startDate AND :endDate
        ORDER BY amount DESC
    """)
    suspend fun getByAmountRange(minAmount: Double, maxAmount: Double, startDate: Int, endDate: Int): List<DailyTransactionEntity>

    /**
     * 获取有附件的交易
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE attachments != '[]'
        ORDER BY date DESC
        LIMIT :limit
    """)
    suspend fun getTransactionsWithAttachments(limit: Int = 100): List<DailyTransactionEntity>

    /**
     * 按标签搜索
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE tags LIKE '%' || :tag || '%'
        ORDER BY date DESC
    """)
    suspend fun getByTag(tag: String): List<DailyTransactionEntity>

    /**
     * 获取统计汇总
     */
    @Query("""
        SELECT
            COUNT(*) as count,
            SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as totalExpense,
            AVG(CASE WHEN type = 'EXPENSE' THEN amount END) as avgExpense
        FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getStatsSummary(startDate: Int, endDate: Int): TransactionStatsSummary

    /**
     * 获取每日消费趋势
     */
    @Query("""
        SELECT date, type, SUM(amount) as amount
        FROM daily_transactions
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date, type
        ORDER BY date
    """)
    suspend fun getDailyTrend(startDate: Int, endDate: Int): List<DailyTrend>

    /**
     * 获取消费最高的日期
     */
    @Query("""
        SELECT date, SUM(amount) as total
        FROM daily_transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getTopSpendingDays(startDate: Int, endDate: Int, limit: Int = 5): List<DateTotal>

    /**
     * 获取消费最高的分类
     */
    @Query("""
        SELECT categoryId, SUM(amount) as total, COUNT(*) as count
        FROM daily_transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate
        GROUP BY categoryId
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getTopCategories(startDate: Int, endDate: Int, limit: Int = 10): List<CategoryStats>

    // ==================== 重复检测 ====================

    /**
     * 查找潜在的重复交易
     * 检查同一天、相同类型、相同金额、相同分类的交易
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date = :date
        AND type = :type
        AND amount = :amount
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY createdAt DESC
        LIMIT 5
    """)
    suspend fun findPotentialDuplicates(
        date: Int,
        type: String,
        amount: Double,
        categoryId: Long?
    ): List<DailyTransactionEntity>

    /**
     * 查找时间窗口内的重复交易（更严格）
     * 检查同一天、相同类型、相同金额，且创建时间在指定时间窗口内
     */
    @Query("""
        SELECT * FROM daily_transactions
        WHERE date = :date
        AND type = :type
        AND amount = :amount
        AND createdAt >= :minCreatedAt
        AND createdAt <= :maxCreatedAt
        ORDER BY createdAt DESC
    """)
    suspend fun findDuplicatesInTimeWindow(
        date: Int,
        type: String,
        amount: Double,
        minCreatedAt: Long,
        maxCreatedAt: Long
    ): List<DailyTransactionEntity>
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

/**
 * 交易统计汇总
 */
data class TransactionStatsSummary(
    val count: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val avgExpense: Double?
)

/**
 * 每日趋势
 */
data class DailyTrend(
    val date: Int,
    val type: String,
    val amount: Double
)

/**
 * 分类统计
 */
data class CategoryStats(
    val categoryId: Long?,
    val total: Double,
    val count: Int
)
