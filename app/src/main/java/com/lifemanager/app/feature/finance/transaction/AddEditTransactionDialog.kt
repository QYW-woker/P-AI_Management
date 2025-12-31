package com.lifemanager.app.feature.finance.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifemanager.app.core.database.entity.AccountType
import com.lifemanager.app.core.database.entity.ChineseBank
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.FundAccountEntity
import com.lifemanager.app.domain.model.TransactionType
import com.lifemanager.app.ui.component.DatePickerButton
import com.lifemanager.app.ui.component.DatePickerDialog
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.component.TimePickerButton
import com.lifemanager.app.ui.component.TimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 添加/编辑交易对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    viewModel: DailyTransactionViewModel,
    onDismiss: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit = {}
) {
    val editState by viewModel.editState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var amountText by remember(editState.amount) {
        mutableStateOf(if (editState.amount > 0) editState.amount.toString() else "")
    }

    // 日期和时间选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 将epochDay转换为LocalDate
    val selectedDate = LocalDate.ofEpochDay(editState.date.toLong())
    // 将时间字符串转换为LocalTime
    val selectedTime = remember(editState.time) {
        try {
            val parts = editState.time.split(":")
            if (parts.size == 2) {
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } else {
                LocalTime.now()
            }
        } catch (e: Exception) {
            LocalTime.now()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(if (editState.isEditing) "编辑记录" else "记一笔")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveTransaction() },
                            enabled = !editState.isSaving
                        ) {
                            if (editState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("保存")
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 错误提示
                    editState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 收入/支出切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (editState.type == TransactionType.EXPENSE) {
                            Button(
                                onClick = { viewModel.updateEditType(TransactionType.EXPENSE) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFF44336)
                                )
                            ) {
                                Text("支出")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.updateEditType(TransactionType.EXPENSE) }
                            ) {
                                Text("支出")
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        if (editState.type == TransactionType.INCOME) {
                            Button(
                                onClick = { viewModel.updateEditType(TransactionType.INCOME) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    contentColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text("收入")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.updateEditType(TransactionType.INCOME) }
                            ) {
                                Text("收入")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 金额输入
                    Text(
                        text = "金额",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = amountText,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split(".")
                            val newValue = when {
                                parts.size <= 1 -> filtered
                                parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}"
                                else -> amountText
                            }
                            amountText = newValue
                            newValue.toDoubleOrNull()?.let { viewModel.updateEditAmount(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "0.00",
                        leadingIcon = { Text("¥ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 日期和时间选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 日期选择按钮
                        DatePickerButton(
                            selectedDate = selectedDate,
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                            placeholder = "选择日期"
                        )

                        // 时间选择按钮
                        TimePickerButton(
                            selectedTime = selectedTime,
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            placeholder = "选择时间"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 分类选择标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "分类",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = onNavigateToCategoryManagement) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("管理分类")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (categories.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "暂无分类",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onNavigateToCategoryManagement) {
                                    Text("点击添加分类")
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories, key = { it.id }) { category ->
                                CategoryChip(
                                    category = category,
                                    selected = editState.categoryId == category.id,
                                    onClick = { viewModel.updateEditCategory(category.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 账户选择
                    Text(
                        text = "账户",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (accounts.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "暂无账户（可选）",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // 账户选择下拉菜单
                        var accountExpanded by remember { mutableStateOf(false) }
                        val selectedAccount = accounts.find { it.id == editState.accountId }

                        ExposedDropdownMenuBox(
                            expanded = accountExpanded,
                            onExpandedChange = { accountExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedAccount?.let {
                                    "${AccountType.getIcon(it.accountType)} ${it.name}"
                                } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("选择账户（可选）") },
                                trailingIcon = {
                                    Row {
                                        if (selectedAccount != null) {
                                            IconButton(
                                                onClick = { viewModel.updateEditAccount(null) }
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "清除",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = accountExpanded,
                                onDismissRequest = { accountExpanded = false }
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(AccountType.getIcon(account.accountType))
                                                Column {
                                                    Text(account.name)
                                                    if (account.cardNumber != null) {
                                                        Text(
                                                            text = "尾号 ${account.cardNumber.takeLast(4)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.updateEditAccount(account.id)
                                            accountExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 备注输入
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.note,
                        onValueChange = { viewModel.updateEditNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "添加备注（可选）",
                        maxLines = 3,
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                viewModel.updateEditDate(date.toEpochDay().toInt())
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // 时间选择器对话框
    if (showTimePicker) {
        TimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                viewModel.updateEditTime(String.format("%02d:%02d", time.hour, time.minute))
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

/**
 * 格式化日期显示
 */
private fun formatDate(epochDay: Int): String {
    val date = LocalDate.ofEpochDay(epochDay.toLong())
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (date) {
        today -> "今天"
        yesterday -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("MM月dd日"))
    }
}

@Composable
private fun CategoryChip(
    category: CustomFieldEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // 获取卡通图标
    val emoji = com.lifemanager.app.ui.component.CategoryIcons.getIcon(
        name = category.name,
        iconName = category.iconName,
        moduleType = category.moduleType
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) categoryColor.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) categoryColor else categoryColor.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                // 选中时显示勾选图标
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // 未选中时显示emoji图标
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (selected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
