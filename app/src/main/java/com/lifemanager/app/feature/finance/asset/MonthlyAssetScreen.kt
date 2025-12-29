package com.lifemanager.app.feature.finance.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.AssetFieldStats
import com.lifemanager.app.domain.model.AssetStats
import com.lifemanager.app.domain.model.AssetUiState
import com.lifemanager.app.domain.model.MonthlyAssetWithField
import com.lifemanager.app.ui.component.charts.PieChartView
import com.lifemanager.app.ui.component.charts.PieChartData
import com.lifemanager.app.ui.component.charts.TrendLineChart
import com.lifemanager.app.ui.component.charts.LineChartSeries
import com.lifemanager.app.domain.model.NetWorthTrendPoint
import java.text.NumberFormat
import java.util.Locale

/**
 * 月度资产主界面
 *
 * 展示资产负债统计、净资产变化和详细记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAssetScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyAssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val records by viewModel.records.collectAsState()
    val assetStats by viewModel.assetStats.collectAsState()
    val assetFieldStats by viewModel.assetFieldStats.collectAsState()
    val liabilityFieldStats by viewModel.liabilityFieldStats.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showCopyDialog by viewModel.showCopyDialog.collectAsState()
    val netWorthTrend by viewModel.netWorthTrend.collectAsState()

    // 当前选中的标签页 (0: 资产, 1: 负债)
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("月度资产") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 复制上月数据按钮
                    IconButton(onClick = { viewModel.showCopyFromPreviousMonth() }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "复制上月数据")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.showAddDialog(isAsset = selectedTab == 0)
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加记录")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 月份选择器
            MonthSelector(
                yearMonth = currentYearMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                formatYearMonth = { viewModel.formatYearMonth(it) }
            )

            when (uiState) {
                is AssetUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is AssetUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as AssetUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is AssetUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 统计卡片
                        item {
                            AssetStatsCard(stats = assetStats)
                        }

                        // 净资产趋势图
                        if (netWorthTrend.isNotEmpty()) {
                            item {
                                NetWorthTrendCard(trendData = netWorthTrend)
                            }
                        }

                        // 标签页切换
                        item {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("资产") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("负债") }
                                )
                            }
                        }

                        // 分类统计图表
                        item {
                            val chartData = if (selectedTab == 0) assetFieldStats else liabilityFieldStats
                            if (chartData.isNotEmpty()) {
                                FieldStatsChart(
                                    title = if (selectedTab == 0) "资产分类" else "负债分类",
                                    stats = chartData
                                )
                            }
                        }

                        // 记录列表标题
                        item {
                            Text(
                                text = "详细记录",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 筛选当前类型的记录
                        val filteredRecords = records.filter {
                            if (selectedTab == 0) {
                                it.record.type == "ASSET"
                            } else {
                                it.record.type == "LIABILITY"
                            }
                        }

                        if (filteredRecords.isEmpty()) {
                            item {
                                EmptyState(
                                    message = if (selectedTab == 0) "暂无资产记录" else "暂无负债记录"
                                )
                            }
                        } else {
                            items(filteredRecords, key = { it.record.id }) { record ->
                                RecordItem(
                                    record = record,
                                    onClick = { viewModel.showEditDialog(record.record.id) },
                                    onDelete = { viewModel.showDeleteConfirm(record.record.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AddEditAssetDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }

    // 复制上月数据对话框
    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCopyDialog() },
            title = { Text("复制上月数据") },
            text = { Text("将上月的资产负债数据复制到本月，方便快速填写。是否继续？") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmCopyFromPreviousMonth() }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCopyDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 月份选择器
 */
@Composable
private fun MonthSelector(
    yearMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    formatYearMonth: (Int) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上个月")
            }

            Text(
                text = formatYearMonth(yearMonth),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下个月")
            }
        }
    }
}

/**
 * 资产统计卡片
 */
@Composable
private fun AssetStatsCard(stats: AssetStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 净资产
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "净资产",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥${numberFormat.format(stats.netWorth)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.netWorth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 资产
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总资产",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${numberFormat.format(stats.totalAssets)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }

                // 负债
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总负债",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥${numberFormat.format(stats.totalLiabilities)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }

                // 负债率
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "负债率",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1f%%", stats.debtRatio),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            stats.debtRatio < 30 -> Color(0xFF4CAF50)
                            stats.debtRatio < 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 净资产趋势图卡片
 */
@Composable
private fun NetWorthTrendCard(trendData: List<NetWorthTrendPoint>) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "净资产趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "近${trendData.size}个月",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (trendData.size >= 2) {
                // 计算趋势变化
                val firstValue = trendData.firstOrNull()?.netWorth ?: 0.0
                val lastValue = trendData.lastOrNull()?.netWorth ?: 0.0
                val change = lastValue - firstValue
                val changePercent = if (firstValue != 0.0) {
                    (change / kotlin.math.abs(firstValue)) * 100
                } else 0.0

                // 趋势指示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                change > 0 -> Icons.Default.TrendingUp
                                change < 0 -> Icons.Default.TrendingDown
                                else -> Icons.Default.TrendingFlat
                            },
                            contentDescription = null,
                            tint = when {
                                change > 0 -> Color(0xFF4CAF50)
                                change < 0 -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                change > 0 -> "+¥${numberFormat.format(change)}"
                                change < 0 -> "-¥${numberFormat.format(-change)}"
                                else -> "¥0"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                change > 0 -> Color(0xFF4CAF50)
                                change < 0 -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Text(
                        text = String.format("%+.1f%%", changePercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            change > 0 -> Color(0xFF4CAF50)
                            change < 0 -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 折线图
                TrendLineChart(
                    series = listOf(
                        LineChartSeries(
                            label = "净资产",
                            values = trendData.map { it.netWorth.toFloat() },
                            color = MaterialTheme.colorScheme.primary
                        )
                    ),
                    xLabels = trendData.map { it.formatMonth() },
                    modifier = Modifier.height(180.dp),
                    showLegend = false
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "数据不足，至少需要2个月的记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 字段统计图表
 */
@Composable
private fun FieldStatsChart(
    title: String,
    stats: List<AssetFieldStats>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp)
                ) {
                    PieChartView(
                        data = stats.map {
                            PieChartData(
                                label = it.fieldName,
                                value = it.amount,
                                color = parseColor(it.fieldColor)
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                        showLegend = false
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.take(5).forEach { stat ->
                        LegendItem(stat = stat)
                    }
                    if (stats.size > 5) {
                        Text(
                            text = "...还有${stats.size - 5}项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图例项
 */
@Composable
private fun LegendItem(stat: AssetFieldStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(parseColor(stat.fieldColor))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stat.fieldName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = String.format("%.1f%%", stat.percentage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 记录项
 */
@Composable
private fun RecordItem(
    record: MonthlyAssetWithField,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        record.field?.let { parseColor(it.color) }
                            ?: MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.record.type == "ASSET") {
                        Icons.Filled.AccountBalance
                    } else {
                        Icons.Filled.CreditCard
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.field?.name ?: "未分类",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (record.record.note.isNotBlank()) {
                    Text(
                        text = record.record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "¥${numberFormat.format(record.record.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (record.record.type == "ASSET") {
                    Color(0xFF2196F3)
                } else {
                    Color(0xFFF44336)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 空状态提示
 */
@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 解析颜色字符串
 */
private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
