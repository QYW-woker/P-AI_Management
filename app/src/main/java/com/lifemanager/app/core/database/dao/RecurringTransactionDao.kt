package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 周期记账DAO接口
 */
@Dao
interface RecurringTransactionDao {

    /**
     * 获取所有启用的周期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions
        WHERE isEnabled = 1
        ORDER BY nextDueDate ASC
    """)
    fun getEnabledRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    /**
     * 获取所有周期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions
        ORDER BY isEnabled DESC, nextDueDate ASC
    """)
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    /**
     * 获取指定账本的周期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions
        WHERE ledgerId = :ledgerId AND isEnabled = 1
        ORDER BY nextDueDate ASC
    """)
    fun getRecurringTransactionsByLedger(ledgerId: Long): Flow<List<RecurringTransactionEntity>>

    /**
     * 获取到期需要执行的周期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions
        WHERE isEnabled = 1 AND nextDueDate <= :today
        ORDER BY nextDueDate ASC
    """)
    suspend fun getDueTransactions(today: Int): List<RecurringTransactionEntity>

    /**
     * 获取即将到期需要提醒的周期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions
        WHERE isEnabled = 1
        AND autoExecute = 0
        AND nextDueDate <= :reminderDate
        AND nextDueDate > :today
        ORDER BY nextDueDate ASC
    """)
    suspend fun getUpcomingReminders(today: Int, reminderDate: Int): List<RecurringTransactionEntity>

    /**
     * 根据ID获取周期交易
     */
    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    /**
     * 根据ID获取周期交易（Flow版本）
     */
    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<RecurringTransactionEntity?>

    /**
     * 插入周期交易
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransactionEntity): Long

    /**
     * 更新周期交易
     */
    @Update
    suspend fun update(recurringTransaction: RecurringTransactionEntity)

    /**
     * 删除周期交易
     */
    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * 更新下次执行日期和执行计数
     */
    @Query("""
        UPDATE recurring_transactions
        SET nextDueDate = :nextDueDate,
            lastExecutedDate = :lastExecutedDate,
            executedCount = executedCount + 1,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateAfterExecution(
        id: Long,
        nextDueDate: Int,
        lastExecutedDate: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * 启用/禁用周期交易
     */
    @Query("UPDATE recurring_transactions SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * 统计启用的周期交易数量
     */
    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE isEnabled = 1")
    suspend fun countEnabled(): Int

    /**
     * 按类型统计周期交易
     */
    @Query("""
        SELECT type, COUNT(*) as count, SUM(amount) as total
        FROM recurring_transactions
        WHERE isEnabled = 1
        GROUP BY type
    """)
    suspend fun getRecurringSummary(): List<RecurringSummary>
}

/**
 * 周期交易汇总
 */
data class RecurringSummary(
    val type: String,
    val count: Int,
    val total: Double
)
