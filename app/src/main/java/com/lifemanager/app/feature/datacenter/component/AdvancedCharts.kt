package com.lifemanager.app.feature.datacenter.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifemanager.app.feature.datacenter.model.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 热力图视图
 */
@Composable
fun HeatmapChartView(
    data: HeatmapData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = data.title.ifEmpty { "热力图" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.cells.isEmpty()) {
                EmptyDataPlaceholder()
            } else {
                // 列标签
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(40.dp))
                    data.columnLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 热力图网格
                data.rowLabels.forEachIndexed { rowIndex, rowLabel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 行标签
                        Text(
                            text = rowLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(40.dp)
                        )

                        // 单元格
                        data.columnLabels.indices.forEach { colIndex ->
                            val cell = data.cells.find { it.row == rowIndex && it.column == colIndex }
                            val value = cell?.value ?: 0f
                            val cellColor = lerpColor(data.colorStart, data.colorEnd, value)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell?.label?.isNotBlank() == true) {
                                    Text(
                                        text = cell.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (value > 0.5f) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // 图例
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "低",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(12.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(data.colorStart, data.colorEnd)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "高",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * 雷达图视图
 */
@Composable
fun RadarChartView(
    data: RadarChartData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "多维度评估雷达图",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.axes.isEmpty()) {
                EmptyDataPlaceholder()
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = min(centerX, centerY) * 0.7f
                    val axisCount = data.axes.size
                    val angleStep = 360f / axisCount

                    // 绘制背景网格
                    for (level in 1..5) {
                        val levelRadius = radius * level / 5
                        val path = Path()
                        for (i in 0 until axisCount) {
                            val angle = Math.toRadians((angleStep * i - 90).toDouble())
                            val x = centerX + (levelRadius * cos(angle)).toFloat()
                            val y = centerY + (levelRadius * sin(angle)).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(
                            path = path,
                            color = Color.Gray.copy(alpha = 0.2f),
                            style = Stroke(width = 1f)
                        )
                    }

                    // 绘制轴线
                    for (i in 0 until axisCount) {
                        val angle = Math.toRadians((angleStep * i - 90).toDouble())
                        val endX = centerX + (radius * cos(angle)).toFloat()
                        val endY = centerY + (radius * sin(angle)).toFloat()
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(centerX, centerY),
                            end = Offset(endX, endY),
                            strokeWidth = 1f
                        )
                    }

                    // 绘制数据
                    data.dataSets.forEach { dataSet ->
                        val dataPath = Path()
                        dataSet.points.forEachIndexed { index, point ->
                            val angle = Math.toRadians((angleStep * index - 90).toDouble())
                            val pointRadius = radius * (point.value / data.maxValue)
                            val x = centerX + (pointRadius * cos(angle)).toFloat()
                            val y = centerY + (pointRadius * sin(angle)).toFloat()
                            if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                        }
                        dataPath.close()

                        // 填充区域
                        drawPath(
                            path = dataPath,
                            color = dataSet.color.copy(alpha = dataSet.fillAlpha)
                        )

                        // 边框
                        drawPath(
                            path = dataPath,
                            color = dataSet.color,
                            style = Stroke(width = 2f)
                        )

                        // 数据点
                        dataSet.points.forEachIndexed { index, point ->
                            val angle = Math.toRadians((angleStep * index - 90).toDouble())
                            val pointRadius = radius * (point.value / data.maxValue)
                            val x = centerX + (pointRadius * cos(angle)).toFloat()
                            val y = centerY + (pointRadius * sin(angle)).toFloat()
                            drawCircle(
                                color = dataSet.color,
                                radius = 6f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                // 轴标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.axes.take(6).forEach { axis ->
                        Text(
                            text = axis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 图例
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    data.dataSets.forEach { dataSet ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(dataSet.color, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dataSet.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 树状图视图
 */
@Composable
fun TreemapChartView(
    data: TreemapData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.title.ifEmpty { "分类占比" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "总计: ¥${formatLargeNumber(data.totalValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (data.rootNodes.isEmpty()) {
                EmptyDataPlaceholder()
            } else {
                // 简化的树状图实现 - 使用卡片网格
                val nodes = data.rootNodes.take(8)
                val totalValue = nodes.sumOf { it.value }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    nodes.chunked(2).forEach { rowNodes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowNodes.forEach { node ->
                                val weight = if (totalValue > 0) {
                                    (node.value / totalValue).toFloat().coerceIn(0.3f, 1f)
                                } else 0.5f

                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(node.color.copy(alpha = 0.8f))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = node.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${String.format("%.1f", node.percentage)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            // 填充空白
                            if (rowNodes.size == 1) {
                                Spacer(modifier = Modifier.weight(0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 瀑布图视图
 */
@Composable
fun WaterfallChartView(
    data: WaterfallData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = data.title.ifEmpty { "收支流水" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.items.isEmpty()) {
                EmptyDataPlaceholder()
            } else {
                val maxValue = data.items.maxOfOrNull { maxOf(it.startValue, it.endValue) } ?: 1.0
                val minValue = data.items.minOfOrNull { minOf(it.startValue, it.endValue) } ?: 0.0
                val range = (maxValue - minValue).coerceAtLeast(1.0)

                data.items.take(10).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 标签
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(60.dp),
                            maxLines = 1
                        )

                        // 条形图
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                        ) {
                            val startOffset = ((item.startValue - minValue) / range).toFloat()
                            val endOffset = ((item.endValue - minValue) / range).toFloat()
                            val barStart = minOf(startOffset, endOffset)
                            val barEnd = maxOf(startOffset, endOffset)

                            val barColor = when {
                                item.isTotal -> data.totalColor
                                item.isPositive -> data.positiveColor
                                else -> data.negativeColor
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(barEnd)
                                    .padding(start = (barStart * 200).dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(barColor)
                            )
                        }

                        // 金额
                        Text(
                            text = "${if (item.isPositive) "+" else "-"}¥${formatLargeNumber(item.value)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                item.isTotal -> data.totalColor
                                item.isPositive -> data.positiveColor
                                else -> data.negativeColor
                            },
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }

                // 图例
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LegendItem(color = data.positiveColor, label = "收入")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = data.negativeColor, label = "支出")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = data.totalColor, label = "余额")
                }
            }
        }
    }
}

/**
 * 漏斗图视图
 */
@Composable
fun FunnelChartView(
    data: FunnelData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = data.title.ifEmpty { "转化漏斗" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.items.isEmpty()) {
                EmptyDataPlaceholder()
            } else {
                val maxValue = data.items.firstOrNull()?.value ?: 1.0

                data.items.forEach { item ->
                    val widthFraction = if (maxValue > 0) {
                        (item.value / maxValue).toFloat().coerceIn(0.2f, 1f)
                    } else 0.5f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(widthFraction)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(item.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${item.value.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${String.format("%.0f", item.percentage)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // 转化率指示
                        if (item.conversionRate < 100 && item.conversionRate > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.0f", item.conversionRate)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 对比数据卡片
 */
@Composable
fun CompareDataCard(
    data: CompareData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "数据对比分析",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // 总体变化
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (data.totalChangePercentage >= 0)
                            Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (data.totalChangePercentage >= 0)
                            Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (data.totalChangePercentage >= 0) "+" else ""}${
                            String.format("%.1f", data.totalChangePercentage)
                        }%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (data.totalChangePercentage >= 0)
                            Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 期间标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = data.currentPeriodLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "vs ${data.comparePeriodLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.items.isEmpty()) {
                EmptyDataPlaceholder()
            } else {
                data.items.take(8).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 分类颜色
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(item.color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // 分类名称
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // 本期金额
                        Text(
                            text = "¥${formatLargeNumber(item.currentValue)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // 变化
                        val changeColor = when {
                            item.changePercentage > 5 -> Color(0xFFF44336)
                            item.changePercentage < -5 -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = "${if (item.changePercentage >= 0) "+" else ""}${
                                String.format("%.0f", item.changePercentage)
                            }%",
                            style = MaterialTheme.typography.labelMedium,
                            color = changeColor,
                            modifier = Modifier.width(50.dp)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/**
 * 数据洞察卡片
 */
@Composable
fun DataInsightsCard(
    insights: List<DataInsight>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "数据洞察",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (insights.isEmpty()) {
                Text(
                    text = "暂无洞察数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                insights.forEach { insight ->
                    InsightItem(insight = insight)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InsightItem(insight: DataInsight) {
    val (iconVector, iconColor) = when (insight.type) {
        InsightType.WARNING -> Icons.Default.Warning to Color(0xFFF44336)
        InsightType.ACHIEVEMENT -> Icons.Default.EmojiEvents to Color(0xFFFFD700)
        InsightType.TREND -> Icons.Default.TrendingUp to Color(0xFF2196F3)
        InsightType.ANOMALY -> Icons.Default.ErrorOutline to Color(0xFFFF9800)
        InsightType.SUGGESTION -> Icons.Default.Lightbulb to Color(0xFF4CAF50)
    }

    val importanceColor = when (insight.importance) {
        InsightImportance.HIGH -> MaterialTheme.colorScheme.errorContainer
        InsightImportance.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        InsightImportance.LOW -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = importanceColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (insight.actionSuggestion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight.actionSuggestion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

@Composable
private fun EmptyDataPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 颜色插值
 */
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

/**
 * 格式化大数字
 */
private fun formatLargeNumber(value: Double): String {
    return when {
        value >= 100000000 -> String.format("%.1f亿", value / 100000000)
        value >= 10000 -> String.format("%.1f万", value / 10000)
        else -> String.format("%.0f", value)
    }
}
