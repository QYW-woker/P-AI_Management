package com.lifemanager.app.feature.todo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.Priority
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.TodoStatus
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

/**
 * 待办记事主界面 - 简洁设计版本
 *
 * 设计原则:
 * - 干净、克制、有呼吸感
 * - 轻灵不花哨
 * - 使用统一的设计系统
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanTodoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: TodoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val todoGroups by viewModel.todoGroups.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val quadrantData by viewModel.quadrantData.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val calendarTodoCount by viewModel.calendarTodoCount.collectAsState()
    val selectedDateTodos by viewModel.selectedDateTodos.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val showBatchDeleteDialog by viewModel.showBatchDeleteDialog.collectAsState()

    // 处理返回键
    if (isSelectionMode) {
        BackHandler {
            viewModel.exitSelectionMode()
        }
    }

    Scaffold(
        containerColor = CleanColors.background,
        topBar = {
            CleanTodoTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedIds.size,
                totalCount = todoGroups.sumOf { it.todos.size },
                viewMode = viewMode,
                onNavigateBack = onNavigateBack,
                onExitSelection = { viewModel.exitSelectionMode() },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.deselectAll() },
                onDeleteSelected = { viewModel.showBatchDeleteConfirm() },
                onEnterSelection = { viewModel.enterSelectionMode() },
                onToggleViewMode = { viewModel.toggleViewMode() }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    containerColor = CleanColors.primary,
                    contentColor = CleanColors.onPrimary,
                    shape = RoundedCornerShape(Radius.md)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "添加待办")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 简洁统计卡片
            CleanStatisticsCard(statistics = statistics)

            // 简洁筛选栏
            CleanFilterBar(
                currentFilter = currentFilter,
                onFilterChange = { viewModel.setFilter(it) }
            )

            when (uiState) {
                is TodoUiState.Loading -> {
                    PageLoadingState()
                }

                is TodoUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as TodoUiState.Error).message,
                                style = CleanTypography.body,
                                color = CleanColors.error
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            CleanSecondaryButton(
                                text = "重试",
                                onClick = { viewModel.refresh() }
                            )
                        }
                    }
                }

                is TodoUiState.Success -> {
                    when (viewMode) {
                        "LIST" -> CleanTodoListView(
                            groups = todoGroups,
                            viewModel = viewModel,
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedIds,
                            onNavigateToDetail = onNavigateToDetail
                        )
                        "QUADRANT" -> CleanQuadrantView(
                            data = quadrantData,
                            viewModel = viewModel,
                            onNavigateToDetail = onNavigateToDetail
                        )
                        "CALENDAR" -> CleanCalendarView(
                            selectedDate = selectedDate,
                            todoCount = calendarTodoCount,
                            todos = selectedDateTodos,
                            onDateSelect = { viewModel.selectDate(it) },
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            viewModel = viewModel,
                            onNavigateToDetail = onNavigateToDetail
                        )
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AddEditTodoDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        CleanDeleteConfirmDialog(
            title = "确认删除",
            message = "确定要删除这条待办吗？此操作不可恢复。",
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.hideDeleteConfirm() }
        )
    }

    // 批量删除确认对话框
    if (showBatchDeleteDialog) {
        CleanDeleteConfirmDialog(
            title = "确认批量删除",
            message = "确定要删除选中的 ${selectedIds.size} 条待办吗？此操作不可恢复。",
            onConfirm = { viewModel.confirmBatchDelete() },
            onDismiss = { viewModel.hideBatchDeleteConfirm() }
        )
    }
}

/**
 * 简洁顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanTodoTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    viewMode: String,
    onNavigateBack: () -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEnterSelection: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) "已选择 $selectedCount 项" else "待办记事",
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = if (isSelectionMode) onExitSelection else onNavigateBack) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                    contentDescription = if (isSelectionMode) "退出选择" else "返回",
                    tint = CleanColors.textPrimary
                )
            }
        },
        actions = {
            if (isSelectionMode) {
                // 选择模式操作
                if (selectedCount < totalCount) {
                    IconButton(onClick = onSelectAll) {
                        Icon(
                            Icons.Default.SelectAll,
                            contentDescription = "全选",
                            tint = CleanColors.textSecondary
                        )
                    }
                } else {
                    IconButton(onClick = onDeselectAll) {
                        Icon(
                            Icons.Default.Deselect,
                            contentDescription = "取消全选",
                            tint = CleanColors.textSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除选中",
                        tint = if (selectedCount > 0) CleanColors.error else CleanColors.textDisabled
                    )
                }
            } else {
                // 正常模式操作
                IconButton(onClick = onEnterSelection) {
                    Icon(
                        Icons.Outlined.Checklist,
                        contentDescription = "批量选择",
                        tint = CleanColors.textSecondary
                    )
                }
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = when (viewMode) {
                            "LIST" -> Icons.Outlined.GridView
                            "QUADRANT" -> Icons.Outlined.CalendarMonth
                            else -> Icons.Outlined.ViewList
                        },
                        contentDescription = "切换视图",
                        tint = CleanColors.textSecondary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = if (isSelectionMode) CleanColors.primaryLight else CleanColors.background
        )
    )
}

/**
 * 简洁统计卡片 - 无渐变，干净的数据展示
 */
@Composable
private fun CleanStatisticsCard(statistics: TodoStatistics) {
    val completionRate = if (statistics.todayTotal > 0)
        statistics.todayCompleted.toFloat() / statistics.todayTotal else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageHorizontal, vertical = Spacing.md),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "任务概览",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
                Text(
                    text = "${(completionRate * 100).toInt()}%",
                    style = CleanTypography.secondary,
                    color = CleanColors.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 简洁进度条
            LinearProgressIndicator(
                progress = completionRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CleanColors.primary,
                trackColor = CleanColors.borderLight
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CleanStatItem(
                    label = "待完成",
                    value = statistics.totalPending.toString(),
                    icon = Icons.Outlined.Assignment
                )
                CleanStatItem(
                    label = "今日完成",
                    value = "${statistics.todayCompleted}/${statistics.todayTotal}",
                    icon = Icons.Outlined.CheckCircle
                )
                CleanStatItem(
                    label = "逾期",
                    value = statistics.overdueCount.toString(),
                    icon = Icons.Outlined.Warning,
                    valueColor = if (statistics.overdueCount > 0) CleanColors.error else CleanColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun CleanStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = CleanColors.textPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CleanColors.textTertiary,
            modifier = Modifier.size(IconSize.md)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = value,
            style = CleanTypography.amountMedium,
            color = valueColor
        )
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textTertiary
        )
    }
}

/**
 * 简洁筛选栏 - 克制的颜色
 */
@Composable
private fun CleanFilterBar(
    currentFilter: TodoFilter,
    onFilterChange: (TodoFilter) -> Unit
) {
    data class FilterItem(
        val filter: TodoFilter,
        val label: String,
        val icon: ImageVector
    )

    val filters = remember {
        listOf(
            FilterItem(TodoFilter.ALL, "全部", Icons.Outlined.ViewList),
            FilterItem(TodoFilter.TODAY, "今日", Icons.Outlined.Today),
            FilterItem(TodoFilter.UPCOMING, "计划", Icons.Outlined.Event),
            FilterItem(TodoFilter.OVERDUE, "逾期", Icons.Outlined.Warning),
            FilterItem(TodoFilter.COMPLETED, "完成", Icons.Outlined.Done)
        )
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = Spacing.pageHorizontal, vertical = Spacing.sm)
    ) {
        items(filters, key = { it.filter }) { item ->
            val isSelected = currentFilter == item.filter

            Surface(
                onClick = { onFilterChange(item.filter) },
                shape = RoundedCornerShape(Radius.sm),
                color = if (isSelected) CleanColors.primary else CleanColors.surface,
                border = if (!isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, CleanColors.border)
                } else null,
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) CleanColors.onPrimary else CleanColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.label,
                        style = CleanTypography.button,
                        color = if (isSelected) CleanColors.onPrimary else CleanColors.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * 简洁待办列表视图
 */
@Composable
private fun CleanTodoListView(
    groups: List<TodoGroup>,
    viewModel: TodoViewModel,
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onNavigateToDetail: (Long) -> Unit = {}
) {
    if (groups.isEmpty()) {
        EmptyStateView(
            message = "暂无待办事项",
            icon = Icons.Outlined.CheckCircle,
            actionText = "添加待办",
            onActionClick = { viewModel.showAddDialog() }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = Spacing.pageHorizontal,
                vertical = Spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            groups.forEach { group ->
                item(key = "header_${group.title}") {
                    Text(
                        text = group.title,
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary,
                        modifier = Modifier.padding(vertical = Spacing.sm)
                    )
                }

                items(group.todos, key = { it.id }) { todo ->
                    CleanTodoItem(
                        todo = todo,
                        isOverdue = viewModel.isOverdue(todo),
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedIds.contains(todo.id),
                        onToggleComplete = { viewModel.toggleComplete(todo.id) },
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(todo.id)
                            } else {
                                onNavigateToDetail(todo.id)
                            }
                        },
                        onDelete = { viewModel.showDeleteConfirm(todo.id) },
                        onLongClick = {
                            if (!isSelectionMode) {
                                viewModel.enterSelectionMode()
                                viewModel.toggleSelection(todo.id)
                            }
                        },
                        formatDueDate = { viewModel.formatDueDate(it) }
                    )
                }
            }

            // 底部安全间距
            item {
                Spacer(modifier = Modifier.height(Spacing.bottomSafe + 56.dp)) // FAB高度
            }
        }
    }
}

/**
 * 简洁待办项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanTodoItem(
    todo: TodoEntity,
    isOverdue: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit = {},
    formatDueDate: (Int?) -> String
) {
    val isCompleted = todo.status == TodoStatus.COMPLETED

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(Radius.md),
        color = when {
            isSelected -> CleanColors.primaryLight
            isCompleted -> CleanColors.surfaceVariant
            else -> CleanColors.surface
        },
        shadowElevation = if (isCompleted) Elevation.none else Elevation.xs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择模式下显示复选框
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = CleanColors.primary,
                        uncheckedColor = CleanColors.border
                    )
                )
                Spacer(modifier = Modifier.width(Spacing.md))
            }

            // 完成状态指示器
            PriorityCheckbox(
                isCompleted = isCompleted,
                priority = todo.priority,
                onToggle = onToggleComplete
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            // 内容区域
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = CleanTypography.body,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) CleanColors.textTertiary else CleanColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (todo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = todo.description,
                        style = CleanTypography.secondary,
                        color = CleanColors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                todo.dueDate?.let { date ->
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Outlined.Warning else Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isOverdue) CleanColors.error else CleanColors.textTertiary
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = formatDueDate(date),
                            style = CleanTypography.caption,
                            color = if (isOverdue) CleanColors.error else CleanColors.textTertiary
                        )
                    }
                }
            }

            // 优先级指示
            if (todo.priority != Priority.NONE && !isCompleted) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                PriorityDot(priority = todo.priority)
            }

            // 箭头指示
            if (!isSelectionMode) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CleanColors.textTertiary,
                    modifier = Modifier.size(IconSize.sm)
                )
            }
        }
    }
}

/**
 * 优先级复选框
 */
@Composable
private fun PriorityCheckbox(
    isCompleted: Boolean,
    priority: String,
    onToggle: () -> Unit
) {
    val priorityColor = when (priority) {
        Priority.HIGH -> CleanColors.priorityHigh
        Priority.MEDIUM -> CleanColors.priorityMedium
        Priority.LOW -> CleanColors.priorityLow
        else -> CleanColors.border
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isCompleted) priorityColor else priorityColor.copy(alpha = 0.15f)
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 简洁四象限视图
 */
@Composable
private fun CleanQuadrantView(
    data: QuadrantData,
    viewModel: TodoViewModel,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            CleanQuadrantCard(
                title = "重要且紧急",
                color = CleanColors.error,
                todos = data.importantUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("IMPORTANT_URGENT") },
                onTodoClick = { onNavigateToDetail(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
            CleanQuadrantCard(
                title = "重要不紧急",
                color = CleanColors.primary,
                todos = data.importantNotUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("IMPORTANT_NOT_URGENT") },
                onTodoClick = { onNavigateToDetail(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            CleanQuadrantCard(
                title = "不重要但紧急",
                color = CleanColors.warning,
                todos = data.notImportantUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("NOT_IMPORTANT_URGENT") },
                onTodoClick = { onNavigateToDetail(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
            CleanQuadrantCard(
                title = "不重要不紧急",
                color = CleanColors.textTertiary,
                todos = data.notImportantNotUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("NOT_IMPORTANT_NOT_URGENT") },
                onTodoClick = { onNavigateToDetail(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
        }
    }
}

@Composable
private fun CleanQuadrantCard(
    title: String,
    color: Color,
    todos: List<TodoEntity>,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
    onTodoClick: (Long) -> Unit,
    onToggleComplete: (Long) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(Radius.md),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = CleanTypography.caption,
                    color = color
                )
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "添加",
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(todos, key = { it.id }) { todo ->
                    CleanQuadrantTodoItem(
                        todo = todo,
                        onClick = { onTodoClick(todo.id) },
                        onToggle = { onToggleComplete(todo.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanQuadrantTodoItem(
    todo: TodoEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.status == TodoStatus.COMPLETED,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(18.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = CleanColors.primary,
                uncheckedColor = CleanColors.border
            )
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = todo.title,
            style = CleanTypography.caption,
            color = if (todo.status == TodoStatus.COMPLETED)
                CleanColors.textTertiary else CleanColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (todo.status == TodoStatus.COMPLETED)
                TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

/**
 * 简洁日历视图
 */
@Composable
private fun CleanCalendarView(
    selectedDate: LocalDate,
    todoCount: Map<Int, Int>,
    todos: List<TodoEntity>,
    onDateSelect: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    viewModel: TodoViewModel,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.pageHorizontal)
    ) {
        // 月份导航
        CleanCalendarHeader(
            currentMonth = YearMonth.from(selectedDate),
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // 日历网格
        CleanCalendarGrid(
            currentMonth = YearMonth.from(selectedDate),
            selectedDate = selectedDate,
            todoCount = todoCount,
            onDateSelect = onDateSelect
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // 选中日期的待办列表
        Text(
            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
            style = CleanTypography.title,
            color = CleanColors.textPrimary
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        if (todos.isEmpty()) {
            Text(
                text = "当日无待办事项",
                style = CleanTypography.secondary,
                color = CleanColors.textTertiary,
                modifier = Modifier.padding(vertical = Spacing.xl)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(todos, key = { it.id }) { todo ->
                    CleanTodoItem(
                        todo = todo,
                        isOverdue = viewModel.isOverdue(todo),
                        onToggleComplete = { viewModel.toggleComplete(todo.id) },
                        onClick = { onNavigateToDetail(todo.id) },
                        onDelete = { viewModel.showDeleteConfirm(todo.id) },
                        formatDueDate = { viewModel.formatDueDate(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanCalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "上个月",
                tint = CleanColors.textSecondary
            )
        }

        Text(
            text = "${currentMonth.year}年${currentMonth.monthValue}月",
            style = CleanTypography.title,
            color = CleanColors.textPrimary
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "下个月",
                tint = CleanColors.textSecondary
            )
        }
    }
}

@Composable
private fun CleanCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    todoCount: Map<Int, Int>,
    onDateSelect: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val today = LocalDate.now()

    Column {
        // 星期标题
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(Spacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 日期网格
        val totalDays = lastDayOfMonth.dayOfMonth
        val weeks = (startDayOfWeek + totalDays + 6) / 7

        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayOfWeek ->
                    val cellIndex = week * 7 + dayOfWeek
                    val dayNumber = cellIndex - startDayOfWeek + 1

                    if (dayNumber in 1..totalDays) {
                        val date = currentMonth.atDay(dayNumber)
                        val epochDay = date.toEpochDay().toInt()
                        val count = todoCount[epochDay] ?: 0
                        val isSelected = date == selectedDate
                        val isToday = date == today

                        CleanCalendarDay(
                            day = dayNumber,
                            hasTodos = count > 0,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanCalendarDay(
    day: Int,
    hasTodos: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> CleanColors.primary
                    isToday -> CleanColors.primaryLight
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = CleanTypography.body,
                color = when {
                    isSelected -> CleanColors.onPrimary
                    isToday -> CleanColors.primary
                    else -> CleanColors.textPrimary
                }
            )

            if (hasTodos) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (isSelected) CleanColors.onPrimary else CleanColors.primary,
                            CircleShape
                        )
                )
            }
        }
    }
}

/**
 * 删除确认对话框
 */
@Composable
private fun CleanDeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = CleanTypography.body,
                color = CleanColors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "删除",
                    style = CleanTypography.button,
                    color = CleanColors.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    style = CleanTypography.button,
                    color = CleanColors.textSecondary
                )
            }
        },
        containerColor = CleanColors.surface,
        shape = RoundedCornerShape(Radius.lg)
    )
}
