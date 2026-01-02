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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * 待办详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    todoId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToGoal: (Long) -> Unit = {},
    viewModel: TodoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val todo by viewModel.todo.collectAsState()
    val subTodos by viewModel.subTodos.collectAsState()
    val linkedGoal by viewModel.linkedGoal.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showAddSubTodoDialog by viewModel.showAddSubTodoDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showEditDialog() }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { viewModel.showDeleteConfirm() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is TodoDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is TodoDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as TodoDetailUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("返回")
                        }
                    }
                }
            }

            is TodoDetailUiState.Success -> {
                todo?.let { todoEntity ->
                    TodoDetailContent(
                        todo = todoEntity,
                        subTodos = subTodos,
                        linkedGoalName = linkedGoal?.title,
                        isOverdue = viewModel.isOverdue(),
                        onToggleComplete = { viewModel.toggleComplete() },
                        onToggleSubTodoComplete = { viewModel.toggleSubTodoComplete(it) },
                        onAddSubTodo = { viewModel.showAddSubTodoDialog() },
                        onDeleteSubTodo = { viewModel.deleteSubTodo(it) },
                        onNavigateToGoal = linkedGoal?.id?.let { { onNavigateToGoal(it) } },
                        formatDueDate = { viewModel.formatDueDate(it) },
                        formatReminderTime = { viewModel.formatReminderTime(it) },
                        formatQuadrant = { viewModel.formatQuadrant(it) },
                        formatPriority = { viewModel.formatPriority(it) },
                        formatRepeatRule = { viewModel.formatRepeatRule(it) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条待办吗？相关的子任务也会被删除。此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete(onNavigateBack) },
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

    // 添加子任务对话框
    if (showAddSubTodoDialog) {
        var subTodoTitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.hideAddSubTodoDialog() },
            title = { Text("添加子任务") },
            text = {
                OutlinedTextField(
                    value = subTodoTitle,
                    onValueChange = { subTodoTitle = it },
                    label = { Text("子任务标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addSubTodo(subTodoTitle)
                        subTodoTitle = ""
                    },
                    enabled = subTodoTitle.isNotBlank()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddSubTodoDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑对话框 - 复用现有的AddEditTodoDialog
    // 需要在TodoScreen中导入并使用
}

@Composable
private fun TodoDetailContent(
    todo: TodoEntity,
    subTodos: List<TodoEntity>,
    linkedGoalName: String?,
    isOverdue: Boolean,
    onToggleComplete: () -> Unit,
    onToggleSubTodoComplete: (Long) -> Unit,
    onAddSubTodo: () -> Unit,
    onDeleteSubTodo: (Long) -> Unit,
    onNavigateToGoal: (() -> Unit)?,
    formatDueDate: (Int?) -> String,
    formatReminderTime: (Long?) -> String,
    formatQuadrant: (String?) -> String,
    formatPriority: (String) -> String,
    formatRepeatRule: (String) -> String,
    modifier: Modifier = Modifier
) {
    val isCompleted = todo.status == TodoStatus.COMPLETED
    val priorityColor = when (todo.priority) {
        Priority.HIGH -> Color(0xFFEF4444)
        Priority.MEDIUM -> Color(0xFFF59E0B)
        Priority.LOW -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.outline
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 头部状态卡片
        item {
            StatusHeader(
                isCompleted = isCompleted,
                isOverdue = isOverdue,
                priorityColor = priorityColor,
                onToggleComplete = onToggleComplete
            )
        }

        // 标题和描述
        item {
            TitleSection(
                title = todo.title,
                description = todo.description,
                isCompleted = isCompleted
            )
        }

        // 详情信息
        item {
            DetailsCard(
                todo = todo,
                isOverdue = isOverdue,
                linkedGoalName = linkedGoalName,
                formatDueDate = formatDueDate,
                formatReminderTime = formatReminderTime,
                formatQuadrant = formatQuadrant,
                formatPriority = formatPriority,
                formatRepeatRule = formatRepeatRule,
                onNavigateToGoal = onNavigateToGoal
            )
        }

        // 子任务部分
        item {
            SubTodosSection(
                subTodos = subTodos,
                onToggleComplete = onToggleSubTodoComplete,
                onAddSubTodo = onAddSubTodo,
                onDelete = onDeleteSubTodo
            )
        }

        // 底部创建时间
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "创建时间",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(todo.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (todo.completedAt != null) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "完成时间",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTimestamp(todo.completedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(
    isCompleted: Boolean,
    isOverdue: Boolean,
    priorityColor: Color,
    onToggleComplete: () -> Unit
) {
    val statusColor = when {
        isCompleted -> Color(0xFF10B981)
        isOverdue -> Color(0xFFEF4444)
        else -> priorityColor
    }

    val statusText = when {
        isCompleted -> "已完成"
        isOverdue -> "已逾期"
        else -> "进行中"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            statusColor,
                            statusColor.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isCompleted -> Icons.Default.CheckCircle
                            isOverdue -> Icons.Default.Warning
                            else -> Icons.Default.PlayCircle
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                FilledTonalButton(
                    onClick = onToggleComplete,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Replay else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isCompleted) "恢复" else "完成")
                }
            }
        }
    }
}

@Composable
private fun TitleSection(
    title: String,
    description: String,
    isCompleted: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            color = if (isCompleted)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.onSurface
        )

        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}

@Composable
private fun DetailsCard(
    todo: TodoEntity,
    isOverdue: Boolean,
    linkedGoalName: String?,
    formatDueDate: (Int?) -> String,
    formatReminderTime: (Long?) -> String,
    formatQuadrant: (String?) -> String,
    formatPriority: (String) -> String,
    formatRepeatRule: (String) -> String,
    onNavigateToGoal: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 截止日期
            todo.dueDate?.let { date ->
                DetailRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "截止日期",
                    value = formatDueDate(date),
                    valueColor = if (isOverdue) Color(0xFFEF4444) else null
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // 截止时间
            todo.dueTime?.let { time ->
                DetailRow(
                    icon = Icons.Outlined.Schedule,
                    label = "截止时间",
                    value = time
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // 地点
            todo.location?.let { location ->
                if (location.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Outlined.LocationOn,
                        label = "地点",
                        value = location
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            // 优先级
            if (todo.priority != Priority.NONE) {
                val priorityColor = when (todo.priority) {
                    Priority.HIGH -> Color(0xFFEF4444)
                    Priority.MEDIUM -> Color(0xFFF59E0B)
                    Priority.LOW -> Color(0xFF10B981)
                    else -> null
                }
                DetailRow(
                    icon = Icons.Outlined.Flag,
                    label = "优先级",
                    value = formatPriority(todo.priority),
                    valueColor = priorityColor
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // 四象限
            todo.quadrant?.let { quadrant ->
                DetailRow(
                    icon = Icons.Outlined.GridView,
                    label = "象限",
                    value = formatQuadrant(quadrant)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // 提醒时间
            todo.reminderAt?.let { reminder ->
                DetailRow(
                    icon = Icons.Outlined.Notifications,
                    label = "提醒",
                    value = formatReminderTime(reminder)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // 重复规则
            if (todo.repeatRule != "NONE") {
                DetailRow(
                    icon = Icons.Outlined.Repeat,
                    label = "重复",
                    value = formatRepeatRule(todo.repeatRule)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // 关联目标
            linkedGoalName?.let { goalName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onNavigateToGoal != null) {
                            onNavigateToGoal?.invoke()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "关联目标",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = goalName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (onNavigateToGoal != null) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "查看目标",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SubTodosSection(
    subTodos: List<TodoEntity>,
    onToggleComplete: (Long) -> Unit,
    onAddSubTodo: () -> Unit,
    onDelete: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "子任务",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subTodos.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val completed = subTodos.count { it.status == TodoStatus.COMPLETED }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$completed/${subTodos.size}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                IconButton(onClick = onAddSubTodo) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加子任务",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (subTodos.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无子任务",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                subTodos.forEach { subTodo ->
                    SubTodoItem(
                        subTodo = subTodo,
                        onToggleComplete = { onToggleComplete(subTodo.id) },
                        onDelete = { onDelete(subTodo.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubTodoItem(
    subTodo: TodoEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = subTodo.status == TodoStatus.COMPLETED
    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        label = "subTodoBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 复选框
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) Color(0xFF10B981)
                        else Color(0xFF10B981).copy(alpha = 0.15f)
                    )
                    .clickable(onClick = onToggleComplete),
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

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = subTodo.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isCompleted)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除子任务",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    return try {
        val dateTime = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (e: Exception) {
        ""
    }
}
