package com.lifemanager.app.feature.finance.transaction.billimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.lifemanager.app.core.database.entity.CustomFieldEntity

/**
 * 账单导入界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: BillImportViewModel = hiltViewModel()
) {
    val importState by viewModel.importState.collectAsState()
    val parsedRecords by viewModel.parsedRecords.collectAsState()
    val importStats by viewModel.importStats.collectAsState()
    val billSource by viewModel.billSource.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseFile(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入账单") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (importState is ImportState.Preview && parsedRecords.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.executeImport() }
                        ) {
                            Text("导入 (${importStats.selectedRecords})")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = importState) {
                is ImportState.Idle -> {
                    IdleContent(
                        onSelectFile = { filePickerLauncher.launch("*/*") }
                    )
                }

                is ImportState.Parsing -> {
                    LoadingContent(message = "正在解析账单...")
                }

                is ImportState.Preview -> {
                    PreviewContent(
                        records = parsedRecords,
                        stats = importStats,
                        source = billSource,
                        incomeCategories = incomeCategories,
                        expenseCategories = expenseCategories,
                        onToggleRecord = { viewModel.toggleRecordSelection(it) },
                        onToggleSelectAll = { viewModel.toggleSelectAll() },
                        onUpdateCategory = { index, categoryId ->
                            viewModel.updateRecordCategory(index, categoryId)
                        },
                        onSelectNewFile = { filePickerLauncher.launch("*/*") }
                    )
                }

                is ImportState.Importing -> {
                    LoadingContent(message = "正在导入...")
                }

                is ImportState.Success -> {
                    SuccessContent(
                        importedCount = state.importedCount,
                        totalAmount = state.totalAmount,
                        onDone = onNavigateBack,
                        onImportMore = {
                            viewModel.reset()
                            filePickerLauncher.launch("*/*")
                        }
                    )
                }

                is ImportState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { filePickerLauncher.launch("*/*") },
                        onDismiss = { viewModel.clearError() }
                    )
                }

                else -> {}
            }
        }
    }
}

/**
 * 空闲状态 - 选择文件
 */
@Composable
private fun IdleContent(onSelectFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FileUpload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "导入微信/支付宝账单",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "支持微信和支付宝导出的账单文件",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "支持格式：CSV、Excel(.xlsx/.xls)、Word(.docx)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSelectFile,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择账单文件")
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 使用说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "如何导出账单？",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "微信：我 → 服务 → 钱包 → 账单 → 常见问题 → 下载账单",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "支付宝：我的 → 账单 → 右上角... → 开具交易流水证明",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 加载中状态
 */
@Composable
private fun LoadingContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * 预览内容
 */
@Composable
private fun PreviewContent(
    records: List<ParsedBillRecord>,
    stats: ImportStats,
    source: BillSource,
    incomeCategories: List<CustomFieldEntity>,
    expenseCategories: List<CustomFieldEntity>,
    onToggleRecord: (Int) -> Unit,
    onToggleSelectAll: () -> Unit,
    onUpdateCategory: (Int, Long) -> Unit,
    onSelectNewFile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 统计卡片
        StatsCard(stats = stats, source = source)

        // 操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onToggleSelectAll) {
                val allSelected = records.all { it.isSelected }
                Icon(
                    imageVector = if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (allSelected) "取消全选" else "全选")
            }

            TextButton(onClick = onSelectNewFile) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重新选择")
            }
        }

        Divider()

        // 记录列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(records) { index, record ->
                RecordItem(
                    record = record,
                    categories = if (record.type == "收入") incomeCategories else expenseCategories,
                    onToggle = { onToggleRecord(index) },
                    onCategoryChange = { categoryId -> onUpdateCategory(index, categoryId) }
                )
            }
        }
    }
}

/**
 * 统计卡片
 */
@Composable
private fun StatsCard(stats: ImportStats, source: BillSource) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.displayName + "账单",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共 ${stats.totalRecords} 条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "¥${String.format("%.2f", stats.totalIncome)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "¥${String.format("%.2f", stats.totalExpense)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.selectedRecords}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "已选中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 记录项
 */
@Composable
private fun RecordItem(
    record: ParsedBillRecord,
    categories: List<CustomFieldEntity>,
    onToggle: () -> Unit,
    onCategoryChange: (Long) -> Unit
) {
    var showCategoryDialog by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.id == record.suggestedCategoryId }
    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
    val categoryColor = selectedCategory?.color?.let { colorString ->
        try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            null
        }
    } ?: fallbackColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isSelected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择框
            Checkbox(
                checked = record.isSelected,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 主要内容
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = record.counterparty.ifBlank { record.goods },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${if (record.type == "收入") "+" else "-"}¥${String.format("%.2f", record.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (record.type == "收入") Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.datetime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 分类选择
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(categoryColor.copy(alpha = 0.2f))
                            .clickable { showCategoryDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedCategory?.name ?: "选择分类",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedCategory != null)
                                categoryColor
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 分类选择对话框
    if (showCategoryDialog) {
        CategorySelectDialog(
            categories = categories,
            selectedId = record.suggestedCategoryId,
            onSelect = { categoryId ->
                onCategoryChange(categoryId)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }
}

/**
 * 分类选择对话框
 */
@Composable
private fun CategorySelectDialog(
    categories: List<CustomFieldEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择分类") },
        text = {
            LazyColumn {
                items(categories.size) { index ->
                    val category = categories[index]
                    val color = try {
                        Color(android.graphics.Color.parseColor(category.color))
                    } catch (e: Exception) {
                        primaryColor
                    }
                    // 获取卡通图标
                    val emoji = com.lifemanager.app.ui.component.CategoryIcons.getIcon(
                        name = category.name,
                        iconName = category.iconName,
                        moduleType = category.moduleType
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(category.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (category.id == selectedId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 成功状态
 */
@Composable
private fun SuccessContent(
    importedCount: Int,
    totalAmount: Double,
    onDone: () -> Unit,
    onImportMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "导入成功！",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "成功导入 $importedCount 条记录",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "总金额 ¥${String.format("%.2f", totalAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("完成")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onImportMore,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("继续导入")
        }
    }
}

/**
 * 错误状态
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "导入失败",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("重新选择文件")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onDismiss) {
            Text("返回")
        }
    }
}
