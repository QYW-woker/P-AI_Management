package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 存钱记录DAO接口
 */
@Dao
interface SavingsRecordDao {

    /**
     * 获取指定计划的所有记录
     */
    @Query("""
        SELECT * FROM savings_records
        WHERE planId = :planId
        ORDER BY date DESC, createdAt DESC
    """)
    fun getByPlan(planId: Long): Flow<List<SavingsRecordEntity>>

    /**
     * 获取指定计划指定日期范围的记录
     */
    @Query("""
        SELECT * FROM savings_records
        WHERE planId = :planId AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    fun getByPlanAndDateRange(planId: Long, startDate: Int, endDate: Int): Flow<List<SavingsRecordEntity>>

    /**
     * 获取指定计划的净存款金额（存款-取款）
     * 存款为正，取款记录的amount存储为正数但type为WITHDRAWAL
     */
    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE -amount END),
            0.0
        ) FROM savings_records WHERE planId = :planId
    """)
    suspend fun getNetTotalByPlan(planId: Long): Double

    /**
     * 获取指定计划的总存款金额（仅存款）
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM savings_records WHERE planId = :planId AND type = 'DEPOSIT'")
    suspend fun getTotalDepositsByPlan(planId: Long): Double

    /**
     * 获取指定计划的总取款金额
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM savings_records WHERE planId = :planId AND type = 'WITHDRAWAL'")
    suspend fun getTotalWithdrawalsByPlan(planId: Long): Double

    /**
     * 获取指定计划的总存款金额（兼容旧版本）
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM savings_records WHERE planId = :planId")
    suspend fun getTotalByPlan(planId: Long): Double

    /**
     * 根据ID获取记录
     */
    @Query("SELECT * FROM savings_records WHERE id = :id")
    suspend fun getById(id: Long): SavingsRecordEntity?

    /**
     * 插入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SavingsRecordEntity): Long

    /**
     * 更新记录
     */
    @Update
    suspend fun update(record: SavingsRecordEntity)

    /**
     * 删除记录
     */
    @Query("DELETE FROM savings_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 删除计划的所有记录
     */
    @Query("DELETE FROM savings_records WHERE planId = :planId")
    suspend fun deleteByPlan(planId: Long)

    /**
     * 获取最近的存款记录
     */
    @Query("""
        SELECT * FROM savings_records
        ORDER BY date DESC, createdAt DESC
        LIMIT :limit
    """)
    fun getRecentRecords(limit: Int = 20): Flow<List<SavingsRecordEntity>>

    /**
     * 统计指定计划的记录数
     */
    @Query("SELECT COUNT(*) FROM savings_records WHERE planId = :planId")
    suspend fun countByPlan(planId: Long): Int

    /**
     * 统计指定计划的存款次数
     */
    @Query("SELECT COUNT(*) FROM savings_records WHERE planId = :planId AND type = 'DEPOSIT'")
    suspend fun countDepositsByPlan(planId: Long): Int

    /**
     * 统计指定计划的取款次数
     */
    @Query("SELECT COUNT(*) FROM savings_records WHERE planId = :planId AND type = 'WITHDRAWAL'")
    suspend fun countWithdrawalsByPlan(planId: Long): Int

    /**
     * 获取本月存款总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM savings_records
        WHERE date >= :startOfMonth AND type = 'DEPOSIT'
    """)
    suspend fun getThisMonthDeposits(startOfMonth: Int): Double

    /**
     * 获取上月存款总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM savings_records
        WHERE date >= :startOfLastMonth AND date < :startOfThisMonth AND type = 'DEPOSIT'
    """)
    suspend fun getLastMonthDeposits(startOfLastMonth: Int, startOfThisMonth: Int): Double

    /**
     * 获取所有存款的日期列表（去重，用于计算连续存款天数）
     */
    @Query("""
        SELECT DISTINCT date FROM savings_records
        WHERE type = 'DEPOSIT'
        ORDER BY date DESC
    """)
    suspend fun getAllDepositDates(): List<Int>

    /**
     * 获取最近一次存款的日期
     */
    @Query("""
        SELECT MAX(date) FROM savings_records
        WHERE type = 'DEPOSIT'
    """)
    suspend fun getLastDepositDate(): Int?

    /**
     * 获取指定日期范围内有存款的天数
     */
    @Query("""
        SELECT COUNT(DISTINCT date) FROM savings_records
        WHERE type = 'DEPOSIT' AND date >= :startDate AND date <= :endDate
    """)
    suspend fun countDepositDaysInRange(startDate: Int, endDate: Int): Int

    /**
     * 获取本周存款总额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM savings_records
        WHERE date >= :startOfWeek AND type = 'DEPOSIT'
    """)
    suspend fun getThisWeekDeposits(startOfWeek: Int): Double

    /**
     * 获取总存款天数
     */
    @Query("""
        SELECT COUNT(DISTINCT date) FROM savings_records
        WHERE type = 'DEPOSIT'
    """)
    suspend fun getTotalDepositDays(): Int
}
