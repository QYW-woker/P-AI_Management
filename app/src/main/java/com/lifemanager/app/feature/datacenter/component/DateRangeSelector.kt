package com.lifemanager.app.feature.datacenter.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifemanager.app.feature.datacenter.model.DateRangeType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日期范围选择器组件
 *
 * 支持快速选择（本周/本月/本年/全部）和自定义日期范围
 *
 * @param selectedType 当前选中的日期范围类型
 * @param customStartDate 自定义开始日期
 * @param customEndDate 自定义结束日期
 * @param onTypeChange 日期类型变更回调
 * @param onCustomRangeChange 自定义日期范围变更回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    selectedType: DateRangeType,
    customStartDate: LocalDate?,
    customEndDate: LocalDate?,
    onTypeChange: (DateRangeType) -> Unit,
    onCustomRangeChange: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd") }

    // 快速选择选项（不包含自定义）
    val quickOptions = remember {
        listOf(
            DateRangeType.WEEK,
            DateRangeType.MONTH,
            DateRangeType.YEAR,
            DateRangeType.ALL
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // 快速选择按钮
        items(quickOptions) { type ->
            if (selectedType == type) {
                Button(
                    onClick = { onTypeChange(type) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(type.displayName)
                }
            } else {
                OutlinedButton(
                    onClick = { onTypeChange(type) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(type.displayName)
                }
            }
        }

        // 自定义日期按钮
        item {
            if (selectedType == DateRangeType.CUSTOM) {
                Button(
                    onClick = { showDateRangePicker = true },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    if (customStartDate != null && customEndDate != null) {
                        Text("${customStartDate.format(dateFormatter)} - ${customEndDate.format(dateFormatter)}")
                    } else {
                        Text("自定义")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showDateRangePicker = true },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("自定义")
                }
            }
        }
    }

    // 日期范围选择对话框
    if (showDateRangePicker) {
        DateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startMillis, endMillis ->
                val startDate = Instant.ofEpochMilli(startMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val endDate = Instant.ofEpochMilli(endMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                onCustomRangeChange(startDate, endDate)
                onTypeChange(DateRangeType.CUSTOM)
                showDateRangePicker = false
            }
        )
    }
}

/**
 * 日期范围选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
        initialSelectedEndDateMillis = System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        onConfirm(startMillis, endMillis)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                        dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "选择日期范围",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            headline = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val startDate = dateRangePickerState.selectedStartDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    val endDate = dateRangePickerState.selectedEndDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

                    Text(
                        text = startDate?.format(formatter) ?: "开始日期",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = " - ",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = endDate?.format(formatter) ?: "结束日期",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            showModeToggle = false,
            modifier = Modifier.heightIn(max = 500.dp)
        )
    }
}
