package com.lifemanager.app.feature.datacenter.tab

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
import com.lifemanager.app.feature.datacenter.model.CategoryChartItem
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
