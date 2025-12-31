package com.lifemanager.app.feature.finance.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.AccountType
import com.lifemanager.app.core.database.entity.FundAccountEntity
import java.text.NumberFormat
import java.util.Locale

/**
 * 资金账户管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: FundAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val groupedAccounts by viewModel.groupedAccounts.collectAsState()
    val assetSummary by viewModel.assetSummary.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val editState by viewModel.editState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资金账户") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "添加账户")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is FundAccountUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FundAccountUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as FundAccountUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("重试")
                        }
                    }
                }
            }
            is FundAccountUiState.Success -> {
                if (accounts.isEmpty()) {
                    EmptyAccountsView(onAddAccount = { viewModel.showAddDialog() })
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 资产概览卡片
                        item {
                            AssetSummaryCard(assetSummary = assetSummary)
                        }

                        // 按类型分组的账户列表
                        val accountTypeOrder = listOf(
                            AccountType.CASH,
                            AccountType.BANK_CARD,
                            AccountType.ALIPAY,
                            AccountType.WECHAT,
                            AccountType.CREDIT_CARD,
                            AccountType.CREDIT_LOAN,
                            AccountType.INVESTMENT,
                            AccountType.OTHER
                        )

                        accountTypeOrder.forEach { type ->
                            val accountsOfType = groupedAccounts[type] ?: emptyList()
                            if (accountsOfType.isNotEmpty()) {
                                item {
                                    AccountTypeSection(
                                        type = type,
                                        accounts = accountsOfType,
                                        viewModel = viewModel,
                                        onEdit = { viewModel.showEditDialog(it.id) },
                                        onDelete = { viewModel.showDeleteConfirm(it.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        EditAccountDialog(
            editState = editState,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { viewModel.saveAccount() },
            onNameChange = { viewModel.updateEditName(it) },
            onTypeChange = { viewModel.updateEditAccountType(it) },
            onBalanceChange = { viewModel.updateEditBalance(it) },
            onCreditLimitChange = { viewModel.updateEditCreditLimit(it) },
            onBillDayChange = { viewModel.updateEditBillDay(it) },
            onRepaymentDayChange = { viewModel.updateEditRepaymentDay(it) },
            onNoteChange = { viewModel.updateEditNote(it) },
            onIncludeInTotalChange = { viewModel.updateEditIncludeInTotal(it) }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除此账户吗？删除后可以在设置中恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 空账户视图
 */
@Composable
private fun EmptyAccountsView(onAddAccount: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无资金账户",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "添加账户来追踪您的资金流动",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加账户")
            }
        }
    }
}

/**
 * 资产概览卡片
 */
@Composable
private fun AssetSummaryCard(assetSummary: AssetSummary) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "资产概览",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 总资产
                Column {
                    Text(
                        text = "总资产",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "¥${numberFormat.format(assetSummary.totalAssets)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // 总负债
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总负债",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "¥${numberFormat.format(assetSummary.totalLiabilities)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 净资产
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "净资产",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "¥${numberFormat.format(assetSummary.netWorth)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (assetSummary.netWorth >= 0)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 账户类型分组
 */
@Composable
private fun AccountTypeSection(
    type: String,
    accounts: List<FundAccountEntity>,
    viewModel: FundAccountViewModel,
    onEdit: (FundAccountEntity) -> Unit,
    onDelete: (FundAccountEntity) -> Unit
) {
    Column {
        // 分组标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = viewModel.getAccountTypeIcon(type),
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = viewModel.getAccountTypeName(type),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            // 该类型总额
            val total = accounts.sumOf { it.balance }
            Text(
                text = "¥${String.format("%.2f", total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (viewModel.isDebtAccount(type))
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 账户列表
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                accounts.forEachIndexed { index, account ->
                    AccountItem(
                        account = account,
                        isDebt = viewModel.isDebtAccount(type),
                        onEdit = { onEdit(account) },
                        onDelete = { onDelete(account) }
                    )
                    if (index < accounts.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 账户项
 */
@Composable
private fun AccountItem(
    account: FundAccountEntity,
    isDebt: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (account.note.isNotBlank()) {
                Text(
                    text = account.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 信用卡额外信息
            if (isDebt && account.creditLimit != null) {
                Text(
                    text = "额度: ¥${numberFormat.format(account.creditLimit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 余额
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isDebt) "-" else ""}¥${numberFormat.format(kotlin.math.abs(account.balance))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            if (!account.includeInTotal) {
                Text(
                    text = "不计入统计",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 更多操作
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
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

/**
 * 编辑账户对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountDialog(
    editState: FundAccountEditState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onCreditLimitChange: (String) -> Unit,
    onBillDayChange: (String) -> Unit,
    onRepaymentDayChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onIncludeInTotalChange: (Boolean) -> Unit
) {
    val isDebt = AccountType.isDebtAccount(editState.accountType)
    val accountTypes = listOf(
        AccountType.CASH to "现金",
        AccountType.BANK_CARD to "银行卡",
        AccountType.ALIPAY to "支付宝",
        AccountType.WECHAT to "微信支付",
        AccountType.CREDIT_CARD to "信用卡",
        AccountType.CREDIT_LOAN to "信贷账户",
        AccountType.INVESTMENT to "投资账户",
        AccountType.OTHER to "其他"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editState.isEditing) "编辑账户" else "添加账户") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 账户名称
                OutlinedTextField(
                    value = editState.name,
                    onValueChange = onNameChange,
                    label = { Text("账户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = editState.error != null && editState.name.isBlank()
                )

                // 账户类型
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountTypes.find { it.first == editState.accountType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("账户类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        accountTypes.forEach { (type, name) ->
                            DropdownMenuItem(
                                text = { Text("${AccountType.getIcon(type)} $name") },
                                onClick = {
                                    onTypeChange(type)
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // 余额/欠款
                OutlinedTextField(
                    value = editState.balance,
                    onValueChange = onBalanceChange,
                    label = { Text(if (isDebt) "当前欠款" else "当前余额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("¥") }
                )

                // 信用额度（仅信贷账户）
                if (isDebt) {
                    OutlinedTextField(
                        value = editState.creditLimit,
                        onValueChange = onCreditLimitChange,
                        label = { Text("信用额度") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("¥") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editState.billDay,
                            onValueChange = onBillDayChange,
                            label = { Text("账单日") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("日") }
                        )
                        OutlinedTextField(
                            value = editState.repaymentDay,
                            onValueChange = onRepaymentDayChange,
                            label = { Text("还款日") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("日") }
                        )
                    }
                }

                // 备注
                OutlinedTextField(
                    value = editState.note,
                    onValueChange = onNoteChange,
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 计入统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "计入资产统计",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = editState.includeInTotal,
                        onCheckedChange = onIncludeInTotalChange
                    )
                }

                // 错误提示
                if (editState.error != null) {
                    Text(
                        text = editState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
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
