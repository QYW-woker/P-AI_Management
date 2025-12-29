package com.lifemanager.app.feature.datacenter.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifemanager.app.feature.datacenter.model.ChartType

/**
 * 图表类型选择器组件
 *
 * 使用Button形式切换不同图表类型
 *
 * @param selected 当前选中的图表类型
 * @param options 可选图表类型列表
 * @param onSelect 选择变更回调
 * @param modifier 修饰符
 */
@Composable
fun ChartTypeSelector(
    selected: ChartType,
    options: List<ChartType> = listOf(ChartType.PIE, ChartType.BAR, ChartType.LINE),
    onSelect: (ChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { chartType ->
            if (selected == chartType) {
                Button(
                    onClick = { onSelect(chartType) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = chartType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(chartType.displayName)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(chartType) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = chartType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(chartType.displayName)
                }
            }
        }
    }
}

/**
 * 图表类型对应的图标
 */
private val ChartType.icon: ImageVector
    get() = when (this) {
        ChartType.PIE -> Icons.Default.PieChart
        ChartType.BAR -> Icons.Default.BarChart
        ChartType.LINE -> Icons.Default.ShowChart
    }

/**
 * 紧凑版图表类型选择器（仅图标）
 */
@Composable
fun CompactChartTypeSelector(
    selected: ChartType,
    options: List<ChartType> = listOf(ChartType.PIE, ChartType.BAR, ChartType.LINE),
    onSelect: (ChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { chartType ->
            IconButton(
                onClick = { onSelect(chartType) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (selected == chartType) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (selected == chartType) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            ) {
                Icon(
                    imageVector = chartType.icon,
                    contentDescription = chartType.displayName,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 带标签的图表类型选择器
 */
@Composable
fun LabeledChartTypeSelector(
    label: String,
    selected: ChartType,
    options: List<ChartType> = listOf(ChartType.PIE, ChartType.BAR, ChartType.LINE),
    onSelect: (ChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CompactChartTypeSelector(
            selected = selected,
            options = options,
            onSelect = onSelect
        )
    }
}
