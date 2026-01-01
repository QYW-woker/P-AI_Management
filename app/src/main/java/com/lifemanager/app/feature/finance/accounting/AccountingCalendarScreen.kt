package com.lifemanager.app.feature.finance.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * 记账日历界面
 *
 * 展示按日期的收支数据，支持按月查看
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingCalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    viewModel: AccountingCalendarViewModel = hiltViewModel()
) {
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val calendarData by viewModel.calendarData.collectAsState()
    val selectedDateTransactions by viewModel.selectedDateTransactions.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toLong() * 24 * 60 * 60 * 1000
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记账日历") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 跳转到指定日期
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期")
                    }
                    // 回到今天
                    IconButton(onClick = { viewModel.goToToday() }) {
                        Icon(Icons.Default.Today, contentDescription = "今天")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "记一笔")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 月度汇总卡片
            MonthSummaryCard(
                yearMonth = currentYearMonth,
                monthStats = monthStats,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )

            // 星期标题
            WeekDayHeaders()

            // 日历网格
            CalendarGrid(
                yearMonth = currentYearMonth,
                selectedDate = selectedDate,
                calendarData = calendarData,
                onDateSelect = { viewModel.selectDate(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 选中日期的交易列表
            SelectedDateTransactions(
                selectedDate = selectedDate,
                transactions = selectedDateTransactions,
                onTransactionClick = { /* TODO: 编辑 */ }
            )
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            viewModel.goToDate(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * 月度汇总卡片
 */
@Composable
private fun MonthSummaryCard(
    yearMonth: Int,
    monthStats: PeriodStats,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val year = yearMonth / 100
    val month = yearMonth % 100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 月份导航
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                }

                Text(
                    text = "${year}年${month}月",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 收支统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "¥${numberFormat.format(monthStats.totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "¥${numberFormat.format(monthStats.totalExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "结余",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    val balance = monthStats.totalIncome - monthStats.totalExpense
                    Text(
                        text = "¥${numberFormat.format(balance)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (balance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 星期标题
 */
@Composable
private fun WeekDayHeaders() {
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
}

/**
 * 日历网格
 */
@Composable
private fun CalendarGrid(
    yearMonth: Int,
    selectedDate: Int,
    calendarData: Map<Int, DayData>,
    onDateSelect: (Int) -> Unit
) {
    val calendarDays = remember(yearMonth) {
        generateCalendarDays(yearMonth)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(calendarDays, key = { it.epochDay }) { day ->
            CalendarDayCell(
                day = day,
                isSelected = day.epochDay == selectedDate,
                dayData = calendarData[day.epochDay],
                onClick = {
                    if (day.isCurrentMonth) {
                        onDateSelect(day.epochDay)
                    }
                }
            )
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
    dayData: DayData?,
    onClick: () -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay().toInt() }
    val isToday = day.epochDay == today
    val hasData = dayData != null && (dayData.income > 0 || dayData.expense > 0)

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
            .padding(2.dp),
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

            if (hasData && day.isCurrentMonth) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if ((dayData?.income ?: 0.0) > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    else Color(0xFF4CAF50)
                                )
                        )
                    }
                    if ((dayData?.expense ?: 0.0) > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    else Color(0xFFF44336)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 选中日期的交易列表
 */
@Composable
private fun SelectedDateTransactions(
    selectedDate: Int,
    transactions: List<DailyTransactionWithCategory>,
    onTransactionClick: (Long) -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val dateText = remember(selectedDate) {
        val date = LocalDate.ofEpochDay(selectedDate.toLong())
        "${date.monthValue}月${date.dayOfMonth}日"
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${dateText}的记录",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            if (transactions.isNotEmpty()) {
                val dayIncome = transactions.filter { it.transaction.type == TransactionType.INCOME }
                    .sumOf { it.transaction.amount }
                val dayExpense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }
                    .sumOf { it.transaction.amount }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dayIncome > 0) {
                        Text(
                            text = "+¥${numberFormat.format(dayIncome)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    if (dayExpense > 0) {
                        Text(
                            text = "-¥${numberFormat.format(dayExpense)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当日无记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = transactions,
                    key = { it.transaction.id }
                ) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        numberFormat = numberFormat,
                        onClick = { onTransactionClick(transaction.transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionListItem(
    transaction: DailyTransactionWithCategory,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    val isExpense = transaction.transaction.type == TransactionType.EXPENSE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        }
    }
}

/**
 * 日历数据类
 */
data class CalendarDay(
    val epochDay: Int,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

/**
 * 日期数据
 */
data class DayData(
    val income: Double = 0.0,
    val expense: Double = 0.0
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

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
