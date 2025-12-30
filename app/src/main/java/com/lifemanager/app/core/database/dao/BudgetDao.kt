package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * 预算DAO接口
 */
@Dao
interface BudgetDao {

    /**
     * 获取指定月份的预算
     */
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun getByYearMonth(yearMonth: Int): BudgetEntity?

    /**
     * 获取指定月份的预算（Flow版本）
     */
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun getByYearMonthFlow(yearMonth: Int): Flow<BudgetEntity?>

    /**
     * 获取所有预算记录
     */
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    /**
     * 获取最近N个月的预算
     */
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC LIMIT :limit")
    fun getRecentBudgets(limit: Int): Flow<List<BudgetEntity>>

    /**
     * 获取指定日期范围的预算
     */
    @Query("SELECT * FROM budgets WHERE yearMonth BETWEEN :startYearMonth AND :endYearMonth ORDER BY yearMonth DESC")
    fun getBudgetsByRange(startYearMonth: Int, endYearMonth: Int): Flow<List<BudgetEntity>>

    /**
     * 插入或更新预算（如果已存在相同月份则替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity): Long

    /**
     * 更新预算
     */
    @Update
    suspend fun update(budget: BudgetEntity)

    /**
     * 删除预算
     */
    @Delete
    suspend fun delete(budget: BudgetEntity)

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 检查指定月份是否有预算
     */
    @Query("SELECT EXISTS(SELECT 1 FROM budgets WHERE yearMonth = :yearMonth)")
    suspend fun hasBudget(yearMonth: Int): Boolean

    /**
     * 获取预算总数
     */
    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int

    /**
     * 获取最新的预算记录（用于复制到新月份）
     */
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC LIMIT 1")
    suspend fun getLatestBudget(): BudgetEntity?

    /**
     * 获取所有预算记录（同步版本，用于AI分析）
     */
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC")
    suspend fun getAllSync(): List<BudgetEntity>
}
