package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

/**
 * 待办事项DAO接口
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
}

/**
 * 待办统计数据类
 */
data class TodoStats(
    val total: Int,
    val completed: Int
)
