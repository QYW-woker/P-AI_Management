package com.lifemanager.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 日期选择器对话框（日历样式）
 */
@Composable
fun DatePickerDialog(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    var currentMonth by remember { mutableStateOf(selectedDate?.let { YearMonth.from(it) } ?: YearMonth.now()) }
    var tempSelectedDate by remember { mutableStateOf(selectedDate ?: LocalDate.now()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 标题
                Text(
                    text = "选择日期",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 月份导航
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一月")
                    }

                    Text(
                        text = "${currentMonth.year}年${currentMonth.monthValue}月",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一月")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 星期标题
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 日历网格
                CalendarGrid(
                    yearMonth = currentMonth,
                    selectedDate = tempSelectedDate,
                    onDateClick = { date ->
                        val isValid = (minDate == null || !date.isBefore(minDate)) &&
                                (maxDate == null || !date.isAfter(maxDate))
                        if (isValid) {
                            tempSelectedDate = date
                        }
                    },
                    minDate = minDate,
                    maxDate = maxDate
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 快捷选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val today = LocalDate.now()
                    QuickDateButton(
                        text = "今天",
                        onClick = {
                            tempSelectedDate = today
                            currentMonth = YearMonth.from(today)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickDateButton(
                        text = "明天",
                        onClick = {
                            tempSelectedDate = today.plusDays(1)
                            currentMonth = YearMonth.from(tempSelectedDate)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickDateButton(
                        text = "下周",
                        onClick = {
                            tempSelectedDate = today.plusWeeks(1)
                            currentMonth = YearMonth.from(tempSelectedDate)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onDateSelected(tempSelectedDate)
                        onDismiss()
                    }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    minDate: LocalDate?,
    maxDate: LocalDate?
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday
    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()

    Column {
        var dayCounter = 1
        for (week in 0..5) {
            if (dayCounter > daysInMonth) break

            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0..6) {
                    if (week == 0 && dayOfWeek < firstDayOfWeek || dayCounter > daysInMonth) {
                        // 空白格子
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = yearMonth.atDay(dayCounter)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val isDisabled = (minDate != null && date.isBefore(minDate)) ||
                                (maxDate != null && date.isAfter(maxDate))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = !isDisabled) { onDateClick(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayCounter.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * 时间选择器对话框（滚轮样式）
 */
@Composable
fun TimePickerDialog(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(selectedTime?.hour ?: 9) }
    var selectedMinute by remember { mutableStateOf(selectedTime?.minute ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "选择时间",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 当前选择的时间显示
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 滚轮选择器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 小时滚轮
                    WheelPicker(
                        items = (0..23).toList(),
                        selectedIndex = selectedHour,
                        onSelectedChange = { selectedHour = it },
                        modifier = Modifier.weight(1f),
                        label = "时"
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // 分钟滚轮
                    WheelPicker(
                        items = (0..59).toList(),
                        selectedIndex = selectedMinute,
                        onSelectedChange = { selectedMinute = it },
                        modifier = Modifier.weight(1f),
                        label = "分"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 快捷选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickTimeButton("09:00", { selectedHour = 9; selectedMinute = 0 }, Modifier.weight(1f))
                    QuickTimeButton("12:00", { selectedHour = 12; selectedMinute = 0 }, Modifier.weight(1f))
                    QuickTimeButton("18:00", { selectedHour = 18; selectedMinute = 0 }, Modifier.weight(1f))
                    QuickTimeButton("21:00", { selectedHour = 21; selectedMinute = 0 }, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
                        onDismiss()
                    }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelPicker(
    items: List<Int>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedIndex - 2))

    // 当滚动停止时更新选中项
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex + 2
            if (centerIndex in items.indices && centerIndex != selectedIndex) {
                onSelectedChange(centerIndex)
            }
        }
    }

    // 当选中项变化时滚动到该位置
    LaunchedEffect(selectedIndex) {
        val targetIndex = maxOf(0, selectedIndex - 2)
        if (listState.firstVisibleItemIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
        ) {
            // 选中区域高亮
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 70.dp)
            ) {
                items(items.size) { index ->
                    val isSelected = index == selectedIndex
                    val distanceFromCenter = kotlin.math.abs(index - selectedIndex)
                    val alpha = when (distanceFromCenter) {
                        0 -> 1f
                        1 -> 0.6f
                        2 -> 0.3f
                        else -> 0.1f
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable { onSelectedChange(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", items[index]),
                            style = if (isSelected)
                                MaterialTheme.typography.headlineSmall
                            else
                                MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        )
                    }
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickTimeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * 日期显示按钮（点击打开日期选择器）
 */
@Composable
fun DatePickerButton(
    selectedDate: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "选择日期"
) {
    val today = LocalDate.now()
    val displayText = selectedDate?.let { date ->
        when {
            date == today -> "今天"
            date == today.plusDays(1) -> "明天"
            date == today.minusDays(1) -> "昨天"
            date.year == today.year -> "${date.monthValue}月${date.dayOfMonth}日"
            else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
        }
    } ?: placeholder

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(displayText)
    }
}

/**
 * 时间显示按钮（点击打开时间选择器）
 */
@Composable
fun TimePickerButton(
    selectedTime: LocalTime?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "选择时间"
) {
    val displayText = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: placeholder

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(displayText)
    }
}
