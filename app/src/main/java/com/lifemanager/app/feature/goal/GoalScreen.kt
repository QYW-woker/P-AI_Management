package com.lifemanager.app.feature.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.domain.model.AIAnalysisState
import com.lifemanager.app.domain.model.GoalStructureType
import com.lifemanager.app.domain.model.GoalTreeNode
import com.lifemanager.app.domain.model.GoalUiState
import com.lifemanager.app.domain.model.getCategoryDisplayName
import com.lifemanager.app.domain.model.getGoalTypeDisplayName
import com.lifemanager.app.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * 目标管理页面
 *
 * 重构后使用页面导航代替弹窗，支持树形展示多级目标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToAdd: () -> Unit = {},
    onNavigateToAddMultiLevel: () -> Unit = {},
    viewModel: GoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val flattenedGoals by viewModel.flattenedGoals.collectAsState()
    val expandedGoalIds by viewModel.expandedGoalIds.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val aiAnalysisState by viewModel.aiAnalysisState.collectAsState()

    // 是否使用树形视图
    var useTreeView by remember { mutableStateOf(true) }

    // 目标类型选择弹窗状态
    var showTypeSelectSheet by remember { mutableStateOf(false) }

    // AI分析弹窗状态
    var showAIAnalysisSheet by remember { mutableStateOf(false) }

    // 初始化时加载目标树
    LaunchedEffect(currentFilter) {
        viewModel.loadGoalTree()
    }

    Scaffold(
        containerColor = CleanColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "目标管理",
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = CleanColors.textPrimary
                        )
                    }
                },
                actions = {
                    // AI分析按钮
                    if (viewModel.isAIConfigured() && flattenedGoals.isNotEmpty()) {
                        IconButton(
                            onClick = { showAIAnalysisSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI分析",
                                tint = CleanColors.primary
                            )
                        }
                    }
                    // 展开/收起所有按钮
                    if (useTreeView && flattenedGoals.any { it.childCount > 0 }) {
                        IconButton(
                            onClick = {
                                if (expandedGoalIds.isEmpty()) {
                                    viewModel.expandAll()
                                } else {
                                    viewModel.collapseAll()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (expandedGoalIds.isEmpty()) {
                                    Icons.Default.UnfoldMore
                                } else {
                                    Icons.Default.UnfoldLess
                                },
                                contentDescription = if (expandedGoalIds.isEmpty()) "展开全部" else "收起全部",
                                tint = CleanColors.textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CleanColors.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypeSelectSheet = true },
                containerColor = CleanColors.primary,
                contentColor = CleanColors.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加目标")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 统计卡片
            StatsCard(statistics = statistics)

            // 筛选标签
            FilterChips(
                currentFilter = currentFilter,
                onFilterChange = {
                    viewModel.setFilter(it)
                    viewModel.loadGoalTree()
                }
            )

            // 目标列表
            when (uiState) {
                is GoalUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CleanColors.primary)
                    }
                }

                is GoalUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as GoalUiState.Error).message,
                                style = CleanTypography.body,
                                color = CleanColors.error
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            Button(
                                onClick = { viewModel.loadGoalTree() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanColors.primary,
                                    contentColor = CleanColors.onPrimary
                                )
                            ) {
                                Text("重试", style = CleanTypography.button)
                            }
                        }
                    }
                }

                is GoalUiState.Success -> {
                    if (flattenedGoals.isEmpty()) {
                        EmptyState(currentFilter = currentFilter)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Spacing.pageHorizontal,
                                vertical = Spacing.md
                            ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(flattenedGoals, key = { it.goal.id }) { treeNode ->
                                GoalTreeCard(
                                    treeNode = treeNode,
                                    isExpanded = expandedGoalIds.contains(treeNode.goal.id),
                                    onToggleExpand = { viewModel.toggleExpand(treeNode.goal.id) },
                                    remainingDays = viewModel.getRemainingDays(treeNode.goal),
                                    onClick = { onNavigateToDetail(treeNode.goal.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 目标类型选择弹窗
    if (showTypeSelectSheet) {
        GoalTypeSelectSheet(
            onDismiss = { showTypeSelectSheet = false },
            onTypeSelected = { type ->
                showTypeSelectSheet = false
                when (type) {
                    GoalStructureType.SINGLE -> onNavigateToAdd()
                    GoalStructureType.MULTI_LEVEL -> onNavigateToAddMultiLevel()
                }
            }
        )
    }

    // AI分析弹窗
    if (showAIAnalysisSheet) {
        AIAnalysisBottomSheet(
            statistics = statistics,
            aiAnalysisState = aiAnalysisState,
            onAnalyze = {
                // 分析所有活跃目标的第一个
                flattenedGoals.firstOrNull()?.let { node ->
                    viewModel.analyzeGoal(node.goal.id)
                }
            },
            onDismiss = {
                showAIAnalysisSheet = false
                viewModel.clearAIAnalysis()
            }
        )
    }
}

@Composable
private fun StatsCard(
    statistics: com.lifemanager.app.domain.model.GoalStatistics
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageHorizontal, vertical = Spacing.md),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.primaryLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "进行中",
                value = statistics.activeCount.toString(),
                color = CleanColors.primary
            )
            StatItem(
                label = "已完成",
                value = statistics.completedCount.toString(),
                color = CleanColors.success
            )
            StatItem(
                label = "平均进度",
                value = "${(statistics.totalProgress * 100).toInt()}%",
                color = CleanColors.warning
            )
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
            style = CleanTypography.amountMedium,
            color = color
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChips(
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    // 筛选项配置：值、标签、图标
    data class FilterItem(
        val value: String,
        val label: String,
        val icon: ImageVector
    )

    val filters = listOf(
        FilterItem("ACTIVE", "进行中", Icons.Default.PlayArrow),
        FilterItem("COMPLETED", "已完成", Icons.Default.CheckCircle),
        FilterItem("ABANDONED", "已放弃", Icons.Default.Cancel),
        FilterItem("ALL", "全部", Icons.Default.List)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.pageHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(filters) { filter ->
            val isSelected = currentFilter == filter.value
            val containerColor = when {
                !isSelected -> CleanColors.surface
                filter.value == "ABANDONED" -> CleanColors.errorLight
                filter.value == "COMPLETED" -> CleanColors.successLight
                else -> CleanColors.primaryLight
            }
            val contentColor = when {
                !isSelected -> CleanColors.textSecondary
                filter.value == "ABANDONED" -> CleanColors.error
                filter.value == "COMPLETED" -> CleanColors.success
                else -> CleanColors.primary
            }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterChange(filter.value) },
                label = {
                    Text(
                        text = filter.label,
                        style = CleanTypography.button
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    labelColor = contentColor,
                    iconColor = contentColor,
                    selectedContainerColor = containerColor,
                    selectedLabelColor = contentColor,
                    selectedLeadingIconColor = contentColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) Color.Transparent else CleanColors.borderLight,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * 树形目标卡片 - 支持展开/收起子目标
 */
@Composable
private fun GoalTreeCard(
    treeNode: GoalTreeNode,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    remainingDays: Int?,
    onClick: () -> Unit
) {
    val goal = treeNode.goal
    val progress = treeNode.progress
    val categoryColor = getCategoryColor(goal.category)
    val hasChildren = treeNode.childCount > 0

    // 根据层级计算缩进
    val indentDp = (treeNode.level * Spacing.xl.value).dp

    // 展开箭头旋转动画
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "expand_arrow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentDp),
        verticalAlignment = Alignment.Top
    ) {
        // 树形连接线和展开按钮
        if (treeNode.level > 0 || hasChildren) {
            Column(
                modifier = Modifier.width(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasChildren) {
                    // 有子目标时显示展开/收起按钮
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotationAngle),
                            tint = CleanColors.primary
                        )
                    }
                } else if (treeNode.level > 0) {
                    // 子目标但无下级子目标，显示圆点
                    Box(
                        modifier = Modifier
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CleanColors.borderLight)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }

        // 目标卡片
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(Radius.md),
            color = if (treeNode.level > 0) {
                CleanColors.surfaceVariant
            } else {
                CleanColors.surface
            },
            shadowElevation = if (treeNode.level == 0) Elevation.xs else Elevation.none
        ) {
            Column(
                modifier = Modifier.padding(if (treeNode.level > 0) Spacing.md else Spacing.lg)
            ) {
                // 头部：分类标签和状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 分类标签
                        Surface(
                            shape = RoundedCornerShape(Radius.sm),
                            color = categoryColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = getCategoryDisplayName(goal.category),
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                                style = CleanTypography.caption,
                                color = categoryColor
                            )
                        }

                        // 多级目标标识
                        if (hasChildren) {
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Surface(
                                shape = RoundedCornerShape(Radius.sm),
                                color = CleanColors.primaryLight
                            ) {
                                Text(
                                    text = "${treeNode.childCount}个子目标",
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                                    style = CleanTypography.caption,
                                    color = CleanColors.primary
                                )
                            }
                        }
                    }

                    // 状态指示
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (goal.status) {
                            GoalStatus.COMPLETED -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "已完成",
                                    tint = CleanColors.success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                            }
                            GoalStatus.ABANDONED -> {
                                Surface(
                                    shape = RoundedCornerShape(Radius.sm),
                                    color = CleanColors.errorLight
                                ) {
                                    Text(
                                        text = "已放弃",
                                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                                        style = CleanTypography.caption,
                                        color = CleanColors.error
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.xs))
                            }
                            else -> {}
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "查看详情",
                            tint = CleanColors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // 标题
                Text(
                    text = goal.title,
                    style = if (treeNode.level > 0) CleanTypography.body else CleanTypography.title,
                    color = CleanColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (goal.description.isNotBlank() && treeNode.level == 0) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = goal.description,
                        style = CleanTypography.caption,
                        color = CleanColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // 进度条
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (goal.progressType == "NUMERIC" && goal.targetValue != null && !hasChildren) {
                                "${goal.currentValue.toInt()}${goal.unit} / ${goal.targetValue.toInt()}${goal.unit}"
                            } else {
                                "${(progress * 100).toInt()}%"
                            },
                            style = CleanTypography.caption,
                            color = CleanColors.textPrimary
                        )
                        remainingDays?.let { days ->
                            Text(
                                text = when {
                                    days > 0 -> "剩余${days}天"
                                    days == 0 -> "今天截止"
                                    else -> "已逾期${-days}天"
                                },
                                style = CleanTypography.caption,
                                color = if (days < 0) CleanColors.error else CleanColors.textTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (treeNode.level > 0) 6.dp else 8.dp)
                            .clip(RoundedCornerShape(Radius.sm)),
                        color = categoryColor,
                        trackColor = CleanColors.borderLight
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(currentFilter: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when (currentFilter) {
                    "ABANDONED" -> Icons.Default.Block
                    "COMPLETED" -> Icons.Default.CheckCircle
                    else -> Icons.Default.Flag
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CleanColors.textTertiary
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = when (currentFilter) {
                    "COMPLETED" -> "暂无已完成的目标"
                    "ABANDONED" -> "暂无已放弃的目标"
                    else -> "暂无目标"
                },
                style = CleanTypography.body,
                color = CleanColors.textSecondary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = when (currentFilter) {
                    "ABANDONED" -> "放弃的目标会显示在这里"
                    else -> "点击右下角按钮创建新目标"
                },
                style = CleanTypography.secondary,
                color = CleanColors.textTertiary
            )
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "CAREER" -> Color(0xFF2196F3)
        "FINANCE" -> Color(0xFF4CAF50)
        "HEALTH" -> Color(0xFFE91E63)
        "LEARNING" -> Color(0xFFFF9800)
        "RELATIONSHIP" -> Color(0xFF9C27B0)
        "LIFESTYLE" -> Color(0xFF00BCD4)
        "HOBBY" -> Color(0xFFFF5722)
        else -> Color.Gray
    }
}

/**
 * AI分析底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIAnalysisBottomSheet(
    statistics: com.lifemanager.app.domain.model.GoalStatistics,
    aiAnalysisState: AIAnalysisState,
    onAnalyze: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl),
        containerColor = CleanColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.pageHorizontal)
                .padding(bottom = Spacing.xxl)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CleanColors.primary,
                    modifier = Modifier.size(IconSize.md)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "AI 目标分析",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 目标概况卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md),
                color = CleanColors.primaryLight
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    Text(
                        text = "当前目标概况",
                        style = CleanTypography.secondary,
                        color = CleanColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${statistics.activeCount}",
                                style = CleanTypography.amountMedium,
                                color = CleanColors.primary
                            )
                            Text(
                                text = "进行中",
                                style = CleanTypography.caption,
                                color = CleanColors.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${statistics.completedCount}",
                                style = CleanTypography.amountMedium,
                                color = CleanColors.success
                            )
                            Text(
                                text = "已完成",
                                style = CleanTypography.caption,
                                color = CleanColors.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(statistics.totalProgress * 100).toInt()}%",
                                style = CleanTypography.amountMedium,
                                color = CleanColors.warning
                            )
                            Text(
                                text = "平均进度",
                                style = CleanTypography.caption,
                                color = CleanColors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // AI分析结果或按钮
            when (aiAnalysisState) {
                is AIAnalysisState.Idle -> {
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanColors.primary,
                            contentColor = CleanColors.onPrimary
                        ),
                        shape = RoundedCornerShape(Radius.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.sm)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "开始 AI 分析",
                            style = CleanTypography.button
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "AI将分析您的目标进度并给出优化建议",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is AIAnalysisState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = CleanColors.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "AI 正在分析您的目标...",
                                style = CleanTypography.secondary,
                                color = CleanColors.textSecondary
                            )
                        }
                    }
                }

                is AIAnalysisState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.md),
                        color = CleanColors.successLight
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(Spacing.lg)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = CleanColors.success,
                                    modifier = Modifier.size(IconSize.sm)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = "AI 分析建议",
                                    style = CleanTypography.button,
                                    color = CleanColors.success
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = aiAnalysisState.analysis,
                                style = CleanTypography.body,
                                color = CleanColors.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                    OutlinedButton(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.md),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CleanColors.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.sm)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "重新分析",
                            style = CleanTypography.button
                        )
                    }
                }

                is AIAnalysisState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.md),
                        color = CleanColors.errorLight
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CleanColors.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = aiAnalysisState.message,
                                style = CleanTypography.secondary,
                                color = CleanColors.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanColors.primary,
                            contentColor = CleanColors.onPrimary
                        ),
                        shape = RoundedCornerShape(Radius.md)
                    ) {
                        Text(
                            text = "重试",
                            style = CleanTypography.button
                        )
                    }
                }
            }
        }
    }
}
