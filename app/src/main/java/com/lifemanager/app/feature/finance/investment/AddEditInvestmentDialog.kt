package com.lifemanager.app.feature.finance.investment

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.theme.*

/**
 * 添加/编辑定投记录对话框
 *
 * 支持设置预算金额、实际金额、选择定投类型和添加备注
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInvestmentDialog(
    viewModel: MonthlyInvestmentViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val investmentFields by viewModel.investmentFields.collectAsState()

    var budgetText by remember(editState.budgetAmount) {
        mutableStateOf(if (editState.budgetAmount > 0) editState.budgetAmount.toString() else "")
    }

    var actualText by remember(editState.actualAmount) {
        mutableStateOf(if (editState.actualAmount > 0) editState.actualAmount.toString() else "")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(Radius.lg),
            colors = CardDefaults.cardColors(containerColor = CleanColors.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部栏
                TopAppBar(
                    title = {
                        Text(
                            text = if (editState.isEditing) "编辑定投记录" else "添加定投记录",
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
                            enabled = !editState.isSaving
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
                                    color = CleanColors.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CleanColors.surface
                    )
                )

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
                            color = CleanColors.error.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(Spacing.md),
                                style = CleanTypography.caption,
                                color = CleanColors.error
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    // 预算金额输入
                    Text(
                        text = "预算金额",
                        style = CleanTypography.secondary,
                        color = CleanColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    PremiumTextField(
                        value = budgetText,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split(".")
                            val newValue = when {
                                parts.size <= 1 -> filtered
                                parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}"
                                else -> budgetText
                            }
                            budgetText = newValue
                            newValue.toDoubleOrNull()?.let { viewModel.updateEditBudgetAmount(it) }
                                ?: if (newValue.isEmpty()) viewModel.updateEditBudgetAmount(0.0) else Unit
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "请输入本月预算金额",
                        leadingIcon = { Text("¥ ", color = CleanColors.textTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 实际金额输入
                    Text(
                        text = "实际金额",
                        style = CleanTypography.secondary,
                        color = CleanColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    PremiumTextField(
                        value = actualText,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split(".")
                            val newValue = when {
                                parts.size <= 1 -> filtered
                                parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}"
                                else -> actualText
                            }
                            actualText = newValue
                            newValue.toDoubleOrNull()?.let { viewModel.updateEditActualAmount(it) }
                                ?: if (newValue.isEmpty()) viewModel.updateEditActualAmount(0.0) else Unit
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "请输入实际投入金额",
                        leadingIcon = { Text("¥ ", color = CleanColors.textTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 类别选择
                    Text(
                        text = "定投类型",
                        style = CleanTypography.secondary,
                        color = CleanColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    if (investmentFields.isEmpty()) {
                        Text(
                            text = "暂无可用的定投类型",
                            style = CleanTypography.body,
                            color = CleanColors.textTertiary
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(investmentFields, key = { it.id }) { field ->
                                InvestmentFieldChip(
                                    field = field,
                                    selected = editState.fieldId == field.id,
                                    onClick = { viewModel.updateEditField(field.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 备注输入
                    Text(
                        text = "备注",
                        style = CleanTypography.secondary,
                        color = CleanColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    PremiumTextField(
                        value = editState.note,
                        onValueChange = { viewModel.updateEditNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = "添加备注（可选）",
                        maxLines = 2,
                        minLines = 2
                    )
                }
            }
        }
    }
}

/**
 * 定投类型选择项
 */
@Composable
private fun InvestmentFieldChip(
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
                if (selected) fieldColor.copy(alpha = 0.15f)
                else CleanColors.surfaceVariant
            )
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) fieldColor else fieldColor.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
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
