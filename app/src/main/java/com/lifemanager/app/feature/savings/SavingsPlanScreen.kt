package com.lifemanager.app.feature.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.Milestone
import com.lifemanager.app.domain.model.SavingsPlanWithDetails
import com.lifemanager.app.domain.model.SavingsStats
import com.lifemanager.app.domain.model.SavingsUiState
import com.lifemanager.app.domain.model.getStatusDisplayText
import java.text.NumberFormat
import java.util.Locale

/**
 * 存钱计划主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsPlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavingsPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val plans by viewModel.plans.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val showPlanDialog by viewModel.showPlanDialog.collectAsState()
    val showDepositDialog by viewModel.showDepositDialog.collectAsState()
    val showWithdrawDialog by viewModel.showWithdrawDialog.collectAsState()
    val showHistoryDialog by viewModel.showHistoryDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val currentHistoryPlan by viewModel.currentHistoryPlan.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("存钱计划") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddPlanDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = "添加计划")
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is SavingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SavingsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as SavingsUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("重试")
                        }
                    }
                }
            }

            is SavingsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 统计卡片
                    item {
                        SavingsStatsCard(stats = stats)
                    }

                    // 计划列表标题
                    item {
                        Text(
                            text = "我的计划",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (plans.isEmpty()) {
                        item {
                            EmptyState(
                                message = "暂无存钱计划",
                                actionText = "创建第一个计划",
                                onAction = { viewModel.showAddPlanDialog() }
                            )
                        }
                    } else {
                        items(plans, key = { it.plan.id }) { planWithDetails ->
                            PlanItem(
                                planWithDetails = planWithDetails,
                                onDeposit = { viewModel.showDepositDialog(planWithDetails.plan.id) },
                                onWithdraw = { viewModel.showWithdrawDialog(planWithDetails.plan.id) },
                                onShowHistory = { viewModel.showHistoryDialog(planWithDetails.plan.id) },
                                onClick = { viewModel.showEditPlanDialog(planWithDetails.plan.id) },
                                onDelete = { viewModel.showDeleteConfirm(planWithDetails.plan.id) },
                                formatDate = { viewModel.formatDate(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑计划对话框
    if (showPlanDialog) {
        AddEditPlanDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hidePlanDialog() }
        )
    }

    // 存款对话框
    if (showDepositDialog) {
        DepositDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideDepositDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("删除计划将同时删除所有存款记录，确定要删除吗？") },
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

    // 取款对话框
    if (showWithdrawDialog) {
        val currentPlan = plans.find { viewModel.recordEditState.value.planId == it.plan.id }
        WithdrawDialog(
            viewModel = viewModel,
            maxAmount = currentPlan?.plan?.currentAmount ?: 0.0,
            onDismiss = { viewModel.hideWithdrawDialog() }
        )
    }

    // 历史记录对话框
    if (showHistoryDialog && currentHistoryPlan != null) {
        RecordHistoryDialog(
            planDetails = currentHistoryPlan!!,
            onDismiss = { viewModel.hideHistoryDialog() },
            formatDate = { viewModel.formatDate(it) }
        )
    }
}

@Composable
private fun SavingsStatsCard(stats: SavingsStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 总进度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总进度",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.1f%%", stats.overallProgress * 100),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            LinearProgressIndicator(
                progress = stats.overallProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "已存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${numberFormat.format(stats.totalCurrent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "目标",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${numberFormat.format(stats.totalTarget)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "进行中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.activePlans}个",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 本月存款统计
            if (stats.thisMonthDeposit > 0 || stats.lastMonthDeposit > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "本月存款",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${numberFormat.format(stats.thisMonthDeposit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    // 月度变化
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPositive = stats.isPositiveChange()
                        val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Icon(
                            imageVector = if (isPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stats.getMonthlyChangeText(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = changeColor
                        )
                    }
                }
            }

            // 存款连续天数和总天数统计
            if (stats.savingsStreak > 0 || stats.totalRecords > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 连续存款天数
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "连续存款",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${stats.savingsStreak}天",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.savingsStreak >= 7) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 总存款天数
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📅",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "累计存款",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${stats.totalRecords}天",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanItem(
    planWithDetails: SavingsPlanWithDetails,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onShowHistory: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    formatDate: (Int) -> String
) {
    val plan = planWithDetails.plan
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    val planColor = try {
        Color(android.graphics.Color.parseColor(plan.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(planColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Savings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "目标: ${formatDate(plan.targetDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (plan.status != "ACTIVE") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = getStatusDisplayText(plan.status),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "¥${numberFormat.format(plan.currentAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = planColor
                    )
                    Text(
                        text = "/ ¥${numberFormat.format(plan.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = String.format("%.1f%%", planWithDetails.progress * 100),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = planColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = planWithDetails.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = planColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 里程碑进度
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${planWithDetails.currentMilestone.icon} ${planWithDetails.currentMilestone.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = planColor
                )
                planWithDetails.nextMilestone?.let { next ->
                    Text(
                        text = " → ${next.icon} ${next.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!planWithDetails.isOnTrack && plan.status == "ACTIVE") {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "进度落后",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "剩余${planWithDetails.daysRemaining}天",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plan.status == "ACTIVE") {
                    // 存款按钮
                    OutlinedButton(
                        onClick = onDeposit,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("存款", style = MaterialTheme.typography.labelMedium)
                    }

                    // 取款按钮
                    if (plan.currentAmount > 0) {
                        OutlinedButton(
                            onClick = onWithdraw,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF44336)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("取款", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 记录按钮
                OutlinedButton(
                    onClick = onShowHistory,
                    modifier = if (plan.status != "ACTIVE") Modifier.weight(1f) else Modifier,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("记录", style = MaterialTheme.typography.labelMedium)
                }

                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
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
private fun EmptyState(
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Savings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}
