package com.lifemanager.app.feature.health

import androidx.compose.animation.core.*
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.*
import com.lifemanager.app.ui.component.PremiumCircularProgress
import com.lifemanager.app.ui.component.PremiumLinearProgress
import com.lifemanager.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 健康记录主页面 - Premium Design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRecordScreen(
    onNavigateBack: () -> Unit,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val weeklyAnalysis by viewModel.weeklyAnalysis.collectAsState()
    val filteredRecords by viewModel.filteredRecords.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val addDialogType by viewModel.addDialogType.collectAsState()
    val editingRecord by viewModel.editingRecord.collectAsState()
    val showDeleteConfirm by viewModel.showDeleteConfirm.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 处理UI状态
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is HealthUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearUiState()
            }
            is HealthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color(0xFF1a1a2e),
                                Color(0xFF16213e),
                                Color(0xFF0f3460)
                            )
                        } else {
                            listOf(
                                Color(0xFFF0FFF4),
                                Color(0xFFF5F7FF),
                                Color(0xFFFFF5F8)
                            )
                        }
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                HealthTopBar(onNavigateBack = onNavigateBack)
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog(HealthRecordType.WEIGHT) },
                    containerColor = AppColors.Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加记录")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 今日健康概览
                item(key = "today_summary") {
                    TodaySummaryCard(
                        summary = todaySummary,
                        onQuickRecord = { type -> viewModel.showAddDialog(type) }
                    )
                }

                // 快速记录按钮
                item(key = "quick_actions") {
                    QuickActionsRow(
                        onRecordWeight = { viewModel.showAddDialog(HealthRecordType.WEIGHT) },
                        onRecordSleep = { viewModel.showAddDialog(HealthRecordType.SLEEP) },
                        onRecordExercise = { viewModel.showAddDialog(HealthRecordType.EXERCISE) },
                        onRecordMood = { viewModel.showAddDialog(HealthRecordType.MOOD) },
                        onRecordWater = { viewModel.quickRecordWater() },
                        onRecordSteps = { viewModel.showAddDialog(HealthRecordType.STEPS) }
                    )
                }

                // 周统计卡片
                weeklyAnalysis?.let { analysis ->
                    item(key = "weekly_stats") {
                        WeeklyStatsCard(analysis = analysis)
                    }
                }

                // 类型筛选
                item(key = "type_filter") {
                    TypeFilterRow(
                        selectedType = selectedType,
                        onSelectType = { viewModel.selectType(it) }
                    )
                }

                // 历史记录列表
                if (filteredRecords.isEmpty()) {
                    item {
                        EmptyRecordsCard()
                    }
                } else {
                    items(filteredRecords, key = { it.id }) { record ->
                        HealthRecordItem(
                            record = record,
                            onEdit = { viewModel.showEditDialog(record) },
                            onDelete = { viewModel.showDeleteConfirm(record) }
                        )
                    }
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showAddDialog) {
        AddHealthRecordDialog(
            type = addDialogType,
            existingRecord = editingRecord,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { value, secondaryValue, rating, category, note ->
                viewModel.saveRecord(addDialogType, value, secondaryValue, rating, category, note)
            }
        )
    }

    // 删除确认对话框
    showDeleteConfirm?.let { record ->
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            shape = RoundedCornerShape(24.dp),
            icon = { Text("🗑️", fontSize = 36.sp) },
            title = { Text("确认删除", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这条${HealthRecordType.getDisplayName(record.recordType)}记录吗？") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteRecord(record) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
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

    // 加载指示器
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.Primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💚", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "健康记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

/**
 * 今日健康概览卡片
 */
@Composable
private fun TodaySummaryCard(
    summary: TodayHealthSummary,
    onQuickRecord: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = AppColors.GradientEmerald.first().copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = AppColors.GradientEmerald.map { it.copy(alpha = 0.9f) }
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日健康",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 体重
                SummaryStatItem(
                    icon = "⚖️",
                    label = "体重",
                    value = summary.weight?.let { "${String.format("%.1f", it)} kg" } ?: "--",
                    onClick = { onQuickRecord(HealthRecordType.WEIGHT) }
                )

                // 睡眠
                SummaryStatItem(
                    icon = "😴",
                    label = "睡眠",
                    value = summary.sleepHours?.let { "${String.format("%.1f", it)} 小时" } ?: "--",
                    onClick = { onQuickRecord(HealthRecordType.SLEEP) }
                )

                // 心情
                SummaryStatItem(
                    icon = summary.moodRating?.let { MoodRating.getIcon(it) } ?: "😊",
                    label = "心情",
                    value = summary.moodRating?.let { MoodRating.getDisplayName(it) } ?: "--",
                    onClick = { onQuickRecord(HealthRecordType.MOOD) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 饮水
                SummaryStatItem(
                    icon = "💧",
                    label = "饮水",
                    value = "${summary.waterIntake.toInt()} ml",
                    onClick = { onQuickRecord(HealthRecordType.WATER) }
                )

                // 运动
                SummaryStatItem(
                    icon = "🏃",
                    label = "运动",
                    value = "${summary.exerciseMinutes.toInt()} 分钟",
                    onClick = { onQuickRecord(HealthRecordType.EXERCISE) }
                )

                // 步数
                SummaryStatItem(
                    icon = "👣",
                    label = "步数",
                    value = "${summary.steps}",
                    onClick = { onQuickRecord(HealthRecordType.STEPS) }
                )
            }
        }
    }
}

@Composable
private fun SummaryStatItem(
    icon: String,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(6.dp))
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

/**
 * 快速记录按钮行
 */
@Composable
private fun QuickActionsRow(
    onRecordWeight: () -> Unit,
    onRecordSleep: () -> Unit,
    onRecordExercise: () -> Unit,
    onRecordMood: () -> Unit,
    onRecordWater: () -> Unit,
    onRecordSteps: () -> Unit
) {
    val actions = listOf(
        Triple("⚖️", "体重", onRecordWeight),
        Triple("😴", "睡眠", onRecordSleep),
        Triple("🏃", "运动", onRecordExercise),
        Triple("😊", "心情", onRecordMood),
        Triple("💧", "喝水+", onRecordWater),
        Triple("👣", "步数", onRecordSteps)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(actions) { (icon, label, onClick) ->
            QuickActionButton(icon = icon, label = label, onClick = onClick)
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = AppColors.Primary.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppColors.GlassWhite,
                            Color.White.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 26.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 周统计卡片
 */
@Composable
private fun WeeklyStatsCard(
    analysis: com.lifemanager.app.data.repository.HealthAnalysisData
) {
    GlassCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(AppColors.GradientAurora.map { it.copy(alpha = 0.15f) }),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "本周统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeeklyStatItem(
                    icon = "🏃",
                    label = "运动天数",
                    value = "${analysis.exerciseDays}/7",
                    progress = analysis.exerciseDays / 7f
                )

                WeeklyStatItem(
                    icon = "😴",
                    label = "平均睡眠",
                    value = analysis.avgSleepHours?.let { "${String.format("%.1f", it)}h" } ?: "--",
                    progress = ((analysis.avgSleepHours ?: 0.0) / 8.0).toFloat().coerceIn(0f, 1f)
                )

                WeeklyStatItem(
                    icon = "😊",
                    label = "平均心情",
                    value = analysis.avgMoodRating?.let { String.format("%.1f", it) } ?: "--",
                    progress = ((analysis.avgMoodRating ?: 0.0) / 5.0).toFloat().coerceIn(0f, 1f)
                )
            }
        }
    }
}

@Composable
private fun WeeklyStatItem(
    icon: String,
    label: String,
    value: String,
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            PremiumCircularProgress(
                progress = progress,
                size = 60.dp,
                strokeWidth = 5.dp,
                trackColor = AppColors.Primary.copy(alpha = 0.15f),
                gradientColors = AppColors.GradientAurora
            ) {
                Text(icon, fontSize = 22.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 类型筛选行
 */
@Composable
private fun TypeFilterRow(
    selectedType: String?,
    onSelectType: (String?) -> Unit
) {
    val types = listOf(
        null to "全部",
        HealthRecordType.WEIGHT to "⚖️ 体重",
        HealthRecordType.SLEEP to "😴 睡眠",
        HealthRecordType.EXERCISE to "🏃 运动",
        HealthRecordType.MOOD to "😊 心情",
        HealthRecordType.WATER to "💧 饮水",
        HealthRecordType.STEPS to "👣 步数"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(types) { (type, label) ->
            FilterChipItem(
                label = label,
                selected = selectedType == type,
                onClick = { onSelectType(type) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) AppColors.Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * 健康记录项
 */
@Composable
private fun HealthRecordItem(
    record: HealthRecordEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = LocalDate.ofEpochDay(record.date.toLong())
    val dateStr = date.format(DateTimeFormatter.ofPattern("M月d日"))

    GlassCard(onClick = onEdit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = AppColors.Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = HealthRecordType.getIcon(record.recordType),
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = HealthRecordType.getDisplayName(record.recordType),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (record.category != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppColors.Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = when (record.recordType) {
                                    HealthRecordType.EXERCISE -> ExerciseCategory.getDisplayName(record.category)
                                    HealthRecordType.MOOD -> MoodSource.getDisplayName(record.category)
                                    else -> record.category
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 主要数值
                    Text(
                        text = formatRecordValue(record),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )

                    // 辅助数值（如血压的舒张压、睡眠质量等）
                    record.secondaryValue?.let { secondary ->
                        when (record.recordType) {
                            HealthRecordType.BLOOD_PRESSURE -> {
                                Text(
                                    text = " / ${secondary.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary
                                )
                            }
                            HealthRecordType.EXERCISE -> {
                                if (secondary > 0) {
                                    Text(
                                        text = " · ${secondary.toInt()} kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {}
                        }
                    }

                    // 评分（心情、睡眠质量）
                    record.rating?.let { rating ->
                        Spacer(modifier = Modifier.width(8.dp))
                        when (record.recordType) {
                            HealthRecordType.MOOD -> {
                                Text(
                                    text = MoodRating.getIcon(rating),
                                    fontSize = 18.sp
                                )
                            }
                            HealthRecordType.SLEEP -> {
                                Text(
                                    text = "质量: ${SleepQuality.getDisplayName(rating)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {}
                        }
                    }
                }

                // 备注
                if (record.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 日期和删除
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                record.time?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatRecordValue(record: HealthRecordEntity): String {
    return when (record.recordType) {
        HealthRecordType.WEIGHT -> "${String.format("%.1f", record.value)} kg"
        HealthRecordType.SLEEP -> "${String.format("%.1f", record.value)} 小时"
        HealthRecordType.EXERCISE -> "${record.value.toInt()} 分钟"
        HealthRecordType.MOOD -> MoodRating.getDisplayName(record.value.toInt())
        HealthRecordType.WATER -> "${record.value.toInt()} ml"
        HealthRecordType.BLOOD_PRESSURE -> "${record.value.toInt()}"
        HealthRecordType.HEART_RATE -> "${record.value.toInt()} bpm"
        HealthRecordType.STEPS -> "${record.value.toInt()} 步"
        else -> "${record.value} ${record.unit}"
    }
}

/**
 * 空记录卡片
 */
@Composable
private fun EmptyRecordsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📝", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击下方按钮开始记录健康数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 玻璃态卡片
 */
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AppColors.Primary.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AppColors.GlassWhite,
                        Color.White.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

/**
 * 添加健康记录对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHealthRecordDialog(
    type: String,
    existingRecord: HealthRecordEntity?,
    onDismiss: () -> Unit,
    onSave: (value: Double, secondaryValue: Double?, rating: Int?, category: String?, note: String) -> Unit
) {
    var value by remember(existingRecord) {
        mutableStateOf(existingRecord?.value?.toString() ?: "")
    }
    var secondaryValue by remember(existingRecord) {
        mutableStateOf(existingRecord?.secondaryValue?.toString() ?: "")
    }
    var rating by remember(existingRecord) {
        mutableStateOf(existingRecord?.rating ?: 3)
    }
    var selectedCategory by remember(existingRecord) {
        mutableStateOf(existingRecord?.category)
    }
    var note by remember(existingRecord) {
        mutableStateOf(existingRecord?.note ?: "")
    }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = AppColors.Primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(HealthRecordType.getIcon(type), fontSize = 28.sp)
            }
        },
        title = {
            Text(
                text = if (existingRecord != null) "编辑${HealthRecordType.getDisplayName(type)}"
                       else "记录${HealthRecordType.getDisplayName(type)}",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (type) {
                    HealthRecordType.WEIGHT -> {
                        // 体重输入
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("体重 (kg)") },
                            placeholder = { Text("例如: 65.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HealthRecordType.SLEEP -> {
                        // 睡眠时长
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("睡眠时长 (小时)") },
                            placeholder = { Text("例如: 7.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 睡眠质量
                        Text(
                            text = "睡眠质量",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { quality ->
                                Surface(
                                    onClick = { rating = quality },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (rating == quality) AppColors.Primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = SleepQuality.getIcon(quality),
                                            fontSize = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HealthRecordType.EXERCISE -> {
                        // 运动时长
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = { Text("运动时长 (分钟)") },
                            placeholder = { Text("例如: 30") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 运动类型
                        ExposedDropdownMenuBox(
                            expanded = showCategoryDropdown,
                            onExpandedChange = { showCategoryDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.let { ExerciseCategory.getDisplayName(it) } ?: "选择运动类型",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("运动类型") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                ExerciseCategory.getAllCategories().forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text("${ExerciseCategory.getIcon(category)} ${ExerciseCategory.getDisplayName(category)}") },
                                        onClick = {
                                            selectedCategory = category
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // 消耗热量（可选）
                        OutlinedTextField(
                            value = secondaryValue,
                            onValueChange = { secondaryValue = it.filter { c -> c.isDigit() } },
                            label = { Text("消耗热量 (kcal) - 可选") },
                            placeholder = { Text("例如: 200") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HealthRecordType.MOOD -> {
                        // 心情选择
                        Text(
                            text = "选择你的心情",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { mood ->
                                Surface(
                                    onClick = {
                                        rating = mood
                                        value = mood.toString()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (rating == mood) AppColors.Primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (rating == mood) androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary) else null,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = MoodRating.getIcon(mood),
                                            fontSize = 28.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = MoodRating.getDisplayName(rating),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HealthRecordType.WATER -> {
                        // 饮水量快速选择
                        Text(
                            text = "选择饮水量",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(150, 250, 350, 500).forEach { ml ->
                                Surface(
                                    onClick = { value = ml.toString() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (value == ml.toString()) AppColors.Primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${ml}ml",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (value == ml.toString()) Color.White
                                                else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // 自定义输入
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = { Text("自定义 (ml)") },
                            placeholder = { Text("例如: 300") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HealthRecordType.STEPS -> {
                        // 步数输入
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = { Text("步数") },
                            placeholder = { Text("例如: 8000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HealthRecordType.BLOOD_PRESSURE -> {
                        // 收缩压
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = { Text("收缩压 (mmHg)") },
                            placeholder = { Text("例如: 120") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 舒张压
                        OutlinedTextField(
                            value = secondaryValue,
                            onValueChange = { secondaryValue = it.filter { c -> c.isDigit() } },
                            label = { Text("舒张压 (mmHg)") },
                            placeholder = { Text("例如: 80") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HealthRecordType.HEART_RATE -> {
                        // 心率输入
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = { Text("心率 (bpm)") },
                            placeholder = { Text("例如: 72") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 备注（所有类型通用）
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注 (可选)") },
                    placeholder = { Text("添加一些备注...") },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedValue = when (type) {
                        HealthRecordType.MOOD -> rating.toDouble()
                        else -> value.toDoubleOrNull() ?: return@Button
                    }
                    val parsedSecondary = secondaryValue.toDoubleOrNull()
                    val parsedRating = when (type) {
                        HealthRecordType.SLEEP, HealthRecordType.MOOD -> rating
                        else -> null
                    }
                    onSave(parsedValue, parsedSecondary, parsedRating, selectedCategory, note)
                },
                enabled = when (type) {
                    HealthRecordType.MOOD -> true
                    HealthRecordType.BLOOD_PRESSURE -> value.isNotEmpty() && secondaryValue.isNotEmpty()
                    else -> value.isNotEmpty()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text(if (existingRecord != null) "保存" else "记录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 扩展函数
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
