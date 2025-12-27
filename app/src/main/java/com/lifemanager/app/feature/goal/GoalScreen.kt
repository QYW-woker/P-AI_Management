package com.lifemanager.app.feature.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.domain.model.GoalUiState
import com.lifemanager.app.domain.model.getCategoryDisplayName
import com.lifemanager.app.domain.model.getGoalTypeDisplayName

/**
 * 目标管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showProgressDialog by viewModel.showProgressDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("目标管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加目标")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 统计卡片
            StatsCard(statistics = statistics)

            // 筛选标签
            FilterChips(
                currentFilter = currentFilter,
                onFilterChange = { viewModel.setFilter(it) }
            )

            // 目标列表
            when (uiState) {
                is GoalUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is GoalUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as GoalUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is GoalUiState.Success -> {
                    if (goals.isEmpty()) {
                        EmptyState(currentFilter = currentFilter)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(goals, key = { it.id }) { goal ->
                                GoalCard(
                                    goal = goal,
                                    progress = viewModel.calculateProgress(goal),
                                    remainingDays = viewModel.getRemainingDays(goal),
                                    onEdit = { viewModel.showEditDialog(goal.id) },
                                    onUpdateProgress = { viewModel.showProgressDialog(goal) },
                                    onComplete = { viewModel.completeGoal(goal.id) },
                                    onDelete = { viewModel.showDeleteConfirm(goal.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        AddEditGoalDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个目标吗？此操作不可恢复。") },
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

    // 更新进度对话框
    if (showProgressDialog) {
        UpdateProgressDialog(
            goal = viewModel.getProgressGoal(),
            onDismiss = { viewModel.hideProgressDialog() },
            onConfirm = { viewModel.updateProgress(it) }
        )
    }
}

@Composable
private fun StatsCard(
    statistics: com.lifemanager.app.domain.model.GoalStatistics
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "进行中",
                value = statistics.activeCount.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "已完成",
                value = statistics.completedCount.toString(),
                color = Color(0xFF4CAF50)
            )
            StatItem(
                label = "平均进度",
                value = "${(statistics.totalProgress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.tertiary
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
private fun FilterChips(
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filters = listOf(
        "ACTIVE" to "进行中",
        "COMPLETED" to "已完成",
        "ALL" to "全部"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (value, label) ->
            FilterChip(
                selected = currentFilter == value,
                onClick = { onFilterChange(value) },
                label = { Text(label) },
                leadingIcon = if (currentFilter == value) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    progress: Float,
    remainingDays: Int?,
    onEdit: () -> Unit,
    onUpdateProgress: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getCategoryColor(goal.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 分类标签
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = getCategoryDisplayName(goal.category),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 类型标签
                    Text(
                        text = getGoalTypeDisplayName(goal.goalType),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 状态指示
                if (goal.status == GoalStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已完成",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标题
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (goal.progressType == "NUMERIC" && goal.targetValue != null) {
                            "${goal.currentValue.toInt()}${goal.unit} / ${goal.targetValue.toInt()}${goal.unit}"
                        } else {
                            "${(progress * 100).toInt()}%"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    remainingDays?.let { days ->
                        Text(
                            text = if (days > 0) "剩余${days}天" else if (days == 0) "今天截止" else "已逾期${-days}天",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (days < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = categoryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // 操作按钮（仅活跃目标显示）
            if (goal.status == GoalStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onUpdateProgress) {
                        Icon(Icons.Default.Update, contentDescription = null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("更新进度")
                    }
                    if (progress >= 1f || goal.progressType == "PERCENTAGE") {
                        TextButton(onClick = onComplete) {
                            Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("完成")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(currentFilter: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (currentFilter) {
                    "COMPLETED" -> "暂无已完成的目标"
                    else -> "暂无目标"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮创建新目标",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun UpdateProgressDialog(
    goal: GoalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    if (goal == null) return

    var value by remember { mutableStateOf(goal.currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新进度") },
        text = {
            Column {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (goal.progressType == "NUMERIC") "当前数值" else "完成百分比"
                        )
                    },
                    suffix = {
                        Text(
                            if (goal.progressType == "NUMERIC") goal.unit else "%"
                        )
                    },
                    singleLine = true
                )
                if (goal.progressType == "NUMERIC" && goal.targetValue != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "目标: ${goal.targetValue.toInt()}${goal.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    value.toDoubleOrNull()?.let { onConfirm(it) }
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
