package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

/**
 * 待办事项DAO接口
 *
 * Room数据访问对象，提供待办事项的CRUD操作
 */
@Dao
interface TodoDao {

    /**
     * 获取所有待完成的待办
     */
    @Query("""
        SELECT * FROM todos
        WHERE status = 'PENDING' AND parentId IS NULL
        ORDER BY
            CASE priority
                WHEN 'HIGH' THEN 0
                WHEN 'MEDIUM' THEN 1
                WHEN 'LOW' THEN 2
                ELSE 3
            END,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun getPendingTodos(): Flow<List<TodoEntity>>

    /**
     * 获取今日待办
     */
    @Query("""
        SELECT * FROM todos
        WHERE status = 'PENDING' AND dueDate = :today
        ORDER BY priority DESC
    """)
    fun getTodayTodos(today: Int): Flow<List<TodoEntity>>

    /**
     * 获取逾期待办
     */
    @Query("""
        SELECT * FROM todos
        WHERE status = 'PENDING' AND dueDate < :today
        ORDER BY dueDate ASC
    """)
    fun getOverdueTodos(today: Int): Flow<List<TodoEntity>>

    /**
     * 获取指定目标关联的待办
     */
    @Query("""
        SELECT * FROM todos
        WHERE linkedGoalId = :goalId
        ORDER BY status ASC, dueDate ASC
    """)
    fun getTodosByGoal(goalId: Long): Flow<List<TodoEntity>>

    /**
     * 获取子任务
     */
    @Query("""
        SELECT * FROM todos
        WHERE parentId = :parentId
        ORDER BY status ASC, createdAt ASC
    """)
    fun getSubTodos(parentId: Long): Flow<List<TodoEntity>>

    /**
     * 根据四象限获取待办
     */
    @Query("""
        SELECT * FROM todos
        WHERE status = 'PENDING' AND quadrant = :quadrant
        ORDER BY dueDate ASC
    """)
    fun getTodosByQuadrant(quadrant: String): Flow<List<TodoEntity>>

    /**
     * 获取已完成的待办
     */
    @Query("""
        SELECT * FROM todos
        WHERE status = 'COMPLETED'
        ORDER BY completedAt DESC
        LIMIT :limit
    """)
    fun getCompletedTodos(limit: Int = 100): Flow<List<TodoEntity>>

    /**
     * 根据ID获取待办
     */
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    /**
     * 获取需要提醒的待办
     */
    @Query("""
        SELECT * FROM todos
        WHERE status = 'PENDING' AND reminderAt IS NOT NULL AND reminderAt > :now
        ORDER BY reminderAt ASC
    """)
    fun getUpcomingReminders(now: Long): Flow<List<TodoEntity>>

    /**
     * 插入待办
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    /**
     * 更新待办
     */
    @Update
    suspend fun update(todo: TodoEntity)

    /**
     * 标记待办完成
     */
    @Query("""
        UPDATE todos
        SET status = 'COMPLETED', completedAt = :completedAt, updatedAt = :completedAt
        WHERE id = :id
    """)
    suspend fun markCompleted(id: Long, completedAt: Long = System.currentTimeMillis())

    /**
     * 标记待办未完成
     */
    @Query("""
        UPDATE todos
        SET status = 'PENDING', completedAt = NULL, updatedAt = :now
        WHERE id = :id
    """)
    suspend fun markPending(id: Long, now: Long = System.currentTimeMillis())

    /**
     * 删除待办
     */
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 删除待办及其子任务
     */
    @Query("DELETE FROM todos WHERE id = :id OR parentId = :id")
    suspend fun deleteWithSubTodos(id: Long)

    /**
     * 批量删除待办
     */
    @Query("DELETE FROM todos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * 统计今日待办完成情况
     */
    @Query("""
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed
        FROM todos
        WHERE dueDate = :today
    """)
    suspend fun getTodayStats(today: Int): TodoStats

    /**
     * 统计待完成数量
     */
    @Query("SELECT COUNT(*) FROM todos WHERE status = 'PENDING'")
    suspend fun countPending(): Int

    /**
     * 获取指定日期的待办列表
     */
    @Query("""
        SELECT * FROM todos
        WHERE dueDate = :epochDay
        ORDER BY status ASC, priority DESC
    """)
    suspend fun getTodosByDate(epochDay: Int): List<TodoEntity>

    /**
     * 获取日期范围内每天的待办数量
     */
    @Query("""
        SELECT dueDate as date, COUNT(*) as count
        FROM todos
        WHERE dueDate >= :startDate AND dueDate <= :endDate
        GROUP BY dueDate
    """)
    suspend fun getTodoCountByDateRangeRaw(startDate: Int, endDate: Int): List<DateTodoCount>
}

/**
 * 待办统计数据类
 */
data class TodoStats(
    val total: Int,
    val completed: Int
)

/**
 * 日期待办数量
 */
data class DateTodoCount(
    val date: Int,
    val count: Int
)

/**
 * 扩展函数：将列表转换为Map
 */
suspend fun TodoDao.getTodoCountByDateRange(startDate: Int, endDate: Int): Map<Int, Int> {
    return getTodoCountByDateRangeRaw(startDate, endDate).associate { it.date to it.count }
}
