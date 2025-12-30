package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * 目标DAO接口
 *
 * 提供对goals表的数据库操作
 */
@Dao
interface GoalDao {

    /**
     * 获取所有活跃目标，按结束日期排序
     */
    @Query("""
        SELECT * FROM goals
        WHERE status = 'ACTIVE'
        ORDER BY
            CASE WHEN endDate IS NULL THEN 1 ELSE 0 END,
            endDate ASC
    """)
    fun getActiveGoals(): Flow<List<GoalEntity>>

    /**
     * 根据目标类型获取活跃目标
     */
    @Query("""
        SELECT * FROM goals
        WHERE goalType = :type AND status = 'ACTIVE'
        ORDER BY endDate ASC
    """)
    fun getGoalsByType(type: String): Flow<List<GoalEntity>>

    /**
     * 根据分类获取活跃目标
     */
    @Query("""
        SELECT * FROM goals
        WHERE category = :category AND status = 'ACTIVE'
        ORDER BY endDate ASC
    """)
    fun getGoalsByCategory(category: String): Flow<List<GoalEntity>>

    /**
     * 根据ID获取目标
     */
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    /**
     * 根据ID获取目标（Flow版本）
     */
    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalByIdFlow(id: Long): Flow<GoalEntity?>

    /**
     * 获取关联指定财务字段的目标
     */
    @Query("SELECT * FROM goals WHERE linkedFieldId = :fieldId AND status = 'ACTIVE'")
    suspend fun getGoalsByLinkedField(fieldId: Long): List<GoalEntity>

    /**
     * 获取所有目标（包括已完成的），按状态和日期排序
     */
    @Query("""
        SELECT * FROM goals
        ORDER BY
            CASE status
                WHEN 'ACTIVE' THEN 0
                WHEN 'COMPLETED' THEN 1
                WHEN 'ABANDONED' THEN 2
                WHEN 'ARCHIVED' THEN 3
            END,
            updatedAt DESC
    """)
    fun getAllGoals(): Flow<List<GoalEntity>>

    /**
     * 插入目标
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    /**
     * 更新目标
     */
    @Update
    suspend fun update(goal: GoalEntity)

    /**
     * 更新目标进度
     */
    @Query("""
        UPDATE goals
        SET currentValue = :value, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateProgress(id: Long, value: Double, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新目标状态
     */
    @Query("""
        UPDATE goals
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 放弃目标（带原因）
     */
    @Query("""
        UPDATE goals
        SET status = 'ABANDONED',
            abandonReason = :reason,
            abandonedAt = :abandonedAt,
            updatedAt = :abandonedAt
        WHERE id = :id
    """)
    suspend fun abandonGoal(id: Long, reason: String, abandonedAt: Long = System.currentTimeMillis())

    /**
     * 完成目标
     */
    @Query("""
        UPDATE goals
        SET status = 'COMPLETED',
            completedAt = :completedAt,
            updatedAt = :completedAt
        WHERE id = :id
    """)
    suspend fun completeGoal(id: Long, completedAt: Long = System.currentTimeMillis())

    /**
     * 删除目标
     */
    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * 删除目标及其所有子目标
     */
    @Query("DELETE FROM goals WHERE id = :id OR parentId = :id")
    suspend fun deleteWithChildren(id: Long)

    /**
     * 统计活跃目标数量
     */
    @Query("SELECT COUNT(*) FROM goals WHERE status = 'ACTIVE'")
    suspend fun countActiveGoals(): Int

    /**
     * 获取日期范围内的目标
     */
    @Query("""
        SELECT * FROM goals
        WHERE status = 'ACTIVE'
        AND startDate <= :endDate
        AND (endDate IS NULL OR endDate >= :startDate)
    """)
    fun getGoalsInDateRange(startDate: Int, endDate: Int): Flow<List<GoalEntity>>

    /**
     * 获取顶级目标（parentId为null）
     */
    @Query("""
        SELECT * FROM goals
        WHERE parentId IS NULL AND status = 'ACTIVE'
        ORDER BY
            CASE WHEN endDate IS NULL THEN 1 ELSE 0 END,
            endDate ASC
    """)
    fun getTopLevelGoals(): Flow<List<GoalEntity>>

    /**
     * 获取子目标
     */
    @Query("""
        SELECT * FROM goals
        WHERE parentId = :parentId
        ORDER BY createdAt ASC
    """)
    fun getChildGoals(parentId: Long): Flow<List<GoalEntity>>

    /**
     * 获取子目标（同步版本）
     */
    @Query("""
        SELECT * FROM goals
        WHERE parentId = :parentId
        ORDER BY createdAt ASC
    """)
    suspend fun getChildGoalsSync(parentId: Long): List<GoalEntity>

    /**
     * 统计子目标数量
     */
    @Query("SELECT COUNT(*) FROM goals WHERE parentId = :parentId")
    suspend fun countChildGoals(parentId: Long): Int

    /**
     * 统计已完成的子目标数量
     */
    @Query("SELECT COUNT(*) FROM goals WHERE parentId = :parentId AND status = 'COMPLETED'")
    suspend fun countCompletedChildGoals(parentId: Long): Int

    /**
     * 获取所有活跃目标（同步版本，用于AI分析）
     */
    @Query("""
        SELECT * FROM goals
        WHERE status = 'ACTIVE'
        ORDER BY updatedAt DESC
    """)
    suspend fun getActiveGoalsSync(): List<GoalEntity>
}
