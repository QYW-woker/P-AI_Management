package com.lifemanager.app.ui.component.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

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
 * 使用 Vico 库实现的折线图，支持多系列数据展示
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

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineSeries {
                series.forEach { s ->
                    series(s.values)
                }
            }
        }
    }

    Column(modifier = modifier) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        series.map { s ->
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(s.color))
                            )
                        }
                    )
                ),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _, _ ->
                        xLabels.getOrElse(value.toInt()) { "" }
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

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
