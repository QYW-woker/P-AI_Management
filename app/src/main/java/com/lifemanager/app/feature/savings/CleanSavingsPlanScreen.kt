package com.lifemanager.app.feature.savings

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * 存钱计划主界�?- 简洁设计版�?
 *
 * 设计原则:
 * - 干净、克制、有呼吸�?
 * - 轻灵不花�?
 * - 使用统一的设计系�?
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanSavingsPlanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
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
        containerColor = CleanColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "存钱计划",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CleanColors.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddPlanDialog() },
                containerColor = CleanColors.primary,
                contentColor = CleanColors.onPrimary,
                shape = RoundedCornerShape(Radius.md)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加计划")
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = uiState,
            animationSpec = tween(Duration.standard),
            label = "stateTransition"
        ) { state ->
            when (state) {
                is SavingsUiState.Loading -> {
                    PageLoadingState(modifier = Modifier.padding(paddingValues))
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
                                text = state.message,
                                style = CleanTypography.body,
                                color = CleanColors.error
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            CleanSecondaryButton(
                                text = "重试",
                                onClick = { viewModel.refresh() }
                            )
                        }
                    }
                }

                is SavingsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(
                            horizontal = Spacing.pageHorizontal,
                            vertical = Spacing.pageVertical
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
                    ) {
                        // 统计卡片
                        item(key = "stats") {
                            CleanSavingsStatsCard(stats = stats)
                        }

                        // 计划列表标题
                        item(key = "plans_header") {
                            Text(
                                text = "我的计划",
                                style = CleanTypography.title,
                                color = CleanColors.textPrimary
                            )
                        }

                        if (plans.isEmpty()) {
                            item(key = "empty") {
                                EmptyStateView(
                                    message = "暂无存钱计划",
                                    icon = Icons.Outlined.Savings,
                                    actionText = "创建第一个计�?,
                                    onActionClick = { viewModel.showAddPlanDialog() }
                                )
                            }
                        } else {
                            items(plans, key = { it.plan.id }) { planWithDetails ->
                                CleanPlanItem(
                                    planWithDetails = planWithDetails,
                                    onDeposit = { viewModel.showDepositDialog(planWithDetails.plan.id) },
                                    onWithdraw = { viewModel.showWithdrawDialog(planWithDetails.plan.id) },
                                    onShowHistory = { viewModel.showHistoryDialog(planWithDetails.plan.id) },
                                    onClick = { onNavigateToDetail(planWithDetails.plan.id) },
                                    onDelete = { viewModel.showDeleteConfirm(planWithDetails.plan.id) },
                                    formatDate = { viewModel.formatDate(it) }
                                )
                            }
                        }

                        // 底部安全间距
                        item {
                            Spacer(modifier = Modifier.height(Spacing.bottomSafe + 56.dp))
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑计划对话�?
    if (showPlanDialog) {
        AddEditPlanDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hidePlanDialog() }
        )
    }

    // 存款对话�?
    if (showDepositDialog) {
        DepositDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideDepositDialog() }
        )
    }

    // 删除确认对话�?
    if (showDeleteDialog) {
        CleanDeleteConfirmDialog(
            title = "确认删除",
            message = "删除计划将同时删除所有存款记录，确定要删除吗�?,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.hideDeleteConfirm() }
        )
    }

    // 取款对话�?
    if (showWithdrawDialog) {
        val currentPlan = plans.find { viewModel.recordEditState.value.planId == it.plan.id }
        WithdrawDialog(
            viewModel = viewModel,
            maxAmount = currentPlan?.plan?.currentAmount ?: 0.0,
            onDismiss = { viewModel.hideWithdrawDialog() }
        )
    }

    // 历史记录对话�?
    if (showHistoryDialog && currentHistoryPlan != null) {
        RecordHistoryDialog(
            planDetails = currentHistoryPlan!!,
            onDismiss = { viewModel.hideHistoryDialog() },
            formatDate = { viewModel.formatDate(it) }
        )
    }
}

/**
 * 简洁统计卡�?- 干净的数据展�?
 */
@Composable
private fun CleanSavingsStatsCard(stats: SavingsStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

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
                Text(
                    text = "总进�?,
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
                Text(
                    text = String.format("%.1f%%", stats.overallProgress * 100),
                    style = CleanTypography.secondary,
                    color = CleanColors.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 简洁进度条
            LinearProgressIndicator(
                progress = stats.overallProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CleanColors.primary,
                trackColor = CleanColors.borderLight
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CleanStatItem(
                    label = "已存",
                    value = "¥${numberFormat.format(stats.totalCurrent)}",
                    icon = Icons.Outlined.AccountBalance,
                    valueColor = CleanColors.success
                )
                CleanStatItem(
                    label = "目标",
                    value = "¥${numberFormat.format(stats.totalTarget)}",
                    icon = Icons.Outlined.Flag
                )
                CleanStatItem(
                    label = "进行�?,
                    value = "${stats.activePlans}�?,
                    icon = Icons.Outlined.Loop
                )
            }

            // 本月存款统计
            if (stats.thisMonthDeposit > 0 || stats.lastMonthDeposit > 0) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                CleanHorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "本月存款",
                            style = CleanTypography.caption,
                            color = CleanColors.textTertiary
                        )
                        Text(
                            text = "¥${numberFormat.format(stats.thisMonthDeposit)}",
                            style = CleanTypography.amountMedium,
                            color = CleanColors.success
                        )
                    }

                    // 月度变化
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPositive = stats.isPositiveChange()
                        val changeColor = if (isPositive) CleanColors.success else CleanColors.error
                        Icon(
                            imageVector = if (isPositive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(IconSize.sm)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = stats.getMonthlyChangeText(),
                            style = CleanTypography.secondary,
                            color = changeColor
                        )
                    }
                }
            }

            // 存款连续天数和总天数统�?
            if (stats.savingsStreak > 0 || stats.totalRecords > 0) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                CleanHorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CleanStatItem(
                        label = "连续存款",
                        value = "${stats.savingsStreak}�?,
                        icon = Icons.Outlined.LocalFireDepartment,
                        valueColor = if (stats.savingsStreak >= 7) CleanColors.warning else CleanColors.textPrimary
                    )
                    CleanStatItem(
                        label = "累计存款",
                        value = "${stats.totalRecords}�?,
                        icon = Icons.Outlined.CalendarMonth
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = CleanColors.textPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CleanColors.textTertiary,
            modifier = Modifier.size(IconSize.md)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = value,
            style = CleanTypography.amountSmall,
            color = valueColor
        )
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textTertiary
        )
    }
}

/**
 * 简洁计划项 - 优化�?
 *
 * 设计原则�?
 * - "存一�?按钮最突出，一眼可�?
 * - 所有操作无需滑动、无需寻找
 * - 信息展示清晰，分区明�?
 */
@Composable
private fun CleanPlanItem(
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
        CleanColors.primary
    }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ============ 第一区：信息区（可点击进入详情） ============
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = true, color = planColor),
                        onClick = onClick
                    )
                    .padding(Spacing.lg)
            ) {
                // 标题�?
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(planColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            tint = planColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.md))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plan.name,
                                style = CleanTypography.body,
                                color = CleanColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (plan.status != "ACTIVE") {
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                StatusTag(
                                    text = getStatusDisplayText(plan.status),
                                    color = when (plan.status) {
                                        "COMPLETED" -> CleanColors.success
                                        "PAUSED" -> CleanColors.warning
                                        else -> CleanColors.textTertiary
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "剩余${planWithDetails.daysRemaining}�?,
                                style = CleanTypography.caption,
                                color = CleanColors.textTertiary
                            )
                            if (!planWithDetails.isOnTrack && plan.status == "ACTIVE") {
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "进度落后",
                                    tint = CleanColors.warning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "进度落后",
                                    style = CleanTypography.caption,
                                    color = CleanColors.warning
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "查看详情",
                        tint = CleanColors.textTertiary,
                        modifier = Modifier.size(IconSize.sm)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // 金额和进度显�?
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "¥${numberFormat.format(plan.currentAmount)}",
                            style = CleanTypography.amountMedium,
                            color = planColor
                        )
                        Text(
                            text = "目标 ¥${numberFormat.format(plan.targetAmount)}",
                            style = CleanTypography.caption,
                            color = CleanColors.textTertiary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.0f%%", planWithDetails.progress * 100),
                            style = CleanTypography.amountSmall,
                            color = planColor
                        )
                        Text(
                            text = planWithDetails.currentMilestone.label,
                            style = CleanTypography.caption,
                            color = CleanColors.textTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // 进度�?
                LinearProgressIndicator(
                    progress = planWithDetails.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = planColor,
                    trackColor = planColor.copy(alpha = 0.12f)
                )
            }

            // ============ 第二区：操作区（始终可见，无需滚动�?============
            if (plan.status == "ACTIVE") {
                HorizontalDivider(
                    color = CleanColors.HorizontalDivider,
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ★★�?核心操作：存一笔（最突出�?★★�?
                    Button(
                        onClick = onDeposit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.sm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = planColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "存一�?,
                            style = CleanTypography.button
                        )
                    }

                    // 查看详情按钮
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.sm),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CleanColors.textSecondary
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "详情",
                            style = CleanTypography.button
                        )
                    }

                    // 更多操作（取款、历史、删除）
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "更多",
                                tint = CleanColors.textSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            if (plan.currentAmount > 0) {
                                DropdownMenuItem(
                                    text = { Text("取款") },
                                    onClick = {
                                        showMoreMenu = false
                                        onWithdraw()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Remove, contentDescription = null)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("历史记录") },
                                onClick = {
                                    showMoreMenu = false
                                    onShowHistory()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.History, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除", color = CleanColors.error) },
                                onClick = {
                                    showMoreMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = CleanColors.error
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                // 非活跃状态：只显示查看详�?
                HorizontalDivider(color = CleanColors.HorizontalDivider, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("查看详情")
                    }
                }
            }
        }
    }
}

/**
 * 删除确认对话�?
 */
@Composable
private fun CleanDeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
