package com.lifemanager.app.ui.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 折线图数据系列
 */
data class LineChartSeries(
    val label: String,
    val values: List<Float>,
    val color: Color
)

/**
 * 趋势折线图组件
 *
 * 使用 Canvas 实现的折线图，支持多系列数据展示
 *
 * @param series 数据系列列表
 * @param xLabels X轴标签列表
 * @param showLegend 是否显示图例
 * @param modifier 修饰符
 */
@Composable
fun TrendLineChart(
    series: List<LineChartSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true
) {
    if (series.isEmpty() || series.all { it.values.isEmpty() }) {
        EmptyChartPlaceholder(modifier = modifier)
        return
    }

    Column(modifier = modifier) {
        // 自定义Canvas绘制折线图
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val width = size.width
            val height = size.height
            val padding = 40f
            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            // 找到所有系列的最大值和最小值
            val allValues = series.flatMap { it.values }
            val maxValue = allValues.maxOrNull() ?: 1f
            val minValue = allValues.minOrNull() ?: 0f
            val valueRange = if (maxValue == minValue) 1f else maxValue - minValue

            // 绘制每个系列
            series.forEach { s ->
                if (s.values.size < 2) return@forEach

                val path = Path()
                val pointCount = s.values.size

                s.values.forEachIndexed { index, value ->
                    val x = padding + (index.toFloat() / (pointCount - 1)) * chartWidth
                    val normalizedValue = (value - minValue) / valueRange
                    val y = height - padding - normalizedValue * chartHeight

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // 绘制线条
                drawPath(
                    path = path,
                    color = s.color,
                    style = Stroke(width = 3f)
                )

                // 绘制数据点
                s.values.forEachIndexed { index, value ->
                    val x = padding + (index.toFloat() / (pointCount - 1)) * chartWidth
                    val normalizedValue = (value - minValue) / valueRange
                    val y = height - padding - normalizedValue * chartHeight

                    drawCircle(
                        color = s.color,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // X轴标签
        if (xLabels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 只显示首尾和中间的标签，避免拥挤
                val labelIndices = when {
                    xLabels.size <= 3 -> xLabels.indices.toList()
                    xLabels.size <= 7 -> listOf(0, xLabels.size / 2, xLabels.size - 1)
                    else -> listOf(0, xLabels.size / 4, xLabels.size / 2, 3 * xLabels.size / 4, xLabels.size - 1)
                }
                labelIndices.forEach { index ->
                    if (index < xLabels.size) {
                        Text(
                            text = xLabels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showLegend && series.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            ChartLegend(series = series)
        }
    }
}

/**
 * 图表图例
 */
@Composable
private fun ChartLegend(series: List<LineChartSeries>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        series.forEach { s ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(s.color, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = s.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 空图表占位符
 */
@Composable
fun EmptyChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无数据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 双折线趋势图（收入支出对比专用）
 */
@Composable
fun IncomeExpenseTrendChart(
    incomeData: List<Float>,
    expenseData: List<Float>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    incomeColor: Color = Color(0xFF4CAF50),
    expenseColor: Color = Color(0xFFF44336)
) {
    TrendLineChart(
        series = listOf(
            LineChartSeries(
                label = "收入",
                values = incomeData,
                color = incomeColor
            ),
            LineChartSeries(
                label = "支出",
                values = expenseData,
                color = expenseColor
            )
        ),
        xLabels = xLabels,
        modifier = modifier,
        showLegend = true
    )
}

/**
 * 单折线趋势图
 */
@Composable
fun SingleTrendChart(
    data: List<Float>,
    xLabels: List<String>,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    TrendLineChart(
        series = listOf(
            LineChartSeries(
                label = label,
                values = data,
                color = color
            )
        ),
        xLabels = xLabels,
        modifier = modifier,
        showLegend = false
    )
}
