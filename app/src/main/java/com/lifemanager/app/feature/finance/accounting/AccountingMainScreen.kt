package com.lifemanager.app.feature.finance.accounting

import androidx.compose.animation.*
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/**
 * 记账主界面
 *
 * 参考"时光序"APP的设计：
 * - 左侧抽屉侧边栏，包含功能和管理模块入口
 * - 主界面显示统计数据和快捷功能入口
 * - 支持快速记账
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingMainScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToLedgerManagement: () -> Unit,
    onNavigateToAssetManagement: () -> Unit,
    onNavigateToRecurringTransaction: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDailyTransaction: () -> Unit,
    viewModel: AccountingMainViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val currentLedger by viewModel.currentLedger.collectAsState()
    val showQuickAddDialog by viewModel.showQuickAddDialog.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AccountingSidebar(
                currentLedger = currentLedger,
                onNavigateToCalendar = {
                    scope.launch { drawerState.close() }
                    onNavigateToCalendar()
                },
                onNavigateToSearch = {
                    scope.launch { drawerState.close() }
                    onNavigateToSearch()
                },
                onNavigateToStatistics = {
                    scope.launch { drawerState.close() }
                    onNavigateToStatistics()
                },
                onNavigateToLedgerManagement = {
                    scope.launch { drawerState.close() }
                    onNavigateToLedgerManagement()
                },
                onNavigateToAssetManagement = {
                    scope.launch { drawerState.close() }
                    onNavigateToAssetManagement()
                },
                onNavigateToRecurringTransaction = {
                    scope.launch { drawerState.close() }
                    onNavigateToRecurringTransaction()
                },
                onNavigateToCategoryManagement = {
                    scope.launch { drawerState.close() }
                    onNavigateToCategoryManagement()
                },
                onNavigateToBudget = {
                    scope.launch { drawerState.close() }
                    onNavigateToBudget()
                },
                onNavigateToImport = {
                    scope.launch { drawerState.close() }
                    onNavigateToImport()
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentLedger?.name ?: "默认账本",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = onNavigateToCalendar) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "日历")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showQuickAdd() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("记一笔") }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 月度统计卡片
                item {
                    MonthlyStatisticsCard(
                        monthStats = monthStats,
                        onClick = onNavigateToStatistics
                    )
                }

                // 今日统计
                item {
                    TodayStatisticsCard(todayStats = todayStats)
                }

                // 快捷功能入口
                item {
                    QuickActionsSection(
                        onNavigateToDailyTransaction = onNavigateToDailyTransaction,
                        onNavigateToCalendar = onNavigateToCalendar,
                        onNavigateToBudget = onNavigateToBudget,
                        onNavigateToStatistics = onNavigateToStatistics
                    )
                }

                // 最近交易
                item {
                    RecentTransactionsSection(
                        transactions = recentTransactions,
                        onViewAll = onNavigateToDailyTransaction,
                        onTransactionClick = { /* TODO: 编辑交易 */ }
                    )
                }
            }
        }
    }

    // 快速记账对话框
    if (showQuickAddDialog) {
        QuickAddTransactionDialog(
            onDismiss = { viewModel.hideQuickAdd() },
            onConfirm = { type, amount, categoryId, note ->
                viewModel.quickAddTransaction(type, amount, categoryId, note)
            },
            categories = viewModel.categories.collectAsState().value
        )
    }
}

/**
 * 侧边栏
 */
@Composable
private fun AccountingSidebar(
    currentLedger: LedgerInfo?,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToLedgerManagement: () -> Unit,
    onNavigateToAssetManagement: () -> Unit,
    onNavigateToRecurringTransaction: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        // 顶部账本信息
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentLedger?.name ?: "默认账本",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "记录生活每一笔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 功能模块
        Text(
            text = "功能",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SidebarItem(
            icon = Icons.Outlined.CalendarMonth,
            label = "记账日历",
            description = "按日期查看收支",
            onClick = onNavigateToCalendar
        )

        SidebarItem(
            icon = Icons.Outlined.Search,
            label = "搜索",
            description = "查找交易记录",
            onClick = onNavigateToSearch
        )

        SidebarItem(
            icon = Icons.Outlined.Analytics,
            label = "统计",
            description = "收支分析报表",
            onClick = onNavigateToStatistics
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // 管理模块
        Text(
            text = "管理",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SidebarItem(
            icon = Icons.Outlined.LibraryBooks,
            label = "多账本管理",
            description = "管理多个账本",
            onClick = onNavigateToLedgerManagement
        )

        SidebarItem(
            icon = Icons.Outlined.AccountBalance,
            label = "资产管理",
            description = "管理资产账户",
            onClick = onNavigateToAssetManagement
        )

        SidebarItem(
            icon = Icons.Outlined.Repeat,
            label = "周期记账",
            description = "定期自动记账",
            onClick = onNavigateToRecurringTransaction
        )

        SidebarItem(
            icon = Icons.Outlined.Category,
            label = "分类管理",
            description = "管理收支分类",
            onClick = onNavigateToCategoryManagement
        )

        SidebarItem(
            icon = Icons.Outlined.PieChart,
            label = "预算管理",
            description = "设置月度预算",
            onClick = onNavigateToBudget
        )

        SidebarItem(
            icon = Icons.Outlined.FileUpload,
            label = "记账导入",
            description = "导入账单数据",
            onClick = onNavigateToImport
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        SidebarItem(
            icon = Icons.Outlined.Settings,
            label = "记账设置",
            description = "个性化设置",
            onClick = onNavigateToSettings
        )
    }
}

/**
 * 侧边栏项
 */
@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 月度统计卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyStatisticsCard(
    monthStats: PeriodStats,
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val today = remember { LocalDate.now() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${today.year}年${today.monthValue}月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "查看详情",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 收入
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥${numberFormat.format(monthStats.totalIncome)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }

                // 支出
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥${numberFormat.format(monthStats.totalExpense)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }

                // 结余
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "结余",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val balance = monthStats.totalIncome - monthStats.totalExpense
                    Text(
                        text = "¥${numberFormat.format(balance)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (balance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 今日统计卡片
 */
@Composable
private fun TodayStatisticsCard(todayStats: DailyStats) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 今日支出
            StatItem(
                label = "今日支出",
                value = "¥${numberFormat.format(todayStats.totalExpense)}",
                valueColor = Color(0xFFF44336)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 今日收入
            StatItem(
                label = "今日收入",
                value = "¥${numberFormat.format(todayStats.totalIncome)}",
                valueColor = Color(0xFF4CAF50)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 今日笔数
            StatItem(
                label = "今日笔数",
                value = "${todayStats.transactionCount}笔",
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 快捷功能入口
 */
@Composable
private fun QuickActionsSection(
    onNavigateToDailyTransaction: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToStatistics: () -> Unit
) {
    Column {
        Text(
            text = "快捷入口",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Default.ReceiptLong,
                label = "明细",
                color = Color(0xFF2196F3),
                onClick = onNavigateToDailyTransaction
            )
            QuickActionButton(
                icon = Icons.Default.CalendarMonth,
                label = "日历",
                color = Color(0xFF4CAF50),
                onClick = onNavigateToCalendar
            )
            QuickActionButton(
                icon = Icons.Default.PieChart,
                label = "预算",
                color = Color(0xFFFF9800),
                onClick = onNavigateToBudget
            )
            QuickActionButton(
                icon = Icons.Default.Analytics,
                label = "统计",
                color = Color(0xFF9C27B0),
                onClick = onNavigateToStatistics
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * 最近交易部分
 */
@Composable
private fun RecentTransactionsSection(
    transactions: List<DailyTransactionWithCategory>,
    onViewAll: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onViewAll) {
                Text("查看全部")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    transactions.forEachIndexed { index, transaction ->
                        RecentTransactionItem(
                            transaction = transaction,
                            numberFormat = numberFormat,
                            onClick = { onTransactionClick(transaction.transaction.id) }
                        )
                        if (index < transactions.size - 1) {
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
}

@Composable
private fun RecentTransactionItem(
    transaction: DailyTransactionWithCategory,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    val isExpense = transaction.transaction.type == TransactionType.EXPENSE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    transaction.category?.let { parseColor(it.color) }
                        ?: MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpense) Icons.Filled.ShoppingCart else Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category?.name ?: if (isExpense) "支出" else "收入",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (transaction.transaction.note.isNotBlank()) {
                Text(
                    text = transaction.transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 金额
        Text(
            text = "${if (isExpense) "-" else "+"}¥${numberFormat.format(transaction.transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
        )
    }
}

/**
 * 快速记账对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, amount: Double, categoryId: Long?, note: String) -> Unit,
    categories: List<com.lifemanager.app.core.database.entity.CustomFieldEntity>
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快速记账") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedType == TransactionType.EXPENSE) {
                        Button(
                            onClick = { selectedType = TransactionType.EXPENSE },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("支出")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedType = TransactionType.EXPENSE },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("支出")
                        }
                    }

                    if (selectedType == TransactionType.INCOME) {
                        Button(
                            onClick = { selectedType = TransactionType.INCOME },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("收入")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedType = TransactionType.INCOME },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("收入")
                        }
                    }
                }

                // 金额输入
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("金额") },
                    leadingIcon = { Text("¥") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 分类选择
                val filteredCategories = categories.filter {
                    it.moduleType == if (selectedType == TransactionType.EXPENSE) "EXPENSE_CATEGORY" else "INCOME_CATEGORY"
                }

                if (filteredCategories.isNotEmpty()) {
                    Text(
                        text = "分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCategories) { category ->
                            val isSelected = selectedCategoryId == category.id
                            if (isSelected) {
                                Button(
                                    onClick = { selectedCategoryId = category.id },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(category.name, style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedCategoryId = category.id },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(category.name, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（选填）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0) {
                        onConfirm(selectedType, amountValue, selectedCategoryId, note)
                        onDismiss()
                    }
                }
            ) {
                Text("确认")
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
        Color.Gray
    }
}

/**
 * 账本信息
 */
data class LedgerInfo(
    val id: Long,
    val name: String,
    val icon: String? = null,
    val isDefault: Boolean = false
)
