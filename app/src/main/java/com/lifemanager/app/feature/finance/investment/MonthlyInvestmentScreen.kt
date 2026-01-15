package com.lifemanager.app.feature.finance.investment

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.InvestmentFieldStats
import com.lifemanager.app.domain.model.InvestmentUiState
import com.lifemanager.app.domain.model.MonthlyInvestmentWithField
import com.lifemanager.app.domain.model.InvestmentMonthlyStats
import com.lifemanager.app.ui.component.charts.PieChartView
import com.lifemanager.app.ui.component.charts.PieChartData
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * 月度定投主界面 - CleanColors设计版
 *
 * 展示月度定投预算、实际投入统计、分类占比和详细记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyInvestmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyInvestmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val records by viewModel.records.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val fieldStats by viewModel.fieldStats.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "月度定投",
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = CleanColors.textSecondary
                        )
                    }
                },
                actions = {
                    // 导出按钮
                    IconButton(
                        onClick = {
                            InvestmentExportUtil.exportAndShare(
                                context = context,
                                yearMonth = currentYearMonth,
                                stats = monthlyStats,
                                records = records,
                                fieldStats = fieldStats
                            )
                        }
                    ) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = "导出报表",
                            tint = CleanColors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CleanColors.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = CleanColors.primary,
                contentColor = CleanColors.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加记录")
            }
        },
        containerColor = CleanColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 月份选择器
            CleanMonthSelector(
                yearMonth = currentYearMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                formatYearMonth = { viewModel.formatYearMonth(it) }
            )

            when (uiState) {
                is InvestmentUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CleanColors.primary)
                    }
                }

                is InvestmentUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = CleanColors.error
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = (uiState as InvestmentUiState.Error).message,
                                style = CleanTypography.body,
                                color = CleanColors.error
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanColors.primary
                                )
                            ) {
                                Text("重试", style = CleanTypography.button)
                            }
                        }
                    }
                }

                is InvestmentUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.pageHorizontal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                    ) {
                        // 统计概览卡片
                        item {
                            CleanInvestmentStatsCard(stats = monthlyStats)
                        }

                        // 预算完成率
                        item {
                            CleanCompletionCard(stats = monthlyStats)
                        }

                        // 分类统计图表
                        item {
                            if (fieldStats.isNotEmpty()) {
                                CleanInvestmentChart(
                                    title = "定投分布",
                                    stats = fieldStats
                                )
                            }
                        }

                        // 记录列表标题
                        item {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "定投明细",
                                style = CleanTypography.title,
                                color = CleanColors.textPrimary
                            )
                        }

                        if (records.isEmpty()) {
                            item {
                                CleanEmptyState(
                                    message = "暂无定投记录",
                                    icon = Icons.Outlined.ShowChart
                                )
                            }
                        } else {
                            items(records, key = { it.record.id }) { record ->
                                CleanInvestmentItem(
                                    record = record,
                                    onClick = { viewModel.showEditDialog(record.record.id) },
                                    onDelete = { viewModel.showDeleteConfirm(record.record.id) }
                                )
                            }
                        }

                        // 底部安全间距
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AddEditInvestmentDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = {
                Text(
                    "确认删除",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            },
            text = {
                Text(
                    "确定要删除这条记录吗？此操作不可撤销。",
                    style = CleanTypography.body,
                    color = CleanColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() }
                ) {
                    Text(
                        "删除",
                        style = CleanTypography.button,
                        color = CleanColors.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text(
                        "取消",
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

/**
 * 简洁月份选择器
 */
@Composable
private fun CleanMonthSelector(
    yearMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    formatYearMonth: (Int) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    tint = CleanColors.textSecondary
                )
            }

            Text(
                text = formatYearMonth(yearMonth),
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )

            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "下个月",
                    tint = CleanColors.textSecondary
                )
            }
        }
    }
}

/**
 * 简洁统计卡片
 */
@Composable
private fun CleanInvestmentStatsCard(stats: InvestmentMonthlyStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = CleanColors.surface,
        shadowElevation = Elevation.sm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 预算
                CleanStatItem(
                    label = "预算",
                    amount = stats.totalBudget,
                    color = CleanColors.primary,
                    numberFormat = numberFormat
                )

                // 分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(CleanColors.divider)
                )

                // 实际
                CleanStatItem(
                    label = "实际",
                    amount = stats.totalActual,
                    color = CleanColors.success,
                    numberFormat = numberFormat
                )

                // 分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(CleanColors.divider)
                )

                // 差额
                CleanStatItem(
                    label = "差额",
                    amount = stats.budgetDiff,
                    color = if (stats.budgetDiff >= 0) CleanColors.success else CleanColors.warning,
                    numberFormat = numberFormat
                )
            }
        }
    }
}

/**
 * 简洁统计项
 */
@Composable
private fun CleanStatItem(
    label: String,
    amount: Double,
    color: Color,
    numberFormat: NumberFormat
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textTertiary
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "¥${numberFormat.format(amount)}",
            style = CleanTypography.amountMedium,
            color = color
        )
    }
}

/**
 * 预算完成率卡片
 */
@Composable
private fun CleanCompletionCard(stats: InvestmentMonthlyStats) {
    if (stats.totalBudget <= 0) return

    val completionRate = stats.completionRate

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = CleanColors.surface,
        shadowElevation = Elevation.sm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = "预算完成情况",
                style = CleanTypography.secondary,
                color = CleanColors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // 完成率进度条
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "完成率",
                    style = CleanTypography.body,
                    color = CleanColors.textPrimary,
                    modifier = Modifier.width(60.dp)
                )

                LinearProgressIndicator(
                    progress = (completionRate / 100f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = getCompletionColor(completionRate),
                    trackColor = CleanColors.surfaceVariant
                )

                Text(
                    text = String.format("%.1f%%", completionRate),
                    style = CleanTypography.button,
                    color = getCompletionColor(completionRate),
                    modifier = Modifier
                        .width(60.dp)
                        .padding(start = Spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Divider(color = CleanColors.divider)
            Spacer(modifier = Modifier.height(Spacing.md))

            // 状态提示
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (completionRate >= 100) Icons.Outlined.CheckCircle
                        else if (completionRate >= 80) Icons.Outlined.Info
                        else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = getCompletionColor(completionRate),
                    modifier = Modifier.size(IconSize.sm)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = when {
                        completionRate >= 100 -> "已完成本月定投目标！"
                        completionRate >= 80 -> "定投进度良好，继续保持"
                        completionRate >= 50 -> "定投进度一般，请及时补充"
                        else -> "定投进度较慢，请关注"
                    },
                    style = CleanTypography.caption,
                    color = CleanColors.textSecondary
                )
            }
        }
    }
}

/**
 * 获取完成率颜色
 */
private fun getCompletionColor(rate: Double): Color {
    return when {
        rate >= 100 -> CleanColors.success
        rate >= 80 -> CleanColors.primary
        rate >= 50 -> CleanColors.warning
        else -> CleanColors.error
    }
}

/**
 * 简洁定投图表
 */
@Composable
private fun CleanInvestmentChart(
    title: String,
    stats: List<InvestmentFieldStats>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = CleanColors.surface,
        shadowElevation = Elevation.sm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = title,
                style = CleanTypography.secondary,
                color = CleanColors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 饼图
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(Spacing.xs)
                ) {
                    PieChartView(
                        data = stats.map {
                            PieChartData(
                                label = it.fieldName,
                                value = it.actualAmount,
                                color = parseColor(it.fieldColor)
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                        showLegend = false
                    )
                }

                // 图例
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    stats.take(5).forEach { stat ->
                        CleanInvestmentLegendItem(stat = stat)
                    }
                    if (stats.size > 5) {
                        Text(
                            text = "...还有${stats.size - 5}项",
                            style = CleanTypography.caption,
                            color = CleanColors.textTertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 简洁图例项
 */
@Composable
private fun CleanInvestmentLegendItem(stat: InvestmentFieldStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(parseColor(stat.fieldColor))
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = stat.fieldName,
            style = CleanTypography.caption,
            color = CleanColors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = String.format("%.1f%%", stat.percentage),
            style = CleanTypography.caption,
            color = CleanColors.textTertiary
        )
    }
}

/**
 * 简洁定投记录项
 */
@Composable
private fun CleanInvestmentItem(
    record: MonthlyInvestmentWithField,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val completionRate = if (record.record.budgetAmount > 0) {
        (record.record.actualAmount / record.record.budgetAmount) * 100
    } else 0.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类别图标背景
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(
                        record.field?.let { parseColor(it.color).copy(alpha = 0.15f) }
                            ?: CleanColors.primaryLight
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ShowChart,
                    contentDescription = null,
                    tint = record.field?.let { parseColor(it.color) } ?: CleanColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // 类别名称和完成率
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.field?.name ?: "未分类",
                    style = CleanTypography.body,
                    color = CleanColors.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "预算: ¥${numberFormat.format(record.record.budgetAmount)}",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                    if (record.record.budgetAmount > 0) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = String.format("%.0f%%", completionRate),
                            style = CleanTypography.caption,
                            color = getCompletionColor(completionRate)
                        )
                    }
                }
            }

            // 实际金额
            Text(
                text = "¥${numberFormat.format(record.record.actualAmount)}",
                style = CleanTypography.amountSmall,
                color = CleanColors.success
            )

            Spacer(modifier = Modifier.width(Spacing.xs))

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除",
                    tint = CleanColors.textTertiary,
                    modifier = Modifier.size(IconSize.sm)
                )
            }
        }
    }
}

/**
 * 简洁空状态提示
 */
@Composable
private fun CleanEmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = CleanColors.textPlaceholder
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = message,
                style = CleanTypography.secondary,
                color = CleanColors.textTertiary
            )
        }
    }
}

/**
 * 解析颜色字符串
 */
private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        CleanColors.primary
    }
}
