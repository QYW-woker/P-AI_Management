package com.lifemanager.app.feature.finance.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.*

/**
 * 统计分析页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val periodType by viewModel.periodType.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedQuarter by viewModel.selectedQuarter.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val quarterlyStats by viewModel.quarterlyStats.collectAsState()
    val yearlyStats by viewModel.yearlyStats.collectAsState()
    val monthlyTrend by viewModel.monthlyTrend.collectAsState()
    val quarterlyTrend by viewModel.quarterlyTrend.collectAsState()
    val yearOverYearComparison by viewModel.yearOverYearComparison.collectAsState()
    val monthOverMonthComparison by viewModel.monthOverMonthComparison.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()

    // 计算当前周期标签（响应式）
    val periodLabel = remember(periodType, selectedYear, selectedMonth, selectedQuarter) {
        when (periodType) {
            StatsPeriodType.MONTHLY -> "${selectedYear}年${selectedMonth}月"
            StatsPeriodType.QUARTERLY -> "${selectedYear}年第${selectedQuarter}季度"
            StatsPeriodType.YEARLY -> "${selectedYear}年"
            StatsPeriodType.WEEKLY -> "本周"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计分析") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 周期选择器
            item {
                PeriodSelector(
                    periodType = periodType,
                    periodLabel = periodLabel,
                    onPeriodTypeChange = { viewModel.setPeriodType(it) },
                    onPrevious = { viewModel.previousPeriod() },
                    onNext = { viewModel.nextPeriod() }
                )
            }

            // 概览卡片
            item {
                when (periodType) {
                    StatsPeriodType.MONTHLY -> monthlyStats?.let { OverviewCard(it) }
                    StatsPeriodType.QUARTERLY -> quarterlyStats?.let { QuarterlyOverviewCard(it) }
                    StatsPeriodType.YEARLY -> yearlyStats?.let { YearlyOverviewCard(it) }
                    else -> {}
                }
            }

            // 同比/环比分析（仅月度）
            if (periodType == StatsPeriodType.MONTHLY) {
                item {
                    ComparisonSection(
                        yearOverYear = yearOverYearComparison,
                        monthOverMonth = monthOverMonthComparison
                    )
                }
            }

            // 趋势图表
            item {
                TrendSection(
                    periodType = periodType,
                    monthlyTrend = monthlyTrend,
                    quarterlyTrend = quarterlyTrend
                )
            }

            // 分类统计
            item {
                CategorySection(categoryStats = categoryStats)
            }

            // 月度/季度分解
            when (periodType) {
                StatsPeriodType.QUARTERLY -> quarterlyStats?.let {
                    item {
                        MonthlyBreakdownSection(months = it.monthlyBreakdown)
                    }
                }
                StatsPeriodType.YEARLY -> yearlyStats?.let {
                    item {
                        QuarterlyBreakdownSection(quarters = it.quarterlyBreakdown)
                    }
                }
                else -> {}
            }
        }
    }
}

/**
 * 周期选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    periodType: StatsPeriodType,
    periodLabel: String,
    onPeriodTypeChange: (StatsPeriodType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 周期类型切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                StatsPeriodType.MONTHLY to "月度",
                StatsPeriodType.QUARTERLY to "季度",
                StatsPeriodType.YEARLY to "年度"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = periodType == type,
                    onClick = { onPeriodTypeChange(type) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 周期导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上一个")
            }

            Text(
                text = periodLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onNext) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下一个")
            }
        }
    }
}

/**
 * 月度概览卡片
 */
@Composable
private fun OverviewCard(stats: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stats.fullLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "收入",
                    value = "¥${String.format("%.2f", stats.totalIncome)}",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    label = "支出",
                    value = "¥${String.format("%.2f", stats.totalExpense)}",
                    color = Color(0xFFF44336)
                )
                StatItem(
                    label = "结余",
                    value = "¥${String.format("%.2f", stats.balance)}",
                    color = if (stats.balance >= 0) Color(0xFF2196F3) else Color(0xFFF44336)
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "交易笔数",
                    value = "${stats.transactionCount}笔",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    label = "日均支出",
                    value = "¥${String.format("%.2f", stats.avgDailyExpense)}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    label = "储蓄率",
                    value = "${String.format("%.1f", stats.savingsRate)}%",
                    color = if (stats.savingsRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

/**
 * 季度概览卡片
 */
@Composable
private fun QuarterlyOverviewCard(stats: QuarterlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stats.quarterLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "总收入",
                    value = "¥${String.format("%.2f", stats.totalIncome)}",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    label = "总支出",
                    value = "¥${String.format("%.2f", stats.totalExpense)}",
                    color = Color(0xFFF44336)
                )
                StatItem(
                    label = "总结余",
                    value = "¥${String.format("%.2f", stats.balance)}",
                    color = if (stats.balance >= 0) Color(0xFF2196F3) else Color(0xFFF44336)
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "月均支出",
                    value = "¥${String.format("%.2f", stats.avgMonthlyExpense)}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                StatItem(
                    label = "日均支出",
                    value = "¥${String.format("%.2f", stats.avgDailyExpense)}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                StatItem(
                    label = "储蓄率",
                    value = "${String.format("%.1f", stats.savingsRate)}%",
                    color = if (stats.savingsRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

/**
 * 年度概览卡片
 */
@Composable
private fun YearlyOverviewCard(stats: YearlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stats.yearLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "年度收入",
                    value = "¥${String.format("%.2f", stats.totalIncome)}",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    label = "年度支出",
                    value = "¥${String.format("%.2f", stats.totalExpense)}",
                    color = Color(0xFFF44336)
                )
                StatItem(
                    label = "年度结余",
                    value = "¥${String.format("%.2f", stats.balance)}",
                    color = if (stats.balance >= 0) Color(0xFF2196F3) else Color(0xFFF44336)
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "月均支出",
                    value = "¥${String.format("%.2f", stats.avgMonthlyExpense)}",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                StatItem(
                    label = "交易笔数",
                    value = "${stats.transactionCount}笔",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                StatItem(
                    label = "储蓄率",
                    value = "${String.format("%.1f", stats.savingsRate)}%",
                    color = if (stats.savingsRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 同比/环比分析
 */
@Composable
private fun ComparisonSection(
    yearOverYear: ComparisonStats?,
    monthOverMonth: ComparisonStats?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "对比分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 环比（与上月比）
                monthOverMonth?.let { stats ->
                    ComparisonCard(
                        title = "环比（与上月）",
                        incomeChange = stats.incomeChange,
                        expenseChange = stats.expenseChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 同比（与去年同月比）
                yearOverYear?.let { stats ->
                    ComparisonCard(
                        title = "同比（与去年）",
                        incomeChange = stats.incomeChange,
                        expenseChange = stats.expenseChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 对比卡片
 */
@Composable
private fun ComparisonCard(
    title: String,
    incomeChange: Double,
    expenseChange: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ChangeIndicator(label = "收入", change = incomeChange)
                ChangeIndicator(label = "支出", change = expenseChange, invertColor = true)
            }
        }
    }
}

/**
 * 变化指示器
 */
@Composable
private fun ChangeIndicator(
    label: String,
    change: Double,
    invertColor: Boolean = false
) {
    val isPositive = change >= 0
    // 对于支出，增加是不好的（红色），减少是好的（绿色）
    val color = if (invertColor) {
        if (isPositive) Color(0xFFF44336) else Color(0xFF4CAF50)
    } else {
        if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall
        )
        Icon(
            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "${String.format("%.1f", kotlin.math.abs(change))}%",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 趋势图表区域
 */
@Composable
private fun TrendSection(
    periodType: StatsPeriodType,
    monthlyTrend: List<TrendDataPoint>,
    quarterlyTrend: List<TrendDataPoint>
) {
    val trend = when (periodType) {
        StatsPeriodType.QUARTERLY -> quarterlyTrend
        else -> monthlyTrend
    }

    if (trend.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (periodType == StatsPeriodType.QUARTERLY) "季度趋势" else "月度趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 简易柱状图
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(trend) { point ->
                    TrendBar(point = point)
                }
            }
        }
    }
}

/**
 * 趋势柱状图
 */
@Composable
private fun TrendBar(point: TrendDataPoint) {
    val maxValue = maxOf(point.income, point.expense, 1.0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        // 柱状图
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(80.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 收入柱
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height((point.income / maxValue * 80).dp.coerceIn(4.dp, 80.dp))
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(Color(0xFF4CAF50))
            )
            // 支出柱
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height((point.expense / maxValue * 80).dp.coerceIn(4.dp, 80.dp))
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(Color(0xFFF44336))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = point.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 分类统计区域
 */
@Composable
private fun CategorySection(categoryStats: List<CategoryExpenseStats>) {
    if (categoryStats.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "支出分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            categoryStats.take(5).forEach { category ->
                CategoryRow(category = category)
            }
        }
    }
}

/**
 * 分类行
 */
@Composable
private fun CategoryRow(category: CategoryExpenseStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(parseColor(category.categoryColor))
            )
            Text(
                text = category.categoryName,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "¥${String.format("%.2f", category.totalAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${String.format("%.1f", category.percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 月度分解区域
 */
@Composable
private fun MonthlyBreakdownSection(months: List<MonthlyStats>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "月度分解",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            months.forEach { month ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = month.monthLabel)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "收: ¥${String.format("%.0f", month.totalIncome)}",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "支: ¥${String.format("%.0f", month.totalExpense)}",
                            color = Color(0xFFF44336),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * 季度分解区域
 */
@Composable
private fun QuarterlyBreakdownSection(quarters: List<QuarterlyStats>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "季度分解",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            quarters.forEach { quarter ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Q${quarter.quarter}")
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "收: ¥${String.format("%.0f", quarter.totalIncome)}",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "支: ¥${String.format("%.0f", quarter.totalExpense)}",
                            color = Color(0xFFF44336),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * 解析颜色
 */
private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
