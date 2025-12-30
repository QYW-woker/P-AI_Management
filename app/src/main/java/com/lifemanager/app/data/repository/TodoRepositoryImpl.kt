package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.TodoDao
import com.lifemanager.app.core.database.dao.TodoStats
import com.lifemanager.app.core.database.dao.getTodoCountByDateRange
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 待办记事仓库实现
 */
@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val dao: TodoDao
) : TodoRepository {

    override fun getPendingTodos(): Flow<List<TodoEntity>> {
        return dao.getPendingTodos()
    }

    override fun getTodayTodos(today: Int): Flow<List<TodoEntity>> {
        return dao.getTodayTodos(today)
    }

    override fun getOverdueTodos(today: Int): Flow<List<TodoEntity>> {
        return dao.getOverdueTodos(today)
    }

    override fun getTodosByGoal(goalId: Long): Flow<List<TodoEntity>> {
        return dao.getTodosByGoal(goalId)
    }

    override fun getSubTodos(parentId: Long): Flow<List<TodoEntity>> {
        return dao.getSubTodos(parentId)
    }

    override fun getTodosByQuadrant(quadrant: String): Flow<List<TodoEntity>> {
        return dao.getTodosByQuadrant(quadrant)
    }

    override fun getCompletedTodos(limit: Int): Flow<List<TodoEntity>> {
        return dao.getCompletedTodos(limit)
    }

    override suspend fun getById(id: Long): TodoEntity? {
        return dao.getById(id)
    }

    override fun getUpcomingReminders(now: Long): Flow<List<TodoEntity>> {
        return dao.getUpcomingReminders(now)
    }

    override suspend fun insert(todo: TodoEntity): Long {
        return dao.insert(todo)
    }

    override suspend fun update(todo: TodoEntity) {
        dao.update(todo)
    }

    override suspend fun markCompleted(id: Long) {
        dao.markCompleted(id)
    }

    override suspend fun markPending(id: Long) {
        dao.markPending(id)
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteWithSubTodos(id: Long) {
        dao.deleteWithSubTodos(id)
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        dao.deleteByIds(ids)
    }

    override suspend fun getTodayStats(today: Int): TodoStats {
        return dao.getTodayStats(today)
    }

    override suspend fun countPending(): Int {
        return dao.countPending()
    }

    override suspend fun getTodosByDate(epochDay: Int): List<TodoEntity> {
        return dao.getTodosByDate(epochDay)
    }

    override suspend fun getTodoCountByDateRange(startDate: Int, endDate: Int): Map<Int, Int> {
        return dao.getTodoCountByDateRange(startDate, endDate)
    }
}
