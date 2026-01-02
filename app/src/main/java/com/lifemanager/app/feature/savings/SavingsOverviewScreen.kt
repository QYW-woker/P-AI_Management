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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.SavingsPlanWithDetails
import com.lifemanager.app.domain.model.SavingsStats
import com.lifemanager.app.domain.model.SavingsUiState
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * 存钱总览页面
 *
 * 展示所有存钱计划汇总和快速存钱入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsOverviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuickSavings: (Long?) -> Unit,
    onNavigateToPlanDetail: (Long) -> Unit,
    onNavigateToAddPlan: () -> Unit,
    viewModel: SavingsPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val plans by viewModel.plans.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = "存钱计划",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPlan,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
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
                    contentPadding = PaddingValues(AppDimens.PageHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
                ) {
                    // 总统计卡片
                    item {
                        TotalStatsCard(stats = stats)
                    }

                    // 快速存钱按钮
                    item {
                        QuickSavingsButton(
                            enabled = plans.any { it.plan.status == "ACTIVE" },
                            onClick = { onNavigateToQuickSavings(null) }
                        )
                    }

                    // 进行中的计划
                    val activePlans = plans.filter { it.plan.status == "ACTIVE" }
                    if (activePlans.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = "进行中 (${activePlans.size})",
                                centered = true
                            )
                        }

                        items(activePlans, key = { it.plan.id }) { planWithDetails ->
                            PlanCard(
                                planWithDetails = planWithDetails,
                                onClick = { onNavigateToPlanDetail(planWithDetails.plan.id) },
                                onQuickDeposit = { onNavigateToQuickSavings(planWithDetails.plan.id) },
                                formatDate = { viewModel.formatDate(it) }
                            )
                        }
                    }

                    // 已完成的计划
                    val completedPlans = plans.filter { it.plan.status == "COMPLETED" }
                    if (completedPlans.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = "已完成 (${completedPlans.size})",
                                centered = true,
                                modifier = Modifier.padding(top = AppDimens.SpacingSmall)
                            )
                        }

                        items(completedPlans, key = { it.plan.id }) { planWithDetails ->
                            PlanCard(
                                planWithDetails = planWithDetails,
                                onClick = { onNavigateToPlanDetail(planWithDetails.plan.id) },
                                onQuickDeposit = null,
                                formatDate = { viewModel.formatDate(it) }
                            )
                        }
                    }

                    // 空状态
                    if (plans.isEmpty()) {
                        item {
                            EmptyState(onAction = onNavigateToAddPlan)
                        }
                    }

                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalStatsCard(stats: SavingsStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    UnifiedStatsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "总进度",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("%.1f%%", stats.overallProgress * 100),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            LinearProgressIndicator(
                progress = stats.overallProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.ProgressBarHeight)
                    .clip(AppShapes.Small),
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    label = "已存",
                    value = "¥${numberFormat.format(stats.totalCurrent)}",
                    valueColor = Color(0xFF4CAF50)
                )
                StatColumn(
                    label = "目标",
                    value = "¥${numberFormat.format(stats.totalTarget)}",
                    valueColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatColumn(
                    label = "进行中",
                    value = "${stats.activePlans}个",
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun QuickSavingsButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = AppShapes.Large,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50)
        )
    ) {
        Icon(
            Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
        Text(
            text = "快速存钱",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlanCard(
    planWithDetails: SavingsPlanWithDetails,
    onClick: () -> Unit,
    onQuickDeposit: (() -> Unit)?,
    formatDate: (Int) -> String
) {
    val plan = planWithDetails.plan
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    val planColor = try {
        Color(android.graphics.Color.parseColor(plan.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    UnifiedCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 颜色圆点
            Box(
                modifier = Modifier
                    .size(48.dp)
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

            Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (plan.status == "COMPLETED") {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = AppShapes.Small
                        ) {
                            Text(
                                text = "已完成",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "¥${numberFormat.format(plan.currentAmount)} / ¥${numberFormat.format(plan.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = planColor
                    )
                    Text(
                        text = String.format("%.1f%%", planWithDetails.progress * 100),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = planColor
                    )
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

                LinearProgressIndicator(
                    progress = planWithDetails.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AppShapes.Small),
                    color = planColor,
                    trackColor = planColor.copy(alpha = 0.2f)
                )

                if (plan.status == "ACTIVE") {
                    Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!planWithDetails.isOnTrack) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "剩余${planWithDetails.daysRemaining}天",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (onQuickDeposit != null) {
                            TextButton(
                                onClick = onQuickDeposit,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("存钱")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.SpacingXXLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Savings,
            contentDescription = null,
            modifier = Modifier.size(AppDimens.IconXXLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))
        Text(
            text = "暂无存钱计划",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
        Text(
            text = "创建一个存钱计划，开始积累财富吧！",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))
        Button(
            onClick = onAction,
            shape = AppShapes.Medium
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text("创建第一个计划")
        }
    }
}
