package com.lifemanager.app.feature.habit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.HabitAchievement
import com.lifemanager.app.domain.model.HabitRankItem
import com.lifemanager.app.domain.model.HabitStats
import com.lifemanager.app.domain.model.HabitUiState
import com.lifemanager.app.domain.model.HabitWithStatus
import com.lifemanager.app.domain.model.MonthlyHabitStats
import com.lifemanager.app.domain.model.RetroCheckinState
import com.lifemanager.app.domain.model.WeeklyHabitStats
import com.lifemanager.app.domain.model.getFrequencyDisplayText

/**
 * 习惯打卡主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    onNavigateBack: () -> Unit,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val habitAnalysis by viewModel.habitAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    // 新增状态
    val achievements by viewModel.achievements.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val habitRanking by viewModel.habitRanking.collectAsState()
    val motivationalMessage by viewModel.motivationalMessage.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val retroCheckinState by viewModel.retroCheckinState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("习惯打卡") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = "添加习惯")
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is HabitUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
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
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("重试")
                        }
                    }
                }
            }

            is HabitUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 激励语卡片
                    if (motivationalMessage.isNotEmpty()) {
                        item {
                            MotivationalCard(message = motivationalMessage)
                        }
                    }

                    // 统计卡片
                    item {
                        HabitStatsCard(stats = stats)
                    }

                    // 成就徽章卡片
                    if (achievements.isNotEmpty()) {
                        item {
                            AchievementsCard(
                                achievements = achievements,
                                unlockedCount = viewModel.getUnlockedAchievementsCount(),
                                totalCount = viewModel.getTotalAchievementsCount()
                            )
                        }
                    }

                    // 周度统计卡片
                    weeklyStats?.let { weekly ->
                        item {
                            WeeklyStatsCard(weeklyStats = weekly)
                        }
                    }

                    // 习惯排行卡片
                    if (habitRanking.isNotEmpty()) {
                        item {
                            HabitRankingCard(ranking = habitRanking)
                        }
                    }

                    // AI智能洞察
                    item {
                        com.lifemanager.app.ui.component.AIInsightCard(
                            analysis = habitAnalysis,
                            isLoading = isAnalyzing,
                            onRefresh = { viewModel.refreshAIAnalysis() }
                        )
                    }

                    // 习惯列表标题
                    item {
                        Text(
                            text = "今日习惯",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (habits.isEmpty()) {
                        item {
                            EmptyState(
                                message = "暂无习惯",
                                actionText = "添加第一个习惯",
                                onAction = { viewModel.showAddDialog() }
                            )
                        }
                    } else {
                        items(habits, key = { it.habit.id }) { habitWithStatus ->
                            HabitItem(
                                habitWithStatus = habitWithStatus,
                                onCheckIn = { viewModel.toggleCheckIn(habitWithStatus.habit.id) },
                                onClick = { viewModel.showEditDialog(habitWithStatus.habit.id) },
                                onDelete = { viewModel.showDeleteConfirm(habitWithStatus.habit.id) }
                            )
                        }
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
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("删除习惯将同时删除所有打卡记录，确定要删除吗？") },
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

    // 补打卡对话框
    if (retroCheckinState.isShowing) {
        RetroCheckinDialog(
            state = retroCheckinState,
            onNoteChange = { viewModel.updateRetroCheckinNote(it) },
            onConfirm = { viewModel.performRetroCheckin() },
            onDismiss = { viewModel.hideRetroCheckinDialog() }
        )
    }
}

@Composable
private fun HabitStatsCard(stats: HabitStats) {
    val progress = if (stats.todayTotal > 0) stats.todayCompleted.toFloat() / stats.todayTotal else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFFEC4899).copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEC4899),
                            Color(0xFF8B5CF6)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 进度环
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // 背景圆环
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 进度圆环
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 统计数据
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HabitStatItem(
                        icon = Icons.Default.CheckCircle,
                        label = "今日完成",
                        value = "${stats.todayCompleted}/${stats.todayTotal}",
                        color = Color(0xFF4ADE80)
                    )
                    HabitStatItem(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "最长连续",
                        value = "${stats.longestStreak}天",
                        color = Color(0xFFFBBF24)
                    )
                    HabitStatItem(
                        icon = Icons.Default.Verified,
                        label = "活跃习惯",
                        value = "${stats.todayTotal}个",
                        color = Color(0xFF60A5FA)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
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
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun HabitItem(
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
        Color(0xFF8B5CF6)
    }

    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isChecked) 6.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = habitColor.copy(alpha = 0.2f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked)
                habitColor.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 打卡按钮 - 更大更明显
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = if (isChecked) 4.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = habitColor
                    )
                    .clip(CircleShape)
                    .background(
                        brush = if (isChecked)
                            Brush.linearGradient(
                                colors = listOf(habitColor, habitColor.copy(alpha = 0.8f))
                            )
                        else Brush.linearGradient(
                            colors = listOf(habitColor.copy(alpha = 0.15f), habitColor.copy(alpha = 0.1f))
                        )
                    )
                    .clickable(onClick = onCheckIn),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isChecked,
                    transitionSpec = {
                        scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) togetherWith
                                scaleOut()
                    },
                    label = "checkIcon"
                ) { checked ->
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = if (checked) "已打卡" else "打卡",
                        tint = if (checked) Color.White else habitColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 习惯信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isChecked) habitColor else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 频率标签
                    Surface(
                        color = habitColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = getFrequencyDisplayText(habit.frequency, habit.targetTimes),
                            style = MaterialTheme.typography.labelSmall,
                            color = habitColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // 连续天数 - 火焰样式
                    if (habitWithStatus.streak > 0) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${habitWithStatus.streak}天",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (habit.isNumeric && habit.targetValue != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${habit.targetValue?.toInt() ?: 0} ${habit.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

/**
 * 激励语卡片
 */
@Composable
private fun MotivationalCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFFFBBF24).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFBBF24),
                            Color(0xFFF59E0B)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 成就徽章卡片
 */
@Composable
private fun AchievementsCard(
    achievements: List<HabitAchievement>,
    unlockedCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "成就徽章",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$unlockedCount / $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 成就网格
            val chunkedAchievements = achievements.chunked(4)
            chunkedAchievements.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { achievement ->
                        AchievementBadge(achievement = achievement)
                    }
                    // 填充空位
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.size(60.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: HabitAchievement) {
    val badgeColor = try {
        Color(android.graphics.Color.parseColor(achievement.color))
    } catch (e: Exception) {
        Color(0xFFFFD700)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = if (achievement.isUnlocked) 4.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = badgeColor
                )
                .clip(CircleShape)
                .background(
                    if (achievement.isUnlocked)
                        Brush.linearGradient(listOf(badgeColor, badgeColor.copy(alpha = 0.7f)))
                    else
                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.2f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!achievement.isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = achievement.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 周度统计卡片
 */
@Composable
private fun WeeklyStatsCard(weeklyStats: WeeklyHabitStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = weeklyStats.weekLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (weeklyStats.isCurrentWeek) {
                    Surface(
                        color = Color(0xFFEDE9FE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "本周",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 完成率进度条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "完成率",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(weeklyStats.completionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = weeklyStats.completionRate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF8B5CF6),
                    trackColor = Color(0xFFEDE9FE),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 每日打卡柱状图
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weeklyStats.dailyData.forEach { daily ->
                    DailyBarItem(
                        dayLabel = daily.dayLabel,
                        completed = daily.completed,
                        total = daily.total,
                        isToday = daily.isToday
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 统计数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = weeklyStats.totalCheckins.toString(),
                    label = "总打卡",
                    color = Color(0xFF10B981)
                )
                StatItem(
                    value = weeklyStats.possibleCheckins.toString(),
                    label = "计划数",
                    color = Color(0xFF6366F1)
                )
            }
        }
    }
}

@Composable
private fun DailyBarItem(
    dayLabel: String,
    completed: Int,
    total: Int,
    isToday: Boolean
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val barColor = when {
        isToday -> Color(0xFF8B5CF6)
        progress >= 1f -> Color(0xFF10B981)
        progress > 0f -> Color(0xFFFBBF24)
        else -> Color.Gray.copy(alpha = 0.3f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = 0.1f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .background(barColor, RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday)
                Color(0xFF8B5CF6)
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 习惯排行卡片
 */
@Composable
private fun HabitRankingCard(ranking: List<HabitRankItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "习惯排行",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ranking.take(5).forEachIndexed { index, item ->
                HabitRankingItem(rank = index + 1, item = item)
                if (index < ranking.size - 1 && index < 4) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitRankingItem(rank: Int, item: HabitRankItem) {
    val habitColor = try {
        Color(android.graphics.Color.parseColor(item.habitColor))
    } catch (e: Exception) {
        Color(0xFF8B5CF6)
    }

    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (rank <= 3) rankColor.copy(alpha = 0.15f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (rank <= 3) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = rankColor,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 习惯名称和颜色指示器
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(habitColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.habitName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 统计数据
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.streak}天",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = "连续",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(item.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Text(
                    text = "完成率",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 补打卡对话框
 */
@Composable
private fun RetroCheckinDialog(
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
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("补打卡")
            }
        },
        text = {
            Column {
                Text(
                    text = "为之前的日期补打卡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
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
            Button(
                onClick = onConfirm,
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("确认")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
