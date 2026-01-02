package com.lifemanager.app.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.navigation.Screen
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 首页 - 简洁设计版本
 *
 * 设计原则:
 * - 干净、克制、有呼吸感
 * - 轻灵不花哨
 * - 工具感但不冰冷
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanHomeScreen(
    onNavigateToModule: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val monthlyFinance by viewModel.monthlyFinance.collectAsState()
    val topGoals by viewModel.topGoals.collectAsState()
    val homeCardConfig by viewModel.homeCardConfig.collectAsState()

    val today = remember { LocalDate.now() }
    val greeting = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> "早上好"
            in 12..13 -> "中午好"
            in 14..17 -> "下午好"
            else -> "晚上好"
        }
    }

    Scaffold(
        containerColor = CleanColors.background,
        topBar = {
            CleanTopBar(
                greeting = greeting,
                today = today,
                onSettingsClick = { onNavigateToModule(Screen.Settings.route) }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            PageLoadingState(modifier = Modifier.padding(paddingValues))
        } else {
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
                // 今日概览统计
                if (homeCardConfig.showTodayStats) {
                    item(key = "today_stats") {
                        TodayStatsSection(
                            stats = todayStats,
                            onNavigateToModule = onNavigateToModule
                        )
                    }
                }

                // 快捷入口
                if (homeCardConfig.showQuickActions) {
                    item(key = "quick_actions") {
                        QuickActionsSection(onNavigateToModule = onNavigateToModule)
                    }
                }

                // 本月财务
                if (homeCardConfig.showMonthlyFinance) {
                    item(key = "monthly_finance") {
                        MonthlyFinanceSection(
                            finance = monthlyFinance,
                            onClick = { onNavigateToModule(Screen.AccountingMain.route) }
                        )
                    }
                }

                // 目标进度
                if (homeCardConfig.showTopGoals && topGoals.isNotEmpty()) {
                    item(key = "goals") {
                        GoalsProgressSection(
                            goals = topGoals,
                            onClick = { onNavigateToModule(Screen.Goal.route) }
                        )
                    }
                }

                // AI 助手入口
                if (homeCardConfig.showAIInsight) {
                    item(key = "ai_assistant") {
                        AIAssistantSection(
                            onClick = { onNavigateToModule(Screen.AIAssistant.route) }
                        )
                    }
                }

                // 底部安全间距
                item {
                    Spacer(modifier = Modifier.height(Spacing.bottomSafe))
                }
            }
        }
    }
}

/**
 * 简洁顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanTopBar(
    greeting: String,
    today: LocalDate,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = greeting,
                    style = CleanTypography.headline,
                    color = CleanColors.textPrimary
                )
                Text(
                    text = "${today.monthValue}月${today.dayOfMonth}日 ${today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}",
                    style = CleanTypography.caption,
                    color = CleanColors.textTertiary
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "设置",
                    tint = CleanColors.textSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CleanColors.background
        )
    )
}

/**
 * 今日统计区域
 */
@Composable
private fun TodayStatsSection(
    stats: TodayStatsData,
    onNavigateToModule: (String) -> Unit
) {
    Column {
        SectionHeader(
            title = "今日概览",
            action = "更多",
            onActionClick = { onNavigateToModule(Screen.DataCenter.route) }
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 待办统计
            StatCard(
                title = "待办完成",
                value = "${stats.completedTodos}/${stats.totalTodos}",
                icon = Icons.Outlined.CheckCircle,
                iconTint = CleanColors.success,
                progress = if (stats.totalTodos > 0)
                    stats.completedTodos.toFloat() / stats.totalTodos else 0f,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToModule(Screen.Todo.route) }
            )

            // 习惯打卡
            StatCard(
                title = "习惯打卡",
                value = "${stats.completedHabits}/${stats.totalHabits}",
                icon = Icons.Outlined.Loop,
                iconTint = CleanColors.primary,
                progress = if (stats.totalHabits > 0)
                    stats.completedHabits.toFloat() / stats.totalHabits else 0f,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToModule(Screen.Habit.route) }
            )
        }
    }
}

/**
 * 统计卡片
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(IconSize.md)
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = CleanTypography.caption,
                    color = CleanColors.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = value,
                style = CleanTypography.amountMedium,
                color = CleanColors.textPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = title,
                style = CleanTypography.secondary,
                color = CleanColors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // 简洁进度条
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = iconTint,
                trackColor = CleanColors.borderLight
            )
        }
    }
}

/**
 * 快捷入口区域
 */
@Composable
private fun QuickActionsSection(
    onNavigateToModule: (String) -> Unit
) {
    val actions = listOf(
        QuickAction("记账", Icons.Outlined.AccountBalanceWallet, Screen.AccountingMain.route),
        QuickAction("待办", Icons.Outlined.CheckBox, Screen.Todo.route),
        QuickAction("习惯", Icons.Outlined.Loop, Screen.Habit.route),
        QuickAction("日记", Icons.Outlined.Book, Screen.Diary.route),
        QuickAction("健康", Icons.Outlined.MonitorHeart, Screen.HealthRecord.route),
        QuickAction("存钱", Icons.Outlined.Savings, Screen.SavingsPlan.route)
    )

    Column {
        Text(
            text = "快捷入口",
            style = CleanTypography.title,
            color = CleanColors.textPrimary,
            modifier = Modifier.padding(horizontal = Spacing.pageHorizontal)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(actions.size) { index ->
                QuickActionItem(
                    action = actions[index],
                    onClick = { onNavigateToModule(actions[index].route) }
                )
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
private fun QuickActionItem(
    action: QuickAction,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(Radius.md),
            color = CleanColors.primaryLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = CleanColors.primary,
                    modifier = Modifier.size(IconSize.md)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = action.label,
            style = CleanTypography.caption,
            color = CleanColors.textSecondary
        )
    }
}

/**
 * 本月财务区域
 */
@Composable
private fun MonthlyFinanceSection(
    finance: MonthlyFinanceData,
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = "本月财务",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CleanColors.textTertiary,
                    modifier = Modifier.size(IconSize.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 收入
                FinanceItem(
                    label = "收入",
                    amount = finance.totalIncome,
                    isIncome = true,
                    numberFormat = numberFormat
                )

                // 支出
                FinanceItem(
                    label = "支出",
                    amount = finance.totalExpense,
                    isIncome = false,
                    numberFormat = numberFormat
                )

                // 结余
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "结余",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "¥${numberFormat.format(finance.balance.toInt())}",
                        style = CleanTypography.amountMedium,
                        color = if (finance.balance >= 0) CleanColors.textPrimary else CleanColors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceItem(
    label: String,
    amount: Double,
    isIncome: Boolean,
    numberFormat: NumberFormat
) {
    Column {
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textTertiary
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "¥${numberFormat.format(amount.toInt())}",
            style = CleanTypography.amountMedium,
            color = if (isIncome) CleanColors.success else CleanColors.error
        )
    }
}

/**
 * 目标进度区域
 */
@Composable
private fun GoalsProgressSection(
    goals: List<GoalProgressData>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = "目标进度",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CleanColors.textTertiary,
                    modifier = Modifier.size(IconSize.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            goals.forEachIndexed { index, goal ->
                GoalItem(goal = goal)
                if (index < goals.lastIndex) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }
        }
    }
}

@Composable
private fun GoalItem(goal: GoalProgressData) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = goal.title,
                style = CleanTypography.body,
                color = CleanColors.textPrimary
            )
            Text(
                text = goal.progressText,
                style = CleanTypography.secondary,
                color = CleanColors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        LinearProgressIndicator(
            progress = goal.progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = CleanColors.primary,
            trackColor = CleanColors.borderLight
        )
    }
}

/**
 * AI 助手入口
 */
@Composable
private fun AIAssistantSection(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.primaryLight
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(Radius.md),
                color = CleanColors.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = CleanColors.onPrimary,
                        modifier = Modifier.size(IconSize.md)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI 助手",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
                Text(
                    text = "语音记录、智能分析、数据洞察",
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CleanColors.textTertiary
            )
        }
    }
}
