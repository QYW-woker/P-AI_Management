package com.lifemanager.app.feature.todo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.TodoStatus
import com.lifemanager.app.domain.repository.GoalRepository
import com.lifemanager.app.domain.repository.TodoRepository
import com.lifemanager.app.domain.usecase.TodoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 待办详情页ViewModel
 */
@HiltViewModel
class TodoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val todoRepository: TodoRepository,
    private val todoUseCase: TodoUseCase,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val todoId: Long = savedStateHandle.get<Long>("id") ?: 0L

    // UI状态
    private val _uiState = MutableStateFlow<TodoDetailUiState>(TodoDetailUiState.Loading)
    val uiState: StateFlow<TodoDetailUiState> = _uiState.asStateFlow()

    // 待办数据
    private val _todo = MutableStateFlow<TodoEntity?>(null)
    val todo: StateFlow<TodoEntity?> = _todo.asStateFlow()

    // 子任务
    private val _subTodos = MutableStateFlow<List<TodoEntity>>(emptyList())
    val subTodos: StateFlow<List<TodoEntity>> = _subTodos.asStateFlow()

    // 关联目标
    private val _linkedGoal = MutableStateFlow<GoalEntity?>(null)
    val linkedGoal: StateFlow<GoalEntity?> = _linkedGoal.asStateFlow()

    // 编辑对话框状态
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 删除确认对话框
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 添加子任务对话框
    private val _showAddSubTodoDialog = MutableStateFlow(false)
    val showAddSubTodoDialog: StateFlow<Boolean> = _showAddSubTodoDialog.asStateFlow()

    init {
        loadTodo()
        observeSubTodos()
    }

    /**
     * 加载待办详情
     */
    private fun loadTodo() {
        viewModelScope.launch {
            try {
                val todoEntity = todoRepository.getById(todoId)
                if (todoEntity != null) {
                    _todo.value = todoEntity
                    _uiState.value = TodoDetailUiState.Success

                    // 加载关联目标
                    todoEntity.linkedGoalId?.let { goalId ->
                        loadLinkedGoal(goalId)
                    }
                } else {
                    _uiState.value = TodoDetailUiState.Error("待办不存在")
                }
            } catch (e: Exception) {
                _uiState.value = TodoDetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 观察子任务变化
     */
    private fun observeSubTodos() {
        viewModelScope.launch {
            todoUseCase.getSubTodos(todoId).collect { subTodos ->
                _subTodos.value = subTodos
            }
        }
    }

    /**
     * 加载关联目标
     */
    private suspend fun loadLinkedGoal(goalId: Long) {
        try {
            val goal = goalRepository.getGoalById(goalId)
            _linkedGoal.value = goal
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 切换完成状态
     */
    fun toggleComplete() {
        viewModelScope.launch {
            try {
                todoUseCase.toggleComplete(todoId)
                loadTodo() // 重新加载
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 切换子任务完成状态
     */
    fun toggleSubTodoComplete(subTodoId: Long) {
        viewModelScope.launch {
            try {
                todoUseCase.toggleComplete(subTodoId)
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    fun showEditDialog() {
        _showEditDialog.value = true
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        loadTodo() // 编辑后重新加载
    }

    /**
     * 显示删除确认
     */
    fun showDeleteConfirm() {
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
    }

    /**
     * 确认删除
     */
    fun confirmDelete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                todoUseCase.deleteTodo(todoId)
                onDeleted()
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 显示添加子任务对话框
     */
    fun showAddSubTodoDialog() {
        _showAddSubTodoDialog.value = true
    }

    /**
     * 隐藏添加子任务对话框
     */
    fun hideAddSubTodoDialog() {
        _showAddSubTodoDialog.value = false
    }

    /**
     * 添加子任务
     */
    fun addSubTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val parentTodo = _todo.value ?: return@launch
                val subTodo = TodoEntity(
                    title = title,
                    parentId = todoId,
                    dueDate = parentTodo.dueDate,
                    priority = parentTodo.priority,
                    linkedGoalId = parentTodo.linkedGoalId
                )
                todoRepository.insert(subTodo)
                hideAddSubTodoDialog()
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 删除子任务
     */
    fun deleteSubTodo(subTodoId: Long) {
        viewModelScope.launch {
            try {
                todoUseCase.deleteTodo(subTodoId)
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 格式化日期
     */
    fun formatDueDate(epochDay: Int?): String {
        return todoUseCase.formatDueDate(epochDay)
    }

    /**
     * 格式化提醒时间
     */
    fun formatReminderTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        return try {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            )
            val today = LocalDate.now()
            val reminderDate = dateTime.toLocalDate()

            val dateStr = when {
                reminderDate == today -> "今天"
                reminderDate == today.plusDays(1) -> "明天"
                reminderDate == today.minusDays(1) -> "昨天"
                reminderDate.year == today.year ->
                    "${reminderDate.monthValue}月${reminderDate.dayOfMonth}日"
                else -> "${reminderDate.year}年${reminderDate.monthValue}月${reminderDate.dayOfMonth}日"
            }

            val timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            "$dateStr $timeStr"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 格式化四象限
     */
    fun formatQuadrant(quadrant: String?): String {
        return when (quadrant) {
            "IMPORTANT_URGENT" -> "重要且紧急"
            "IMPORTANT_NOT_URGENT" -> "重要不紧急"
            "NOT_IMPORTANT_URGENT" -> "不重要但紧急"
            "NOT_IMPORTANT_NOT_URGENT" -> "不重要不紧急"
            else -> ""
        }
    }

    /**
     * 格式化优先级
     */
    fun formatPriority(priority: String): String {
        return when (priority) {
            "HIGH" -> "高优先级"
            "MEDIUM" -> "中优先级"
            "LOW" -> "低优先级"
            else -> ""
        }
    }

    /**
     * 格式化重复规则
     */
    fun formatRepeatRule(rule: String): String {
        return when (rule) {
            "DAILY" -> "每天"
            "WEEKLY" -> "每周"
            "MONTHLY" -> "每月"
            "CUSTOM" -> "自定义"
            else -> ""
        }
    }

    /**
     * 判断是否逾期
     */
    fun isOverdue(): Boolean {
        val todo = _todo.value ?: return false
        return todoUseCase.isOverdue(todo)
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadTodo()
    }
}

/**
 * 待办详情UI状态
 */
sealed class TodoDetailUiState {
    object Loading : TodoDetailUiState()
    object Success : TodoDetailUiState()
    data class Error(val message: String) : TodoDetailUiState()
}
