package com.lifemanager.app.feature.datacenter.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * 效率标签页
 *
 * 显示待办、习惯打卡、时间追踪的统计图表
 */
@Composable
fun ProductivityTab(
    data: ProductivityChartData?,
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
        // 待办统计
        item {
            TodoStatsCard(todoData = data.todoStats)
        }

        // 习惯打卡统计
        item {
            HabitStatsCard(habitData = data.habitStats)
        }

        // 时间追踪统计
        item {
            TimeStatsCard(timeData = data.timeStats)
        }
    }
}

/**
 * 待办统计卡片
 */
@Composable
private fun TodoStatsCard(todoData: TodoChartData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            SectionHeader(
                title = "待办统计",
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 统计数据行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TodoStatItem(
                    label = "已完成",
                    value = todoData.completed,
                    color = Color(0xFF4CAF50)
                )
                TodoStatItem(
                    label = "进行中",
                    value = todoData.pending,
                    color = Color(0xFFFF9800)
                )
                TodoStatItem(
                    label = "已逾期",
                    value = todoData.overdue,
                    color = Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 完成率进度环
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ProgressRingWithLabel(
                    progress = todoData.completionRate,
                    label = "完成率",
                    value = "${(todoData.completionRate * 100).toInt()}%",
                    size = 100.dp,
                    progressColor = Color(0xFF4CAF50)
                )
            }

            // 每日完成趋势
            if (todoData.dailyCompletions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "每日完成趋势",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SingleTrendChart(
                    data = todoData.dailyCompletions.map { it.count.toFloat() },
                    xLabels = todoData.dailyCompletions.map { formatDate(it.date) },
                    label = "完成数",
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun TodoStatItem(
    label: String,
    value: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 习惯打卡统计卡片
 */
@Composable
private fun HabitStatsCard(habitData: HabitChartData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            SectionHeader(
                title = "习惯打卡",
                icon = Icons.Default.Verified,
                iconColor = Color(0xFF9C27B0)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 统计概览
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${habitData.activeHabits}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "活跃习惯",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${habitData.todayCheckedIn}/${habitData.activeHabits}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0)
                    )
                    Text(
                        text = "今日打卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProgressRingWithLabel(
                    progress = habitData.overallRate,
                    label = "总完成率",
                    value = "${(habitData.overallRate * 100).toInt()}%",
                    size = 80.dp,
                    progressColor = Color(0xFF9C27B0)
                )
            }

            // 各习惯完成率
            if (habitData.habitCompletionRates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "各习惯完成率",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                habitData.habitCompletionRates.forEach { habit ->
                    HabitProgressRow(
                        name = habit.name,
                        progress = habit.completionRate,
                        color = habit.color
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 周打卡趋势
            if (habitData.weeklyCheckins.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "周打卡趋势",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                VerticalBarChart(
                    data = habitData.weeklyCheckins.map {
                        BarChartData(
                            label = formatDate(it.date),
                            value = it.count.toDouble(),
                            color = Color(0xFF9C27B0)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HabitProgressRow(
    name: String,
    progress: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp)
        )
    }
}

/**
 * 时间追踪统计卡片
 */
@Composable
private fun TimeStatsCard(timeData: TimeChartData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            SectionHeader(
                title = "时间追踪",
                icon = Icons.Default.Timer,
                iconColor = Color(0xFFFF9800)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 总专注时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMinutes(timeData.totalMinutes),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = "总专注时长",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 分类分布
            if (timeData.categoryBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "时间分布",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PieChartView(
                    data = timeData.categoryBreakdown.map { item ->
                        PieChartData(
                            label = item.name,
                            value = item.value,
                            color = item.color,
                            id = item.fieldId
                        )
                    }
                )
            }

            // 每日专注趋势
            if (timeData.dailyDurations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "每日专注趋势",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SingleTrendChart(
                    data = timeData.dailyDurations.map { it.minutes.toFloat() },
                    xLabels = timeData.dailyDurations.map { formatDate(it.date) },
                    label = "专注时长(分钟)",
                    color = Color(0xFFFF9800)
                )
            }
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

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
