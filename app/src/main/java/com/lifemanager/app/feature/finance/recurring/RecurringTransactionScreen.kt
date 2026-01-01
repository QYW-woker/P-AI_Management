package com.lifemanager.app.feature.finance.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.RecurringFrequency
import com.lifemanager.app.core.database.entity.RecurringTransactionEntity
import com.lifemanager.app.ui.component.PremiumTextField
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/**
 * 周期记账界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurringTransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.recurringTransactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val deletingTransaction by viewModel.deletingTransaction.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("周期记账") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "添加周期记账")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无周期记账",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "添加周期记账，自动记录固定收支",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加周期记账")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 启用的周期记账
                val enabledTransactions = transactions.filter { it.isEnabled }
                if (enabledTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "进行中",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(enabledTransactions) { transaction ->
                        RecurringTransactionCard(
                            transaction = transaction,
                            viewModel = viewModel,
                            onEdit = { viewModel.showEditDialog(transaction) },
                            onToggleEnabled = { viewModel.toggleEnabled(transaction) },
                            onExecuteNow = { viewModel.executeNow(transaction) },
                            onDelete = { viewModel.showDeleteConfirmDialog(transaction) }
                        )
                    }
                }

                // 禁用的周期记账
                val disabledTransactions = transactions.filter { !it.isEnabled }
                if (disabledTransactions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已暂停",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(disabledTransactions) { transaction ->
                        RecurringTransactionCard(
                            transaction = transaction,
                            viewModel = viewModel,
                            onEdit = { viewModel.showEditDialog(transaction) },
                            onToggleEnabled = { viewModel.toggleEnabled(transaction) },
                            onExecuteNow = { viewModel.executeNow(transaction) },
                            onDelete = { viewModel.showDeleteConfirmDialog(transaction) },
                            isDisabled = true
                        )
                    }
                }
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        EditRecurringDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && deletingTransaction != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("删除周期记账") },
            text = {
                Text("确定要删除「${deletingTransaction!!.name}」吗？此操作不可恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteRecurring() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTransactionCard(
    transaction: RecurringTransactionEntity,
    viewModel: RecurringTransactionViewModel,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onExecuteNow: () -> Unit,
    onDelete: () -> Unit,
    isDisabled: Boolean = false
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    var showMenu by remember { mutableStateOf(false) }

    val isExpense = transaction.type == "EXPENSE"
    val typeColor = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 类型图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpense) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = transaction.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = viewModel.formatFrequencyDescription(transaction),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (isExpense) "-" else "+") + "¥${numberFormat.format(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            if (!isDisabled) {
                                DropdownMenuItem(
                                    text = { Text("立即执行") },
                                    onClick = {
                                        showMenu = false
                                        onExecuteNow()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (isDisabled) "启用" else "暂停") },
                                onClick = {
                                    showMenu = false
                                    onToggleEnabled()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isDisabled) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 分类和下次执行日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val categoryName = viewModel.getCategoryName(transaction.categoryId, transaction.type)
                if (categoryName.isNotEmpty()) {
                    val emoji = com.lifemanager.app.ui.component.CategoryIcons.getIcon(
                        name = categoryName,
                        moduleType = if (transaction.type == com.lifemanager.app.domain.model.TransactionType.EXPENSE) "EXPENSE" else "INCOME"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "下次: ${viewModel.formatDate(transaction.nextDueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 执行统计
            if (transaction.executedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "已执行 ${transaction.executedCount} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    transaction.lastExecutedDate?.let { lastDate ->
                        Text(
                            text = "  |  上次: ${viewModel.formatDate(lastDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRecurringDialog(
    viewModel: RecurringTransactionViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val editingTransaction by viewModel.editingTransaction.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val isEditing = editingTransaction != null

    val categories = if (editState.type == "INCOME") incomeCategories else expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "编辑周期记账" else "添加周期记账") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 错误提示
                editState.error?.let { error ->
                    item {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // 名称
                item {
                    PremiumTextField(
                        value = editState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = "名称",
                        placeholder = "例如：房租、工资",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 类型选择
                item {
                    Text(
                        text = "类型",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EXPENSE" to "支出", "INCOME" to "收入").forEach { (type, label) ->
                            val isSelected = editState.type == type
                            if (isSelected) {
                                Button(
                                    onClick = { viewModel.updateType(type) },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(label)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.updateType(type) },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }

                // 金额
                item {
                    PremiumTextField(
                        value = editState.amount,
                        onValueChange = { viewModel.updateAmount(it.filter { c -> c.isDigit() || c == '.' }) },
                        label = "金额",
                        leadingIcon = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 分类选择
                if (categories.isNotEmpty()) {
                    item {
                        Text(
                            text = "分类",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                val isSelected = editState.categoryId == category.id
                                val emoji = com.lifemanager.app.ui.component.CategoryIcons.getIcon(
                                    name = category.name,
                                    iconName = category.iconName,
                                    moduleType = category.moduleType
                                )
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateCategory(category.id) },
                                    label = { Text("$emoji ${category.name}") }
                                )
                            }
                        }
                    }
                }

                // 周期类型
                item {
                    Text(
                        text = "重复周期",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            RecurringFrequency.DAILY to "每天",
                            RecurringFrequency.WEEKLY to "每周",
                            RecurringFrequency.MONTHLY to "每月",
                            RecurringFrequency.YEARLY to "每年"
                        ).forEach { (frequency, label) ->
                            val isSelected = editState.frequency == frequency
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateFrequency(frequency) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // 周几（仅WEEKLY显示）
                if (editState.frequency == RecurringFrequency.WEEKLY) {
                    item {
                        Text(
                            text = "每周几",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6, "日" to 7).forEach { (label, day) ->
                                val isSelected = editState.dayOfWeek == day
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateDayOfWeek(day) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                // 每月几号（仅MONTHLY显示）
                if (editState.frequency == RecurringFrequency.MONTHLY) {
                    item {
                        PremiumTextField(
                            value = (editState.dayOfMonth ?: 1).toString(),
                            onValueChange = {
                                val day = it.filter { c -> c.isDigit() }.toIntOrNull()?.coerceIn(1, 31)
                                viewModel.updateDayOfMonth(day)
                            },
                            label = "每月几号",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 自动执行开关
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "自动记账",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "到期自动创建交易记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editState.autoExecute,
                            onCheckedChange = { viewModel.updateAutoExecute(it) }
                        )
                    }
                }

                // 备注
                item {
                    PremiumTextField(
                        value = editState.note,
                        onValueChange = { viewModel.updateNote(it) },
                        label = "备注（可选）",
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveRecurring() },
                enabled = !editState.isSaving
            ) {
                if (editState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
