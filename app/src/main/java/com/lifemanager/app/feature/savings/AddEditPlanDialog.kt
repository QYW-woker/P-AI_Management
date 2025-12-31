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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
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
import com.lifemanager.app.domain.model.savingsPlanTemplates
import com.lifemanager.app.domain.model.SavingsPlanTemplate
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.component.PremiumDialog
import com.lifemanager.app.ui.component.PremiumConfirmButton
import com.lifemanager.app.ui.component.PremiumDismissButton
import com.lifemanager.app.ui.theme.AppColors
import com.lifemanager.app.core.database.entity.RecordType
import com.lifemanager.app.domain.model.SavingsPlanWithDetails
import com.lifemanager.app.domain.model.quickDepositAmounts
import java.time.LocalDate

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

                    // 模板选择（仅新建时显示）
                    if (!editState.isEditing) {
                        Text(
                            text = "快速创建",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savingsPlanTemplates.take(6)) { template ->
                                TemplateCard(
                                    template = template,
                                    onClick = {
                                        viewModel.applyTemplate(template)
                                        amountText = template.suggestedAmount.toInt().toString()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "自定义设置",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
 * 模板卡片
 */
@Composable
private fun TemplateCard(
    template: SavingsPlanTemplate,
    onClick: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(template.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = cardColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.icon,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column {
                Text(
                    text = "¥${String.format("%,.0f", template.suggestedAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = cardColor
                )
                Text(
                    text = "${template.suggestedMonths}个月",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

        Spacer(modifier = Modifier.height(16.dp))

        // 快速存款金额
        Text(
            text = "快速选择",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickDepositAmounts.take(3).forEach { (amount, label) ->
                OutlinedButton(
                    onClick = {
                        amountText = amount.toInt().toString()
                        viewModel.updateDepositAmount(amount)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickDepositAmounts.drop(3).forEach { (amount, label) ->
                OutlinedButton(
                    onClick = {
                        amountText = amount.toInt().toString()
                        viewModel.updateDepositAmount(amount)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * 取款对话框
 */
@Composable
fun WithdrawDialog(
    viewModel: SavingsPlanViewModel,
    maxAmount: Double,
    onDismiss: () -> Unit
) {
    val editState by viewModel.recordEditState.collectAsState()

    var amountText by remember { mutableStateOf("") }

    PremiumDialog(
        onDismissRequest = onDismiss,
        icon = "💸",
        iconBackgroundColor = Color(0xFFF44336).copy(alpha = 0.1f),
        title = "取款",
        confirmButton = {
            if (editState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                PremiumConfirmButton(
                    text = "确认取款",
                    onClick = { viewModel.confirmWithdraw() }
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

        // 可取余额提示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "可取余额：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "¥${String.format("%,.2f", maxAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            label = "取款金额 (¥)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        PremiumTextField(
            value = editState.note,
            onValueChange = { viewModel.updateDepositNote(it) },
            modifier = Modifier.fillMaxWidth(),
            label = "取款原因（可选）",
            placeholder = "如：紧急用钱、购物等",
            singleLine = true
        )
    }
}

/**
 * 存取记录历史对话框
 */
@Composable
fun RecordHistoryDialog(
    planDetails: SavingsPlanWithDetails,
    onDismiss: () -> Unit,
    formatDate: (Int) -> String
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
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
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${planDetails.plan.name} - 记录",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "共 ${planDetails.records.size} 条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                            contentDescription = "关闭"
                        )
                    }
                }

                Divider()

                // 统计信息
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "存款",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${String.format("%,.0f", planDetails.totalDeposits)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "${planDetails.depositCount}次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "取款",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${String.format("%,.0f", planDetails.totalWithdrawals)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            text = "${planDetails.withdrawalCount}次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "净存",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${String.format("%,.0f", planDetails.plan.currentAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
                        )
                    }
                }

                Divider()

                // 记录列表
                if (planDetails.records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", style = MaterialTheme.typography.displaySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = planDetails.records,
                            key = { it.id }
                        ) { record ->
                            RecordItem(
                                record = record,
                                formatDate = formatDate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordItem(
    record: com.lifemanager.app.core.database.entity.SavingsRecordEntity,
    formatDate: (Int) -> String
) {
    val isDeposit = record.type == RecordType.DEPOSIT

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDeposit)
                Color(0xFF4CAF50).copy(alpha = 0.08f)
            else
                Color(0xFFF44336).copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDeposit) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else Color(0xFFF44336).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDeposit) "💰" else "💸",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDeposit) "存款" else "取款",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(record.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.note.isNotBlank()) {
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            // 金额
            Text(
                text = "${if (isDeposit) "+" else "-"}¥${String.format("%,.2f", record.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDeposit) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
