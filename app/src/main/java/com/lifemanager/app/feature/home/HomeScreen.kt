package com.lifemanager.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifemanager.app.ui.component.card.StatCard
import com.lifemanager.app.ui.navigation.Screen
import com.lifemanager.app.ui.theme.AppColors

/**
 * 首页屏幕
 *
 * 展示今日概览和快捷入口
 *
 * @param onNavigateToModule 导航到指定模块的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToModule: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI生活管家",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigateToModule(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
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
            // 今日概览卡片
            item {
                TodayOverviewSection()
            }

            // 快捷功能入口
            item {
                QuickAccessSection(onNavigateToModule = onNavigateToModule)
            }

            // 本月财务概览
            item {
                MonthlyFinanceSection(onNavigateToModule = onNavigateToModule)
            }

            // 目标进度
            item {
                GoalProgressSection(onNavigateToModule = onNavigateToModule)
            }

            // AI建议卡片
            item {
                AISuggestionCard(onNavigateToAI = { onNavigateToModule(Screen.AIAssistant.route) })
            }
        }
    }
}

/**
 * 今日概览部分
 */
@Composable
private fun TodayOverviewSection() {
    Column {
        Text(
            text = "今日概览",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatCard(
                    title = "待办完成",
                    value = "3/5",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.width(140.dp)
                )
            }
            item {
                StatCard(
                    title = "今日消费",
                    value = "¥126",
                    valueColor = AppColors.Expense,
                    icon = Icons.Default.ShoppingCart,
                    modifier = Modifier.width(140.dp)
                )
            }
            item {
                StatCard(
                    title = "专注时长",
                    value = "2h 30m",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.width(140.dp)
                )
            }
            item {
                StatCard(
                    title = "习惯打卡",
                    value = "4/6",
                    icon = Icons.Default.Verified,
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

/**
 * 快捷功能入口
 */
@Composable
private fun QuickAccessSection(
    onNavigateToModule: (String) -> Unit
) {
    // 快捷功能列表
    val quickAccessItems = listOf(
        QuickAccessItem(
            icon = Icons.Default.AutoAwesome,
            label = "AI助手",
            color = Color(0xFF2196F3),
            route = Screen.AIAssistant.route
        ),
        QuickAccessItem(
            icon = Icons.Default.AccountBalance,
            label = "记账",
            color = AppColors.Primary,
            route = Screen.AccountingMain.route
        ),
        QuickAccessItem(
            icon = Icons.Default.Assignment,
            label = "待办",
            color = AppColors.Secondary,
            route = Screen.Todo.route
        ),
        QuickAccessItem(
            icon = Icons.Default.Schedule,
            label = "计时",
            color = AppColors.Tertiary,
            route = Screen.TimeTrack.route
        ),
        QuickAccessItem(
            icon = Icons.Default.CheckCircle,
            label = "打卡",
            color = Color(0xFF9C27B0),
            route = Screen.Habit.route
        ),
        QuickAccessItem(
            icon = Icons.Default.Book,
            label = "日记",
            color = Color(0xFFE91E63),
            route = Screen.Diary.route
        ),
        QuickAccessItem(
            icon = Icons.Default.Savings,
            label = "存钱",
            color = Color(0xFF00BCD4),
            route = Screen.SavingsPlan.route
        ),
        QuickAccessItem(
            icon = Icons.Default.AccountBalanceWallet,
            label = "预算",
            color = Color(0xFF673AB7),
            route = Screen.Budget.route
        )
    )

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
            quickAccessItems.forEach { item ->
                QuickAccessButton(
                    item = item,
                    onClick = { onNavigateToModule(item.route) }
                )
            }
        }
    }
}

/**
 * 快捷入口按钮
 */
@Composable
private fun QuickAccessButton(
    item: QuickAccessItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = item.color.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = item.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * 本月财务概览
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyFinanceSection(
    onNavigateToModule: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onNavigateToModule(Screen.AccountingMain.route) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月财务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "查看详情",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 收入
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥15,000",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.Income,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 支出
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥8,500",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.Expense,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 结余
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "结余",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "¥6,500",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 目标进度
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalProgressSection(
    onNavigateToModule: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onNavigateToModule(Screen.Goal.route) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "目标进度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "查看详情",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 示例目标进度
            GoalProgressItem(
                title = "存款10万元",
                progress = 0.65f,
                progressText = "¥65,000 / ¥100,000"
            )

            Spacer(modifier = Modifier.height(8.dp))

            GoalProgressItem(
                title = "阅读12本书",
                progress = 0.5f,
                progressText = "6 / 12本"
            )
        }
    }
}

/**
 * 目标进度项
 */
@Composable
private fun GoalProgressItem(
    title: String,
    progress: Float,
    progressText: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * AI建议卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AISuggestionCard(
    onNavigateToAI: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = onNavigateToAI
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI建议",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI智能助手",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "点击使用语音或文字命令，快速记账、添加待办、查询数据等",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 快捷入口数据类
 */
private data class QuickAccessItem(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val route: String
)
