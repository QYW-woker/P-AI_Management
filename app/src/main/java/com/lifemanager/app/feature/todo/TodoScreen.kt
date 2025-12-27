package com.lifemanager.app.feature.todo

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.Priority
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.TodoStatus
import com.lifemanager.app.domain.model.*

/**
 * 待办记事主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onNavigateBack: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办记事") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == "LIST") Icons.Filled.GridView else Icons.Filled.List,
                            contentDescription = if (viewMode == "LIST") "四象限视图" else "列表视图"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加待办")
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
                    if (viewMode == "LIST") {
                        TodoListView(
                            groups = todoGroups,
                            viewModel = viewModel
                        )
                    } else {
                        QuadrantView(
                            data = quadrantData,
                            viewModel = viewModel
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
            text = { Text("确定要删除这条待办吗？") },
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
}

@Composable
private fun StatisticsCard(statistics: TodoStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "待完成",
                value = statistics.totalPending.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "今日",
                value = "${statistics.todayCompleted}/${statistics.todayTotal}",
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    currentFilter: TodoFilter,
    onFilterChange: (TodoFilter) -> Unit
) {
    val filters = listOf(
        TodoFilter.ALL to "全部",
        TodoFilter.TODAY to "今日",
        TodoFilter.OVERDUE to "逾期",
        TodoFilter.COMPLETED to "已完成"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun TodoListView(
    groups: List<TodoGroup>,
    viewModel: TodoViewModel
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
                        onToggleComplete = { viewModel.toggleComplete(todo.id) },
                        onClick = { viewModel.showEditDialog(todo.id) },
                        onDelete = { viewModel.showDeleteConfirm(todo.id) },
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
    viewModel: TodoViewModel
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
                onTodoClick = { viewModel.showEditDialog(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
            QuadrantCard(
                title = "重要不紧急",
                color = Color(0xFF2196F3),
                todos = data.importantNotUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("IMPORTANT_NOT_URGENT") },
                onTodoClick = { viewModel.showEditDialog(it) },
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
                onTodoClick = { viewModel.showEditDialog(it) },
                onToggleComplete = { viewModel.toggleComplete(it) }
            )
            QuadrantCard(
                title = "不重要不紧急",
                color = Color(0xFF9E9E9E),
                todos = data.notImportantNotUrgent,
                modifier = Modifier.weight(1f),
                onAddClick = { viewModel.showAddDialogWithQuadrant("NOT_IMPORTANT_NOT_URGENT") },
                onTodoClick = { viewModel.showEditDialog(it) },
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

@Composable
private fun TodoItem(
    todo: TodoEntity,
    isOverdue: Boolean,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    formatDueDate: (Int?) -> String
) {
    val isCompleted = todo.status == TodoStatus.COMPLETED
    val priorityColor = when (todo.priority) {
        Priority.HIGH -> Color(0xFFF44336)
        Priority.MEDIUM -> Color(0xFFFF9800)
        Priority.LOW -> Color(0xFF4CAF50)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 复选框
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggleComplete() }
            )

            // 优先级指示条
            if (priorityColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(priorityColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                todo.dueDate?.let { date ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isOverdue) Color(0xFFF44336)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDueDate(date),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) Color(0xFFF44336)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
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
