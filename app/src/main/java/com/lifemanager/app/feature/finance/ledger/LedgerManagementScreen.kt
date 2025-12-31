package com.lifemanager.app.feature.finance.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.LedgerEntity
import com.lifemanager.app.core.database.entity.LedgerType
import com.lifemanager.app.ui.component.PremiumTextField
import java.text.NumberFormat
import java.util.Locale

/**
 * 账本管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: LedgerManagementViewModel = hiltViewModel()
) {
    val ledgers by viewModel.ledgers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val deletingLedger by viewModel.deletingLedger.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账本管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "创建账本")
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
        } else if (ledgers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无账本",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("创建账本")
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
                // 活跃账本
                val activeLedgers = ledgers.filter { !it.ledger.isArchived }
                if (activeLedgers.isNotEmpty()) {
                    item {
                        Text(
                            text = "我的账本",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(activeLedgers) { ledgerWithStats ->
                        LedgerCard(
                            ledgerWithStats = ledgerWithStats,
                            onEdit = { viewModel.showEditDialog(ledgerWithStats.ledger) },
                            onSetDefault = { viewModel.setDefaultLedger(ledgerWithStats.ledger.id) },
                            onArchive = { viewModel.toggleArchive(ledgerWithStats.ledger) },
                            onDelete = { viewModel.showDeleteConfirmDialog(ledgerWithStats.ledger) }
                        )
                    }
                }

                // 归档账本
                val archivedLedgers = ledgers.filter { it.ledger.isArchived }
                if (archivedLedgers.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已归档",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(archivedLedgers) { ledgerWithStats ->
                        LedgerCard(
                            ledgerWithStats = ledgerWithStats,
                            onEdit = { viewModel.showEditDialog(ledgerWithStats.ledger) },
                            onSetDefault = { },
                            onArchive = { viewModel.toggleArchive(ledgerWithStats.ledger) },
                            onDelete = { viewModel.showDeleteConfirmDialog(ledgerWithStats.ledger) },
                            isArchived = true
                        )
                    }
                }
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        EditLedgerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && deletingLedger != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("删除账本") },
            text = {
                Text("确定要删除「${deletingLedger!!.name}」吗？此操作不可恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteLedger() },
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
private fun LedgerCard(
    ledgerWithStats: LedgerWithStats,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    isArchived: Boolean = false
) {
    val ledger = ledgerWithStats.ledger
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isArchived)
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
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColor(ledger.color).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getLedgerIcon(ledger.icon),
                            contentDescription = null,
                            tint = parseColor(ledger.color),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ledger.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (ledger.isDefault) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "默认",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (ledger.description.isNotBlank()) {
                            Text(
                                text = ledger.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

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
                        if (!ledger.isDefault && !isArchived) {
                            DropdownMenuItem(
                                text = { Text("设为默认") },
                                onClick = {
                                    showMenu = false
                                    onSetDefault()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (isArchived) "取消归档" else "归档") },
                            onClick = {
                                showMenu = false
                                onArchive()
                            },
                            leadingIcon = {
                                Icon(
                                    if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            }
                        )
                        if (!ledger.isDefault) {
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

            // 统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "收入",
                    value = "¥${numberFormat.format(ledgerWithStats.totalIncome)}",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    label = "支出",
                    value = "¥${numberFormat.format(ledgerWithStats.totalExpense)}",
                    color = Color(0xFFF44336)
                )
                StatItem(
                    label = "结余",
                    value = "¥${numberFormat.format(ledgerWithStats.balance)}",
                    color = if (ledgerWithStats.balance >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        Color(0xFFF44336)
                )
            }

            // 预算信息
            ledger.budgetAmount?.let { budget ->
                if (budget > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val usagePercentage = if (budget > 0)
                        (ledgerWithStats.totalExpense / budget * 100).toInt().coerceIn(0, 100)
                    else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "预算: ¥${numberFormat.format(budget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "已用 $usagePercentage%",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                usagePercentage >= 100 -> Color(0xFFF44336)
                                usagePercentage >= 80 -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (usagePercentage / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = when {
                            usagePercentage >= 100 -> Color(0xFFF44336)
                            usagePercentage >= 80 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLedgerDialog(
    viewModel: LedgerManagementViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val editingLedger by viewModel.editingLedger.collectAsState()
    val isEditing = editingLedger != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "编辑账本" else "创建账本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 错误提示
                editState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 账本名称
                PremiumTextField(
                    value = editState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = "账本名称",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 账本描述
                PremiumTextField(
                    value = editState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = "描述（可选）",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // 账本类型 - 使用网格布局支持更多类型
                Text(
                    text = "账本类型",
                    style = MaterialTheme.typography.labelMedium
                )
                val allTypes = listOf(
                    LedgerType.PERSONAL to Pair("👤", "个人"),
                    LedgerType.FAMILY to Pair("👨‍👩‍👧", "家庭"),
                    LedgerType.BUSINESS to Pair("💼", "生意"),
                    LedgerType.TRAVEL to Pair("✈️", "旅行"),
                    LedgerType.PROJECT to Pair("📋", "项目"),
                    LedgerType.INVESTMENT to Pair("📈", "投资")
                )
                // 使用3列网格
                val columns = 3
                val typeRows = (allTypes.size + columns - 1) / columns
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until typeRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until columns) {
                                val index = row * columns + col
                                if (index < allTypes.size) {
                                    val (type, iconLabel) = allTypes[index]
                                    val (icon, label) = iconLabel
                                    val isSelected = editState.ledgerType == type
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clickable { viewModel.updateLedgerType(type) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected)
                                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        else null
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(icon, modifier = Modifier.padding(end = 4.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 颜色选择
                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LEDGER_COLORS) { color ->
                        val isSelected = editState.color == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseColor(color))
                                .clickable { viewModel.updateColor(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 预算金额
                PremiumTextField(
                    value = editState.budgetAmount,
                    onValueChange = { viewModel.updateBudgetAmount(it.filter { c -> c.isDigit() || c == '.' }) },
                    label = "月预算（可选）",
                    leadingIcon = { Text("¥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveLedger() },
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

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}

private fun getLedgerIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "book" -> Icons.Default.Book
        "wallet" -> Icons.Default.AccountBalanceWallet
        "card" -> Icons.Default.CreditCard
        "cash" -> Icons.Default.Money
        "savings" -> Icons.Default.Savings
        "home" -> Icons.Default.Home
        "work" -> Icons.Default.Work
        "travel" -> Icons.Default.Flight
        "food" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingCart
        "health" -> Icons.Default.HealthAndSafety
        "education" -> Icons.Default.School
        "entertainment" -> Icons.Default.Theaters
        "gift" -> Icons.Default.CardGiftcard
        else -> Icons.Default.Book
    }
}
