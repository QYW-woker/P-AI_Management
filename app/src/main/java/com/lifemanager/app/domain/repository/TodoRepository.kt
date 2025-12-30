package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.dao.TodoStats
import com.lifemanager.app.core.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

/**
 * 待办记事仓库接口
 */
interface TodoRepository {

    /**
     * 获取所有待完成的待办
     */
    fun getPendingTodos(): Flow<List<TodoEntity>>

    /**
     * 获取今日待办
     */
    fun getTodayTodos(today: Int): Flow<List<TodoEntity>>

    /**
     * 获取逾期待办
     */
    fun getOverdueTodos(today: Int): Flow<List<TodoEntity>>

    /**
     * 获取指定目标关联的待办
     */
    fun getTodosByGoal(goalId: Long): Flow<List<TodoEntity>>

    /**
     * 获取子任务
     */
    fun getSubTodos(parentId: Long): Flow<List<TodoEntity>>

    /**
     * 根据四象限获取待办
     */
    fun getTodosByQuadrant(quadrant: String): Flow<List<TodoEntity>>

    /**
     * 获取已完成的待办
     */
    fun getCompletedTodos(limit: Int = 100): Flow<List<TodoEntity>>

    /**
     * 根据ID获取待办
     */
    suspend fun getById(id: Long): TodoEntity?

    /**
     * 获取需要提醒的待办
     */
    fun getUpcomingReminders(now: Long): Flow<List<TodoEntity>>

    /**
     * 插入待办
     */
    suspend fun insert(todo: TodoEntity): Long

    /**
     * 更新待办
     */
    suspend fun update(todo: TodoEntity)

    /**
     * 标记待办完成
     */
    suspend fun markCompleted(id: Long)

    /**
     * 标记待办未完成
     */
    suspend fun markPending(id: Long)

    /**
     * 删除待办
     */
    suspend fun deleteById(id: Long)

    /**
     * 删除待办及其子任务
     */
    suspend fun deleteWithSubTodos(id: Long)

    /**
     * 批量删除待办
     */
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * 获取今日统计
     */
    suspend fun getTodayStats(today: Int): TodoStats

    /**
     * 统计待完成数量
     */
    suspend fun countPending(): Int

    /**
     * 获取指定日期的待办列表
     */
    suspend fun getTodosByDate(epochDay: Int): List<TodoEntity>

    /**
     * 获取日期范围内每天的待办数量
     */
    suspend fun getTodoCountByDateRange(startDate: Int, endDate: Int): Map<Int, Int>
}
