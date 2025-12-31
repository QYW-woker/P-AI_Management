package com.lifemanager.app.feature.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalRecordEntity
import com.lifemanager.app.core.database.entity.GoalRecordType
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.ui.component.PremiumTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 目标详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    onNavigateBack: () -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val goal by viewModel.goal.collectAsState()
    val records by viewModel.records.collectAsState()
    val childGoals by viewModel.childGoals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 对话框状态
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var showAbandonDialog by remember { mutableStateOf(false) }
    var showAddSubGoalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(goalId) {
        viewModel.loadGoal(goalId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("目标详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    goal?.let { g ->
                        if (g.status == GoalStatus.ACTIVE) {
                            IconButton(onClick = { showAbandonDialog = true }) {
                                Icon(Icons.Default.Close, contentDescription = "放弃目标")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            goal?.let { g ->
                if (g.status == GoalStatus.ACTIVE) {
                    FloatingActionButton(
                        onClick = { showAddRecordDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加记录")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            goal?.let { currentGoal ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 目标标题和状态
                    item {
                        GoalHeaderCard(
                            goal = currentGoal,
                            progress = viewModel.calculateProgress(currentGoal)
                        )
                    }

                    // 进度展示（带动画）
                    item {
                        AnimatedProgressCard(
                            goal = currentGoal,
                            progress = viewModel.calculateProgress(currentGoal),
                            onUpdateProgress = { showAddRecordDialog = true }
                        )
                    }

                    // 子目标列表
                    if (childGoals.isNotEmpty()) {
                        item {
                            Text(
                                text = "子目标",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(childGoals) { childGoal ->
                            SubGoalCard(
                                goal = childGoal,
                                progress = viewModel.calculateProgress(childGoal),
                                onClick = { /* Navigate to child goal detail */ }
                            )
                        }
                    }

                    // 添加子目标按钮
                    if (currentGoal.status == GoalStatus.ACTIVE) {
                        item {
                            OutlinedButton(
                                onClick = { showAddSubGoalDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("添加子目标")
                            }
                        }
                    }

                    // 时间轴标题
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "时间轴",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${records.size} 条记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 时间轴记录
                    if (records.isEmpty()) {
                        item {
                            EmptyTimelineCard()
                        }
                    } else {
                        items(records) { record ->
                            TimelineItem(
                                record = record,
                                isFirst = record == records.first(),
                                isLast = record == records.last()
                            )
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("目标不存在")
                }
            }
        }
    }

    // 添加记录对话框
    if (showAddRecordDialog) {
        AddRecordDialog(
            goal = goal,
            onDismiss = { showAddRecordDialog = false },
            onConfirm = { title, content, progressValue ->
                viewModel.addRecord(title, content, progressValue)
                showAddRecordDialog = false
            }
        )
    }

    // 放弃目标对话框
    if (showAbandonDialog) {
        AbandonGoalDialog(
            onDismiss = { showAbandonDialog = false },
            onConfirm = { reason ->
                viewModel.abandonGoal(reason)
                showAbandonDialog = false
            }
        )
    }

    // 添加子目标对话框
    if (showAddSubGoalDialog) {
        AddSubGoalDialog(
            parentGoal = goal,
            onDismiss = { showAddSubGoalDialog = false },
            onConfirm = { title, description, targetValue ->
                viewModel.addSubGoal(title, description, targetValue)
                showAddSubGoalDialog = false
            }
        )
    }
}

/**
 * 目标头部卡片
 */
@Composable
private fun GoalHeaderCard(
    goal: GoalEntity,
    progress: Float
) {
    val statusColor = when (goal.status) {
        GoalStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        GoalStatus.COMPLETED -> Color(0xFF4CAF50)
        GoalStatus.ABANDONED -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.outline
    }

    val statusText = when (goal.status) {
        GoalStatus.ACTIVE -> "进行中"
        GoalStatus.COMPLETED -> "已完成"
        GoalStatus.ABANDONED -> "已放弃"
        else -> "已归档"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 状态标签
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 日期信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "开始日期",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatEpochDay(goal.startDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                goal.endDate?.let { endDate ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "结束日期",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatEpochDay(endDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 放弃原因（如有）
            if (goal.status == GoalStatus.ABANDONED && !goal.abandonReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF44336).copy(alpha = 0.1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "放弃原因",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            text = goal.abandonReason,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 动画进度卡片
 */
@Composable
private fun AnimatedProgressCard(
    goal: GoalEntity,
    progress: Float,
    onUpdateProgress: () -> Unit
) {
    // 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // 颜色动画
    val progressColor by animateColorAsState(
        targetValue = when {
            progress >= 1f -> Color(0xFF4CAF50)
            progress >= 0.7f -> Color(0xFF8BC34A)
            progress >= 0.3f -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        },
        label = "color"
    )

    // 脉冲动画（进行中状态）
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 圆形进度指示器
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // 背景圆环
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawArc(
                        color = progressColor.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 进度圆环
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                progressColor.copy(alpha = if (goal.status == GoalStatus.ACTIVE) pulseAlpha else 1f),
                                progressColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 中间文字
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    if (goal.progressType == "NUMERIC") {
                        Text(
                            text = "${goal.currentValue.toInt()}/${goal.targetValue?.toInt() ?: 0} ${goal.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 更新进度按钮
            if (goal.status == GoalStatus.ACTIVE) {
                Button(
                    onClick = onUpdateProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Update, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("更新进度")
                }
            }
        }
    }
}

/**
 * 子目标卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubGoalCard(
    goal: GoalEntity,
    progress: Float,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 进度圆圈
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = when {
                        goal.status == GoalStatus.COMPLETED -> Color(0xFF4CAF50)
                        progress >= 0.5f -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFFFF9800)
                    }
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (goal.status == GoalStatus.COMPLETED) {
                    Text(
                        text = "已完成",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 时间轴项目
 */
@Composable
private fun TimelineItem(
    record: GoalRecordEntity,
    isFirst: Boolean,
    isLast: Boolean
) {
    val iconColor = when (record.recordType) {
        GoalRecordType.START -> Color(0xFF2196F3)
        GoalRecordType.PROGRESS -> MaterialTheme.colorScheme.primary
        GoalRecordType.MILESTONE -> Color(0xFFFF9800)
        GoalRecordType.COMPLETE -> Color(0xFF4CAF50)
        GoalRecordType.ABANDON -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.outline
    }

    val icon = when (record.recordType) {
        GoalRecordType.START -> Icons.Default.PlayArrow
        GoalRecordType.PROGRESS -> Icons.Default.TrendingUp
        GoalRecordType.MILESTONE -> Icons.Default.Flag
        GoalRecordType.COMPLETE -> Icons.Default.CheckCircle
        GoalRecordType.ABANDON -> Icons.Default.Cancel
        else -> Icons.Default.Notes
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 时间轴线和图标
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconColor
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        // 内容卡片
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatEpochDay(record.recordDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (record.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 进度变化
                if (record.progressValue != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${record.progressValue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空时间轴提示
 */
@Composable
private fun EmptyTimelineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Timeline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角按钮添加进度记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 添加记录对话框
 */
@Composable
private fun AddRecordDialog(
    goal: GoalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, progressValue: Double?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记录") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "标题",
                    placeholder = "如：完成第一阶段",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PremiumTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = "详细内容（可选）",
                    placeholder = "记录更多细节...",
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                if (goal?.progressType == "NUMERIC") {
                    PremiumTextField(
                        value = progressValue,
                        onValueChange = { progressValue = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "进度增加值",
                        placeholder = "如：10",
                        trailingIcon = { Text(goal.unit) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.ifBlank { "进度更新" },
                        content,
                        progressValue.toDoubleOrNull()
                    )
                },
                enabled = title.isNotBlank() || content.isNotBlank() || progressValue.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 放弃目标对话框
 */
@Composable
private fun AbandonGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("放弃目标") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "确定要放弃这个目标吗？此操作无法撤销。",
                    style = MaterialTheme.typography.bodyMedium
                )

                PremiumTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "放弃原因（必填）",
                    placeholder = "请说明放弃的原因...",
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("确认放弃")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 添加子目标对话框
 */
@Composable
private fun AddSubGoalDialog(
    parentGoal: GoalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, targetValue: Double?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加子目标") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "子目标标题",
                    placeholder = "如：完成第一章",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PremiumTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "描述（可选）",
                    placeholder = "详细说明...",
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (parentGoal?.progressType == "NUMERIC") {
                    PremiumTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "目标值",
                        placeholder = "如：100",
                        trailingIcon = { Text(parentGoal.unit) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, targetValue.toDoubleOrNull()) },
                enabled = title.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 格式化epochDay为日期字符串
 */
private fun formatEpochDay(epochDay: Int): String {
    val date = LocalDate.ofEpochDay(epochDay.toLong())
    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    return date.format(formatter)
}
