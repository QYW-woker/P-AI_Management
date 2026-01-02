package com.lifemanager.app.feature.todo

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*

/**
 * 待办详情页面 - 简洁设计版本
 *
 * 设计原则:
 * - 干净、克制、有呼吸感
 * - 轻灵不花哨
 * - 使用统一的设计系统
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanTodoDetailScreen(
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
        containerColor = CleanColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "待办详情",
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = CleanColors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showEditDialog() }) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "编辑",
                            tint = CleanColors.textSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.showDeleteConfirm() }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除",
                            tint = CleanColors.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CleanColors.background
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is TodoDetailUiState.Loading -> {
                PageLoadingState(modifier = Modifier.padding(paddingValues))
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
                            style = CleanTypography.body,
                            color = CleanColors.error
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        CleanSecondaryButton(
                            text = "返回",
                            onClick = onNavigateBack
                        )
                    }
                }
            }

            is TodoDetailUiState.Success -> {
                todo?.let { todoEntity ->
                    CleanTodoDetailContent(
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
        CleanDeleteDialog(
            title = "确认删除",
            message = "确定要删除这条待办吗？相关的子任务也会被删除。此操作不可恢复。",
            onConfirm = { viewModel.confirmDelete(onNavigateBack) },
            onDismiss = { viewModel.hideDeleteConfirm() }
        )
    }

    // 添加子任务对话框
    if (showAddSubTodoDialog) {
        var subTodoTitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.hideAddSubTodoDialog() },
            title = {
                Text(
                    text = "添加子任务",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            },
            text = {
                CleanTextField(
                    value = subTodoTitle,
                    onValueChange = { subTodoTitle = it },
                    label = "子任务标题",
                    singleLine = true
                )
            },
            confirmButton = {
                CleanPrimaryButton(
                    text = "添加",
                    onClick = {
                        viewModel.addSubTodo(subTodoTitle)
                        subTodoTitle = ""
                    },
                    enabled = subTodoTitle.isNotBlank()
                )
            },
            dismissButton = {
                CleanTextButton(
                    text = "取消",
                    onClick = { viewModel.hideAddSubTodoDialog() },
                    color = CleanColors.textSecondary
                )
            },
            containerColor = CleanColors.surface,
            shape = RoundedCornerShape(Radius.lg)
        )
    }
}

@Composable
private fun CleanTodoDetailContent(
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        // 头部状态卡片
        item {
            Spacer(modifier = Modifier.height(Spacing.sm))
            CleanStatusHeader(
                isCompleted = isCompleted,
                isOverdue = isOverdue,
                priority = todo.priority,
                onToggleComplete = onToggleComplete
            )
        }

        // 标题和描述
        item {
            CleanTitleSection(
                title = todo.title,
                description = todo.description,
                isCompleted = isCompleted
            )
        }

        // 详情信息
        item {
            CleanDetailsCard(
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
            CleanSubTodosSection(
                subTodos = subTodos,
                onToggleComplete = onToggleSubTodoComplete,
                onAddSubTodo = onAddSubTodo,
                onDelete = onDeleteSubTodo
            )
        }

        // 底部创建时间
        item {
            CleanTimestampCard(
                createdAt = todo.createdAt,
                completedAt = todo.completedAt
            )
            Spacer(modifier = Modifier.height(Spacing.bottomSafe))
        }
    }
}

/**
 * 简洁状态头部 - 无渐变
 */
@Composable
private fun CleanStatusHeader(
    isCompleted: Boolean,
    isOverdue: Boolean,
    priority: String,
    onToggleComplete: () -> Unit
) {
    val statusColor = when {
        isCompleted -> CleanColors.success
        isOverdue -> CleanColors.error
        else -> when (priority) {
            Priority.HIGH -> CleanColors.priorityHigh
            Priority.MEDIUM -> CleanColors.priorityMedium
            Priority.LOW -> CleanColors.priorityLow
            else -> CleanColors.primary
        }
    }

    val statusText = when {
        isCompleted -> "已完成"
        isOverdue -> "已逾期"
        else -> "进行中"
    }

    val statusIcon = when {
        isCompleted -> Icons.Outlined.CheckCircle
        isOverdue -> Icons.Outlined.Warning
        else -> Icons.Outlined.PlayCircle
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = statusColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(IconSize.lg)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = statusText,
                    style = CleanTypography.title,
                    color = statusColor
                )
            }

            Surface(
                onClick = onToggleComplete,
                shape = RoundedCornerShape(Radius.sm),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Outlined.Replay else Icons.Outlined.Check,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = if (isCompleted) "恢复" else "完成",
                        style = CleanTypography.button,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanTitleSection(
    title: String,
    description: String,
    isCompleted: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = CleanTypography.headline,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            color = if (isCompleted) CleanColors.textTertiary else CleanColors.textPrimary
        )

        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = description,
                style = CleanTypography.body,
                color = CleanColors.textSecondary,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}

@Composable
private fun CleanDetailsCard(
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = "详细信息",
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // 截止日期
            todo.dueDate?.let { date ->
                CleanDetailRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "截止日期",
                    value = formatDueDate(date),
                    valueColor = if (isOverdue) CleanColors.error else null
                )
                CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))
            }

            // 截止时间
            todo.dueTime?.let { time ->
                CleanDetailRow(
                    icon = Icons.Outlined.Schedule,
                    label = "截止时间",
                    value = time
                )
                CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))
            }

            // 地点
            todo.location?.let { location ->
                if (location.isNotBlank()) {
                    CleanDetailRow(
                        icon = Icons.Outlined.LocationOn,
                        label = "地点",
                        value = location
                    )
                    CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))
                }
            }

            // 优先级
            if (todo.priority != Priority.NONE) {
                val priorityColor = when (todo.priority) {
                    Priority.HIGH -> CleanColors.priorityHigh
                    Priority.MEDIUM -> CleanColors.priorityMedium
                    Priority.LOW -> CleanColors.priorityLow
                    else -> null
                }
                CleanDetailRow(
                    icon = Icons.Outlined.Flag,
                    label = "优先级",
                    value = formatPriority(todo.priority),
                    valueColor = priorityColor
                )
                CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))
            }

            // 四象限
            todo.quadrant?.let { quadrant ->
                CleanDetailRow(
                    icon = Icons.Outlined.GridView,
                    label = "象限",
                    value = formatQuadrant(quadrant)
                )
                CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))
            }

            // 提醒时间
            todo.reminderAt?.let { reminder ->
                CleanDetailRow(
                    icon = Icons.Outlined.Notifications,
                    label = "提醒",
                    value = formatReminderTime(reminder)
                )
                CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))
            }

            // 重复规则
            if (todo.repeatRule != "NONE") {
                CleanDetailRow(
                    icon = Icons.Outlined.Repeat,
                    label = "重复",
                    value = formatRepeatRule(todo.repeatRule)
                )
                CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))
            }

            // 关联目标
            linkedGoalName?.let { goalName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onNavigateToGoal != null) {
                            onNavigateToGoal?.invoke()
                        }
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = null,
                        tint = CleanColors.primary,
                        modifier = Modifier.size(IconSize.md)
                    )
                    Spacer(modifier = Modifier.width(Spacing.lg))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "关联目标",
                            style = CleanTypography.caption,
                            color = CleanColors.textTertiary
                        )
                        Text(
                            text = goalName,
                            style = CleanTypography.body,
                            color = CleanColors.primary
                        )
                    }
                    if (onNavigateToGoal != null) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "查看目标",
                            tint = CleanColors.textTertiary,
                            modifier = Modifier.size(IconSize.sm)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanDetailRow(
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
            tint = CleanColors.textTertiary,
            modifier = Modifier.size(IconSize.md)
        )
        Spacer(modifier = Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = CleanTypography.caption,
                color = CleanColors.textTertiary
            )
            Text(
                text = value,
                style = CleanTypography.body,
                color = valueColor ?: CleanColors.textPrimary
            )
        }
    }
}

@Composable
private fun CleanSubTodosSection(
    subTodos: List<TodoEntity>,
    onToggleComplete: (Long) -> Unit,
    onAddSubTodo: () -> Unit,
    onDelete: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "子任务",
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                    if (subTodos.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        val completed = subTodos.count { it.status == TodoStatus.COMPLETED }
                        StatusTag(
                            text = "$completed/${subTodos.size}",
                            color = CleanColors.primary
                        )
                    }
                }
                IconButton(onClick = onAddSubTodo) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加子任务",
                        tint = CleanColors.primary
                    )
                }
            }

            if (subTodos.isEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                EmptyStateView(
                    message = "暂无子任务",
                    icon = Icons.Outlined.Checklist
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.md))
                subTodos.forEach { subTodo ->
                    CleanSubTodoItem(
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
private fun CleanSubTodoItem(
    subTodo: TodoEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = subTodo.status == TodoStatus.COMPLETED
    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) CleanColors.surfaceVariant else CleanColors.surface,
        label = "subTodoBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        shape = RoundedCornerShape(Radius.sm),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 复选框
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) CleanColors.success else CleanColors.success.copy(alpha = 0.15f)
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

            Spacer(modifier = Modifier.width(Spacing.md))

            Text(
                text = subTodo.title,
                style = CleanTypography.body,
                modifier = Modifier.weight(1f),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isCompleted) CleanColors.textTertiary else CleanColors.textPrimary,
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
                    tint = CleanColors.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CleanTimestampCard(
    createdAt: Long,
    completedAt: Long?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "创建时间",
                    style = CleanTypography.secondary,
                    color = CleanColors.textTertiary
                )
                Text(
                    text = formatTimestamp(createdAt),
                    style = CleanTypography.secondary,
                    color = CleanColors.textTertiary
                )
            }
            if (completedAt != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                CleanDivider()
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "完成时间",
                        style = CleanTypography.secondary,
                        color = CleanColors.textTertiary
                    )
                    Text(
                        text = formatTimestamp(completedAt),
                        style = CleanTypography.secondary,
                        color = CleanColors.success
                    )
                }
            }
        }
    }
}

/**
 * 删除确认对话框
 */
@Composable
private fun CleanDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = CleanColors.error,
                modifier = Modifier.size(IconSize.lg)
            )
        },
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
