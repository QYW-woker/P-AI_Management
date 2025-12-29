package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.Priority
import com.lifemanager.app.core.database.entity.Quadrant
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.TodoStatus
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 待办记事用例
 */
class TodoUseCase @Inject constructor(
    private val repository: TodoRepository
) {

    /**
     * 获取待办分组列表
     */
    fun getTodoGroups(filter: TodoFilter): Flow<List<TodoGroup>> {
        val today = LocalDate.now().toEpochDay().toInt()

        return when (filter) {
            TodoFilter.ALL -> getPendingTodoGroups(today)
            TodoFilter.TODAY -> getTodayTodoGroups(today)
            TodoFilter.UPCOMING -> getUpcomingTodoGroups(today)
            TodoFilter.OVERDUE -> getOverdueTodoGroups(today)
            TodoFilter.COMPLETED -> getCompletedTodoGroups()
        }
    }

    /**
     * 获取待办分组（全部待完成）
     */
    private fun getPendingTodoGroups(today: Int): Flow<List<TodoGroup>> {
        return combine(
            repository.getOverdueTodos(today),
            repository.getTodayTodos(today),
            repository.getPendingTodos()
        ) { overdue, todayTodos, all ->
            val groups = mutableListOf<TodoGroup>()

            if (overdue.isNotEmpty()) {
                groups.add(TodoGroup("已逾期", overdue))
            }

            if (todayTodos.isNotEmpty()) {
                groups.add(TodoGroup("今天", todayTodos))
            }

            // 未来的待办
            val futureTodos = all.filter { todo ->
                todo.dueDate?.let { it > today } ?: (todo !in overdue && todo !in todayTodos)
            }

            if (futureTodos.isNotEmpty()) {
                groups.add(TodoGroup("待办", futureTodos))
            }

            groups
        }
    }

    /**
     * 获取今日待办分组
     */
    private fun getTodayTodoGroups(today: Int): Flow<List<TodoGroup>> {
        return repository.getTodayTodos(today).map { todos ->
            if (todos.isEmpty()) emptyList()
            else listOf(TodoGroup("今天", todos))
        }
    }

    /**
     * 获取未来待办分组（按日期分组）
     */
    private fun getUpcomingTodoGroups(today: Int): Flow<List<TodoGroup>> {
        return repository.getPendingTodos().map { allTodos ->
            // 筛选未来的待办（dueDate > today 或 dueDate == null）
            val futureTodos = allTodos.filter { todo ->
                todo.dueDate?.let { it > today } ?: true
            }

            // 按日期分组
            val groups = mutableListOf<TodoGroup>()
            val tomorrow = today + 1
            val dayAfterTomorrow = today + 2
            val weekLater = today + 7

            val tomorrowTodos = futureTodos.filter { it.dueDate == tomorrow }
            val dayAfterTodos = futureTodos.filter { it.dueDate == dayAfterTomorrow }
            val thisWeekTodos = futureTodos.filter {
                it.dueDate != null && it.dueDate!! > dayAfterTomorrow && it.dueDate!! <= weekLater
            }
            val laterTodos = futureTodos.filter {
                it.dueDate == null || it.dueDate!! > weekLater
            }

            if (tomorrowTodos.isNotEmpty()) groups.add(TodoGroup("明天", tomorrowTodos))
            if (dayAfterTodos.isNotEmpty()) groups.add(TodoGroup("后天", dayAfterTodos))
            if (thisWeekTodos.isNotEmpty()) groups.add(TodoGroup("本周", thisWeekTodos))
            if (laterTodos.isNotEmpty()) groups.add(TodoGroup("以后", laterTodos))

            groups
        }
    }

    /**
     * 获取逾期待办分组
     */
    private fun getOverdueTodoGroups(today: Int): Flow<List<TodoGroup>> {
        return repository.getOverdueTodos(today).map { todos ->
            if (todos.isEmpty()) emptyList()
            else listOf(TodoGroup("已逾期", todos))
        }
    }

    /**
     * 获取已完成待办分组
     */
    private fun getCompletedTodoGroups(): Flow<List<TodoGroup>> {
        return repository.getCompletedTodos().map { todos ->
            if (todos.isEmpty()) emptyList()
            else listOf(TodoGroup("已完成", todos))
        }
    }

    /**
     * 获取四象限数据
     */
    fun getQuadrantData(): Flow<QuadrantData> {
        return combine(
            repository.getTodosByQuadrant(Quadrant.IMPORTANT_URGENT),
            repository.getTodosByQuadrant(Quadrant.IMPORTANT_NOT_URGENT),
            repository.getTodosByQuadrant(Quadrant.NOT_IMPORTANT_URGENT),
            repository.getTodosByQuadrant(Quadrant.NOT_IMPORTANT_NOT_URGENT)
        ) { iu, inu, niu, ninu ->
            QuadrantData(
                importantUrgent = iu,
                importantNotUrgent = inu,
                notImportantUrgent = niu,
                notImportantNotUrgent = ninu
            )
        }
    }

    /**
     * 获取统计数据
     */
    suspend fun getStatistics(): TodoStatistics {
        val today = LocalDate.now().toEpochDay().toInt()
        val todayStats = repository.getTodayStats(today)
        val pending = repository.countPending()

        return TodoStatistics(
            totalPending = pending,
            todayTotal = todayStats.total,
            todayCompleted = todayStats.completed
        )
    }

    /**
     * 添加待办
     */
    suspend fun addTodo(
        title: String,
        description: String = "",
        priority: String = Priority.NONE,
        quadrant: String? = null,
        dueDate: Int? = null,
        dueTime: String? = null,
        reminderAt: Long? = null,
        repeatRule: String = "NONE"
    ): Long {
        val todo = TodoEntity(
            title = title,
            description = description,
            priority = priority,
            quadrant = quadrant,
            dueDate = dueDate,
            dueTime = dueTime,
            reminderAt = reminderAt,
            repeatRule = repeatRule
        )
        return repository.insert(todo)
    }

    /**
     * 更新待办
     */
    suspend fun updateTodo(
        id: Long,
        title: String,
        description: String = "",
        priority: String = Priority.NONE,
        quadrant: String? = null,
        dueDate: Int? = null,
        dueTime: String? = null,
        reminderAt: Long? = null,
        repeatRule: String = "NONE"
    ) {
        val existing = repository.getById(id) ?: return
        val updated = existing.copy(
            title = title,
            description = description,
            priority = priority,
            quadrant = quadrant,
            dueDate = dueDate,
            dueTime = dueTime,
            reminderAt = reminderAt,
            repeatRule = repeatRule,
            updatedAt = System.currentTimeMillis()
        )
        repository.update(updated)
    }

    /**
     * 切换待办完成状态
     */
    suspend fun toggleComplete(id: Long) {
        val todo = repository.getById(id) ?: return
        if (todo.status == TodoStatus.COMPLETED) {
            repository.markPending(id)
        } else {
            repository.markCompleted(id)
        }
    }

    /**
     * 标记待办完成
     */
    suspend fun markCompleted(id: Long) {
        repository.markCompleted(id)
    }

    /**
     * 标记待办未完成
     */
    suspend fun markPending(id: Long) {
        repository.markPending(id)
    }

    /**
     * 删除待办
     */
    suspend fun deleteTodo(id: Long) {
        repository.deleteWithSubTodos(id)
    }

    /**
     * 获取待办详情
     */
    suspend fun getTodoById(id: Long): TodoEntity? {
        return repository.getById(id)
    }

    /**
     * 获取子任务
     */
    fun getSubTodos(parentId: Long): Flow<List<TodoEntity>> {
        return repository.getSubTodos(parentId)
    }

    /**
     * 格式化日期
     */
    fun formatDueDate(epochDay: Int?): String {
        if (epochDay == null) return ""

        val today = LocalDate.now().toEpochDay().toInt()
        val date = LocalDate.ofEpochDay(epochDay.toLong())

        return when (epochDay) {
            today -> "今天"
            today + 1 -> "明天"
            today - 1 -> "昨天"
            else -> if (date.year == LocalDate.now().year) {
                "${date.monthValue}月${date.dayOfMonth}日"
            } else {
                "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
            }
        }
    }

    /**
     * 判断是否逾期
     */
    fun isOverdue(todo: TodoEntity): Boolean {
        val today = LocalDate.now().toEpochDay().toInt()
        return todo.status == TodoStatus.PENDING && todo.dueDate != null && todo.dueDate < today
    }
}
