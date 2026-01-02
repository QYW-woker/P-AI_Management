package com.lifemanager.app.feature.goal

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
import com.lifemanager.app.domain.model.getCategoryDisplayName
import com.lifemanager.app.domain.model.getGoalTypeDisplayName
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
    val goal by viewModel.getGoalById(goalId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProgressInput by remember { mutableStateOf(false) }
    var progressValue by remember { mutableStateOf("") }

    LaunchedEffect(goalId) {
        viewModel.loadGoalDetail(goalId)
    }

    Scaffold(
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
        goal?.let { currentGoal ->
            val progress = viewModel.calculateProgress(currentGoal)
            val remainingDays = viewModel.getRemainingDays(currentGoal)
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

                // 快速更新进度
                if (currentGoal.status == GoalStatus.ACTIVE) {
                    item {
                        QuickProgressUpdateCard(
                            goal = currentGoal,
                            showInput = showProgressInput,
                            progressValue = progressValue,
                            onToggleInput = { showProgressInput = !showProgressInput },
                            onValueChange = { progressValue = it },
                            onConfirm = {
                                progressValue.toDoubleOrNull()?.let {
                                    viewModel.updateProgress(it)
                                    progressValue = ""
                                    showProgressInput = false
                                }
                            },
                            onComplete = {
                                viewModel.completeGoal(currentGoal.id)
                            }
                        )
                    }
                }

                // 目标信息卡片
                item {
                    GoalInfoCard(
                        goal = currentGoal,
                        remainingDays = remainingDays,
                        categoryColor = categoryColor
                    )
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
                    ProgressHistoryCard(goal = currentGoal)
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
            if (goal.status == GoalStatus.COMPLETED) {
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
        }
    }
}

@Composable
private fun QuickProgressUpdateCard(
    goal: GoalEntity,
    showInput: Boolean,
    progressValue: String,
    onToggleInput: () -> Unit,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onComplete: () -> Unit
) {
    UnifiedCard {
        Column {
            SectionTitle(title = "更新进度", centered = true)

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            if (showInput) {
                OutlinedTextField(
                    value = progressValue,
                    onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (goal.progressType == "NUMERIC") "当前数值" else "完成百分比")
                    },
                    suffix = {
                        Text(if (goal.progressType == "NUMERIC") goal.unit else "%")
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
                        shape = AppShapes.Medium
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Medium
                    ) {
                        Text("确认")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Button(
                        onClick = onToggleInput,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Medium
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null)
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text("更新进度")
                    }
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Medium
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text("标记完成")
                    }
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
private fun ProgressHistoryCard(goal: GoalEntity) {
    UnifiedCard {
        // 这里可以展示进度历史时间线
        // 目前简化为创建和最后更新信息
        Column {
            TimelineItem(
                title = "创建目标",
                subtitle = "开始追踪进度",
                time = formatDate(goal.createdAt),
                isFirst = true,
                isLast = goal.updatedAt == goal.createdAt
            )

            if (goal.updatedAt != goal.createdAt) {
                TimelineItem(
                    title = "最近更新",
                    subtitle = "当前进度: ${goal.currentValue.toInt()}${goal.unit}",
                    time = formatDate(goal.updatedAt),
                    isFirst = false,
                    isLast = goal.status != GoalStatus.COMPLETED
                )
            }

            if (goal.status == GoalStatus.COMPLETED) {
                TimelineItem(
                    title = "完成目标",
                    subtitle = "恭喜达成目标！",
                    time = formatDate(goal.updatedAt),
                    isFirst = false,
                    isLast = true,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
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

private fun formatDateFromInt(dateInt: Int): String {
    val year = dateInt / 10000
    val month = (dateInt % 10000) / 100
    val day = dateInt % 100
    return String.format("%02d-%02d", month, day)
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
