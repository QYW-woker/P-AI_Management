package com.lifemanager.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 个人中心页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statistics by viewModel.statistics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
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
            // 用户信息卡片
            item {
                UserInfoCard(onLoginClick = onNavigateToLogin)
            }

            // 使用统计
            item {
                StatisticsCard(statistics = statistics)
            }

            // 成就概览
            item {
                AchievementsCard(statistics = statistics)
            }

            // 功能入口
            item {
                FunctionListCard(onNavigateToSettings = onNavigateToSettings)
            }
        }
    }
}

/**
 * 用户信息卡片
 */
@Composable
private fun UserInfoCard(
    onLoginClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoginClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "点击登录/注册",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "登录后同步数据，畅享更多功能",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * 统计卡片
 */
@Composable
private fun StatisticsCard(statistics: UserStatistics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "使用统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 第一行
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    icon = Icons.Outlined.CalendarToday,
                    value = "${statistics.totalDays}",
                    label = "使用天数",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Outlined.LocalFireDepartment,
                    value = "${statistics.currentStreak}",
                    label = "连续天数",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Outlined.CheckCircle,
                    value = "${statistics.totalTodos}",
                    label = "累计待办",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 第二行
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    icon = Icons.Outlined.Book,
                    value = "${statistics.totalDiaries}",
                    label = "累计日记",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Outlined.Timer,
                    value = "${statistics.totalFocusHours}h",
                    label = "专注时长",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Outlined.Verified,
                    value = "${statistics.totalHabitCheckins}",
                    label = "习惯打卡",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 成就卡片
 */
@Composable
private fun AchievementsCard(statistics: UserStatistics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "我的成就",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementItem(
                    icon = Icons.Default.Flag,
                    count = statistics.completedGoals,
                    label = "完成目标"
                )
                AchievementItem(
                    icon = Icons.Default.Savings,
                    count = statistics.totalSavingsAmount.toInt(),
                    label = "累计存款",
                    suffix = "元"
                )
                AchievementItem(
                    icon = Icons.Default.Timer,
                    count = statistics.totalFocusHours,
                    label = "专注小时",
                    suffix = "h"
                )
            }
        }
    }
}

/**
 * 成就项
 */
@Composable
private fun AchievementItem(
    icon: ImageVector,
    count: Int,
    label: String,
    suffix: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$count$suffix",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 功能入口卡片
 */
@Composable
private fun FunctionListCard(onNavigateToSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FunctionItem(
                icon = Icons.Outlined.Settings,
                title = "设置",
                subtitle = "主题、通知、数据管理",
                onClick = onNavigateToSettings
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            FunctionItem(
                icon = Icons.Outlined.Help,
                title = "帮助与反馈",
                subtitle = "使用指南、问题反馈",
                onClick = { }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            FunctionItem(
                icon = Icons.Outlined.Info,
                title = "关于",
                subtitle = "版本 1.0.0",
                onClick = { }
            )
        }
    }
}

/**
 * 功能项
 */
@Composable
private fun FunctionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
