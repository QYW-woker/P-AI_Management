package com.lifemanager.app.feature.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.domain.model.AIAnalysisState
import com.lifemanager.app.domain.model.GoalProgressRecordUI
import com.lifemanager.app.domain.model.OperationResult
import com.lifemanager.app.domain.model.getCategoryDisplayName
import com.lifemanager.app.domain.model.getGoalTypeDisplayName
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 目标详情页面
 *
 * 展示目标完整信息、进度和历史记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val goalDetailState by viewModel.goalDetailState.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val aiAnalysisState by viewModel.aiAnalysisState.collectAsState()
    val goal = goalDetailState.goal
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProgressInput by remember { mutableStateOf(false) }
    var progressValue by remember { mutableStateOf("") }
    var showAIAnalysis by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val isAIConfigured = remember { viewModel.isAIConfigured() }

    // 观察操作结果并显示Snackbar
    LaunchedEffect(operationResult) {
        when (val result = operationResult) {
            is OperationResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = result.message,
                    duration = SnackbarDuration.Short
                )
            }
            is OperationResult.Error -> {
                snackbarHostState.showSnackbar(
                    message = result.message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    LaunchedEffect(goalId) {
        viewModel.loadGoalDetail(goalId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            UnifiedTopAppBar(
                title = "目标详情",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (goal != null) {
                        IconButton(onClick = { onNavigateToEdit(goalId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // 操作进行中显示loading指示
        val isOperationLoading = operationResult is OperationResult.Loading

        goal?.let { currentGoal ->
            val progress = goalDetailState.progress
            val remainingDays = goalDetailState.remainingDays
            val categoryColor = getCategoryColor(currentGoal.category)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(AppDimens.PageHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
            ) {
                // 进度环卡片
                item {
                    ProgressCard(
                        goal = currentGoal,
                        progress = progress,
                        categoryColor = categoryColor
                    )
                }

                // 快速更新进度或恢复已放弃目标
                when (currentGoal.status) {
                    GoalStatus.ACTIVE -> {
                        item {
                            QuickProgressUpdateCard(
                                goal = currentGoal,
                                showInput = showProgressInput,
                                progressValue = progressValue,
                                isLoading = isOperationLoading,
                                onToggleInput = { showProgressInput = !showProgressInput },
                                onValueChange = { progressValue = it },
                                onConfirm = {
                                    progressValue.toDoubleOrNull()?.let {
                                        viewModel.updateGoalProgress(currentGoal.id, it)
                                        progressValue = ""
                                        showProgressInput = false
                                    }
                                },
                                onComplete = {
                                    viewModel.completeGoal(currentGoal.id)
                                },
                                onAbandon = {
                                    viewModel.abandonGoal(currentGoal.id)
                                }
                            )
                        }
                    }
                    GoalStatus.ABANDONED -> {
                        item {
                            ReactivateCard(
                                isLoading = isOperationLoading,
                                onReactivate = {
                                    viewModel.reactivateGoal(currentGoal.id)
                                }
                            )
                        }
                    }
                    else -> {}
                }

                // 目标信息卡片
                item {
                    GoalInfoCard(
                        goal = currentGoal,
                        remainingDays = remainingDays,
                        categoryColor = categoryColor
                    )
                }

                // AI分析卡片
                if (isAIConfigured) {
                    item {
                        AIAnalysisCard(
                            aiAnalysisState = aiAnalysisState,
                            showAnalysis = showAIAnalysis,
                            onToggleAnalysis = { showAIAnalysis = !showAIAnalysis },
                            onAnalyze = {
                                viewModel.analyzeGoal(goalId)
                                showAIAnalysis = true
                            },
                            onClear = {
                                viewModel.clearAIAnalysis()
                                showAIAnalysis = false
                            }
                        )
                    }
                }

                // 进度历史（时间线）
                item {
                    SectionTitle(
                        title = "进度记录",
                        centered = true,
                        modifier = Modifier.padding(top = AppDimens.SpacingMedium)
                    )
                }

                item {
                    ProgressHistoryCard(
                        goal = currentGoal,
                        progressRecords = goalDetailState.progressRecords
                    )
                }
            }
        } ?: run {
            // 加载状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个目标吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(goalId)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ProgressCard(
    goal: GoalEntity,
    progress: Float,
    categoryColor: Color
) {
    UnifiedCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 圆形进度指示器
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 12.dp,
                    color = categoryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                    if (goal.progressType == "NUMERIC" && goal.targetValue != null) {
                        Text(
                            text = "${goal.currentValue.toInt()}${goal.unit} / ${goal.targetValue.toInt()}${goal.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            // 目标标题
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // 状态标签
            when (goal.status) {
                GoalStatus.COMPLETED -> {
                    Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))
                    Surface(
                        shape = AppShapes.Small,
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "已完成",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                GoalStatus.ABANDONED -> {
                    Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))
                    Surface(
                        shape = AppShapes.Small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "已放弃",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun QuickProgressUpdateCard(
    goal: GoalEntity,
    showInput: Boolean,
    progressValue: String,
    isLoading: Boolean = false,
    onToggleInput: () -> Unit,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onComplete: () -> Unit,
    onAbandon: () -> Unit = {}
) {
    UnifiedCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(title = "更新进度", centered = true)
                if (isLoading) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            if (showInput) {
                OutlinedTextField(
                    value = progressValue,
                    onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' || c == '-' }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (goal.progressType == "NUMERIC") "本次增加（如+1000）" else "增加百分比")
                    },
                    suffix = {
                        Text(if (goal.progressType == "NUMERIC") goal.unit else "%")
                    },
                    supportingText = {
                        if (goal.progressType == "NUMERIC" && goal.targetValue != null) {
                            Text("当前：${goal.currentValue.toInt()}${goal.unit} / 目标：${goal.targetValue.toInt()}${goal.unit}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = AppShapes.Medium
                )

                Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    OutlinedButton(
                        onClick = onToggleInput,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Medium,
                        enabled = !isLoading
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Medium,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("确认")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                    ) {
                        Button(
                            onClick = onToggleInput,
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.Medium,
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null)
                            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                            Text("更新进度")
                        }
                        OutlinedButton(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.Medium,
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                Text("标记完成")
                            }
                        }
                    }
                    // 放弃按钮
                    TextButton(
                        onClick = onAbandon,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("放弃目标")
                    }
                }
            }
        }
    }
}

/**
 * 恢复已放弃目标的卡片
 */
@Composable
private fun ReactivateCard(
    isLoading: Boolean = false,
    onReactivate: () -> Unit
) {
    UnifiedCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionTitle(title = "目标已放弃", centered = true)

            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

            Text(
                text = "您可以重新激活这个目标继续追踪进度",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            Button(
                onClick = onReactivate,
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.Medium,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text("重新激活")
                }
            }
        }
    }
}

@Composable
private fun GoalInfoCard(
    goal: GoalEntity,
    remainingDays: Int?,
    categoryColor: Color
) {
    UnifiedCard {
        Column {
            SectionTitle(title = "目标信息", centered = true)

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            // 分类和类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = AppShapes.Small,
                    color = categoryColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = getCategoryDisplayName(goal.category),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = categoryColor
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Surface(
                    shape = AppShapes.Small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = getGoalTypeDisplayName(goal.goalType),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))
            Divider()
            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            // 时间信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumn(
                    label = "创建日期",
                    value = formatDate(goal.createdAt)
                )
                InfoColumn(
                    label = "截止日期",
                    value = goal.endDate?.let { formatDateFromInt(it) } ?: "无"
                )
                InfoColumn(
                    label = "剩余天数",
                    value = remainingDays?.let {
                        when {
                            it > 0 -> "${it}天"
                            it == 0 -> "今天"
                            else -> "已过期"
                        }
                    } ?: "无限期",
                    valueColor = remainingDays?.let {
                        if (it < 0) MaterialTheme.colorScheme.error else null
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoColumn(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProgressHistoryCard(
    goal: GoalEntity,
    progressRecords: List<GoalProgressRecordUI>
) {
    UnifiedCard {
        Column {
            // 创建目标记录
            TimelineItem(
                title = "创建目标",
                subtitle = "开始追踪进度",
                time = formatDate(goal.createdAt),
                isFirst = true,
                isLast = progressRecords.isEmpty() && goal.status != GoalStatus.COMPLETED
            )

            // 进度更新记录（按时间倒序显示）
            progressRecords.forEachIndexed { index, record ->
                val isLast = index == progressRecords.size - 1 && goal.status != GoalStatus.COMPLETED
                val changeText = if (record.changeValue >= 0) {
                    "+${record.changeValue.toInt()}${goal.unit}"
                } else {
                    "${record.changeValue.toInt()}${goal.unit}"
                }

                TimelineItem(
                    title = "进度更新",
                    subtitle = if (goal.progressType == "NUMERIC") {
                        "$changeText → ${record.totalValue.toInt()}${goal.unit}"
                    } else {
                        "${record.previousValue.toInt()}% → ${record.totalValue.toInt()}%"
                    },
                    time = formatDateFromEpochDay(record.recordDate),
                    isFirst = false,
                    isLast = isLast,
                    color = if (record.changeValue >= 0) {
                        Color(0xFF4CAF50) // 绿色表示进度增加
                    } else {
                        MaterialTheme.colorScheme.error // 红色表示进度减少
                    }
                )
            }

            // 完成记录
            if (goal.status == GoalStatus.COMPLETED) {
                TimelineItem(
                    title = "目标完成",
                    subtitle = "恭喜达成目标！",
                    time = formatDate(goal.updatedAt),
                    isFirst = false,
                    isLast = true,
                    color = Color(0xFF4CAF50)
                )
            }

            // 如果没有记录，显示提示
            if (progressRecords.isEmpty() && goal.status != GoalStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                Text(
                    text = "暂无进度记录，点击上方\"更新进度\"开始记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
        }
    }
}

private fun formatDateFromEpochDay(epochDay: Int): String {
    val date = LocalDate.ofEpochDay(epochDay.toLong())
    return date.format(DateTimeFormatter.ofPattern("MM-dd"))
}

@Composable
private fun TimelineItem(
    title: String,
    subtitle: String,
    time: String,
    isFirst: Boolean,
    isLast: Boolean,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall)
    ) {
        // 时间线指示器
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MM-dd"))
}

private fun formatDateFromInt(epochDay: Int): String {
    val date = LocalDate.ofEpochDay(epochDay.toLong())
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "CAREER" -> Color(0xFF2196F3)
        "FINANCE" -> Color(0xFF4CAF50)
        "HEALTH" -> Color(0xFFE91E63)
        "LEARNING" -> Color(0xFFFF9800)
        "RELATIONSHIP" -> Color(0xFF9C27B0)
        "LIFESTYLE" -> Color(0xFF00BCD4)
        "HOBBY" -> Color(0xFFFF5722)
        else -> Color.Gray
    }
}

/**
 * AI分析卡片
 */
@Composable
private fun AIAnalysisCard(
    aiAnalysisState: AIAnalysisState,
    showAnalysis: Boolean,
    onToggleAnalysis: () -> Unit,
    onAnalyze: () -> Unit,
    onClear: () -> Unit
) {
    UnifiedCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI 智能分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (aiAnalysisState !is AIAnalysisState.Loading) {
                    TextButton(onClick = onToggleAnalysis) {
                        Text(if (showAnalysis) "收起" else "展开")
                    }
                }
            }

            AnimatedVisibility(visible = showAnalysis) {
                Column {
                    Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

                    when (aiAnalysisState) {
                        is AIAnalysisState.Idle -> {
                            Text(
                                text = "AI可以分析您的目标进度并给出个性化建议",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))
                            Button(
                                onClick = onAnalyze,
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.Medium
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始分析")
                            }
                        }

                        is AIAnalysisState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                                Text(
                                    text = "AI正在分析中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is AIAnalysisState.Success -> {
                            Surface(
                                shape = AppShapes.Medium,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(AppDimens.SpacingMedium)
                                ) {
                                    Text(
                                        text = aiAnalysisState.analysis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onClear,
                                    modifier = Modifier.weight(1f),
                                    shape = AppShapes.Medium
                                ) {
                                    Text("清除")
                                }
                                Button(
                                    onClick = onAnalyze,
                                    modifier = Modifier.weight(1f),
                                    shape = AppShapes.Medium
                                ) {
                                    Text("重新分析")
                                }
                            }
                        }

                        is AIAnalysisState.Error -> {
                            Surface(
                                shape = AppShapes.Medium,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(AppDimens.SpacingMedium),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = aiAnalysisState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                            Button(
                                onClick = onAnalyze,
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.Medium
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }
    }
}
