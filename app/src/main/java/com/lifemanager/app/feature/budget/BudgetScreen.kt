package com.lifemanager.app.feature.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.ui.component.PremiumTextField
import java.text.NumberFormat
import java.util.Locale

/**
 * 预算管理主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val budgetWithSpending by viewModel.budgetWithSpending.collectAsState()
    val monthlyAnalysis by viewModel.monthlyAnalysis.collectAsState()
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val categoryBudgetStatuses by viewModel.categoryBudgetStatuses.collectAsState()
    val weeklyAnalysis by viewModel.weeklyAnalysis.collectAsState()
    val budgetStats by viewModel.budgetStats.collectAsState()
    val predictedSpending by viewModel.predictedSpending.collectAsState()
    val budgetSuccessRate by viewModel.budgetSuccessRate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showEditDialog() }) {
                        Icon(
                            imageVector = if (budgetWithSpending != null) Icons.Filled.Edit else Icons.Filled.Add,
                            contentDescription = if (budgetWithSpending != null) "编辑预算" else "设置预算"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is BudgetUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is BudgetUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as BudgetUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("重试")
                        }
                    }
                }
            }

            is BudgetUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 月份选择器
                    item {
                        MonthSelector(
                            yearMonth = currentYearMonth,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            formatYearMonth = { viewModel.formatYearMonth(it) }
                        )
                    }

                    // 预算概览卡片
                    item {
                        BudgetOverviewCard(
                            budgetWithSpending = budgetWithSpending,
                            onSetBudget = { viewModel.showEditDialog() },
                            onCopyFromPrevious = { viewModel.copyFromPreviousMonth() }
                        )
                    }

                    // 花销进度
                    if (budgetWithSpending != null) {
                        item {
                            SpendingProgressCard(budgetWithSpending = budgetWithSpending!!)
                        }
                    }

                    // 分类预算卡片
                    if (categoryBudgetStatuses.isNotEmpty()) {
                        item {
                            CategoryBudgetsCard(
                                categoryBudgets = categoryBudgetStatuses,
                                onEditBudgets = { viewModel.showEditDialog() }
                            )
                        }
                    }

                    // 周预算分析卡片
                    if (weeklyAnalysis.isNotEmpty()) {
                        item {
                            WeeklyBudgetCard(weeklyAnalysis = weeklyAnalysis)
                        }
                    }

                    // 预算统计卡片
                    if (budgetStats != null && budgetStats!!.totalMonthsTracked > 0) {
                        item {
                            BudgetStatsCard(
                                stats = budgetStats!!,
                                successRate = budgetSuccessRate,
                                predictedSpending = predictedSpending,
                                currentBudget = budgetWithSpending?.budget?.totalBudget ?: 0.0,
                                formatYearMonth = { viewModel.formatYearMonth(it) }
                            )
                        }
                    }

                    // 历史趋势图表
                    if (monthlyAnalysis.isNotEmpty()) {
                        item {
                            BudgetTrendCard(monthlyAnalysis = monthlyAnalysis)
                        }
                    }

                    // AI建议
                    if (aiAdvice.isNotBlank()) {
                        item {
                            AIAdviceCard(advice = aiAdvice)
                        }
                    }
                }
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        EditBudgetDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }
}

@Composable
private fun MonthSelector(
    yearMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    formatYearMonth: (Int) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
            }

            Text(
                text = formatYearMonth(yearMonth),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
            }
        }
    }
}

@Composable
private fun BudgetOverviewCard(
    budgetWithSpending: BudgetWithSpending?,
    onSetBudget: () -> Unit,
    onCopyFromPrevious: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (budgetWithSpending == null) {
            // 未设置预算
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "本月尚未设置预算",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCopyFromPrevious) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制上月")
                    }
                    Button(onClick = onSetBudget) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("设置预算")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "本月预算",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusChip(status = budgetWithSpending.status)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 主要数据
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BudgetStatItem(
                        label = "预算总额",
                        value = "¥${numberFormat.format(budgetWithSpending.budget.totalBudget)}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    BudgetStatItem(
                        label = "已支出",
                        value = "¥${numberFormat.format(budgetWithSpending.totalSpent)}",
                        color = Color(0xFFF44336)
                    )
                    BudgetStatItem(
                        label = "剩余",
                        value = "¥${numberFormat.format(budgetWithSpending.remaining)}",
                        color = if (budgetWithSpending.remaining >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 剩余天数提示
                Text(
                    text = "本月还剩 ${budgetWithSpending.daysRemaining} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BudgetStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusChip(status: BudgetStatus) {
    val (color, text) = when (status) {
        BudgetStatus.NORMAL -> Color(0xFF4CAF50) to "正常"
        BudgetStatus.WARNING -> Color(0xFFFF9800) to "警告"
        BudgetStatus.EXCEEDED -> Color(0xFFF44336) to "超支"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SpendingProgressCard(budgetWithSpending: BudgetWithSpending) {
    val animatedProgress by animateFloatAsState(
        targetValue = (budgetWithSpending.usagePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "progress"
    )

    val progressColor = when (budgetWithSpending.status) {
        BudgetStatus.NORMAL -> Color(0xFF4CAF50)
        BudgetStatus.WARNING -> Color(0xFFFF9800)
        BudgetStatus.EXCEEDED -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "预算使用进度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${budgetWithSpending.usagePercentage}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 日均可用
            if (budgetWithSpending.daysRemaining > 0 && budgetWithSpending.remaining > 0) {
                val dailyBudget = budgetWithSpending.remaining / budgetWithSpending.daysRemaining
                Text(
                    text = "日均可用: ¥${String.format("%.2f", dailyBudget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 分类预算卡片
 */
@Composable
private fun CategoryBudgetsCard(
    categoryBudgets: List<CategoryBudgetItem>,
    onEditBudgets: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分类预算",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onEditBudgets) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            categoryBudgets.forEach { budget ->
                CategoryBudgetProgressItem(
                    budget = budget,
                    numberFormat = numberFormat
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CategoryBudgetProgressItem(
    budget: CategoryBudgetItem,
    numberFormat: NumberFormat
) {
    val budgetAmount = budget.budgetAmount.toDoubleOrNull() ?: 0.0
    val progress = if (budgetAmount > 0) (budget.spentAmount / budgetAmount).coerceIn(0.0, 1.0).toFloat() else 0f

    val progressColor = when (budget.status) {
        BudgetStatus.NORMAL -> Color(0xFF4CAF50)
        BudgetStatus.WARNING -> Color(0xFFFF9800)
        BudgetStatus.EXCEEDED -> Color(0xFFF44336)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 获取卡通图标
                val emoji = com.lifemanager.app.ui.component.CategoryIcons.getExpenseIcon(budget.categoryName)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(parseColor(budget.categoryColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = budget.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "¥${numberFormat.format(budget.spentAmount)} / ¥${numberFormat.format(budgetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun BudgetTrendCard(monthlyAnalysis: List<MonthlyBudgetAnalysis>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "预算执行趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 简易柱状图
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyAnalysis.forEach { analysis ->
                    val maxValue = monthlyAnalysis.maxOf { maxOf(it.budgetAmount, it.spentAmount) }
                    val budgetHeight = if (maxValue > 0) (analysis.budgetAmount / maxValue * 100).toFloat() else 0f
                    val spentHeight = if (maxValue > 0) (analysis.spentAmount / maxValue * 100).toFloat() else 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 柱状图
                        Row(
                            modifier = Modifier.height(80.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // 预算柱
                            if (analysis.hasBudget) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height((budgetHeight * 0.8f).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                )
                            }
                            // 支出柱
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height((spentHeight * 0.8f).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        when (analysis.status) {
                                            BudgetStatus.EXCEEDED -> Color(0xFFF44336)
                                            BudgetStatus.WARNING -> Color(0xFFFF9800)
                                            else -> Color(0xFF4CAF50)
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 月份标签
                        Text(
                            text = analysis.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), label = "预算")
                Spacer(modifier = Modifier.width(24.dp))
                LegendItem(color = Color(0xFF4CAF50), label = "支出")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AIAdviceCard(advice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 预算建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = advice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetDialog(
    viewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val showAddCategoryDialog by viewModel.showAddCategoryBudgetDialog.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets.ime
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editState.isEditing) "✏️ 编辑预算" else "💰 设置预算",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 错误提示
            editState.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 预算金额输入卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "月度预算总额",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editState.totalBudget,
                        onValueChange = { viewModel.updateBudgetAmount(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        leadingIcon = {
                            Text(
                                "¥",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 分类预算区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "分类预算",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.showAddCategoryBudgetDialog() },
                            enabled = viewModel.getAvailableCategories().isNotEmpty()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加分类")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (editState.categoryBudgets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📁", style = MaterialTheme.typography.headlineLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "暂无分类预算",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "点击上方按钮添加",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        editState.categoryBudgets.forEach { categoryBudget ->
                            EnhancedCategoryBudgetEditItem(
                                categoryBudget = categoryBudget,
                                onAmountChange = { amount ->
                                    viewModel.updateCategoryBudgetAmount(categoryBudget.categoryId, amount)
                                },
                                onRemove = {
                                    viewModel.removeCategoryBudget(categoryBudget.categoryId)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提醒设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔔", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "提醒设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 提醒开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "启用预算提醒",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "超过阈值时发送通知",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editState.alertEnabled,
                            onCheckedChange = { viewModel.updateAlertEnabled(it) }
                        )
                    }

                    if (editState.alertEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "提醒阈值: ${editState.alertThreshold}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = editState.alertThreshold.toFloat(),
                            onValueChange = { viewModel.updateAlertThreshold(it.toInt()) },
                            valueRange = 50f..100f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "50%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 备注卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📝", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "备注",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editState.note,
                        onValueChange = { viewModel.updateNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("添加备注（可选）") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮
            Button(
                onClick = { viewModel.saveBudget() },
                enabled = !editState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (editState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存预算",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 添加分类预算对话框
    if (showAddCategoryDialog) {
        EnhancedAddCategoryBudgetDialog(
            availableCategories = viewModel.getAvailableCategories(),
            onAdd = { category, amount ->
                viewModel.addCategoryBudget(
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryColor = category.color ?: "#2196F3",
                    amount = amount
                )
                viewModel.hideAddCategoryBudgetDialog()
            },
            onDismiss = { viewModel.hideAddCategoryBudgetDialog() }
        )
    }
}

@Composable
private fun EnhancedCategoryBudgetEditItem(
    categoryBudget: CategoryBudgetItem,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val emoji = com.lifemanager.app.ui.component.CategoryIcons.getExpenseIcon(categoryBudget.categoryName)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    .background(parseColor(categoryBudget.categoryColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 分类名称
            Text(
                text = categoryBudget.categoryName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 金额输入
            OutlinedTextField(
                value = categoryBudget.budgetAmount,
                onValueChange = onAmountChange,
                modifier = Modifier.width(120.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.End
                ),
                leadingIcon = {
                    Text(
                        "¥",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedAddCategoryBudgetDialog(
    availableCategories: List<CustomFieldEntity>,
    onAdd: (CustomFieldEntity, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<CustomFieldEntity?>(null) }
    var amount by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets.ime
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = "➕ 添加分类预算",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 分类选择 - 网格布局
            Text(
                text = "选择分类",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 使用网格布局展示分类
            val columns = 4
            val rows = (availableCategories.size + columns - 1) / columns

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0 until columns) {
                            val index = row * columns + col
                            if (index < availableCategories.size) {
                                val category = availableCategories[index]
                                val isSelected = selectedCategory?.id == category.id
                                val emoji = com.lifemanager.app.ui.component.CategoryIcons.getIcon(
                                    name = category.name,
                                    iconName = category.iconName,
                                    moduleType = category.moduleType
                                )

                                CategoryGridItem(
                                    emoji = emoji,
                                    name = category.name,
                                    color = category.color ?: "#2196F3",
                                    isSelected = isSelected,
                                    onClick = { selectedCategory = category },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                // 占位空白
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 金额输入
            Text(
                text = "预算金额",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                leadingIcon = {
                    Text(
                        "¥",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                placeholder = { Text("0.00") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }

                Button(
                    onClick = {
                        selectedCategory?.let { category ->
                            if (amount.isNotBlank()) {
                                onAdd(category, amount)
                            }
                        }
                    },
                    enabled = selectedCategory != null && amount.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加")
                }
            }
        }
    }
}

@Composable
private fun CategoryGridItem(
    emoji: String,
    name: String,
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            parseColor(color).copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, parseColor(color))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (isSelected) parseColor(color) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}

/**
 * 周预算分析卡片
 */
@Composable
private fun WeeklyBudgetCard(weeklyAnalysis: List<WeeklyBudgetAnalysis>) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "周预算分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            weeklyAnalysis.forEach { week ->
                WeekBudgetItem(
                    week = week,
                    numberFormat = numberFormat
                )
                if (week != weeklyAnalysis.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekBudgetItem(
    week: WeeklyBudgetAnalysis,
    numberFormat: NumberFormat
) {
    val progress = (week.usagePercentage / 100f).coerceIn(0f, 1f)
    val progressColor = when (week.status) {
        BudgetStatus.NORMAL -> Color(0xFF4CAF50)
        BudgetStatus.WARNING -> Color(0xFFFF9800)
        BudgetStatus.EXCEEDED -> Color(0xFFF44336)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "第${week.weekNumber}周",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (week.isCurrentWeek) FontWeight.Bold else FontWeight.Normal
                )
                if (week.isCurrentWeek) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "本周",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = "¥${numberFormat.format(week.spentAmount)} / ¥${numberFormat.format(week.budgetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = week.weekLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 预算统计卡片
 */
@Composable
private fun BudgetStatsCard(
    stats: BudgetOverviewStats,
    successRate: Double,
    predictedSpending: Double,
    currentBudget: Double,
    formatYearMonth: (Int) -> String
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "预算执行统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        successRate >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        successRate >= 50 -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        else -> Color(0xFFF44336).copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = "达标率 ${String.format("%.0f", successRate)}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            successRate >= 80 -> Color(0xFF4CAF50)
                            successRate >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 统计数据网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "月均预算",
                    value = "¥${numberFormat.format(stats.monthlyAvgBudget.toLong())}",
                    icon = Icons.Filled.AccountBalanceWallet
                )
                StatItem(
                    label = "月均支出",
                    value = "¥${numberFormat.format(stats.monthlyAvgSpending.toLong())}",
                    icon = Icons.Filled.TrendingUp
                )
                StatItem(
                    label = "节省率",
                    value = "${String.format("%.1f", stats.savingsRate)}%",
                    icon = Icons.Filled.Savings
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 连续达标和预测
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "连续达标",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.consecutiveUnderBudget}个月",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (stats.consecutiveUnderBudget > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (predictedSpending > 0 && currentBudget > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "预测月末支出",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${numberFormat.format(predictedSpending.toLong())}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (predictedSpending > currentBudget) Color(0xFFF44336) else Color(0xFF4CAF50)
                        )
                    }
                }
            }

            // 最佳/最差月份
            if (stats.bestMonth > 0 && stats.worstMonth > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "最佳: ${formatYearMonth(stats.bestMonth)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "待改进: ${formatYearMonth(stats.worstMonth)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
