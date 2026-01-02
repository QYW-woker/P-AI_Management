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
import com.lifemanager.app.domain.model.GoalStructureType
import com.lifemanager.app.domain.model.GoalTreeNode
import com.lifemanager.app.domain.model.GoalUiState
import com.lifemanager.app.domain.model.getCategoryDisplayName
import com.lifemanager.app.domain.model.getGoalTypeDisplayName

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

    // 是否使用树形视图
    var useTreeView by remember { mutableStateOf(true) }

    // 目标类型选择弹窗状态
    var showTypeSelectSheet by remember { mutableStateOf(false) }

    // 初始化时加载目标树
    LaunchedEffect(currentFilter) {
        viewModel.loadGoalTree()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "目标管理",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
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
                                contentDescription = if (expandedGoalIds.isEmpty()) "展开全部" else "收起全部"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypeSelectSheet = true }
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
                        CircularProgressIndicator()
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
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadGoalTree() }) {
                                Text("重试")
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
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
}

@Composable
private fun StatsCard(
    statistics: com.lifemanager.app.domain.model.GoalStatistics
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "进行中",
                value = statistics.activeCount.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "已完成",
                value = statistics.completedCount.toString(),
                color = Color(0xFF4CAF50)
            )
            StatItem(
                label = "平均进度",
                value = "${(statistics.totalProgress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.tertiary
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChips(
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filters = listOf(
        "ACTIVE" to "进行中",
        "COMPLETED" to "已完成",
        "ALL" to "全部"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (value, label) ->
            FilterChip(
                selected = currentFilter == value,
                onClick = { onFilterChange(value) },
                label = { Text(label) },
                leadingIcon = if (currentFilter == value) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
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
    val indentDp = (treeNode.level * 24).dp

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
                            tint = MaterialTheme.colorScheme.primary
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
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }

        // 目标卡片
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (treeNode.level > 0) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (treeNode.level == 0) 2.dp else 0.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(if (treeNode.level > 0) 12.dp else 16.dp)
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
                            shape = RoundedCornerShape(4.dp),
                            color = categoryColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = getCategoryDisplayName(goal.category),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor
                            )
                        }

                        // 多级目标标识
                        if (hasChildren) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${treeNode.childCount}个子目标",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 状态指示
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (goal.status == GoalStatus.COMPLETED) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已完成",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "查看详情",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 标题
                Text(
                    text = goal.title,
                    style = if (treeNode.level > 0) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (goal.description.isNotBlank() && treeNode.level == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        remainingDays?.let { days ->
                            Text(
                                text = when {
                                    days > 0 -> "剩余${days}天"
                                    days == 0 -> "今天截止"
                                    else -> "已逾期${-days}天"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (days < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (treeNode.level > 0) 6.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = categoryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
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
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (currentFilter) {
                    "COMPLETED" -> "暂无已完成的目标"
                    else -> "暂无目标"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮创建新目标",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
