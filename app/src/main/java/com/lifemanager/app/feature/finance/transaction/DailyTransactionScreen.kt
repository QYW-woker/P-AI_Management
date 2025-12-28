package com.lifemanager.app.feature.finance.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.lifemanager.app.domain.model.*
import java.text.NumberFormat
import java.util.Locale

/**
 * 日常记账主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToImport: () -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    viewModel: DailyTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactionGroups by viewModel.transactionGroups.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日常记账") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 导入账单按钮
                    IconButton(onClick = onNavigateToImport) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = "导入账单"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == "LIST") Icons.Filled.CalendarMonth else Icons.Filled.List,
                            contentDescription = if (viewMode == "LIST") "日历视图" else "列表视图"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 统计卡片
            StatsCards(
                todayStats = todayStats,
                monthStats = monthStats
            )

            when (uiState) {
                is TransactionUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TransactionUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as TransactionUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is TransactionUiState.Success -> {
                    if (transactionGroups.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            transactionGroups.forEach { group ->
                                item(key = "header_${group.date}") {
                                    DayHeader(group = group)
                                }

                                items(
                                    items = group.transactions,
                                    key = { it.transaction.id }
                                ) { transaction ->
                                    TransactionItem(
                                        transaction = transaction,
                                        onClick = { viewModel.showEditDialog(transaction.transaction.id) },
                                        onDelete = { viewModel.showDeleteConfirm(transaction.transaction.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AddEditTransactionDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() },
            onNavigateToCategoryManagement = {
                viewModel.hideEditDialog()
                onNavigateToCategoryManagement()
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？") },
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

@Composable
private fun StatsCards(
    todayStats: DailyStats,
    monthStats: PeriodStats
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 今日支出
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "今日支出",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥${numberFormat.format(todayStats.totalExpense)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
        }

        // 本月支出
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "本月支出",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1565C0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥${numberFormat.format(monthStats.totalExpense)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}

@Composable
private fun DayHeader(group: DailyTransactionGroup) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = group.dayOfWeek,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            if (group.totalIncome > 0) {
                Text(
                    text = "+${numberFormat.format(group.totalIncome)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (group.totalExpense > 0) {
                Text(
                    text = "-${numberFormat.format(group.totalExpense)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: DailyTransactionWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val isExpense = transaction.transaction.type == TransactionType.EXPENSE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

            // 金额和时间
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}¥${numberFormat.format(transaction.transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
                if (transaction.transaction.time.isNotBlank()) {
                    Text(
                        text = transaction.transaction.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无记账记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮开始记账",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
