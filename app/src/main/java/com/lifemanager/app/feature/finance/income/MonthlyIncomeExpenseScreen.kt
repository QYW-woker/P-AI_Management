package com.lifemanager.app.feature.finance.income

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
import com.lifemanager.app.core.database.entity.IncomeExpenseType
import com.lifemanager.app.domain.model.FieldStats
import com.lifemanager.app.domain.model.IncomeExpenseUiState
import com.lifemanager.app.domain.model.MonthlyIncomeExpenseWithField
import com.lifemanager.app.domain.model.IncomeExpenseMonthlyStats
import com.lifemanager.app.ui.component.charts.PieChartView
import com.lifemanager.app.ui.component.charts.PieChartData
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * 月度收支主界面 - CleanColors设计版
 *
 * 展示月度收入支出统计、分类占比和详细记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyIncomeExpenseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFieldManagement: () -> Unit = {},
    viewModel: MonthlyIncomeExpenseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val records by viewModel.records.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val incomeFieldStats by viewModel.incomeFieldStats.collectAsState()
    val expenseFieldStats by viewModel.expenseFieldStats.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    // 当前选中的标签页 (0: 收入, 1: 支出)
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "月度收支",
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
                            IncomeExpenseExportUtil.exportAndShare(
                                context = context,
                                yearMonth = currentYearMonth,
                                stats = monthlyStats,
                                records = records,
                                incomeFieldStats = incomeFieldStats,
                                expenseFieldStats = expenseFieldStats
                            )
                        }
                    ) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = "导出报表",
                            tint = CleanColors.textSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToFieldManagement) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "字段管理",
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
                onClick = {
                    viewModel.showAddDialog(
                        if (selectedTab == 0) IncomeExpenseType.INCOME else IncomeExpenseType.EXPENSE
                    )
                },
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
                is IncomeExpenseUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CleanColors.primary)
                    }
                }

                is IncomeExpenseUiState.Error -> {
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
                                text = (uiState as IncomeExpenseUiState.Error).message,
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

                is IncomeExpenseUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.pageHorizontal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                    ) {
                        // 统计概览卡片
                        item {
                            CleanStatsCard(stats = monthlyStats)
                        }

                        // 储蓄率和开销率可视化
                        item {
                            CleanRateCard(stats = monthlyStats)
                        }

                        // 标签页切换
                        item {
                            CleanTabRow(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }

                        // 分类统计图表
                        item {
                            val chartData = if (selectedTab == 0) incomeFieldStats else expenseFieldStats
                            if (chartData.isNotEmpty()) {
                                CleanFieldStatsChart(
                                    title = if (selectedTab == 0) "收入分类" else "支出分类",
                                    stats = chartData
                                )
                            }
                        }

                        // 记录列表标题
                        item {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "详细记录",
                                style = CleanTypography.title,
                                color = CleanColors.textPrimary
                            )
                        }

                        // 筛选当前类型的记录
                        val filteredRecords = records.filter {
                            if (selectedTab == 0) {
                                it.record.type == IncomeExpenseType.INCOME
                            } else {
                                it.record.type == IncomeExpenseType.EXPENSE
                            }
                        }

                        if (filteredRecords.isEmpty()) {
                            item {
                                CleanEmptyState(
                                    message = if (selectedTab == 0) "暂无收入记录" else "暂无支出记录",
                                    icon = if (selectedTab == 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown
                                )
                            }
                        } else {
                            items(filteredRecords, key = { it.record.id }) { record ->
                                CleanRecordItem(
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
        AddEditRecordDialog(
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
private fun CleanStatsCard(stats: IncomeExpenseMonthlyStats) {
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
                // 收入
                CleanStatItem(
                    label = "收入",
                    amount = stats.totalIncome,
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

                // 支出
                CleanStatItem(
                    label = "支出",
                    amount = stats.totalExpense,
                    color = CleanColors.error,
                    numberFormat = numberFormat
                )

                // 分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(CleanColors.divider)
                )

                // 结余
                CleanStatItem(
                    label = "结余",
                    amount = stats.netIncome,
                    color = if (stats.netIncome >= 0) CleanColors.primary else CleanColors.warning,
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
 * 储蓄率和开销率卡片
 */
@Composable
private fun CleanRateCard(stats: IncomeExpenseMonthlyStats) {
    if (stats.totalIncome <= 0) return

    val savingsRate = stats.savingsRate
    val expenseRate = if (stats.totalIncome > 0) {
        (stats.totalExpense / stats.totalIncome) * 100
    } else 0.0

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
                text = "收支比率",
                style = CleanTypography.secondary,
                color = CleanColors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // 储蓄率
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "储蓄率",
                    style = CleanTypography.body,
                    color = CleanColors.textPrimary,
                    modifier = Modifier.width(60.dp)
                )

                LinearProgressIndicator(
                    progress = (savingsRate / 100f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = getSavingsRateColor(savingsRate),
                    trackColor = CleanColors.surfaceVariant
                )

                Text(
                    text = String.format("%.1f%%", savingsRate),
                    style = CleanTypography.button,
                    color = getSavingsRateColor(savingsRate),
                    modifier = Modifier
                        .width(60.dp)
                        .padding(start = Spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 开销率
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "开销率",
                    style = CleanTypography.body,
                    color = CleanColors.textPrimary,
                    modifier = Modifier.width(60.dp)
                )

                LinearProgressIndicator(
                    progress = (expenseRate / 100f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = getExpenseRateColor(expenseRate),
                    trackColor = CleanColors.surfaceVariant
                )

                Text(
                    text = String.format("%.1f%%", expenseRate),
                    style = CleanTypography.button,
                    color = getExpenseRateColor(expenseRate),
                    modifier = Modifier
                        .width(60.dp)
                        .padding(start = Spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Divider(color = CleanColors.divider)
            Spacer(modifier = Modifier.height(Spacing.md))

            // 健康提示
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (savingsRate >= 30) Icons.Outlined.CheckCircle
                        else if (savingsRate >= 10) Icons.Outlined.Info
                        else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = getSavingsRateColor(savingsRate),
                    modifier = Modifier.size(IconSize.sm)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = when {
                        savingsRate >= 30 -> "储蓄率健康，继续保持！"
                        savingsRate >= 10 -> "储蓄率一般，建议适当控制支出"
                        else -> "储蓄率较低，需要关注支出情况"
                    },
                    style = CleanTypography.caption,
                    color = CleanColors.textSecondary
                )
            }
        }
    }
}

/**
 * 获取储蓄率颜色
 */
private fun getSavingsRateColor(rate: Double): Color {
    return when {
        rate >= 30 -> CleanColors.success
        rate >= 10 -> CleanColors.warning
        else -> CleanColors.error
    }
}

/**
 * 获取开销率颜色
 */
private fun getExpenseRateColor(rate: Double): Color {
    return when {
        rate <= 70 -> CleanColors.success
        rate <= 90 -> CleanColors.warning
        else -> CleanColors.error
    }
}

/**
 * 简洁Tab切换
 */
@Composable
private fun CleanTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xs)
        ) {
            CleanTab(
                text = "收入",
                selected = selectedTab == 0,
                color = CleanColors.success,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            CleanTab(
                text = "支出",
                selected = selectedTab == 1,
                color = CleanColors.error,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 简洁Tab项
 */
@Composable
private fun CleanTab(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick),
        color = if (selected) color else Color.Transparent,
        shape = RoundedCornerShape(Radius.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = CleanTypography.button,
                color = if (selected) Color.White else CleanColors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * 简洁字段统计图表
 */
@Composable
private fun CleanFieldStatsChart(
    title: String,
    stats: List<FieldStats>
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
                                value = it.amount,
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
                        CleanLegendItem(stat = stat)
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
private fun CleanLegendItem(stat: FieldStats) {
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
 * 简洁记录项
 */
@Composable
private fun CleanRecordItem(
    record: MonthlyIncomeExpenseWithField,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

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
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            record.field?.let { parseColor(it.color) }
                                ?: CleanColors.primary
                        )
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // 类别名称和备注
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.field?.name ?: "未分类",
                    style = CleanTypography.body,
                    color = CleanColors.textPrimary
                )
                if (record.record.note.isNotBlank()) {
                    Text(
                        text = record.record.note,
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 金额
            Text(
                text = "${if (record.record.type == IncomeExpenseType.INCOME) "+" else "-"}¥${
                    numberFormat.format(record.record.amount)
                }",
                style = CleanTypography.amountSmall,
                color = if (record.record.type == IncomeExpenseType.INCOME) {
                    CleanColors.success
                } else {
                    CleanColors.error
                }
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
