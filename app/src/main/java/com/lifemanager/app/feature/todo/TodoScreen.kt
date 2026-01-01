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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.Priority
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.TodoStatus
import com.lifemanager.app.domain.model.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * 待办记事主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
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
        topBar = {
            if (isSelectionMode) {
                // 选择模式的顶部栏
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出选择")
                        }
                    },
                    actions = {
                        // 全选/取消全选
                        val allCount = todoGroups.sumOf { it.todos.size }
                        if (selectedIds.size < allCount) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "全选")
                            }
                        } else {
                            IconButton(onClick = { viewModel.deselectAll() }) {
                                Icon(Icons.Default.Deselect, contentDescription = "取消全选")
                            }
                        }
                        // 删除按钮
                        IconButton(
                            onClick = { viewModel.showBatchDeleteConfirm() },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除选中",
                                tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                // 正常模式的顶部栏
                TopAppBar(
                    title = { Text("待办记事") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // 批量选择按钮
                        IconButton(onClick = { viewModel.enterSelectionMode() }) {
                            Icon(Icons.Default.Checklist, contentDescription = "批量选择")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = when (viewMode) {
                                    "LIST" -> Icons.Filled.GridView
                                    "QUADRANT" -> Icons.Filled.CalendarMonth
                                    else -> Icons.Filled.List
                                },
                                contentDescription = when (viewMode) {
                                    "LIST" -> "四象限视图"
                                    "QUADRANT" -> "日历视图"
                                    else -> "列表视图"
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() }
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
            // 统计卡片
            StatisticsCard(statistics = statistics)

            // 筛选栏
            FilterBar(
                currentFilter = currentFilter,
                onFilterChange = { viewModel.setFilter(it) }
            )

            when (uiState) {
                is TodoUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TodoUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as TodoUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is TodoUiState.Success -> {
                    when (viewMode) {
                        "LIST" -> TodoListView(
                            groups = todoGroups,
                            viewModel = viewModel,
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedIds,
                            onNavigateToDetail = onNavigateToDetail
                        )
                        "QUADRANT" -> QuadrantView(
                            data = quadrantData,
                            viewModel = viewModel,
                            onNavigateToDetail = onNavigateToDetail
                        )
                        "CALENDAR" -> CalendarView(
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
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条待办吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }

    // 批量删除确认对话框
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideBatchDeleteConfirm() },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("确认批量删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 条待办吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBatchDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideBatchDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StatisticsCard(statistics: TodoStatistics) {
    val completionRate = if (statistics.todayTotal > 0)
        statistics.todayCompleted.toFloat() / statistics.todayTotal else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = completionRate,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFFF59E0B).copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF59E0B),
                            Color(0xFFEF4444)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "任务概览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Assignment,
                        label = "待完成",
                        value = statistics.totalPending.toString(),
                        color = Color(0xFFFCD34D)
                    )
                    StatItem(
                        icon = Icons.Default.CheckCircle,
                        label = "今日完成",
                        value = "${statistics.todayCompleted}/${statistics.todayTotal}",
                        color = Color(0xFF4ADE80)
                    )
                    StatItem(
                        icon = Icons.Default.TrendingUp,
                        label = "完成率",
                        value = "${(completionRate * 100).toInt()}%",
                        color = Color(0xFF60A5FA)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(Color.White, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun FilterBar(
    currentFilter: TodoFilter,
    onFilterChange: (TodoFilter) -> Unit
) {
    data class FilterItem(
        val filter: TodoFilter,
        val label: String,
        val icon: ImageVector,
        val color: Color
    )

    val filters = remember {
        listOf(
            FilterItem(TodoFilter.ALL, "全部", Icons.Default.ViewList, Color(0xFF6366F1)),
            FilterItem(TodoFilter.TODAY, "今日", Icons.Default.Today, Color(0xFF10B981)),
            FilterItem(TodoFilter.UPCOMING, "计划", Icons.Default.Event, Color(0xFF3B82F6)),
            FilterItem(TodoFilter.OVERDUE, "逾期", Icons.Default.Warning, Color(0xFFEF4444)),
            FilterItem(TodoFilter.COMPLETED, "完成", Icons.Default.Done, Color(0xFF8B5CF6))
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filters, key = { it.filter }) { item ->
            val isSelected = currentFilter == item.filter
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) item.color else item.color.copy(alpha = 0.1f),
                label = "bgColor"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else item.color,
                label = "contentColor"
            )

            Surface(
                onClick = { onFilterChange(item.filter) },
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor,
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoListView(
    groups: List<TodoGroup>,
    viewModel: TodoViewModel,
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onNavigateToDetail: (Long) -> Unit = {}
) {
    if (groups.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { group ->
                item(key = "header_${group.title}") {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(group.todos, key = { it.id }) { todo ->
                    TodoItem(
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
        }
    }
}

@Composable
private fun QuadrantView(
    data: QuadrantData,
    viewModel: TodoViewModel,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuadrantCard(
                title = "重要且紧急",
                color = Color(0xFFF44336),
                todos = data.importantUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("IMPORTANT_URGENT") },
                onTodoClick = { onNavigateToDetail(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
            QuadrantCard(
                title = "重要不紧急",
                color = Color(0xFF2196F3),
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuadrantCard(
                title = "不重要但紧急",
                color = Color(0xFFFF9800),
                todos = data.notImportantUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("NOT_IMPORTANT_URGENT") },
                onTodoClick = { onNavigateToDetail(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
            QuadrantCard(
                title = "不重要不紧急",
                color = Color(0xFF9E9E9E),
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
private fun QuadrantCard(
    title: String,
    color: Color,
    todos: List<TodoEntity>,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
    onTodoClick: (Long) -> Unit,
    onToggleComplete: (Long) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(todos, key = { it.id }) { todo ->
                    QuadrantTodoItem(
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
private fun QuadrantTodoItem(
    todo: TodoEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.status == TodoStatus.COMPLETED,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (todo.status == TodoStatus.COMPLETED)
                TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoItem(
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
    val priorityColor = when (todo.priority) {
        Priority.HIGH -> Color(0xFFEF4444)
        Priority.MEDIUM -> Color(0xFFF59E0B)
        Priority.LOW -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.outline
    }

    val cardBackground by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "cardBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isCompleted) 0.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = priorityColor.copy(alpha = 0.15f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择模式下显示复选框
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 自定义复选框（任务状态）
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) priorityColor
                        else priorityColor.copy(alpha = 0.15f)
                    )
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )

                if (todo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                todo.dueDate?.let { date ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOverdue) Icons.Default.Warning else Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDueDate(date),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 优先级徽章
            if (todo.priority != Priority.NONE) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(priorityColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 非选择模式下显示删除按钮
            if (!isSelectionMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无待办事项",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮添加待办",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 日历视图
 */
@Composable
private fun CalendarView(
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
            .padding(16.dp)
    ) {
        // 月份导航
        CalendarHeader(
            currentMonth = YearMonth.from(selectedDate),
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 日历网格
        CalendarGrid(
            currentMonth = YearMonth.from(selectedDate),
            selectedDate = selectedDate,
            todoCount = todoCount,
            onDateSelect = onDateSelect
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 选中日期的待办列表
        Text(
            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日的待办",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当日无待办事项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todos, key = { it.id }) { todo ->
                    TodoItem(
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
private fun CalendarHeader(
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
            Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
        }

        Text(
            text = "${currentMonth.year}年${currentMonth.monthValue}月",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
        }
    }
}

@Composable
private fun CalendarGrid(
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
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日期网格
        var dayCounter = 1
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

                        CalendarDay(
                            day = dayNumber,
                            todoCount = count,
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
private fun CalendarDay(
    day: Int,
    todoCount: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (todoCount > 0) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                )
            }
        }
    }
}
