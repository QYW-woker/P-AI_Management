package com.lifemanager.app.feature.finance.income

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.IncomeExpenseType

/**
 * 添加/编辑收支记录对话框
 *
 * 支持选择类型、类别、输入金额和备注
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecordDialog(
    viewModel: MonthlyIncomeExpenseViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val incomeFields by viewModel.incomeFields.collectAsState()
    val expenseFields by viewModel.expenseFields.collectAsState()

    // 当前显示的字段列表
    val currentFields = if (editState.type == IncomeExpenseType.INCOME) {
        incomeFields
    } else {
        expenseFields
    }

    // 金额输入文本
    var amountText by remember(editState.amount) {
        mutableStateOf(if (editState.amount > 0) editState.amount.toString() else "")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                TopAppBar(
                    title = {
                        Text(
                            text = if (editState.isEditing) "编辑记录" else "添加记录"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveRecord() },
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

                    // 类型选择
                    Text(
                        text = "类型",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TypeChip(
                            text = "收入",
                            selected = editState.type == IncomeExpenseType.INCOME,
                            color = Color(0xFF4CAF50),
                            onClick = { viewModel.updateEditType(IncomeExpenseType.INCOME) },
                            modifier = Modifier.weight(1f)
                        )
                        TypeChip(
                            text = "支出",
                            selected = editState.type == IncomeExpenseType.EXPENSE,
                            color = Color(0xFFF44336),
                            onClick = { viewModel.updateEditType(IncomeExpenseType.EXPENSE) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 金额输入
                    Text(
                        text = "金额",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { value ->
                            // 只允许输入数字和小数点
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            // 最多两位小数
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
                        placeholder = { Text("请输入金额") },
                        prefix = { Text("¥ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 类别选择
                    Text(
                        text = "类别",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentFields.isEmpty()) {
                        Text(
                            text = "暂无可用类别，请先在设置中添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentFields, key = { it.id }) { field ->
                                FieldChip(
                                    field = field,
                                    selected = editState.fieldId == field.id,
                                    onClick = { viewModel.updateEditField(field.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 备注输入
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editState.note,
                        onValueChange = { viewModel.updateEditNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("添加备注（可选）") },
                        maxLines = 3,
                        minLines = 2
                    )
                }
            }
        }
    }
}

/**
 * 类型选择芯片
 */
@Composable
private fun TypeChip(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * 类别选择芯片
 */
@Composable
private fun FieldChip(
    field: CustomFieldEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fieldColor = try {
        Color(android.graphics.Color.parseColor(field.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) {
                    fieldColor.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标圆圈
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (selected) fieldColor else fieldColor.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = field.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (selected) fieldColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
