package com.lifemanager.app.feature.finance.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * 日常记账主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToImport: () -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    viewModel: DailyTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactionGroups by viewModel.transactionGroups.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val showBatchDeleteDialog by viewModel.showBatchDeleteDialog.collectAsState()

    // 使用 BackHandler 处理返回键
    if (isSelectionMode) {
        androidx.activity.compose.BackHandler {
            viewModel.exitSelectionMode()
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // 选择模式的顶部栏
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出选择")
                        }
                    },
                    actions = {
                        // 全选/取消全选
                        val allCount = transactionGroups.sumOf { it.transactions.size }
                        if (selectedIds.size < allCount) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "全选")
                            }
                        } else {
                            IconButton(onClick = { viewModel.deselectAll() }) {
                                Icon(Icons.Default.Deselect, contentDescription = "取消全选")
                            }
                        }
                        // 删除按钮
                        IconButton(
                            onClick = { viewModel.showBatchDeleteConfirm() },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除选中",
                                tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                // 正常模式的顶部栏
                TopAppBar(
                    title = { Text("日常记账") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // 批量选择按钮
                        IconButton(onClick = { viewModel.enterSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "批量选择"
                            )
                        }
                        // 导入账单按钮
                        IconButton(onClick = onNavigateToImport) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = "导入账单"
                            )
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (viewMode == "LIST") Icons.Filled.CalendarMonth else Icons.Filled.List,
                                contentDescription = if (viewMode == "LIST") "日历视图" else "列表视图"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "记一笔")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 统计卡片
            StatsCards(
                todayStats = todayStats,
                monthStats = monthStats
            )

            when (uiState) {
                is TransactionUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TransactionUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as TransactionUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is TransactionUiState.Success -> {
                    if (viewMode == "CALENDAR") {
                        // 日历视图
                        CalendarView(
                            viewModel = viewModel,
                            transactionGroups = transactionGroups,
                            onShowEditDialog = { viewModel.showEditDialog(it) },
                            onShowDeleteConfirm = { viewModel.showDeleteConfirm(it) }
                        )
                    } else {
                        // 列表视图
                        if (transactionGroups.isEmpty()) {
                            EmptyState()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                transactionGroups.forEach { group ->
                                    item(key = "header_${group.date}") {
                                        DayHeader(group = group)
                                    }

                                    items(
                                        items = group.transactions,
                                        key = { it.transaction.id }
                                    ) { transaction ->
                                        TransactionItem(
                                            transaction = transaction,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedIds.contains(transaction.transaction.id),
                                            onClick = {
                                                if (isSelectionMode) {
                                                    viewModel.toggleSelection(transaction.transaction.id)
                                                } else {
                                                    viewModel.showEditDialog(transaction.transaction.id)
                                                }
                                            },
                                            onDelete = { viewModel.showDeleteConfirm(transaction.transaction.id) },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    viewModel.enterSelectionMode()
                                                    viewModel.toggleSelection(transaction.transaction.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AddEditTransactionDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() },
            onNavigateToCategoryManagement = {
                viewModel.hideEditDialog()
                onNavigateToCategoryManagement()
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？此操作不可恢复。") },
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

    // 批量删除确认对话框
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideBatchDeleteConfirm() },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("确认批量删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 条记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBatchDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideBatchDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StatsCards(
    todayStats: DailyStats,
    monthStats: PeriodStats
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 今日支出
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "今日支出",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥${numberFormat.format(todayStats.totalExpense)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
        }

        // 本月支出
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "本月支出",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1565C0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥${numberFormat.format(monthStats.totalExpense)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}

@Composable
private fun DayHeader(group: DailyTransactionGroup) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = group.dayOfWeek,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            if (group.totalIncome > 0) {
                Text(
                    text = "+${numberFormat.format(group.totalIncome)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (group.totalExpense > 0) {
                Text(
                    text = "-${numberFormat.format(group.totalExpense)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: DailyTransactionWithCategory,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val isExpense = transaction.transaction.type == TransactionType.EXPENSE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择模式下显示复选框
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        transaction.category?.let { parseColor(it.color) }
                            ?: MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpense) Icons.Filled.ShoppingCart else Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category?.name ?: if (isExpense) "支出" else "收入",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (transaction.transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 金额和时间
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}¥${numberFormat.format(transaction.transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
                if (transaction.transaction.time.isNotBlank()) {
                    Text(
                        text = transaction.transaction.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 非选择模式下显示删除按钮
            if (!isSelectionMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无记账记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮开始记账",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}

/**
 * 日历视图
 */
@Composable
private fun CalendarView(
    viewModel: DailyTransactionViewModel,
    transactionGroups: List<DailyTransactionGroup>,
    onShowEditDialog: (Long) -> Unit,
    onShowDeleteConfirm: (Long) -> Unit
) {
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val calendarData by viewModel.calendarData.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 月份导航
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "上个月")
            }

            Text(
                text = viewModel.formatYearMonth(currentYearMonth),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "下个月")
            }
        }

        // 星期标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日历网格
        val calendarDays = remember(currentYearMonth) {
            generateCalendarDays(currentYearMonth)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(calendarDays, key = { it.epochDay }) { day ->
                CalendarDayCell(
                    day = day,
                    isSelected = day.epochDay == selectedDate,
                    expense = calendarData[day.epochDay] ?: 0.0,
                    onClick = {
                        if (day.isCurrentMonth) {
                            viewModel.selectDate(day.epochDay)
                        }
                    }
                )
            }
        }

        // 选中日期的交易列表
        val selectedDayTransactions = remember(selectedDate, transactionGroups) {
            transactionGroups.find { it.date == selectedDate }?.transactions ?: emptyList()
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "选中日期的记录",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        if (selectedDayTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当日无记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = selectedDayTransactions,
                    key = { it.transaction.id }
                ) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onShowEditDialog(transaction.transaction.id) },
                        onDelete = { onShowDeleteConfirm(transaction.transaction.id) }
                    )
                }
            }
        }
    }
}

/**
 * 日历单元格
 */
@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    expense: Double,
    onClick: () -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay().toInt() }
    val isToday = day.epochDay == today
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(enabled = day.isCurrentMonth, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (expense > 0 && day.isCurrentMonth) {
                Text(
                    text = "¥${if (expense >= 1000) "${(expense / 1000).toInt()}k" else expense.toInt().toString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else -> Color(0xFFF44336)
                    },
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 日历日期数据类
 */
data class CalendarDay(
    val epochDay: Int,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

/**
 * 生成日历天数
 */
private fun generateCalendarDays(yearMonth: Int): List<CalendarDay> {
    val year = yearMonth / 100
    val month = yearMonth % 100
    val ym = YearMonth.of(year, month)
    val firstDayOfMonth = ym.atDay(1)
    val lastDayOfMonth = ym.atEndOfMonth()

    val days = mutableListOf<CalendarDay>()

    // 上个月的天数填充
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday
    if (firstDayOfWeek > 0) {
        val prevMonth = ym.minusMonths(1)
        val prevMonthLastDay = prevMonth.atEndOfMonth()
        for (i in (firstDayOfWeek - 1) downTo 0) {
            val date = prevMonthLastDay.minusDays(i.toLong())
            days.add(
                CalendarDay(
                    epochDay = date.toEpochDay().toInt(),
                    dayOfMonth = date.dayOfMonth,
                    isCurrentMonth = false
                )
            )
        }
    }

    // 当月天数
    for (day in 1..lastDayOfMonth.dayOfMonth) {
        val date = ym.atDay(day)
        days.add(
            CalendarDay(
                epochDay = date.toEpochDay().toInt(),
                dayOfMonth = day,
                isCurrentMonth = true
            )
        )
    }

    // 下个月的天数填充（填满6行）
    val remainingDays = 42 - days.size
    val nextMonth = ym.plusMonths(1)
    for (day in 1..remainingDays) {
        val date = nextMonth.atDay(day)
        days.add(
            CalendarDay(
                epochDay = date.toEpochDay().toInt(),
                dayOfMonth = day,
                isCurrentMonth = false
            )
        )
    }

    return days
}
