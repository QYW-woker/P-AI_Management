package com.lifemanager.app.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.Priority
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.TodoStatus
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.domain.usecase.TodoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 待办记事ViewModel
 */
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoUseCase: TodoUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<TodoUiState>(TodoUiState.Loading)
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    // 当前筛选器
    private val _currentFilter = MutableStateFlow(TodoFilter.ALL)
    val currentFilter: StateFlow<TodoFilter> = _currentFilter.asStateFlow()

    // 待办分组
    private val _todoGroups = MutableStateFlow<List<TodoGroup>>(emptyList())
    val todoGroups: StateFlow<List<TodoGroup>> = _todoGroups.asStateFlow()

    // 统计数据
    private val _statistics = MutableStateFlow(TodoStatistics())
    val statistics: StateFlow<TodoStatistics> = _statistics.asStateFlow()

    // 四象限数据
    private val _quadrantData = MutableStateFlow(QuadrantData(emptyList(), emptyList(), emptyList(), emptyList()))
    val quadrantData: StateFlow<QuadrantData> = _quadrantData.asStateFlow()

    // 日历选中日期
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // 日历中每天的待办数量
    private val _calendarTodoCount = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val calendarTodoCount: StateFlow<Map<Int, Int>> = _calendarTodoCount.asStateFlow()

    // 选中日期的待办列表
    private val _selectedDateTodos = MutableStateFlow<List<TodoEntity>>(emptyList())
    val selectedDateTodos: StateFlow<List<TodoEntity>> = _selectedDateTodos.asStateFlow()

    // 视图模式: LIST, QUADRANT, CALENDAR
    private val _viewMode = MutableStateFlow("LIST")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    // 显示添加/编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 显示删除确认
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(TodoEditState())
    val editState: StateFlow<TodoEditState> = _editState.asStateFlow()

    // 待删除的待办ID
    private var deleteTodoId: Long? = null

    // ============ 批量删除功能 ============
    // 选择模式
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // 已选中的待办ID
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // 显示批量删除确认对话框
    private val _showBatchDeleteDialog = MutableStateFlow(false)
    val showBatchDeleteDialog: StateFlow<Boolean> = _showBatchDeleteDialog.asStateFlow()

    /**
     * 进入选择模式
     */
    fun enterSelectionMode() {
        _isSelectionMode.value = true
        _selectedIds.value = emptySet()
    }

    /**
     * 退出选择模式
     */
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    /**
     * 切换选中状态
     */
    fun toggleSelection(id: Long) {
        val currentSet = _selectedIds.value.toMutableSet()
        if (currentSet.contains(id)) {
            currentSet.remove(id)
        } else {
            currentSet.add(id)
        }
        _selectedIds.value = currentSet
    }

    /**
     * 全选当前列表
     */
    fun selectAll() {
        val allIds = _todoGroups.value.flatMap { group ->
            group.todos.map { it.id }
        }.toSet()
        _selectedIds.value = allIds
    }

    /**
     * 取消全选
     */
    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    /**
     * 显示批量删除确认
     */
    fun showBatchDeleteConfirm() {
        if (_selectedIds.value.isNotEmpty()) {
            _showBatchDeleteDialog.value = true
        }
    }

    /**
     * 隐藏批量删除确认
     */
    fun hideBatchDeleteConfirm() {
        _showBatchDeleteDialog.value = false
    }

    /**
     * 确认批量删除
     */
    fun confirmBatchDelete() {
        val idsToDelete = _selectedIds.value.toList()
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                todoUseCase.deleteTodos(idsToDelete)
                hideBatchDeleteConfirm()
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    init {
        loadData()
        observeQuadrantData()
        observeCalendarData()
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = TodoUiState.Loading
                _statistics.value = todoUseCase.getStatistics()
                _uiState.value = TodoUiState.Success
            } catch (e: Exception) {
                _uiState.value = TodoUiState.Error(e.message ?: "加载失败")
            }
        }

        // 观察待办分组
        viewModelScope.launch {
            _currentFilter.flatMapLatest { filter ->
                todoUseCase.getTodoGroups(filter)
            }.catch { e ->
                _uiState.value = TodoUiState.Error(e.message ?: "加载失败")
            }.collect { groups ->
                _todoGroups.value = groups
                if (_uiState.value is TodoUiState.Loading) {
                    _uiState.value = TodoUiState.Success
                }
            }
        }
    }

    /**
     * 观察四象限数据
     */
    private fun observeQuadrantData() {
        viewModelScope.launch {
            todoUseCase.getQuadrantData().collect { data ->
                _quadrantData.value = data
            }
        }
    }

    /**
     * 观察日历数据
     */
    private fun observeCalendarData() {
        viewModelScope.launch {
            // 观察选中日期的变化，加载该日期的待办
            _selectedDate.collect { date ->
                loadTodosForDate(date)
            }
        }

        // 加载当月所有日期的待办数量
        loadCalendarTodoCount()
    }

    /**
     * 加载指定日期的待办
     */
    private suspend fun loadTodosForDate(date: LocalDate) {
        try {
            val epochDay = date.toEpochDay().toInt()
            val todos = todoUseCase.getTodosByDate(epochDay)
            _selectedDateTodos.value = todos
        } catch (e: Exception) {
            _selectedDateTodos.value = emptyList()
        }
    }

    /**
     * 加载日历待办数量
     */
    private fun loadCalendarTodoCount() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val startDate = today.withDayOfMonth(1).minusMonths(1).toEpochDay().toInt()
                val endDate = today.withDayOfMonth(1).plusMonths(2).toEpochDay().toInt()
                val counts = todoUseCase.getTodoCountByDateRange(startDate, endDate)
                _calendarTodoCount.value = counts
            } catch (e: Exception) {
                _calendarTodoCount.value = emptyMap()
            }
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * 切换到上个月
     */
    fun previousMonth() {
        _selectedDate.value = _selectedDate.value.minusMonths(1)
        loadCalendarTodoCount()
    }

    /**
     * 切换到下个月
     */
    fun nextMonth() {
        _selectedDate.value = _selectedDate.value.plusMonths(1)
        loadCalendarTodoCount()
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }

    /**
     * 切换筛选器
     */
    fun setFilter(filter: TodoFilter) {
        _currentFilter.value = filter
    }

    /**
     * 切换视图模式 (LIST -> QUADRANT -> CALENDAR -> LIST)
     */
    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            "LIST" -> "QUADRANT"
            "QUADRANT" -> "CALENDAR"
            else -> "LIST"
        }
    }

    /**
     * 切换待办完成状态
     */
    fun toggleComplete(id: Long) {
        viewModelScope.launch {
            try {
                todoUseCase.toggleComplete(id)
                _statistics.value = todoUseCase.getStatistics()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 显示添加对话框
     */
    fun showAddDialog() {
        _editState.value = TodoEditState(
            dueDate = LocalDate.now().toEpochDay().toInt()
        )
        _showEditDialog.value = true
    }

    /**
     * 显示添加到指定象限的对话框
     */
    fun showAddDialogWithQuadrant(quadrant: String) {
        _editState.value = TodoEditState(
            dueDate = LocalDate.now().toEpochDay().toInt(),
            quadrant = quadrant
        )
        _showEditDialog.value = true
    }

    /**
     * 显示编辑对话框
     */
    fun showEditDialog(id: Long) {
        viewModelScope.launch {
            try {
                val todo = todoUseCase.getTodoById(id)
                if (todo != null) {
                    _editState.value = TodoEditState(
                        id = todo.id,
                        isEditing = true,
                        title = todo.title,
                        description = todo.description,
                        priority = todo.priority,
                        quadrant = todo.quadrant,
                        dueDate = todo.dueDate,
                        dueTime = todo.dueTime,
                        reminderAt = todo.reminderAt,
                        repeatRule = todo.repeatRule
                    )
                    _showEditDialog.value = true
                }
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(error = e.message)
            }
        }
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editState.value = TodoEditState()
    }

    /**
     * 更新编辑标题
     */
    fun updateEditTitle(title: String) {
        _editState.value = _editState.value.copy(title = title)
    }

    /**
     * 更新编辑描述
     */
    fun updateEditDescription(description: String) {
        _editState.value = _editState.value.copy(description = description)
    }

    /**
     * 更新编辑优先级
     */
    fun updateEditPriority(priority: String) {
        _editState.value = _editState.value.copy(priority = priority)
    }

    /**
     * 更新编辑象限
     */
    fun updateEditQuadrant(quadrant: String?) {
        _editState.value = _editState.value.copy(quadrant = quadrant)
    }

    /**
     * 更新编辑日期
     */
    fun updateEditDueDate(date: Int?) {
        _editState.value = _editState.value.copy(dueDate = date)
    }

    /**
     * 更新编辑时间
     */
    fun updateEditDueTime(time: String?) {
        _editState.value = _editState.value.copy(dueTime = time)
    }

    /**
     * 保存待办
     */
    fun saveTodo() {
        val state = _editState.value
        if (state.title.isBlank()) {
            _editState.value = state.copy(error = "请输入标题")
            return
        }

        viewModelScope.launch {
            try {
                _editState.value = state.copy(isSaving = true, error = null)

                if (state.isEditing) {
                    todoUseCase.updateTodo(
                        id = state.id,
                        title = state.title,
                        description = state.description,
                        priority = state.priority,
                        quadrant = state.quadrant,
                        dueDate = state.dueDate,
                        dueTime = state.dueTime,
                        reminderAt = state.reminderAt,
                        repeatRule = state.repeatRule
                    )
                } else {
                    todoUseCase.addTodo(
                        title = state.title,
                        description = state.description,
                        priority = state.priority,
                        quadrant = state.quadrant,
                        dueDate = state.dueDate,
                        dueTime = state.dueTime,
                        reminderAt = state.reminderAt,
                        repeatRule = state.repeatRule
                    )
                }

                hideEditDialog()
                refresh()
            } catch (e: Exception) {
                _editState.value = state.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 显示删除确认
     */
    fun showDeleteConfirm(id: Long) {
        deleteTodoId = id
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
        deleteTodoId = null
    }

    /**
     * 确认删除
     */
    fun confirmDelete() {
        val id = deleteTodoId ?: return

        viewModelScope.launch {
            try {
                todoUseCase.deleteTodo(id)
                hideDeleteConfirm()
                refresh()
            } catch (e: Exception) {
                // 处理错误
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
     * 判断是否逾期
     */
    fun isOverdue(todo: TodoEntity): Boolean {
        return todoUseCase.isOverdue(todo)
    }
}
