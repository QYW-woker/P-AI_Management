package com.lifemanager.app.feature.datacenter.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifemanager.app.core.database.entity.AIAnalysisEntity
import com.lifemanager.app.feature.datacenter.OverviewStats
import com.lifemanager.app.feature.datacenter.model.AssetTrendData
import com.lifemanager.app.feature.datacenter.model.FinanceChartData
import com.lifemanager.app.feature.datacenter.model.LifestyleChartData
import com.lifemanager.app.feature.datacenter.model.ProductivityChartData
import com.lifemanager.app.ui.component.AIInsightMiniCard
import com.lifemanager.app.ui.component.OverallHealthCard
import com.lifemanager.app.ui.component.charts.IncomeExpenseComparisonBar
import com.lifemanager.app.ui.component.charts.ProgressRingWithLabel
import com.lifemanager.app.ui.component.charts.TrendLineChart
import com.lifemanager.app.ui.component.charts.LineChartSeries

/**
 * 总览标签页
 *
 * 显示所有模块的关键指标汇总
 */
@Composable
fun OverviewTab(
    overviewStats: OverviewStats,
    financeData: FinanceChartData?,
    productivityData: ProductivityChartData?,
    lifestyleData: LifestyleChartData?,
    assetTrendData: AssetTrendData? = null,
    overallHealthScore: AIAnalysisEntity? = null,
    isAIAnalyzing: Boolean = false,
    financeAnalysis: AIAnalysisEntity? = null,
    goalAnalysis: AIAnalysisEntity? = null,
    habitAnalysis: AIAnalysisEntity? = null,
    onRefreshAI: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI综合健康评分卡片
        item {
            OverallHealthCard(
                analysis = overallHealthScore,
                isLoading = isAIAnalyzing,
                onRefresh = onRefreshAI
            )
        }

        // 数据总览卡片
        item {
            OverviewSummaryCard(stats = overviewStats)
        }

        // AI模块洞察
        item {
            AIModuleInsightsSection(
                financeAnalysis = financeAnalysis,
                goalAnalysis = goalAnalysis,
                habitAnalysis = habitAnalysis
            )
        }

        // 资产趋势图
        item {
            AssetTrendCard(assetTrendData = assetTrendData)
        }

        // 财务概览
        item {
            SectionCard(title = "财务概览", icon = Icons.Default.AccountBalance) {
                financeData?.let { data ->
                    IncomeExpenseComparisonBar(
                        income = data.totalIncome,
                        expense = data.totalExpense
                    )
                } ?: EmptyDataHint()
            }
        }

        // 效率概览
        item {
            SectionCard(title = "效率概览", icon = Icons.Default.Speed) {
                productivityData?.let { data ->
                    EfficiencyOverview(
                        todoCompletionRate = data.todoStats.completionRate,
                        habitCompletionRate = data.habitStats.overallRate,
                        focusMinutes = data.timeStats.totalMinutes
                    )
                } ?: EmptyDataHint()
            }
        }

        // 生活概览
        item {
            SectionCard(title = "生活概览", icon = Icons.Default.Favorite) {
                lifestyleData?.let { data ->
                    LifestyleOverview(
                        diaryCount = data.diaryStats.totalEntries,
                        averageMood = data.diaryStats.averageMood,
                        savingsProgress = data.savingsStats.overallProgress,
                        totalSavings = data.savingsStats.totalCurrent
                    )
                } ?: EmptyDataHint()
            }
        }
    }
}

/**
 * AI模块洞察区域
 */
@Composable
private fun AIModuleInsightsSection(
    financeAnalysis: AIAnalysisEntity?,
    goalAnalysis: AIAnalysisEntity?,
    habitAnalysis: AIAnalysisEntity?
) {
    val hasAnyAnalysis = financeAnalysis != null || goalAnalysis != null || habitAnalysis != null
    if (!hasAnyAnalysis) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 模块洞察",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 财务洞察
            financeAnalysis?.let { analysis ->
                AIInsightMiniCard(
                    analysis = analysis,
                    onClick = { /* 可扩展为跳转到详情 */ }
                )
            }

            // 目标洞察
            goalAnalysis?.let { analysis ->
                AIInsightMiniCard(
                    analysis = analysis,
                    onClick = { /* 可扩展为跳转到详情 */ }
                )
            }

            // 习惯洞察
            habitAnalysis?.let { analysis ->
                AIInsightMiniCard(
                    analysis = analysis,
                    onClick = { /* 可扩展为跳转到详情 */ }
                )
            }
        }
    }
}

/**
 * 总览摘要卡片
 */
@Composable
private fun OverviewSummaryCard(stats: OverviewStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "数据总览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(
                    icon = Icons.Default.AccountBalance,
                    label = "储蓄金额",
                    value = "¥${formatNumber(stats.totalSavings)}"
                )
                SummaryStatItem(
                    icon = Icons.Default.CheckCircle,
                    label = "完成任务",
                    value = "${stats.todoCompleted}"
                )
                SummaryStatItem(
                    icon = Icons.Default.Flag,
                    label = "达成目标",
                    value = "${stats.completedGoals}"
                )
            }
        }
    }
}

/**
 * 摘要统计项
 */
@Composable
private fun SummaryStatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * 效率概览组件
 */
@Composable
private fun EfficiencyOverview(
    todoCompletionRate: Float,
    habitCompletionRate: Float,
    focusMinutes: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ProgressRingWithLabel(
            progress = todoCompletionRate,
            label = "待办完成",
            value = "${(todoCompletionRate * 100).toInt()}%",
            size = 90.dp,
            progressColor = Color(0xFF4CAF50)
        )
        ProgressRingWithLabel(
            progress = habitCompletionRate,
            label = "习惯打卡",
            value = "${(habitCompletionRate * 100).toInt()}%",
            size = 90.dp,
            progressColor = Color(0xFF9C27B0)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatMinutes(focusMinutes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            Text(
                text = "专注时长",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 生活概览组件
 */
@Composable
private fun LifestyleOverview(
    diaryCount: Int,
    averageMood: Float,
    savingsProgress: Float,
    totalSavings: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 日记统计
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$diaryCount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE91E63)
            )
            Text(
                text = "日记数量",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 平均心情
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (averageMood > 0) String.format("%.1f", averageMood) else "-",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = getMoodColor(averageMood)
            )
            Text(
                text = "平均心情",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 存钱进度
        ProgressRingWithLabel(
            progress = savingsProgress,
            label = "存钱进度",
            value = "${(savingsProgress * 100).toInt()}%",
            size = 80.dp,
            progressColor = Color(0xFF2196F3)
        )
    }
}

/**
 * 区块卡片组件
 */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

/**
 * 空数据提示
 */
@Composable
fun EmptyDataHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无数据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNumber(value: Double): String {
    return if (value >= 10000) {
        String.format("%.1f万", value / 10000)
    } else {
        String.format("%.0f", value)
    }
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

private fun getMoodColor(mood: Float): Color {
    return when {
        mood >= 4 -> Color(0xFF4CAF50)
        mood >= 3 -> Color(0xFF8BC34A)
        mood >= 2 -> Color(0xFFFF9800)
        mood > 0 -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

/**
 * 资产趋势卡片
 */
@Composable
private fun AssetTrendCard(assetTrendData: AssetTrendData?) {
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
                    text = "资产趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            assetTrendData?.let { data ->
                if (data.trendPoints.isEmpty()) {
                    EmptyDataHint()
                } else {
                    // 净资产变化摘要
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "当前净资产",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "¥${formatNumber(data.latestNetWorth)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (data.latestNetWorth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }

                        // 变化指示
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "较上月",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (data.netWorthChange >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (data.netWorthChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (data.netWorthChange >= 0) "+" else ""}${formatNumber(data.netWorthChange)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (data.netWorthChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                                Text(
                                    text = " (${String.format("%+.1f", data.netWorthChangePercentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (data.netWorthChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }

                    // 趋势图
                    val netWorthValues = data.trendPoints.map { it.netWorth.toFloat() }
                    val assetValues = data.trendPoints.map { it.totalAssets.toFloat() }
                    val liabilityValues = data.trendPoints.map { it.totalLiabilities.toFloat() }
                    val labels = data.trendPoints.map { formatYearMonth(it.yearMonth) }

                    TrendLineChart(
                        series = listOf(
                            LineChartSeries(
                                label = "净资产",
                                values = netWorthValues,
                                color = Color(0xFF2196F3)
                            ),
                            LineChartSeries(
                                label = "总资产",
                                values = assetValues,
                                color = Color(0xFF4CAF50)
                            ),
                            LineChartSeries(
                                label = "总负债",
                                values = liabilityValues,
                                color = Color(0xFFF44336)
                            )
                        ),
                        xLabels = labels,
                        showLegend = true
                    )
                }
            } ?: EmptyDataHint()
        }
    }
}

private fun formatYearMonth(yearMonth: Int): String {
    val year = yearMonth / 100
    val month = yearMonth % 100
    return "${month}月"
}
