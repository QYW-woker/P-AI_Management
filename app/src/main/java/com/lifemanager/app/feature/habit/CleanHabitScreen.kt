package com.lifemanager.app.feature.habit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*

/**
 * 习惯打卡主界面 - 简洁设计版本
 *
 * 设计原则:
 * - 干净、克制、有呼吸感
 * - 轻灵不花哨
 * - 使用统一的设计系统
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanHabitScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToAdd: () -> Unit = {},
    viewModel: HabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val habitAnalysis by viewModel.habitAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val habitRanking by viewModel.habitRanking.collectAsState()
    val retroCheckinState by viewModel.retroCheckinState.collectAsState()

    Scaffold(
        containerColor = CleanColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "习惯打卡",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CleanColors.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = CleanColors.primary,
                contentColor = CleanColors.onPrimary,
                shape = RoundedCornerShape(Radius.md)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加习惯")
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is HabitUiState.Loading -> {
                PageLoadingState(modifier = Modifier.padding(paddingValues))
            }

            is HabitUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as HabitUiState.Error).message,
                            style = CleanTypography.body,
                            color = CleanColors.error
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        CleanSecondaryButton(
                            text = "重试",
                            onClick = { viewModel.refresh() }
                        )
                    }
                }
            }

            is HabitUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        horizontal = Spacing.pageHorizontal,
                        vertical = Spacing.pageVertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
                ) {
                    // 简洁统计卡片
                    item(key = "stats") {
                        CleanHabitStatsCard(stats = stats)
                    }

                    // 周度统计
                    weeklyStats?.let { weekly ->
                        item(key = "weekly") {
                            CleanWeeklyStatsCard(weeklyStats = weekly)
                        }
                    }

                    // AI智能洞察
                    item(key = "ai_insight") {
                        CleanAIInsightCard(
                            analysis = habitAnalysis?.content,
                            isLoading = isAnalyzing,
                            onRefresh = { viewModel.refreshAIAnalysis() }
                        )
                    }

                    // 习惯列表
                    item(key = "habits_header") {
                        Text(
                            text = "今日习惯",
                            style = CleanTypography.title,
                            color = CleanColors.textPrimary
                        )
                    }

                    if (habits.isEmpty()) {
                        item(key = "empty") {
                            EmptyStateView(
                                message = "暂无习惯",
                                icon = Icons.Outlined.Loop,
                                actionText = "添加第一个习惯",
                                onActionClick = onNavigateToAdd
                            )
                        }
                    } else {
                        items(habits, key = { it.habit.id }) { habitWithStatus ->
                            CleanHabitItem(
                                habitWithStatus = habitWithStatus,
                                onCheckIn = { viewModel.toggleCheckIn(habitWithStatus.habit.id) },
                                onClick = { onNavigateToDetail(habitWithStatus.habit.id) },
                                onDelete = { viewModel.showDeleteConfirm(habitWithStatus.habit.id) }
                            )
                        }
                    }

                    // 底部安全间距
                    item {
                        Spacer(modifier = Modifier.height(Spacing.bottomSafe + 56.dp))
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AddEditHabitDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        CleanDeleteDialog(
            title = "确认删除",
            message = "删除习惯将同时删除所有打卡记录，确定要删除吗？",
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.hideDeleteConfirm() }
        )
    }

    // 补打卡对话框
    if (retroCheckinState.isShowing) {
        CleanRetroCheckinDialog(
            state = retroCheckinState,
            onNoteChange = { viewModel.updateRetroCheckinNote(it) },
            onConfirm = { viewModel.performRetroCheckin() },
            onDismiss = { viewModel.hideRetroCheckinDialog() }
        )
    }
}

/**
 * 简洁统计卡片 - 无渐变，干净的数据展示
 */
@Composable
private fun CleanHabitStatsCard(stats: HabitStats) {
    val progress = if (stats.todayTotal > 0) stats.todayCompleted.toFloat() / stats.todayTotal else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日打卡",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = CleanTypography.secondary,
                    color = CleanColors.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 简洁进度条
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CleanColors.primary,
                trackColor = CleanColors.borderLight
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CleanStatItem(
                    label = "今日完成",
                    value = "${stats.todayCompleted}/${stats.todayTotal}",
                    icon = Icons.Outlined.CheckCircle
                )
                CleanStatItem(
                    label = "最长连续",
                    value = "${stats.longestStreak}天",
                    icon = Icons.Outlined.LocalFireDepartment
                )
                CleanStatItem(
                    label = "活跃习惯",
                    value = "${stats.todayTotal}个",
                    icon = Icons.Outlined.Loop
                )
            }
        }
    }
}

@Composable
private fun CleanStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = CleanColors.textPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CleanColors.textTertiary,
            modifier = Modifier.size(IconSize.md)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = value,
            style = CleanTypography.amountMedium,
            color = valueColor
        )
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textTertiary
        )
    }
}

/**
 * 简洁周度统计卡片
 */
@Composable
private fun CleanWeeklyStatsCard(weeklyStats: WeeklyHabitStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weeklyStats.weekLabel,
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
                if (weeklyStats.isCurrentWeek) {
                    StatusTag(text = "本周", color = CleanColors.primary)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // 完成率进度条
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "完成率",
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary
                )
                Text(
                    text = "${(weeklyStats.completionRate * 100).toInt()}%",
                    style = CleanTypography.secondary,
                    color = CleanColors.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            LinearProgressIndicator(
                progress = weeklyStats.completionRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CleanColors.primary,
                trackColor = CleanColors.borderLight
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // 每日打卡柱状图
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weeklyStats.dailyData.forEach { daily ->
                    CleanDailyBarItem(
                        dayLabel = daily.dayLabel,
                        completed = daily.completed,
                        total = daily.total,
                        isToday = daily.isToday
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanDailyBarItem(
    dayLabel: String,
    completed: Int,
    total: Int,
    isToday: Boolean
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val barColor = when {
        isToday -> CleanColors.primary
        progress >= 1f -> CleanColors.success
        progress > 0f -> CleanColors.warning
        else -> CleanColors.borderLight
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CleanColors.borderLight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .background(barColor, RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = dayLabel,
            style = CleanTypography.caption,
            color = if (isToday) CleanColors.primary else CleanColors.textTertiary
        )
    }
}

/**
 * 简洁AI洞察卡片
 */
@Composable
private fun CleanAIInsightCard(
    analysis: String?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.primaryLight
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = CleanColors.primary,
                        modifier = Modifier.size(IconSize.md)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "AI 洞察",
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "刷新",
                        tint = CleanColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CleanLoadingIndicator(size = 20.dp)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "正在分析...",
                        style = CleanTypography.secondary,
                        color = CleanColors.textTertiary
                    )
                }
            } else {
                Text(
                    text = analysis ?: "暂无分析数据，点击刷新获取",
                    style = CleanTypography.body,
                    color = CleanColors.textSecondary
                )
            }
        }
    }
}

/**
 * 简洁习惯项
 */
@Composable
private fun CleanHabitItem(
    habitWithStatus: HabitWithStatus,
    onCheckIn: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = habitWithStatus.habit
    val isChecked = habitWithStatus.isCheckedToday

    val habitColor = try {
        Color(android.graphics.Color.parseColor(habit.color))
    } catch (e: Exception) {
        CleanColors.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        color = if (isChecked) habitColor.copy(alpha = 0.08f) else CleanColors.surface,
        shadowElevation = if (isChecked) Elevation.none else Elevation.xs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 打卡按钮
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isChecked) habitColor else habitColor.copy(alpha = 0.12f)
                    )
                    .clickable(onClick = onCheckIn),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isChecked,
                    transitionSpec = {
                        scaleIn() togetherWith scaleOut()
                    },
                    label = "checkIcon"
                ) { checked ->
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = if (checked) "已打卡" else "打卡",
                        tint = if (checked) Color.White else habitColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            // 习惯信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = CleanTypography.body,
                    color = if (isChecked) habitColor else CleanColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // 频率标签
                    Text(
                        text = getFrequencyDisplayText(habit.frequency, habit.targetTimes),
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )

                    // 连续天数
                    if (habitWithStatus.streak > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = CleanColors.warning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${habitWithStatus.streak}天",
                                style = CleanTypography.caption,
                                color = CleanColors.warning
                            )
                        }
                    }
                }
            }

            // 箭头指示
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CleanColors.textTertiary,
                modifier = Modifier.size(IconSize.sm)
            )
        }
    }
}

/**
 * 删除确认对话框
 */
@Composable
private fun CleanDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = CleanTypography.body,
                color = CleanColors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "删除",
                    style = CleanTypography.button,
                    color = CleanColors.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    style = CleanTypography.button,
                    color = CleanColors.textSecondary
                )
            }
        },
        containerColor = CleanColors.surface,
        shape = RoundedCornerShape(Radius.lg)
    )
}

/**
 * 补打卡对话框
 */
@Composable
private fun CleanRetroCheckinDialog(
    state: RetroCheckinState,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = CleanColors.primary,
                    modifier = Modifier.size(IconSize.md)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "补打卡",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "为之前的日期补打卡",
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                CleanTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = "备注（可选）",
                    singleLine = false
                )
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = state.error,
                        style = CleanTypography.caption,
                        color = CleanColors.error
                    )
                }
            }
        },
        confirmButton = {
            if (state.isSaving) {
                CleanLoadingIndicator(size = 20.dp)
            } else {
                CleanPrimaryButton(
                    text = "确认",
                    onClick = onConfirm
                )
            }
        },
        dismissButton = {
            CleanTextButton(
                text = "取消",
                onClick = onDismiss,
                color = CleanColors.textSecondary
            )
        },
        containerColor = CleanColors.surface,
        shape = RoundedCornerShape(Radius.lg)
    )
}
