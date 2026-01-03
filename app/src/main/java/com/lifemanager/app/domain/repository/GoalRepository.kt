package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 目标仓库接口
 */
interface GoalRepository {
    fun getActiveGoals(): Flow<List<GoalEntity>>
    fun getAllGoals(): Flow<List<GoalEntity>>
    fun getGoalsByType(type: String): Flow<List<GoalEntity>>
    fun getGoalsByCategory(category: String): Flow<List<GoalEntity>>
    suspend fun getGoalById(id: Long): GoalEntity?
    suspend fun insert(goal: GoalEntity): Long
    suspend fun update(goal: GoalEntity)
    suspend fun updateProgress(id: Long, value: Double)
    suspend fun updateStatus(id: Long, status: String)
    suspend fun delete(id: Long)
    suspend fun countActiveGoals(): Int

    // ============ 多级目标相关 ============

    /**
     * 获取顶级目标
     */
    fun getTopLevelGoals(): Flow<List<GoalEntity>>

    /**
     * 获取所有顶级目标（包含所有状态）
     */
    fun getAllTopLevelGoals(): Flow<List<GoalEntity>>

    /**
     * 获取子目标
     */
    fun getChildGoals(parentId: Long): Flow<List<GoalEntity>>

    /**
     * 获取子目标（同步版本）
     */
    suspend fun getChildGoalsSync(parentId: Long): List<GoalEntity>

    /**
     * 统计子目标数量
     */
    suspend fun countChildGoals(parentId: Long): Int

    /**
     * 统计已完成的子目标数量
     */
    suspend fun countCompletedChildGoals(parentId: Long): Int

    /**
     * 更新多级目标标记
     */
    suspend fun updateMultiLevelFlag(id: Long, isMultiLevel: Boolean)

    /**
     * 删除子目标
     */
    suspend fun deleteChildGoals(parentId: Long)

    /**
     * 删除目标及其所有子目标
     */
    suspend fun deleteWithChildren(id: Long)

    // ============ 进度记录相关 ============

    /**
     * 获取目标的进度记录列表
     */
    suspend fun getProgressRecords(goalId: Long): List<GoalRecordEntity>

    /**
     * 插入进度记录
     */
    suspend fun insertProgressRecord(record: GoalRecordEntity): Long
}
