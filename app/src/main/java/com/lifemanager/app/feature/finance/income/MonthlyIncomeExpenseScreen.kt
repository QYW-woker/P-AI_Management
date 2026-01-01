package com.lifemanager.app.feature.finance.income

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
import com.lifemanager.app.core.database.entity.IncomeExpenseType
import com.lifemanager.app.domain.model.FieldStats
import com.lifemanager.app.domain.model.IncomeExpenseUiState
import com.lifemanager.app.domain.model.MonthlyIncomeExpenseWithField
import com.lifemanager.app.domain.model.IncomeExpenseMonthlyStats
import com.lifemanager.app.ui.component.charts.PieChartView
import com.lifemanager.app.ui.component.charts.PieChartData
import java.text.NumberFormat
import java.util.Locale

/**
 * 月度收支主界面
 *
 * 展示月度收入支出统计、分类占比和详细记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyIncomeExpenseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFieldManagement: () -> Unit = {},
    viewModel: MonthlyIncomeExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val records by viewModel.records.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val incomeFieldStats by viewModel.incomeFieldStats.collectAsState()
    val expenseFieldStats by viewModel.expenseFieldStats.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    // 当前选中的标签页 (0: 收入, 1: 支出)
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("月度收支") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToFieldManagement) {
                        Icon(Icons.Filled.Settings, contentDescription = "字段管理")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.showAddDialog(
                        if (selectedTab == 0) IncomeExpenseType.INCOME else IncomeExpenseType.EXPENSE
                    )
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
                is IncomeExpenseUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is IncomeExpenseUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as IncomeExpenseUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is IncomeExpenseUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 统计卡片
                        item {
                            StatsCard(stats = monthlyStats)
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
                                    text = { Text("收入") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("支出") }
                                )
                            }
                        }

                        // 分类统计图表
                        item {
                            val chartData = if (selectedTab == 0) incomeFieldStats else expenseFieldStats
                            if (chartData.isNotEmpty()) {
                                FieldStatsChart(
                                    title = if (selectedTab == 0) "收入分类" else "支出分类",
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
                                it.record.type == IncomeExpenseType.INCOME
                            } else {
                                it.record.type == IncomeExpenseType.EXPENSE
                            }
                        }

                        if (filteredRecords.isEmpty()) {
                            item {
                                EmptyState(
                                    message = if (selectedTab == 0) "暂无收入记录" else "暂无支出记录"
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
        AddEditRecordDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？此操作不可撤销。") },
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
 * 统计卡片
 */
@Composable
private fun StatsCard(stats: IncomeExpenseMonthlyStats) {
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 收入
                StatItem(
                    label = "收入",
                    amount = stats.totalIncome,
                    color = Color(0xFF4CAF50),
                    numberFormat = numberFormat
                )

                // 支出
                StatItem(
                    label = "支出",
                    amount = stats.totalExpense,
                    color = Color(0xFFF44336),
                    numberFormat = numberFormat
                )

                // 结余
                StatItem(
                    label = "结余",
                    amount = stats.netIncome,
                    color = if (stats.netIncome >= 0) Color(0xFF2196F3) else Color(0xFFFF9800),
                    numberFormat = numberFormat
                )
            }

            // 储蓄率
            if (stats.totalIncome > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "储蓄率: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1f%%", stats.savingsRate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.savingsRate >= 30) {
                            Color(0xFF4CAF50)
                        } else if (stats.savingsRate >= 10) {
                            Color(0xFFFF9800)
                        } else {
                            Color(0xFFF44336)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(
    label: String,
    amount: Double,
    color: Color,
    numberFormat: NumberFormat
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¥${numberFormat.format(amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 字段统计图表
 */
@Composable
private fun FieldStatsChart(
    title: String,
    stats: List<FieldStats>
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
                // 饼图
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

                // 图例
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
private fun LegendItem(stat: FieldStats) {
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
    record: MonthlyIncomeExpenseWithField,
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
            // 类别图标背景
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
                    imageVector = Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 类别名称和备注
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

            // 金额
            Text(
                text = "${if (record.record.type == IncomeExpenseType.INCOME) "+" else "-"}¥${
                    numberFormat.format(record.record.amount)
                }",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (record.record.type == IncomeExpenseType.INCOME) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFF44336)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮
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
