package com.lifemanager.app.feature.goal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.domain.model.CategoryGoalStats
import com.lifemanager.app.domain.model.GoalInsights
import com.lifemanager.app.domain.model.GoalTemplate
import com.lifemanager.app.domain.model.GoalUiState
import com.lifemanager.app.domain.model.SubGoalEditState
import com.lifemanager.app.domain.model.getCategoryDisplayName
import com.lifemanager.app.domain.model.getGoalTypeDisplayName
import com.lifemanager.app.domain.model.goalCategoryOptions
import com.lifemanager.app.ui.component.PremiumTextField

/**
 * 目标管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showProgressDialog by viewModel.showProgressDialog.collectAsState()
    val goalAnalysis by viewModel.goalAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    // 多级目标相关状态
    val showSubGoalDialog by viewModel.showSubGoalDialog.collectAsState()
    val subGoalEditState by viewModel.subGoalEditState.collectAsState()
    val childCounts by viewModel.childCounts.collectAsState()
    val expandedGoalIds by viewModel.expandedGoalIds.collectAsState()

    // 扩展功能状态
    val goalInsights by viewModel.goalInsights.collectAsState()
    val recommendedTemplates by viewModel.recommendedTemplates.collectAsState()
    val upcomingDeadlines by viewModel.upcomingDeadlines.collectAsState()
    val overdueGoals by viewModel.overdueGoals.collectAsState()
    val showTemplateDialog by viewModel.showTemplateDialog.collectAsState()
    val selectedTemplateCategory by viewModel.selectedTemplateCategory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("目标管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加目标")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 统计卡片区域
            item {
                EnhancedStatsSection(statistics = statistics)
            }

            // 逾期/即将到期警告
            if (overdueGoals.isNotEmpty() || upcomingDeadlines.isNotEmpty()) {
                item {
                    DeadlineWarningsCard(
                        overdueGoals = overdueGoals,
                        upcomingDeadlines = upcomingDeadlines,
                        onGoalClick = { viewModel.showEditDialog(it) }
                    )
                }
            }

            // 推荐模板快捷入口
            if (recommendedTemplates.isNotEmpty()) {
                item {
                    RecommendedTemplatesSection(
                        templates = recommendedTemplates,
                        onTemplateClick = { viewModel.createFromTemplate(it) },
                        onViewAllClick = { viewModel.showTemplateDialog() }
                    )
                }
            }

            // 分类统计卡片
            goalInsights?.let { insights ->
                if (insights.categoryStats.isNotEmpty()) {
                    item {
                        CategoryStatsCard(categoryStats = insights.categoryStats)
                    }
                }
            }

            // AI智能洞察
            item {
                com.lifemanager.app.ui.component.AIInsightCard(
                    analysis = goalAnalysis,
                    isLoading = isAnalyzing,
                    onRefresh = { viewModel.refreshAIAnalysis() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 筛选标签
            item {
                Spacer(modifier = Modifier.height(8.dp))
                EnhancedFilterChips(
                    currentFilter = currentFilter,
                    onFilterChange = { viewModel.setFilter(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 目标列表
            when (uiState) {
                is GoalUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is GoalUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = (uiState as GoalUiState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.refresh() }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                }

                is GoalUiState.Success -> {
                    if (goals.isEmpty()) {
                        item {
                            EnhancedEmptyState(currentFilter = currentFilter)
                        }
                    } else {
                        // 获取顶级目标
                        val topLevelGoals = goals.filter { it.parentId == null }
                        topLevelGoals.forEach { goal ->
                            item(key = goal.id) {
                                val childCount = childCounts[goal.id] ?: 0
                                val isExpanded = goal.id in expandedGoalIds

                                EnhancedGoalCard(
                                    goal = goal,
                                    progress = viewModel.calculateProgress(goal),
                                    remainingDays = viewModel.getRemainingDays(goal),
                                    childCount = childCount,
                                    isExpanded = isExpanded,
                                    indentLevel = 0,
                                    onEdit = { viewModel.showEditDialog(goal.id) },
                                    onUpdateProgress = { viewModel.showProgressDialog(goal) },
                                    onComplete = { viewModel.completeGoal(goal.id) },
                                    onDelete = { viewModel.showDeleteConfirm(goal.id) },
                                    onAddSubGoal = { viewModel.showAddSubGoalDialog(goal.id) },
                                    onToggleExpand = { viewModel.toggleGoalExpanded(goal.id) }
                                )
                            }

                            // 显示子目标（如果展开）
                            if (goal.id in expandedGoalIds) {
                                val childGoals = goals.filter { it.parentId == goal.id }
                                childGoals.forEach { childGoal ->
                                    item(key = childGoal.id) {
                                        val subChildCount = childCounts[childGoal.id] ?: 0
                                        val isSubExpanded = childGoal.id in expandedGoalIds

                                        EnhancedGoalCard(
                                            goal = childGoal,
                                            progress = viewModel.calculateProgress(childGoal),
                                            remainingDays = viewModel.getRemainingDays(childGoal),
                                            childCount = subChildCount,
                                            isExpanded = isSubExpanded,
                                            indentLevel = 1,
                                            onEdit = { viewModel.showEditDialog(childGoal.id) },
                                            onUpdateProgress = { viewModel.showProgressDialog(childGoal) },
                                            onComplete = { viewModel.completeGoal(childGoal.id) },
                                            onDelete = { viewModel.showDeleteConfirm(childGoal.id) },
                                            onAddSubGoal = { viewModel.showAddSubGoalDialog(childGoal.id) },
                                            onToggleExpand = { viewModel.toggleGoalExpanded(childGoal.id) }
                                        )
                                    }

                                    // 二级子目标
                                    if (childGoal.id in expandedGoalIds) {
                                        val grandChildGoals = goals.filter { it.parentId == childGoal.id }
                                        grandChildGoals.forEach { grandChild ->
                                            item(key = grandChild.id) {
                                                EnhancedGoalCard(
                                                    goal = grandChild,
                                                    progress = viewModel.calculateProgress(grandChild),
                                                    remainingDays = viewModel.getRemainingDays(grandChild),
                                                    childCount = 0,
                                                    isExpanded = false,
                                                    indentLevel = 2,
                                                    onEdit = { viewModel.showEditDialog(grandChild.id) },
                                                    onUpdateProgress = { viewModel.showProgressDialog(grandChild) },
                                                    onComplete = { viewModel.completeGoal(grandChild.id) },
                                                    onDelete = { viewModel.showDeleteConfirm(grandChild.id) },
                                                    onAddSubGoal = null,
                                                    onToggleExpand = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        AddEditGoalDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个目标吗？此操作不可恢复。") },
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

    // 更新进度对话框
    if (showProgressDialog) {
        UpdateProgressDialog(
            goal = viewModel.getProgressGoal(),
            onDismiss = { viewModel.hideProgressDialog() },
            onConfirm = { viewModel.updateProgress(it) }
        )
    }

    // 添加子目标对话框
    if (showSubGoalDialog) {
        AddSubGoalDialog(
            state = subGoalEditState,
            onTitleChange = { viewModel.updateSubGoalTitle(it) },
            onDescriptionChange = { viewModel.updateSubGoalDescription(it) },
            onDismiss = { viewModel.hideSubGoalDialog() },
            onConfirm = { viewModel.saveSubGoal() }
        )
    }

    // 模板选择对话框
    if (showTemplateDialog) {
        TemplateSelectionDialog(
            selectedCategory = selectedTemplateCategory,
            onCategorySelect = { viewModel.selectTemplateCategory(it) },
            onTemplateSelect = { viewModel.createFromTemplate(it) },
            onDismiss = { viewModel.hideTemplateDialog() },
            getTemplatesForCategory = { viewModel.getTemplatesForCategory(it) }
        )
    }
}

/**
 * 增强版统计区域
 */
@Composable
private fun EnhancedStatsSection(
    statistics: com.lifemanager.app.domain.model.GoalStatistics
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "目标概览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AnimatedStatItem(
                        icon = Icons.Default.Flag,
                        label = "进行中",
                        value = statistics.activeCount,
                        color = Color.White
                    )
                    AnimatedStatItem(
                        icon = Icons.Default.CheckCircle,
                        label = "已完成",
                        value = statistics.completedCount,
                        color = Color(0xFF4ADE80)
                    )
                    AnimatedProgressRing(
                        progress = statistics.totalProgress.toFloat(),
                        label = "平均进度"
                    )
                }
            }
        }
    }
}

/**
 * 动画统计项
 */
@Composable
private fun AnimatedStatItem(
    icon: ImageVector,
    label: String,
    value: Int,
    color: Color
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "value"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = animatedValue.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * 动画进度环
 */
@Composable
private fun AnimatedProgressRing(
    progress: Float,
    label: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp)
        ) {
            Canvas(modifier = Modifier.size(56.dp)) {
                // 背景环
                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                // 进度环
                drawArc(
                    color = Color(0xFF4ADE80),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * 增强版筛选标签
 */
@Composable
private fun EnhancedFilterChips(
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filters = listOf(
        Triple("ACTIVE", "进行中", Icons.Default.PlayCircle),
        Triple("COMPLETED", "已完成", Icons.Default.CheckCircle),
        Triple("ABANDONED", "已放弃", Icons.Default.Cancel),
        Triple("ALL", "全部", Icons.Default.List)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filters) { (value, label, icon) ->
            val isSelected = currentFilter == value
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "content"
            )

            Surface(
                onClick = { onFilterChange(value) },
                shape = RoundedCornerShape(16.dp),
                color = backgroundColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * 增强版目标卡片
 */
@Composable
private fun EnhancedGoalCard(
    goal: GoalEntity,
    progress: Float,
    remainingDays: Int?,
    childCount: Int = 0,
    isExpanded: Boolean = false,
    indentLevel: Int = 0,
    onEdit: () -> Unit,
    onUpdateProgress: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onAddSubGoal: (() -> Unit)? = null,
    onToggleExpand: (() -> Unit)? = null
) {
    val categoryColor = getCategoryColor(goal.category)
    val categoryIcon = getCategoryIcon(goal.category)

    // 动画进度
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // 计算缩进
    val startPadding = 16.dp + (indentLevel * 24).dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 16.dp, top = 6.dp, bottom = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = categoryColor.copy(alpha = 0.3f)
            )
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部：分类图标和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 分类图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = categoryColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = getCategoryDisplayName(goal.category),
                            style = MaterialTheme.typography.labelMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = getGoalTypeDisplayName(goal.goalType),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 进度环和状态
                if (goal.status == GoalStatus.COMPLETED) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "已完成",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    MiniProgressRing(
                        progress = animatedProgress,
                        color = categoryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 标题行（含展开/收起按钮）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (goal.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 子目标展开/收起指示器
                if (childCount > 0 && onToggleExpand != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onToggleExpand,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "收起" else "展开",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${childCount}个子目标",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条区域
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 进度文本
                    Text(
                        text = if (goal.progressType == "NUMERIC" && goal.targetValue != null) {
                            "${goal.currentValue.toInt()}${goal.unit} / ${goal.targetValue.toInt()}${goal.unit}"
                        } else {
                            "${(progress * 100).toInt()}% 完成"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = categoryColor
                    )

                    // 剩余天数
                    remainingDays?.let { days ->
                        val daysColor = when {
                            days < 0 -> Color(0xFFF44336)
                            days <= 7 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val daysIcon = when {
                            days < 0 -> Icons.Default.Warning
                            days == 0 -> Icons.Default.Schedule
                            else -> Icons.Default.CalendarToday
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = daysIcon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = daysColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    days > 0 -> "剩余${days}天"
                                    days == 0 -> "今天截止"
                                    else -> "已逾期${-days}天"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = daysColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        categoryColor,
                                        categoryColor.copy(alpha = 0.7f)
                                    )
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            // 操作按钮（仅活跃目标显示）
            if (goal.status == GoalStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 添加子目标按钮（仅限前两级目标）
                    if (onAddSubGoal != null && indentLevel < 2) {
                        TextButton(
                            onClick = onAddSubGoal,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                Icons.Default.AddTask,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加子目标")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(
                            onClick = onUpdateProgress,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = categoryColor
                            )
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("更新进度")
                        }

                        if (progress >= 1f || goal.progressType == "PERCENTAGE") {
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = onComplete,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    contentColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("完成")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 迷你进度环
 */
@Composable
private fun MiniProgressRing(
    progress: Float,
    color: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp)
    ) {
        Canvas(modifier = Modifier.size(44.dp)) {
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(progress * 100).toInt()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 增强版空状态
 */
@Composable
private fun EnhancedEmptyState(currentFilter: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = when (currentFilter) {
                    "COMPLETED" -> "暂无已完成的目标"
                    else -> "暂无目标"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "设定目标，开启你的成长之旅",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UpdateProgressDialog(
    goal: GoalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    if (goal == null) return

    var value by remember { mutableStateOf(goal.currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新进度") },
        text = {
            Column {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                PremiumTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = if (goal.progressType == "NUMERIC") "当前数值" else "完成百分比",
                    trailingIcon = {
                        Text(
                            if (goal.progressType == "NUMERIC") goal.unit else "%"
                        )
                    },
                    singleLine = true
                )
                if (goal.progressType == "NUMERIC" && goal.targetValue != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "目标: ${goal.targetValue.toInt()}${goal.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    value.toDoubleOrNull()?.let { onConfirm(it) }
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

/**
 * 添加子目标对话框
 */
@Composable
private fun AddSubGoalDialog(
    state: SubGoalEditState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddTask,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加子目标")
            }
        },
        text = {
            Column {
                Text(
                    text = "为主目标添加可拆分的子目标，完成所有子目标后主目标将自动完成",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                PremiumTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = "子目标标题",
                    placeholder = "例如：完成技术方案评审",
                    singleLine = true,
                    isError = state.error != null
                )
                Spacer(modifier = Modifier.height(12.dp))
                PremiumTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = "描述（可选）",
                    placeholder = "详细说明这个子目标",
                    singleLine = false,
                    minLines = 2
                )
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "CAREER" -> Color(0xFF3B82F6)
        "FINANCE" -> Color(0xFF10B981)
        "HEALTH" -> Color(0xFFEC4899)
        "LEARNING" -> Color(0xFFF59E0B)
        "RELATIONSHIP" -> Color(0xFF8B5CF6)
        "LIFESTYLE" -> Color(0xFF06B6D4)
        "HOBBY" -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "CAREER" -> Icons.Default.Work
        "FINANCE" -> Icons.Default.AccountBalance
        "HEALTH" -> Icons.Default.FitnessCenter
        "LEARNING" -> Icons.Default.School
        "RELATIONSHIP" -> Icons.Default.People
        "LIFESTYLE" -> Icons.Default.Home
        "HOBBY" -> Icons.Default.Palette
        else -> Icons.Default.Flag
    }
}

/**
 * 截止日期警告卡片
 */
@Composable
private fun DeadlineWarningsCard(
    overdueGoals: List<GoalEntity>,
    upcomingDeadlines: List<GoalEntity>,
    onGoalClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (overdueGoals.isNotEmpty())
                Color(0xFFFEE2E2)
            else
                Color(0xFFFEF3C7)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 逾期目标
            if (overdueGoals.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${overdueGoals.size}个目标已逾期",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                overdueGoals.take(2).forEach { goal ->
                    DeadlineItem(
                        goal = goal,
                        isOverdue = true,
                        onClick = { onGoalClick(goal.id) }
                    )
                }
                if (overdueGoals.size > 2) {
                    Text(
                        text = "还有${overdueGoals.size - 2}个...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626).copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 28.dp, top = 4.dp)
                    )
                }
            }

            // 即将到期目标
            if (upcomingDeadlines.isNotEmpty()) {
                if (overdueGoals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFFBBF24).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${upcomingDeadlines.size}个目标即将到期",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                upcomingDeadlines.take(2).forEach { goal ->
                    DeadlineItem(
                        goal = goal,
                        isOverdue = false,
                        onClick = { onGoalClick(goal.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeadlineItem(
    goal: GoalEntity,
    isOverdue: Boolean,
    onClick: () -> Unit
) {
    val today = java.time.LocalDate.now().toEpochDay().toInt()
    val daysInfo = goal.endDate?.let { endDate ->
        val diff = endDate - today
        when {
            diff < 0 -> "逾期${-diff}天"
            diff == 0 -> "今天截止"
            else -> "剩余${diff}天"
        }
    } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    getCategoryColor(goal.category),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = goal.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            color = if (isOverdue) Color(0xFFDC2626) else Color(0xFFD97706)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = daysInfo,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOverdue) Color(0xFFDC2626) else Color(0xFFD97706)
        )
    }
}

/**
 * 推荐模板区域
 */
@Composable
private fun RecommendedTemplatesSection(
    templates: List<GoalTemplate>,
    onTemplateClick: (GoalTemplate) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "推荐目标",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onViewAllClick) {
                Text("查看全部", style = MaterialTheme.typography.labelMedium)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                TemplateQuickCard(
                    template = template,
                    onClick = { onTemplateClick(template) }
                )
            }
        }
    }
}

@Composable
private fun TemplateQuickCard(
    template: GoalTemplate,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(template.category)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = categoryColor.copy(alpha = 0.1f),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(template.category),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = template.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = getCategoryDisplayName(template.category),
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor
            )
        }
    }
}

/**
 * 分类统计卡片
 */
@Composable
private fun CategoryStatsCard(categoryStats: List<CategoryGoalStats>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "分类概况",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            categoryStats.take(4).forEach { stats ->
                CategoryStatItem(stats = stats)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CategoryStatItem(stats: CategoryGoalStats) {
    val color = try {
        Color(android.graphics.Color.parseColor(stats.color))
    } catch (e: Exception) {
        Color(0xFF6B7280)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(stats.category),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stats.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stats.activeCount}进行中 / ${stats.completedCount}已完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { stats.completionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
            )
        }
    }
}

/**
 * 模板选择对话框
 */
@Composable
private fun TemplateSelectionDialog(
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    onTemplateSelect: (GoalTemplate) -> Unit,
    onDismiss: () -> Unit,
    getTemplatesForCategory: (String) -> List<GoalTemplate>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择目标模板")
            }
        },
        text = {
            Column(
                modifier = Modifier.height(400.dp)
            ) {
                Text(
                    text = "选择一个分类查看可用模板",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 分类选择
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(goalCategoryOptions) { (category, name) ->
                        val isSelected = selectedCategory == category
                        val color = getCategoryColor(category)

                        FilterChip(
                            onClick = { onCategorySelect(if (isSelected) null else category) },
                            label = { Text(name) },
                            selected = isSelected,
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(category),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color,
                                selectedLeadingIconColor = color
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 模板列表
                if (selectedCategory != null) {
                    val templates = getTemplatesForCategory(selectedCategory)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { template ->
                            TemplateListItem(
                                template = template,
                                onClick = { onTemplateSelect(template) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "请先选择一个分类",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun TemplateListItem(
    template: GoalTemplate,
    onClick: () -> Unit
) {
    val color = getCategoryColor(template.category)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(template.category),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "使用模板",
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
