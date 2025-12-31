package com.lifemanager.app.feature.savings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifemanager.app.domain.model.savingsColors
import com.lifemanager.app.domain.model.strategyOptions
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.component.PremiumDialog
import com.lifemanager.app.ui.component.PremiumConfirmButton
import com.lifemanager.app.ui.component.PremiumDismissButton
import com.lifemanager.app.ui.theme.AppColors

/**
 * 添加/编辑存钱计划对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlanDialog(
    viewModel: SavingsPlanViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.planEditState.collectAsState()

    var amountText by remember(editState.targetAmount) {
        mutableStateOf(if (editState.targetAmount > 0) editState.targetAmount.toInt().toString() else "")
    }

    // 动画效果
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialogScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .scale(scale)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = AppColors.Primary.copy(alpha = 0.2f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f),
                            AppColors.Primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(if (editState.isEditing) "编辑计划" else "创建计划")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.savePlan() },
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

                    // 计划名称
                    Text(
                        text = "计划名称",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.name,
                        onValueChange = { viewModel.updatePlanName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "如：旅游基金、应急储蓄",
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 目标金额
                    Text(
                        text = "目标金额",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = amountText,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() }
                            amountText = filtered
                            filtered.toDoubleOrNull()?.let {
                                viewModel.updatePlanTargetAmount(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "请输入目标金额",
                        label = "¥",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 描述
                    Text(
                        text = "描述（可选）",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.description,
                        onValueChange = { viewModel.updatePlanDescription(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "添加描述",
                        maxLines = 2,
                        minLines = 2,
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 颜色选择
                    Text(
                        text = "颜色",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savingsColors) { (color, _) ->
                            ColorChip(
                                color = color,
                                selected = editState.color == color,
                                onClick = { viewModel.updatePlanColor(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 存钱策略
                    Text(
                        text = "存钱策略",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        strategyOptions.take(4).forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updatePlanStrategy(value) }
                                    .background(
                                        if (editState.strategy == value)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = editState.strategy == value,
                                    onClick = { viewModel.updatePlanStrategy(value) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ColorChip(
    color: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipColor = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(chipColor)
            .clickable(onClick = onClick),
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
}

/**
 * 存款对话框
 */
@Composable
fun DepositDialog(
    viewModel: SavingsPlanViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.recordEditState.collectAsState()

    var amountText by remember { mutableStateOf("") }

    PremiumDialog(
        onDismissRequest = onDismiss,
        icon = "💰",
        iconBackgroundColor = AppColors.Primary.copy(alpha = 0.1f),
        title = "存款",
        confirmButton = {
            if (editState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                PremiumConfirmButton(
                    text = "确认",
                    onClick = { viewModel.confirmDeposit() }
                )
            }
        },
        dismissButton = {
            PremiumDismissButton(text = "取消", onClick = onDismiss)
        }
    ) {
        editState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        PremiumTextField(
            value = amountText,
            onValueChange = { value ->
                val filtered = value.filter { it.isDigit() || it == '.' }
                amountText = filtered
                filtered.toDoubleOrNull()?.let {
                    viewModel.updateDepositAmount(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = "存款金额 (¥)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        PremiumTextField(
            value = editState.note,
            onValueChange = { viewModel.updateDepositNote(it) },
            modifier = Modifier.fillMaxWidth(),
            label = "备注（可选）",
            singleLine = true
        )
    }
}
