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
import com.lifemanager.app.feature.datacenter.model.*
import com.lifemanager.app.ui.component.charts.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 生活标签页
 *
 * 显示日记和存钱计划的统计图表
 */
@Composable
fun LifestyleTab(
    data: LifestyleChartData?,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 日记统计
        item {
            DiaryStatsCard(diaryData = data.diaryStats)
        }

        // 存钱计划统计
        item {
            SavingsStatsCard(savingsData = data.savingsStats)
        }
    }
}

/**
 * 日记统计卡片
 */
@Composable
private fun DiaryStatsCard(diaryData: DiaryChartData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            SectionHeader(
                title = "日记统计",
                icon = Icons.Default.Book,
                iconColor = Color(0xFFE91E63)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 统计概览
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${diaryData.totalEntries}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )
                    Text(
                        text = "日记总数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (diaryData.averageMood > 0)
                            String.format("%.1f", diaryData.averageMood) else "-",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = getMoodColor(diaryData.averageMood)
                    )
                    Text(
                        text = "平均心情",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = getMoodIcon(diaryData.averageMood),
                        contentDescription = null,
                        tint = getMoodColor(diaryData.averageMood),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = getMoodLabel(diaryData.averageMood),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 心情分布
            if (diaryData.moodDistribution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "心情分布",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                MoodDistributionChart(distribution = diaryData.moodDistribution)
            }

            // 心情趋势
            if (diaryData.dailyMoodTrend.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "心情趋势",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SingleTrendChart(
                    data = diaryData.dailyMoodTrend.map { it.moodScore },
                    xLabels = diaryData.dailyMoodTrend.map { formatDate(it.date) },
                    label = "心情分数",
                    color = Color(0xFFE91E63)
                )
            }
        }
    }
}

/**
 * 心情分布图表
 */
@Composable
private fun MoodDistributionChart(distribution: List<MoodDistributionItem>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        distribution.sortedBy { it.moodScore }.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // 百分比标签
                Text(
                    text = "${(item.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 柱子
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(item.percentage.coerceIn(0.05f, 1f))
                            .background(item.color)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 心情图标
                Icon(
                    imageVector = getMoodIconByScore(item.moodScore),
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 存钱计划统计卡片
 */
@Composable
private fun SavingsStatsCard(savingsData: SavingsChartData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            SectionHeader(
                title = "存钱计划",
                icon = Icons.Default.Savings,
                iconColor = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 统计概览
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${savingsData.activePlans}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "进行中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "¥${formatAmount(savingsData.totalCurrent)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "已存金额",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProgressRingWithLabel(
                    progress = savingsData.overallProgress,
                    label = "总进度",
                    value = "${(savingsData.overallProgress * 100).toInt()}%",
                    size = 80.dp,
                    progressColor = Color(0xFF2196F3)
                )
            }

            // 目标金额
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "目标总额：¥${formatAmount(savingsData.totalTarget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 各计划进度
            if (savingsData.planProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "各计划进度",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                savingsData.planProgress.forEach { plan ->
                    SavingsPlanProgressRow(plan = plan)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 存钱计划进度行
 */
@Composable
private fun SavingsPlanProgressRow(plan: SavingsPlanProgress) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(plan.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "${(plan.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { plan.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = plan.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "¥${formatAmount(plan.currentAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "/ ¥${formatAmount(plan.targetAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 区块标题组件
 */
@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(epochDay: Int): String {
    val date = LocalDate.ofEpochDay(epochDay.toLong())
    return date.format(DateTimeFormatter.ofPattern("MM/dd"))
}

private fun formatAmount(value: Double): String {
    return if (value >= 10000) {
        String.format("%.1f万", value / 10000)
    } else {
        String.format("%.0f", value)
    }
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

private fun getMoodIcon(mood: Float): ImageVector {
    return when {
        mood >= 4 -> Icons.Default.SentimentVerySatisfied
        mood >= 3 -> Icons.Default.SentimentSatisfied
        mood >= 2 -> Icons.Default.SentimentNeutral
        mood > 0 -> Icons.Default.SentimentDissatisfied
        else -> Icons.Default.SentimentNeutral
    }
}

private fun getMoodIconByScore(score: Int): ImageVector {
    return when (score) {
        5 -> Icons.Default.SentimentVerySatisfied
        4 -> Icons.Default.SentimentSatisfied
        3 -> Icons.Default.SentimentNeutral
        2 -> Icons.Default.SentimentDissatisfied
        1 -> Icons.Default.SentimentVeryDissatisfied
        else -> Icons.Default.SentimentNeutral
    }
}

private fun getMoodLabel(mood: Float): String {
    return when {
        mood >= 4 -> "很开心"
        mood >= 3 -> "还不错"
        mood >= 2 -> "一般般"
        mood > 0 -> "不太好"
        else -> "未知"
    }
}
