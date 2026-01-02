package com.lifemanager.app.feature.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * 存钱计划详情页 - 产品级设计
 *
 * 页面职责：
 * - 展示单个存钱计划的完整信息
 * - 显示进度和里程碑
 * - 显示存取款历史
 * - 提供存款、取款、编辑、删除操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanSavingsPlanDetailScreen(
    planId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: SavingsPlanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val plan by viewModel.plan.collectAsState()
    val records by viewModel.records.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val showDepositDialog by viewModel.showDepositDialog.collectAsState()
    val showWithdrawDialog by viewModel.showWithdrawDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    LaunchedEffect(planId) {
        viewModel.loadPlan(planId)
    }

    Scaffold(
        containerColor = CleanColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "计划详情",
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
                    IconButton(onClick = { onNavigateToEdit(planId) }) {
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
            is SavingsPlanDetailUiState.Loading -> {
                PageLoadingState(modifier = Modifier.padding(paddingValues))
            }

            is SavingsPlanDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as SavingsPlanDetailUiState.Error).message,
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

            is SavingsPlanDetailUiState.Success -> {
                plan?.let { planEntity ->
                    SavingsPlanDetailContent(
                        plan = planEntity,
                        records = records,
                        statistics = statistics,
                        milestones = milestones,
                        onDeposit = { viewModel.showDepositDialog() },
                        onWithdraw = { viewModel.showWithdrawDialog() },
                        onTogglePause = { viewModel.togglePause() },
                        formatDate = { viewModel.formatDate(it) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    // 存款对话框
    if (showDepositDialog) {
        DepositDialogContent(
            viewModel = viewModel,
            onDismiss = { viewModel.hideDepositDialog() }
        )
    }

    // 取款对话框
    if (showWithdrawDialog) {
        WithdrawDialogContent(
            viewModel = viewModel,
            maxAmount = plan?.currentAmount ?: 0.0,
            onDismiss = { viewModel.hideWithdrawDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
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
                    text = "确认删除",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "删除计划将同时删除所有存款记录，此操作不可恢复。确定要删除吗？",
                    style = CleanTypography.body,
                    color = CleanColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete(onNavigateBack) }) {
                    Text(
                        text = "删除",
                        style = CleanTypography.button,
                        color = CleanColors.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
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
}

@Composable
private fun SavingsPlanDetailContent(
    plan: SavingsPlanEntity,
    records: List<SavingsRecordEntity>,
    statistics: SavingsPlanStatistics,
    milestones: List<MilestoneWithStatus>,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onTogglePause: () -> Unit,
    formatDate: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val planColor = try {
        Color(android.graphics.Color.parseColor(plan.color))
    } catch (e: Exception) {
        CleanColors.primary
    }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val isPaused = plan.status == "PAUSED"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item {
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        // 计划头部卡片
        item {
            PlanHeaderCard(
                plan = plan,
                statistics = statistics,
                planColor = planColor,
                isPaused = isPaused,
                onDeposit = onDeposit,
                onWithdraw = onWithdraw,
                onTogglePause = onTogglePause,
                numberFormat = numberFormat
            )
        }

        // 里程碑进度
        item {
            MilestonesCard(
                milestones = milestones,
                planColor = planColor,
                numberFormat = numberFormat
            )
        }

        // 统计数据
        item {
            StatisticsCard(
                statistics = statistics,
                plan = plan,
                planColor = planColor,
                formatDate = formatDate,
                numberFormat = numberFormat
            )
        }

        // 交易记录
        item {
            Text(
                text = "存取记录",
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )
        }

        if (records.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.md),
                    color = CleanColors.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Receipt,
                            contentDescription = null,
                            tint = CleanColors.textTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = "暂无记录",
                            style = CleanTypography.secondary,
                            color = CleanColors.textTertiary
                        )
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                RecordItem(
                    record = record,
                    planColor = planColor,
                    formatDate = formatDate,
                    numberFormat = numberFormat
                )
            }
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(Spacing.bottomSafe))
        }
    }
}

@Composable
private fun PlanHeaderCard(
    plan: SavingsPlanEntity,
    statistics: SavingsPlanStatistics,
    planColor: Color,
    isPaused: Boolean,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onTogglePause: () -> Unit,
    numberFormat: NumberFormat
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = planColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 计划名称
            Text(
                text = plan.name,
                style = CleanTypography.headline,
                color = CleanColors.textPrimary,
                textAlign = TextAlign.Center
            )

            if (plan.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = plan.description,
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // 进度显示
            Text(
                text = "¥${numberFormat.format(statistics.currentAmount)}",
                style = CleanTypography.amountLarge,
                color = planColor
            )
            Text(
                text = "/ ¥${numberFormat.format(statistics.targetAmount)}",
                style = CleanTypography.secondary,
                color = CleanColors.textTertiary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // 进度条
            LinearProgressIndicator(
                progress = statistics.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = planColor,
                trackColor = CleanColors.borderLight
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(statistics.progress * 100).toInt()}%",
                    style = CleanTypography.caption,
                    color = planColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!statistics.isOnTrack && !isPaused) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = CleanColors.warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "剩余${statistics.remainingDays}天",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // 操作按钮
            if (!isPaused) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    CleanPrimaryButton(
                        text = "存款",
                        onClick = onDeposit,
                        modifier = Modifier.weight(1f)
                    )
                    if (plan.currentAmount > 0) {
                        CleanSecondaryButton(
                            text = "取款",
                            onClick = onWithdraw,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 暂停/恢复按钮
            TextButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    contentDescription = null,
                    tint = CleanColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = if (isPaused) "恢复计划" else "暂停计划",
                    style = CleanTypography.button,
                    color = CleanColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun MilestonesCard(
    milestones: List<MilestoneWithStatus>,
    planColor: Color,
    numberFormat: NumberFormat
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = "里程碑",
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                milestones.forEach { milestoneStatus ->
                    MilestoneItem(
                        label = milestoneStatus.milestone.label,
                        description = milestoneStatus.milestone.description,
                        isAchieved = milestoneStatus.isAchieved,
                        amount = milestoneStatus.amountReached,
                        color = planColor,
                        numberFormat = numberFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneItem(
    label: String,
    description: String,
    isAchieved: Boolean,
    amount: Double,
    color: Color,
    numberFormat: NumberFormat
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isAchieved) color else CleanColors.borderLight),
            contentAlignment = Alignment.Center
        ) {
            if (isAchieved) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = label,
                    style = CleanTypography.caption,
                    color = CleanColors.textTertiary
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = description,
            style = CleanTypography.caption,
            color = if (isAchieved) color else CleanColors.textTertiary
        )
    }
}

@Composable
private fun StatisticsCard(
    statistics: SavingsPlanStatistics,
    plan: SavingsPlanEntity,
    planColor: Color,
    formatDate: (Int) -> String,
    numberFormat: NumberFormat
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

            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = "目标日期",
                value = formatDate(plan.targetDate)
            )

            CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))

            InfoRow(
                icon = Icons.Outlined.TrendingUp,
                label = "每日建议存款",
                value = "¥${numberFormat.format(statistics.dailyTarget)}"
            )

            CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))

            InfoRow(
                icon = Icons.Outlined.Savings,
                label = "累计存款",
                value = "¥${numberFormat.format(statistics.totalDeposits)}"
            )

            if (statistics.totalWithdrawals > 0) {
                CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))

                InfoRow(
                    icon = Icons.Outlined.Money,
                    label = "累计取款",
                    value = "¥${numberFormat.format(statistics.totalWithdrawals)}"
                )
            }

            CleanDivider(modifier = Modifier.padding(vertical = Spacing.md))

            InfoRow(
                icon = Icons.Outlined.Receipt,
                label = "交易次数",
                value = "${statistics.recordCount}次"
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
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
                color = CleanColors.textPrimary
            )
        }
    }
}

@Composable
private fun RecordItem(
    record: SavingsRecordEntity,
    planColor: Color,
    formatDate: (Int) -> String,
    numberFormat: NumberFormat
) {
    val isDeposit = record.amount > 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDeposit) CleanColors.success.copy(alpha = 0.1f)
                        else CleanColors.error.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDeposit) Icons.Filled.Add else Icons.Filled.Remove,
                    contentDescription = null,
                    tint = if (isDeposit) CleanColors.success else CleanColors.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDeposit) "存款" else "取款",
                    style = CleanTypography.body,
                    color = CleanColors.textPrimary
                )
                Text(
                    text = formatDate(record.date),
                    style = CleanTypography.caption,
                    color = CleanColors.textTertiary
                )
            }

            Text(
                text = "${if (isDeposit) "+" else ""}¥${numberFormat.format(kotlin.math.abs(record.amount))}",
                style = CleanTypography.amountMedium,
                color = if (isDeposit) CleanColors.success else CleanColors.error
            )
        }
    }
}

@Composable
private fun DepositDialogContent(
    viewModel: SavingsPlanDetailViewModel,
    onDismiss: () -> Unit
) {
    val amount by viewModel.depositAmount.collectAsState()
    val note by viewModel.depositNote.collectAsState()
    val error by viewModel.operationError.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "存款",
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )
        },
        text = {
            Column {
                CleanTextField(
                    value = amount,
                    onValueChange = { viewModel.updateDepositAmount(it) },
                    label = "金额",
                    placeholder = "请输入存款金额",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                CleanTextField(
                    value = note,
                    onValueChange = { viewModel.updateDepositNote(it) },
                    label = "备注（可选）",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = errorMsg,
                        style = CleanTypography.caption,
                        color = CleanColors.error
                    )
                }
            }
        },
        confirmButton = {
            if (isOperating) {
                CleanLoadingIndicator(size = 20.dp)
            } else {
                CleanPrimaryButton(
                    text = "确认",
                    onClick = { viewModel.confirmDeposit() }
                )
            }
        },
        dismissButton = {
            CleanTextButton(
                text = "取消",
                onClick = onDismiss,
                color = CleanColors.textSecondary
            )
        },
        containerColor = CleanColors.surface,
        shape = RoundedCornerShape(Radius.lg)
    )
}

@Composable
private fun WithdrawDialogContent(
    viewModel: SavingsPlanDetailViewModel,
    maxAmount: Double,
    onDismiss: () -> Unit
) {
    val amount by viewModel.withdrawAmount.collectAsState()
    val error by viewModel.operationError.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "取款",
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "可取金额：¥${numberFormat.format(maxAmount)}",
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                CleanTextField(
                    value = amount,
                    onValueChange = { viewModel.updateWithdrawAmount(it) },
                    label = "金额",
                    placeholder = "请输入取款金额",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = errorMsg,
                        style = CleanTypography.caption,
                        color = CleanColors.error
                    )
                }
            }
        },
        confirmButton = {
            if (isOperating) {
                CleanLoadingIndicator(size = 20.dp)
            } else {
                TextButton(onClick = { viewModel.confirmWithdraw() }) {
                    Text(
                        text = "确认",
                        style = CleanTypography.button,
                        color = CleanColors.error
                    )
                }
            }
        },
        dismissButton = {
            CleanTextButton(
                text = "取消",
                onClick = onDismiss,
                color = CleanColors.textSecondary
            )
        },
        containerColor = CleanColors.surface,
        shape = RoundedCornerShape(Radius.lg)
    )
}
