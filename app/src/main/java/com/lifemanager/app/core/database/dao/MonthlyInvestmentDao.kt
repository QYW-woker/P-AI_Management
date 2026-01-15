package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.MonthlyInvestmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * 定投字段统计数据类
 */
data class InvestmentFieldTotal(
    val fieldId: Long?,
    val totalBudget: Double,
    val totalActual: Double
)

/**
 * 月度定投DAO接口
 *
 * 提供对monthly_investments表的数据库操作
 */
@Dao
interface MonthlyInvestmentDao {

    /**
     * 获取指定月份的所有记录
     */
    @Query("""
        SELECT * FROM monthly_investments
        WHERE yearMonth = :yearMonth
        ORDER BY recordDate DESC
    """)
    fun getByMonth(yearMonth: Int): Flow<List<MonthlyInvestmentEntity>>

    /**
     * 获取指定月份范围的记录
     */
    @Query("""
        SELECT * FROM monthly_investments
        WHERE yearMonth BETWEEN :startMonth AND :endMonth
        ORDER BY yearMonth DESC
    """)
    fun getByRange(startMonth: Int, endMonth: Int): Flow<List<MonthlyInvestmentEntity>>

    /**
     * 获取指定月份的总预算金额
     */
    @Query("""
        SELECT COALESCE(SUM(budgetAmount), 0.0) FROM monthly_investments
        WHERE yearMonth = :yearMonth
    """)
    suspend fun getTotalBudget(yearMonth: Int): Double

    /**
     * 获取指定月份的实际投入总额
     */
    @Query("""
        SELECT COALESCE(SUM(actualAmount), 0.0) FROM monthly_investments
        WHERE yearMonth = :yearMonth
    """)
    suspend fun getTotalActual(yearMonth: Int): Double

    /**
     * 获取指定月份各字段的汇总
     */
    @Query("""
        SELECT fieldId,
               SUM(budgetAmount) as totalBudget,
               SUM(actualAmount) as totalActual
        FROM monthly_investments
        WHERE yearMonth = :yearMonth
        GROUP BY fieldId
    """)
    fun getFieldTotals(yearMonth: Int): Flow<List<InvestmentFieldTotal>>

    /**
     * 获取多个月份各字段的汇总
     */
    @Query("""
        SELECT fieldId,
               SUM(budgetAmount) as totalBudget,
               SUM(actualAmount) as totalActual
        FROM monthly_investments
        WHERE yearMonth BETWEEN :startMonth AND :endMonth
        GROUP BY fieldId
    """)
    fun getFieldTotalsInRange(startMonth: Int, endMonth: Int): Flow<List<InvestmentFieldTotal>>

    /**
     * 根据ID获取记录
     */
    @Query("SELECT * FROM monthly_investments WHERE id = :id")
    suspend fun getById(id: Long): MonthlyInvestmentEntity?

    /**
     * 获取最近的记录
     */
    @Query("""
        SELECT * FROM monthly_investments
        ORDER BY yearMonth DESC, recordDate DESC
        LIMIT :limit
    """)
    fun getRecentRecords(limit: Int = 20): Flow<List<MonthlyInvestmentEntity>>

    /**
     * 插入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MonthlyInvestmentEntity): Long

    /**
     * 批量插入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MonthlyInvestmentEntity>)

    /**
     * 更新记录
     */
    @Update
    suspend fun update(record: MonthlyInvestmentEntity)

    /**
     * 删除记录
     */
    @Delete
    suspend fun delete(record: MonthlyInvestmentEntity)

    /**
     * 根据ID删除记录
     */
    @Query("DELETE FROM monthly_investments WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取有数据的月份列表
     */
    @Query("""
        SELECT DISTINCT yearMonth FROM monthly_investments
        ORDER BY yearMonth DESC
    """)
    fun getAvailableMonths(): Flow<List<Int>>

    /**
     * 统计指定月份的记录数
     */
    @Query("SELECT COUNT(*) FROM monthly_investments WHERE yearMonth = :yearMonth")
    suspend fun countByMonth(yearMonth: Int): Int

    /**
     * 获取指定年份各月份的总预算
     */
    @Query("""
        SELECT yearMonth, SUM(budgetAmount) as total
        FROM monthly_investments
        WHERE yearMonth / 100 = :year
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
    """)
    fun getMonthlyBudgetTotalsByYear(year: Int): Flow<List<MonthTotal>>

    /**
     * 获取指定年份各月份的实际投入
     */
    @Query("""
        SELECT yearMonth, SUM(actualAmount) as total
        FROM monthly_investments
        WHERE yearMonth / 100 = :year
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
    """)
    fun getMonthlyActualTotalsByYear(year: Int): Flow<List<MonthTotal>>
}
