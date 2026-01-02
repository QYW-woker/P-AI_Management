package com.lifemanager.app.feature.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import com.lifemanager.app.domain.model.SavingsPlanWithDetails
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 存钱计划详情页面
 *
 * 展示计划完整信息、进度和存款历史
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsPlanDetailScreen(
    planId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToQuickSavings: (Long) -> Unit,
    onNavigateToRecordDetail: (Long) -> Unit,
    viewModel: SavingsPlanViewModel = hiltViewModel()
) {
    val planWithDetails by viewModel.getPlanById(planId).collectAsState(initial = null)
    val deposits by viewModel.getDepositsForPlan(planId).collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(planId) {
        viewModel.loadPlanDetail(planId)
    }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = "计划详情",
                onNavigateBack = onNavigateBack,
                actions = {
                    planWithDetails?.let { plan ->
                        IconButton(onClick = { onNavigateToEdit(planId) }) {
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
        },
        floatingActionButton = {
            planWithDetails?.let { planData ->
                if (planData.plan.status == "ACTIVE") {
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToQuickSavings(planId) },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text("存钱")
                    }
                }
            }
        }
    ) { paddingValues ->
        planWithDetails?.let { planData ->
            val plan = planData.plan
            val planColor = try {
                Color(android.graphics.Color.parseColor(plan.color))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(AppDimens.PageHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
            ) {
                // 进度环卡片
                item {
                    ProgressRingCard(
                        planWithDetails = planData,
                        planColor = planColor,
                        numberFormat = numberFormat
                    )
                }

                // 统计信息卡片
                item {
                    StatsInfoCard(
                        planWithDetails = planData,
                        planColor = planColor,
                        numberFormat = numberFormat,
                        depositsCount = deposits.size
                    )
                }

                // 存款记录标题
                item {
                    SectionTitle(
                        title = "存款记录 (${deposits.size})",
                        centered = true,
                        modifier = Modifier.padding(top = AppDimens.SpacingSmall)
                    )
                }

                // 存款记录列表
                if (deposits.isEmpty()) {
                    item {
                        EmptyDepositsState()
                    }
                } else {
                    items(deposits, key = { it.id }) { deposit ->
                        DepositRecordItem(
                            deposit = deposit,
                            planColor = planColor,
                            onClick = { onNavigateToRecordDetail(deposit.id) }
                        )
                    }
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(80.dp))
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
            text = { Text("删除计划将同时删除所有存款记录，确定要删除吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlan(planId)
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
private fun ProgressRingCard(
    planWithDetails: SavingsPlanWithDetails,
    planColor: Color,
    numberFormat: NumberFormat
) {
    val plan = planWithDetails.plan

    UnifiedCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 圆形进度
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                CircularProgressIndicator(
                    progress = planWithDetails.progress,
                    modifier = Modifier.size(180.dp),
                    strokeWidth = 16.dp,
                    color = planColor,
                    trackColor = planColor.copy(alpha = 0.2f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f%%", planWithDetails.progress * 100),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = planColor
                    )
                    Text(
                        text = "¥${numberFormat.format(plan.currentAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ ¥${numberFormat.format(plan.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            // 计划名称
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // 状态标签
            if (plan.status == "COMPLETED") {
                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
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
private fun StatsInfoCard(
    planWithDetails: SavingsPlanWithDetails,
    planColor: Color,
    numberFormat: NumberFormat,
    depositsCount: Int
) {
    val plan = planWithDetails.plan
    val remaining = plan.targetAmount - plan.currentAmount

    UnifiedCard {
        Column {
            SectionTitle(title = "计划信息", centered = true)

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumn(
                    label = "剩余金额",
                    value = "¥${numberFormat.format(remaining.coerceAtLeast(0.0))}",
                    valueColor = if (remaining <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                InfoColumn(
                    label = "剩余天数",
                    value = "${planWithDetails.daysRemaining}天",
                    valueColor = if (planWithDetails.daysRemaining < 7) MaterialTheme.colorScheme.error else null
                )
                InfoColumn(
                    label = "存款次数",
                    value = "${depositsCount}次"
                )
            }

            if (plan.status == "ACTIVE" && !planWithDetails.isOnTrack) {
                Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Medium,
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(AppDimens.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = "进度落后，建议加快存款速度",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
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
private fun DepositRecordItem(
    deposit: SavingsRecordEntity,
    planColor: Color,
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    UnifiedCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(planColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = planColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateFromInt(deposit.depositDate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (deposit.note?.isNotBlank() == true) {
                    Text(
                        text = deposit.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "+¥${numberFormat.format(deposit.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyDepositsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.SpacingXXLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))
        Text(
            text = "暂无存款记录",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "点击下方按钮开始存钱",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun formatDateFromInt(dateInt: Int): String {
    val year = dateInt / 10000
    val month = (dateInt % 10000) / 100
    val day = dateInt % 100
    return "${year}年${month}月${day}日"
}
