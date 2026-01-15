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
import androidx.compose.material.icons.outlined.Category
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
import com.lifemanager.app.ui.theme.*

/**
 * 添加/编辑收支记录对话框 - CleanColors设计版
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
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(Radius.xl),
            color = CleanColors.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部标题栏
                TopAppBar(
                    title = {
                        Text(
                            text = if (editState.isEditing) "编辑记录" else "添加记录",
                            style = CleanTypography.title,
                            color = CleanColors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = CleanColors.textSecondary
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveRecord() },
                            enabled = !editState.isSaving && amountText.isNotBlank() && editState.fieldId > 0
                        ) {
                            if (editState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = CleanColors.primary
                                )
                            } else {
                                Text(
                                    "保存",
                                    style = CleanTypography.button,
                                    color = if (amountText.isNotBlank() && editState.fieldId > 0)
                                        CleanColors.primary
                                    else
                                        CleanColors.textDisabled
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CleanColors.surface
                    )
                )

                Divider(color = CleanColors.divider)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.pageHorizontal)
                ) {
                    // 错误提示
                    editState.error?.let { error ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.sm),
                            color = CleanColors.errorLight
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(Spacing.md),
                                style = CleanTypography.secondary,
                                color = CleanColors.error
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.lg))
                    }

                    // 类型选择
                    Text(
                        text = "类型",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        CleanTypeChip(
                            text = "收入",
                            selected = editState.type == IncomeExpenseType.INCOME,
                            color = CleanColors.success,
                            onClick = { viewModel.updateEditType(IncomeExpenseType.INCOME) },
                            modifier = Modifier.weight(1f)
                        )
                        CleanTypeChip(
                            text = "支出",
                            selected = editState.type == IncomeExpenseType.EXPENSE,
                            color = CleanColors.error,
                            onClick = { viewModel.updateEditType(IncomeExpenseType.EXPENSE) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xl))

                    // 金额输入
                    Text(
                        text = "金额",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    OutlinedTextField(
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
                        placeholder = {
                            Text(
                                "请输入金额",
                                style = CleanTypography.body,
                                color = CleanColors.textPlaceholder
                            )
                        },
                        prefix = {
                            Text(
                                "¥ ",
                                style = CleanTypography.body,
                                color = CleanColors.textSecondary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(Radius.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanColors.primary,
                            unfocusedBorderColor = CleanColors.border,
                            cursorColor = CleanColors.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(Spacing.xl))

                    // 类别选择
                    Text(
                        text = "类别",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    if (currentFields.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Category,
                                    contentDescription = null,
                                    tint = CleanColors.textPlaceholder,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = "暂无可用类别",
                                    style = CleanTypography.secondary,
                                    color = CleanColors.textTertiary
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(currentFields, key = { it.id }) { field ->
                                CleanFieldChip(
                                    field = field,
                                    selected = editState.fieldId == field.id,
                                    onClick = { viewModel.updateEditField(field.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xl))

                    // 备注输入
                    Text(
                        text = "备注",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = editState.note,
                        onValueChange = { viewModel.updateEditNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "添加备注（可选）",
                                style = CleanTypography.body,
                                color = CleanColors.textPlaceholder
                            )
                        },
                        maxLines = 3,
                        minLines = 2,
                        shape = RoundedCornerShape(Radius.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanColors.primary,
                            unfocusedBorderColor = CleanColors.border,
                            cursorColor = CleanColors.primary
                        )
                    )
                }
            }
        }
    }
}

/**
 * 简洁类型选择按钮
 */
@Composable
private fun CleanTypeChip(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick),
        color = if (selected) color else CleanColors.surfaceVariant,
        shape = RoundedCornerShape(Radius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(IconSize.xs)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = text,
                style = CleanTypography.button,
                color = if (selected) Color.White else CleanColors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * 简洁类别选择按钮
 */
@Composable
private fun CleanFieldChip(
    field: CustomFieldEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fieldColor = try {
        Color(android.graphics.Color.parseColor(field.color))
    } catch (e: Exception) {
        CleanColors.primary
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .background(
                if (selected) fieldColor.copy(alpha = 0.15f) else CleanColors.surfaceVariant
            )
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.sm))
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
                    modifier = Modifier.size(IconSize.sm)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = field.name,
            style = CleanTypography.caption,
            maxLines = 1,
            color = if (selected) fieldColor else CleanColors.textSecondary
        )
    }
}
