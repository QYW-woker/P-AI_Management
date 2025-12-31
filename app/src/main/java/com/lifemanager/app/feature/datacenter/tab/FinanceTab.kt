package com.lifemanager.app.feature.datacenter.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.feature.datacenter.component.CategorySelector
import com.lifemanager.app.feature.datacenter.component.ChartTypeSelector
import com.lifemanager.app.feature.datacenter.model.BillQueryItem
import com.lifemanager.app.feature.datacenter.model.CategoryChartItem
import com.lifemanager.app.feature.datacenter.model.CategoryRankingItem
import com.lifemanager.app.feature.datacenter.model.ChartType
import com.lifemanager.app.feature.datacenter.model.DailyFinanceTrend
import com.lifemanager.app.feature.datacenter.model.FinanceChartData
import com.lifemanager.app.ui.component.charts.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 财务标签页
 *
 * 显示详细的收支分布和趋势图表
 */
@Composable
fun FinanceTab(
    financeData: FinanceChartData?,
    incomeCategories: List<CustomFieldEntity>,
    expenseCategories: List<CustomFieldEntity>,
    selectedIncomeIds: Set<Long>,
    selectedExpenseIds: Set<Long>,
    onIncomeSelectionChange: (Set<Long>) -> Unit,
    onExpenseSelectionChange: (Set<Long>) -> Unit,
    chartType: ChartType,
    onChartTypeChange: (ChartType) -> Unit,
    budgetAnalysis: List<com.lifemanager.app.domain.model.MonthlyBudgetAnalysis> = emptyList(),
    budgetAIAdvice: String = "",
    billList: List<BillQueryItem> = emptyList(),
    expenseRanking: List<CategoryRankingItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 分类筛选器
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "筛选分类",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategorySelector(
                            title = "收入",
                            categories = incomeCategories,
                            selectedIds = selectedIncomeIds,
                            onSelectionChange = onIncomeSelectionChange,
                            modifier = Modifier.weight(1f)
                        )
                        CategorySelector(
                            title = "支出",
                            categories = expenseCategories,
                            selectedIds = selectedExpenseIds,
                            onSelectionChange = onExpenseSelectionChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 图表类型选择器
        item {
            ChartTypeSelector(
                selected = chartType,
                options = listOf(ChartType.PIE, ChartType.BAR, ChartType.LINE),
                onSelect = onChartTypeChange
            )
        }

        // 财务摘要
        item {
            financeData?.let { data ->
                FinanceSummaryCard(
                    totalIncome = data.totalIncome,
                    totalExpense = data.totalExpense,
                    balance = data.balance
                )
            }
        }

        // 收入分布图表
        item {
            FinanceChartCard(
                title = "收入分布",
                icon = Icons.Default.TrendingUp,
                iconColor = Color(0xFF4CAF50),
                data = financeData?.incomeByCategory ?: emptyList(),
                chartType = chartType,
                trendData = financeData?.dailyTrend ?: emptyList(),
                isIncome = true
            )
        }

        // 支出分布图表
        item {
            FinanceChartCard(
                title = "支出分布",
                icon = Icons.Default.TrendingDown,
                iconColor = Color(0xFFF44336),
                data = financeData?.expenseByCategory ?: emptyList(),
                chartType = chartType,
                trendData = financeData?.dailyTrend ?: emptyList(),
                isIncome = false
            )
        }

        // 收支趋势对比
        item {
            TrendComparisonCard(
                trendData = financeData?.dailyTrend ?: emptyList()
            )
        }

        // 预算执行分析
        if (budgetAnalysis.isNotEmpty()) {
            item {
                BudgetAnalysisCard(budgetAnalysis = budgetAnalysis)
            }
        }

        // AI预算建议
        if (budgetAIAdvice.isNotBlank()) {
            item {
                AIBudgetAdviceCard(advice = budgetAIAdvice)
            }
        }

        // 支出分类排名
        if (expenseRanking.isNotEmpty()) {
            item {
                ExpenseRankingCard(ranking = expenseRanking)
            }
        }

        // 账单明细列表
        if (billList.isNotEmpty()) {
            item {
                BillListCard(bills = billList)
            }
        }
    }
}

/**
 * 财务摘要卡片
 */
@Composable
private fun FinanceSummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    balance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FinanceSummaryItem(
                label = "总收入",
                value = totalIncome,
                color = Color(0xFF4CAF50)
            )
            FinanceSummaryItem(
                label = "总支出",
                value = totalExpense,
                color = Color(0xFFF44336)
            )
            FinanceSummaryItem(
                label = "结余",
                value = balance,
                color = if (balance >= 0) Color(0xFF2196F3) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
private fun FinanceSummaryItem(
    label: String,
    value: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¥${formatAmount(value)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 财务图表卡片
 */
@Composable
private fun FinanceChartCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    data: List<CategoryChartItem>,
    chartType: ChartType,
    trendData: List<DailyFinanceTrend>,
    isIncome: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 图表内容
            when (chartType) {
                ChartType.PIE -> {
                    if (data.isEmpty()) {
                        EmptyChartPlaceholder()
                    } else {
                        PieChartView(
                            data = data.map { item ->
                                PieChartData(
                                    label = item.name,
                                    value = item.value,
                                    color = item.color,
                                    id = item.fieldId
                                )
                            }
                        )
                    }
                }
                ChartType.BAR -> {
                    if (data.isEmpty()) {
                        EmptyChartPlaceholder()
                    } else {
                        HorizontalBarChart(
                            data = data.map { item ->
                                BarChartData(
                                    label = item.name,
                                    value = item.value,
                                    color = item.color
                                )
                            }
                        )
                    }
                }
                ChartType.LINE -> {
                    if (trendData.isEmpty()) {
                        EmptyChartPlaceholder()
                    } else {
                        val values = trendData.map {
                            if (isIncome) it.income.toFloat() else it.expense.toFloat()
                        }
                        val labels = trendData.map { formatTrendDate(it.date) }
                        SingleTrendChart(
                            data = values,
                            xLabels = labels,
                            label = title,
                            color = iconColor
                        )
                    }
                }
                else -> {
                    // 其他图表类型暂不支持，显示占位符
                    EmptyChartPlaceholder()
                }
            }
        }
    }
}

/**
 * 收支趋势对比卡片
 */
@Composable
private fun TrendComparisonCard(
    trendData: List<DailyFinanceTrend>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "收支趋势对比",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (trendData.isEmpty()) {
                EmptyChartPlaceholder()
            } else {
                val incomeValues = trendData.map { it.income.toFloat() }
                val expenseValues = trendData.map { it.expense.toFloat() }
                val labels = trendData.map { formatTrendDate(it.date) }

                IncomeExpenseTrendChart(
                    incomeData = incomeValues,
                    expenseData = expenseValues,
                    xLabels = labels
                )
            }
        }
    }
}

private fun formatAmount(value: Double): String {
    return if (value >= 10000) {
        String.format("%.1f万", value / 10000)
    } else {
        String.format("%.2f", value)
    }
}

private fun formatTrendDate(epochDay: Int): String {
    val date = LocalDate.ofEpochDay(epochDay.toLong())
    return date.format(DateTimeFormatter.ofPattern("MM/dd"))
}

/**
 * 预算执行分析卡片
 */
@Composable
private fun BudgetAnalysisCard(
    budgetAnalysis: List<com.lifemanager.app.domain.model.MonthlyBudgetAnalysis>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "预算执行趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 预算执行柱状图
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                budgetAnalysis.forEach { analysis ->
                    val maxValue = budgetAnalysis.maxOf { maxOf(it.budgetAmount, it.spentAmount) }
                    val budgetHeight = if (maxValue > 0) (analysis.budgetAmount / maxValue * 80).toFloat() else 0f
                    val spentHeight = if (maxValue > 0) (analysis.spentAmount / maxValue * 80).toFloat() else 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 柱状图
                        Row(
                            modifier = Modifier.height(70.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // 预算柱
                            if (analysis.hasBudget) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(budgetHeight.dp.coerceAtLeast(4.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }
                            // 支出柱
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(spentHeight.dp.coerceAtLeast(4.dp))
                                    .background(
                                        when {
                                            analysis.usagePercentage >= 100 -> Color(0xFFF44336)
                                            analysis.usagePercentage >= 80 -> Color(0xFFFF9800)
                                            else -> Color(0xFF4CAF50)
                                        },
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 月份标签
                        Text(
                            text = analysis.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 执行率
                        if (analysis.hasBudget) {
                            Text(
                                text = "${analysis.usagePercentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    analysis.usagePercentage >= 100 -> Color(0xFFF44336)
                                    analysis.usagePercentage >= 80 -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    label = "预算"
                )
                Spacer(modifier = Modifier.width(24.dp))
                LegendItem(
                    color = Color(0xFF4CAF50),
                    label = "实际支出"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * AI预算建议卡片
 */
@Composable
private fun AIBudgetAdviceCard(advice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 预算分析建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = advice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 支出分类排名卡片
 */
@Composable
private fun ExpenseRankingCard(ranking: List<CategoryRankingItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "支出分类排名",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            ranking.take(10).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 排名
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                when (item.rank) {
                                    1 -> Color(0xFFFFD700)
                                    2 -> Color(0xFFC0C0C0)
                                    3 -> Color(0xFFCD7F32)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.rank}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (item.rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 颜色指示
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(item.color, RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 分类名称
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    // 金额和百分比
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "¥${formatAmount(item.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            text = "${String.format("%.1f", item.percentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (item.rank < ranking.size.coerceAtMost(10)) {
                    Divider(
                        modifier = Modifier.padding(start = 32.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 账单明细卡片
 */
@Composable
private fun BillListCard(bills: List<BillQueryItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "账单明细",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "共${bills.size}笔",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            bills.take(20).forEach { bill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分类卡通图标
                    val emoji = com.lifemanager.app.ui.component.CategoryIcons.getIcon(
                        name = bill.categoryName,
                        moduleType = bill.type
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(parseColor(bill.categoryColor), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 分类和备注
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bill.categoryName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (bill.note.isNotBlank()) {
                            Text(
                                text = bill.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // 金额和日期
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (bill.type == "INCOME") "+" else "-"}¥${formatAmount(bill.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        Text(
                            text = formatBillDate(bill.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            if (bills.size > 20) {
                Text(
                    text = "...还有${bills.size - 20}笔记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun formatBillDate(epochDay: Int): String {
    val date = LocalDate.ofEpochDay(epochDay.toLong())
    return date.format(DateTimeFormatter.ofPattern("MM-dd"))
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
