package com.lifemanager.app.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.navigation.Screen
import com.lifemanager.app.ui.theme.AppColors
import com.lifemanager.app.ui.theme.CartoonShape
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * 首页屏幕 - Premium Design
 *
 * 采用现代化玻璃态设计、流畅动画和精美渐变
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToModule: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val monthlyFinance by viewModel.monthlyFinance.collectAsState()
    val topGoals by viewModel.topGoals.collectAsState()

    val today = remember { LocalDate.now() }
    val greeting = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> "早安"
            in 12..13 -> "午安"
            in 14..17 -> "下午好"
            else -> "晚安"
        }
    }

    val greetingEmoji = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> "☀️"
            in 12..13 -> "🌤️"
            in 14..17 -> "🌸"
            else -> "🌙"
        }
    }

    // 动画时间
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 高级动态背景
        PremiumBackground(animatedTime)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // 透明顶部栏
                PremiumTopBar(
                    greeting = greeting,
                    greetingEmoji = greetingEmoji,
                    today = today,
                    onAIClick = { onNavigateToModule(Screen.AIAssistant.route) },
                    onSettingsClick = { onNavigateToModule(Screen.Settings.route) }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                // 高级加载动画
                PremiumLoadingScreen(modifier = Modifier.padding(paddingValues))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 英雄区域 - 今日概览
                    item(key = "hero") {
                        HeroSection(
                            todayStats = todayStats,
                            onNavigateToModule = onNavigateToModule
                        )
                    }

                    // 快捷功能入口
                    item(key = "quick_access") {
                        QuickAccessSection(onNavigateToModule = onNavigateToModule)
                    }

                    // 本月财务卡片
                    item(key = "monthly_finance") {
                        FinanceCard(
                            finance = monthlyFinance,
                            onClick = { onNavigateToModule(Screen.AccountingMain.route) }
                        )
                    }

                    // 目标进度
                    if (topGoals.isNotEmpty()) {
                        item(key = "goals") {
                            GoalsSection(
                                goals = topGoals,
                                onClick = { onNavigateToModule(Screen.Goal.route) }
                            )
                        }
                    }

                    // AI 助手卡片
                    item(key = "ai_card") {
                        AIAssistantCard(onClick = { onNavigateToModule(Screen.AIAssistant.route) })
                    }

                    // 底部安全间距
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * 高级动态背景 - 浮动粒子和渐变
 */
@Composable
private fun PremiumBackground(animatedTime: Float) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF0f0c29),
                            Color(0xFF302b63),
                            Color(0xFF24243e)
                        )
                    } else {
                        listOf(
                            Color(0xFFF8F5FF),
                            Color(0xFFFFF9F5),
                            Color(0xFFF5F9FF)
                        )
                    }
                )
            )
    ) {
        // 浮动粒子
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val particles = listOf(
                ParticleData(0.1f, 0.15f, 35f, AppColors.GlowPurple),
                ParticleData(0.85f, 0.08f, 28f, AppColors.GlowBlue),
                ParticleData(0.72f, 0.25f, 22f, AppColors.GlowPink),
                ParticleData(0.15f, 0.45f, 40f, AppColors.CandyLavender.copy(alpha = 0.4f)),
                ParticleData(0.88f, 0.55f, 32f, AppColors.CandyMint.copy(alpha = 0.4f)),
                ParticleData(0.25f, 0.75f, 25f, AppColors.CandyPeach.copy(alpha = 0.4f)),
                ParticleData(0.65f, 0.85f, 30f, AppColors.CandyBlue.copy(alpha = 0.35f)),
                ParticleData(0.45f, 0.35f, 18f, AppColors.GlowPurple.copy(alpha = 0.3f))
            )

            particles.forEachIndexed { index, particle ->
                val offsetX = sin((animatedTime + index * 45) * 0.02f) * 30f
                val offsetY = cos((animatedTime + index * 60) * 0.015f) * 25f

                drawCircle(
                    color = particle.color,
                    radius = particle.radius,
                    center = Offset(
                        width * particle.x + offsetX,
                        height * particle.y + offsetY
                    )
                )
            }
        }
    }
}

private data class ParticleData(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color
)

/**
 * 高级顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumTopBar(
    greeting: String,
    greetingEmoji: String,
    today: LocalDate,
    onAIClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = greetingEmoji,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${today.monthValue}月${today.dayOfMonth}日 ${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            // AI 按钮
            PremiumIconButton(
                onClick = onAIClick,
                icon = "🤖",
                gradientColors = AppColors.GradientCosmic
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 设置按钮
            PremiumIconButton(
                onClick = onSettingsClick,
                icon = "⚙️",
                gradientColors = AppColors.GradientEmerald
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * 高级图标按钮
 */
@Composable
private fun PremiumIconButton(
    onClick: () -> Unit,
    icon: String,
    gradientColors: List<Color>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // 发光动画
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = gradientColors.first().copy(alpha = glowAlpha)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors.map { it.copy(alpha = 0.9f) }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 20.sp)
    }
}

/**
 * 高级加载屏幕
 */
@Composable
private fun PremiumLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 脉冲加载动画
            PulsingCircle(
                color = AppColors.Primary,
                size = 60.dp
            ) {
                Text("✨", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 英雄区域 - 今日概览
 */
@Composable
private fun HeroSection(
    todayStats: TodayStatsData,
    onNavigateToModule: (String) -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    PremiumGradientCard(
        gradientColors = AppColors.GradientHero,
        onClick = null
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "今日概览",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                GradientChip(
                    text = "查看详情 →",
                    gradientColors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    ),
                    onClick = { onNavigateToModule(Screen.DataCenter.route) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroStatItem(
                    emoji = "✅",
                    label = "待办完成",
                    value = "${todayStats.completedTodos}/${todayStats.totalTodos}",
                    progress = if (todayStats.totalTodos > 0)
                        todayStats.completedTodos.toFloat() / todayStats.totalTodos
                    else 0f,
                    progressColors = AppColors.GradientEmerald
                )

                HeroStatItem(
                    emoji = "💰",
                    label = "今日消费",
                    value = "¥${numberFormat.format(todayStats.todayExpense.toInt())}",
                    progress = 0.7f, // 预算进度示例
                    progressColors = AppColors.GradientGold
                )

                HeroStatItem(
                    emoji = "🎯",
                    label = "习惯打卡",
                    value = "${todayStats.completedHabits}/${todayStats.totalHabits}",
                    progress = if (todayStats.totalHabits > 0)
                        todayStats.completedHabits.toFloat() / todayStats.totalHabits
                    else 0f,
                    progressColors = AppColors.GradientRose
                )
            }
        }
    }
}

/**
 * 英雄区域统计项
 */
@Composable
private fun HeroStatItem(
    emoji: String,
    label: String,
    value: String,
    progress: Float,
    progressColors: List<Color>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 圆形进度指示器
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            PremiumCircularProgress(
                progress = progress,
                size = 72.dp,
                strokeWidth = 6.dp,
                trackColor = Color.White.copy(alpha = 0.2f),
                gradientColors = progressColors
            ) {
                Text(emoji, fontSize = 28.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/**
 * 快捷功能入口
 */
@Composable
private fun QuickAccessSection(onNavigateToModule: (String) -> Unit) {
    val quickAccessItems = remember {
        listOf(
            QuickItem("🤖", "AI助手", AppColors.GradientCosmic, Screen.AIAssistant.route),
            QuickItem("💵", "记账", AppColors.GradientEmerald, Screen.AccountingMain.route),
            QuickItem("📝", "待办", AppColors.GradientGold, Screen.Todo.route),
            QuickItem("🎯", "目标", AppColors.GradientAurora, Screen.Goal.route),
            QuickItem("⭐", "打卡", AppColors.GradientRose, Screen.Habit.route),
            QuickItem("💚", "健康", AppColors.GradientEmerald, Screen.HealthRecord.route),
            QuickItem("📔", "日记", AppColors.GradientNeonCity, Screen.Diary.route),
            QuickItem("🐷", "存钱", AppColors.GradientMango, Screen.SavingsPlan.route),
            QuickItem("📊", "预算", AppColors.GradientPurpleHaze, Screen.Budget.route)
        )
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚡", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "快捷入口",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(quickAccessItems, key = { it.route }) { item ->
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
    item: QuickItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = item.gradientColors.first().copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = item.gradientColors.map { it.copy(alpha = 0.85f) }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(item.emoji, fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 本月财务卡片
 */
@Composable
private fun FinanceCard(
    finance: MonthlyFinanceData,
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val today = remember { LocalDate.now() }

    GlassCard(
        onClick = onClick,
        gradientColors = listOf(
            AppColors.GlassWhite,
            Color.White.copy(alpha = 0.95f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                brush = Brush.linearGradient(AppColors.GradientEmerald),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💳", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${today.monthValue}月财务",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "点击查看详情",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FinanceStatItem(
                    icon = "📈",
                    label = "收入",
                    value = "¥${numberFormat.format(finance.totalIncome.toLong())}",
                    color = AppColors.Income,
                    gradientColors = AppColors.GradientEmerald
                )

                FinanceStatItem(
                    icon = "📉",
                    label = "支出",
                    value = "¥${numberFormat.format(finance.totalExpense.toLong())}",
                    color = AppColors.Expense,
                    gradientColors = AppColors.GradientNeonCity
                )

                FinanceStatItem(
                    icon = "💎",
                    label = "结余",
                    value = "¥${numberFormat.format(finance.balance.toLong())}",
                    color = if (finance.balance >= 0) AppColors.Primary else AppColors.Expense,
                    gradientColors = if (finance.balance >= 0) AppColors.GradientAurora else AppColors.GradientNeonCity
                )
            }
        }
    }
}

@Composable
private fun FinanceStatItem(
    icon: String,
    label: String,
    value: String,
    color: Color,
    gradientColors: List<Color>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.15f) }),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 目标进度区域
 */
@Composable
private fun GoalsSection(
    goals: List<GoalProgressData>,
    onClick: () -> Unit
) {
    GlassCard(onClick = onClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                brush = Brush.linearGradient(AppColors.GradientAurora),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚀", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "目标进度",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            goals.forEachIndexed { index, goal ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                GoalProgressItem(
                    title = goal.title,
                    progress = goal.progress,
                    progressText = goal.progressText,
                    medal = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        else -> "🥉"
                    },
                    gradientColors = when (index) {
                        0 -> AppColors.GradientGold
                        1 -> AppColors.GradientSky
                        else -> AppColors.GradientRose
                    }
                )
            }
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
    progressText: String,
    medal: String,
    gradientColors: List<Color>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(medal, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Text(
                text = progressText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        PremiumLinearProgress(
            progress = progress,
            height = 10.dp,
            trackColor = gradientColors.first().copy(alpha = 0.15f),
            gradientColors = gradientColors,
            showShimmer = progress > 0.5f
        )
    }
}

/**
 * AI 助手卡片
 */
@Composable
private fun AIAssistantCard(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    TiltCard(
        onClick = onClick,
        gradientColors = AppColors.GradientCosmic.map { it.copy(alpha = 0.9f) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI 图标容器
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = AppColors.GlowPink.copy(alpha = glowAlpha)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI 智能助手",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✨", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "语音记账、智能分析、快捷操作",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "开始 →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 快捷入口数据类
 */
private data class QuickItem(
    val emoji: String,
    val label: String,
    val gradientColors: List<Color>,
    val route: String
)
