package com.lifemanager.app.feature.health

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.*
import com.lifemanager.app.data.repository.HealthAnalysisData
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 健康记录主页面 - 简洁设计版本
 *
 * 设计原则:
 * - 干净、克制、有呼吸感
 * - 轻灵不花哨
 * - 使用统一的设计系统
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanHealthRecordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
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

    Scaffold(
        containerColor = CleanColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "健康记录",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CleanColors.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog(HealthRecordType.WEIGHT) },
                containerColor = CleanColors.primary,
                contentColor = CleanColors.onPrimary,
                shape = RoundedCornerShape(Radius.md)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加记录")
            }
        }
    ) { paddingValues ->
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
            // 今日健康概览
            item(key = "today_summary") {
                CleanTodaySummaryCard(
                    summary = todaySummary,
                    onQuickRecord = { type -> viewModel.showAddDialog(type) }
                )
            }

            // 快速记录按钮
            item(key = "quick_actions") {
                CleanQuickActionsRow(
                    onRecordWeight = { viewModel.showAddDialog(HealthRecordType.WEIGHT) },
                    onRecordSleep = { viewModel.showAddDialog(HealthRecordType.SLEEP) },
                    onRecordExercise = { viewModel.showAddDialog(HealthRecordType.EXERCISE) },
                    onRecordMood = { viewModel.showAddDialog(HealthRecordType.MOOD) },
                    onRecordWater = { viewModel.showAddDialog(HealthRecordType.WATER) },
                    onRecordSteps = { viewModel.showAddDialog(HealthRecordType.STEPS) }
                )
            }

            // 周统计卡片
            weeklyAnalysis?.let { analysis ->
                item(key = "weekly_stats") {
                    CleanWeeklyStatsCard(analysis = analysis)
                }
            }

            // 类型筛选
            item(key = "type_filter") {
                CleanTypeFilterRow(
                    selectedType = selectedType,
                    onSelectType = { viewModel.selectType(it) }
                )
            }

            // 历史记录列表
            item(key = "records_header") {
                Text(
                    text = "健康记录",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            }

            if (filteredRecords.isEmpty()) {
                item(key = "empty") {
                    EmptyStateView(
                        message = "暂无记录",
                        icon = Icons.Outlined.HealthAndSafety,
                        actionText = "开始记录",
                        onActionClick = { viewModel.showAddDialog(HealthRecordType.WEIGHT) }
                    )
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    CleanHealthRecordItem(
                        record = record,
                        onClick = { onNavigateToDetail(record.id) },
                        onDelete = { viewModel.showDeleteConfirm(record) }
                    )
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(Spacing.bottomSafe + 56.dp))
            }
        }
    }

    // 添加/编辑对话框
    if (showAddDialog) {
        CleanAddHealthRecordDialog(
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
        CleanDeleteConfirmDialog(
            title = "确认删除",
            message = "确定要删除这条${HealthRecordType.getDisplayName(record.recordType)}记录吗？此操作无法撤销。",
            onConfirm = { viewModel.deleteRecord(record) },
            onDismiss = { viewModel.hideDeleteConfirm() }
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
            CleanLoadingIndicator(size = 32.dp)
        }
    }
}

/**
 * 今日健康概览卡片 - 简洁版本
 */
@Composable
private fun CleanTodaySummaryCard(
    summary: TodayHealthSummary,
    onQuickRecord: (String) -> Unit
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
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = CleanColors.primary,
                        modifier = Modifier.size(IconSize.md)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "今日健康",
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                }
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日")),
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CleanSummaryStatItem(
                    icon = Icons.Outlined.MonitorWeight,
                    label = "体重",
                    value = summary.weight?.let { "${String.format("%.1f", it)} kg" } ?: "--",
                    onClick = { onQuickRecord(HealthRecordType.WEIGHT) }
                )
                CleanSummaryStatItem(
                    icon = Icons.Outlined.Bedtime,
                    label = "睡眠",
                    value = summary.sleepHours?.let { "${String.format("%.1f", it)} h" } ?: "--",
                    onClick = { onQuickRecord(HealthRecordType.SLEEP) }
                )
                CleanSummaryStatItem(
                    icon = Icons.Outlined.Mood,
                    label = "心情",
                    value = summary.moodRating?.let { MoodRating.getDisplayName(it) } ?: "--",
                    onClick = { onQuickRecord(HealthRecordType.MOOD) }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CleanSummaryStatItem(
                    icon = Icons.Outlined.WaterDrop,
                    label = "饮水",
                    value = "${summary.waterIntake.toInt()} ml",
                    onClick = { onQuickRecord(HealthRecordType.WATER) }
                )
                CleanSummaryStatItem(
                    icon = Icons.Outlined.DirectionsRun,
                    label = "运动",
                    value = "${summary.exerciseMinutes.toInt()} 分钟",
                    onClick = { onQuickRecord(HealthRecordType.EXERCISE) }
                )
                CleanSummaryStatItem(
                    icon = Icons.Outlined.DirectionsWalk,
                    label = "步数",
                    value = "${summary.steps.toInt()}",
                    onClick = { onQuickRecord(HealthRecordType.STEPS) }
                )
            }
        }
    }
}

@Composable
private fun CleanSummaryStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(Spacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CleanColors.primary,
            modifier = Modifier.size(IconSize.lg)
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = CleanTypography.amountSmall,
            color = CleanColors.textPrimary
        )
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textSecondary
        )
    }
}

/**
 * 快速记录按钮行
 */
@Composable
private fun CleanQuickActionsRow(
    onRecordWeight: () -> Unit,
    onRecordSleep: () -> Unit,
    onRecordExercise: () -> Unit,
    onRecordMood: () -> Unit,
    onRecordWater: () -> Unit,
    onRecordSteps: () -> Unit
) {
    val actions = listOf(
        Triple(Icons.Outlined.MonitorWeight, "体重", onRecordWeight),
        Triple(Icons.Outlined.Bedtime, "睡眠", onRecordSleep),
        Triple(Icons.Outlined.DirectionsRun, "运动", onRecordExercise),
        Triple(Icons.Outlined.Mood, "心情", onRecordMood),
        Triple(Icons.Outlined.WaterDrop, "喝水", onRecordWater),
        Triple(Icons.Outlined.DirectionsWalk, "步数", onRecordSteps)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(actions) { (icon, label, onClick) ->
            CleanQuickActionButton(icon = icon, label = label, onClick = onClick)
        }
    }
}

@Composable
private fun CleanQuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = CleanColors.primary),
                onClick = onClick
            )
            .padding(Spacing.xs)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(Radius.md),
            color = CleanColors.surface,
            shadowElevation = Elevation.xs
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = CleanColors.primary,
                    modifier = Modifier.size(IconSize.md)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textSecondary
        )
    }
}

/**
 * 周统计卡片
 */
@Composable
private fun CleanWeeklyStatsCard(analysis: HealthAnalysisData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = CleanColors.textTertiary,
                    modifier = Modifier.size(IconSize.md)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "本周统计",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CleanWeeklyStatItem(
                    icon = Icons.Outlined.DirectionsRun,
                    label = "运动天数",
                    value = "${analysis.exerciseDays}/7",
                    progress = analysis.exerciseDays / 7f
                )
                CleanWeeklyStatItem(
                    icon = Icons.Outlined.Bedtime,
                    label = "平均睡眠",
                    value = analysis.avgSleepHours?.let { "${String.format("%.1f", it)}h" } ?: "--",
                    progress = ((analysis.avgSleepHours ?: 0.0) / 8.0).toFloat().coerceIn(0f, 1f)
                )
                CleanWeeklyStatItem(
                    icon = Icons.Outlined.Mood,
                    label = "平均心情",
                    value = analysis.avgMoodRating?.let { String.format("%.1f", it) } ?: "--",
                    progress = ((analysis.avgMoodRating ?: 0.0) / 5.0).toFloat().coerceIn(0f, 1f)
                )
            }
        }
    }
}

@Composable
private fun CleanWeeklyStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    progress: Float
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp,
                color = CleanColors.primary,
                trackColor = CleanColors.borderLight
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CleanColors.primary,
                modifier = Modifier.size(IconSize.md)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = value,
            style = CleanTypography.amountSmall,
            color = CleanColors.textPrimary
        )
        Text(
            text = label,
            style = CleanTypography.caption,
            color = CleanColors.textTertiary
        )
    }
}

/**
 * 类型筛选行
 */
@Composable
private fun CleanTypeFilterRow(
    selectedType: String?,
    onSelectType: (String?) -> Unit
) {
    data class TypeFilter(val type: String?, val label: String, val icon: ImageVector)

    val types = listOf(
        TypeFilter(null, "全部", Icons.Outlined.GridView),
        TypeFilter(HealthRecordType.WEIGHT, "体重", Icons.Outlined.MonitorWeight),
        TypeFilter(HealthRecordType.SLEEP, "睡眠", Icons.Outlined.Bedtime),
        TypeFilter(HealthRecordType.EXERCISE, "运动", Icons.Outlined.DirectionsRun),
        TypeFilter(HealthRecordType.MOOD, "心情", Icons.Outlined.Mood),
        TypeFilter(HealthRecordType.WATER, "饮水", Icons.Outlined.WaterDrop),
        TypeFilter(HealthRecordType.STEPS, "步数", Icons.Outlined.DirectionsWalk)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(types) { filter ->
            CleanFilterChip(
                label = filter.label,
                selected = selectedType == filter.type,
                onClick = { onSelectType(filter.type) }
            )
        }
    }
}

@Composable
private fun CleanFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.full),
        color = if (selected) CleanColors.primary else CleanColors.surfaceVariant
    ) {
        Text(
            text = label,
            style = CleanTypography.button,
            color = if (selected) CleanColors.onPrimary else CleanColors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        )
    }
}

/**
 * 健康记录项
 */
@Composable
private fun CleanHealthRecordItem(
    record: HealthRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val date = LocalDate.ofEpochDay(record.date.toLong())
    val dateStr = date.format(DateTimeFormatter.ofPattern("M月d日"))

    val typeIcon = getHealthRecordIcon(record.recordType)
    val typeColor = getHealthRecordColor(record.recordType)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = typeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(Radius.sm)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(IconSize.md)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = HealthRecordType.getDisplayName(record.recordType),
                        style = CleanTypography.body,
                        color = CleanColors.textPrimary
                    )
                    if (record.category != null) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        StatusTag(
                            text = when (record.recordType) {
                                HealthRecordType.EXERCISE -> ExerciseCategory.getDisplayName(record.category)
                                HealthRecordType.MOOD -> MoodSource.getDisplayName(record.category)
                                else -> record.category
                            },
                            color = typeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRecordValue(record),
                        style = CleanTypography.amountSmall,
                        color = typeColor
                    )

                    // 辅助数值
                    record.secondaryValue?.let { secondary ->
                        when (record.recordType) {
                            HealthRecordType.BLOOD_PRESSURE -> {
                                Text(
                                    text = " / ${secondary.toInt()}",
                                    style = CleanTypography.amountSmall,
                                    color = typeColor
                                )
                            }
                            HealthRecordType.EXERCISE -> {
                                if (secondary > 0) {
                                    Text(
                                        text = " · ${secondary.toInt()} kcal",
                                        style = CleanTypography.secondary,
                                        color = CleanColors.textSecondary
                                    )
                                }
                            }
                            else -> {}
                        }
                    }

                    // 评分
                    record.rating?.let { rating ->
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        when (record.recordType) {
                            HealthRecordType.SLEEP -> {
                                Text(
                                    text = "质量: ${SleepQuality.getDisplayName(rating)}",
                                    style = CleanTypography.caption,
                                    color = CleanColors.textSecondary
                                )
                            }
                            else -> {}
                        }
                    }
                }

                // 备注
                if (record.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = record.note,
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 日期和操作
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dateStr,
                    style = CleanTypography.caption,
                    color = CleanColors.textTertiary
                )
                record.time?.let {
                    Text(
                        text = it,
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
                        tint = CleanColors.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(IconSize.xs)
                    )
                }
            }
        }
    }
}

/**
 * 获取健康记录类型图标
 */
private fun getHealthRecordIcon(type: String): ImageVector {
    return when (type) {
        HealthRecordType.WEIGHT -> Icons.Outlined.MonitorWeight
        HealthRecordType.SLEEP -> Icons.Outlined.Bedtime
        HealthRecordType.EXERCISE -> Icons.Outlined.DirectionsRun
        HealthRecordType.MOOD -> Icons.Outlined.Mood
        HealthRecordType.WATER -> Icons.Outlined.WaterDrop
        HealthRecordType.BLOOD_PRESSURE -> Icons.Outlined.Favorite
        HealthRecordType.HEART_RATE -> Icons.Outlined.FavoriteBorder
        HealthRecordType.STEPS -> Icons.Outlined.DirectionsWalk
        else -> Icons.Outlined.HealthAndSafety
    }
}

/**
 * 获取健康记录类型颜色
 */
private fun getHealthRecordColor(type: String): Color {
    return when (type) {
        HealthRecordType.WEIGHT -> CleanColors.primary
        HealthRecordType.SLEEP -> Color(0xFF7C4DFF)
        HealthRecordType.EXERCISE -> CleanColors.success
        HealthRecordType.MOOD -> CleanColors.warning
        HealthRecordType.WATER -> Color(0xFF03A9F4)
        HealthRecordType.BLOOD_PRESSURE -> CleanColors.error
        HealthRecordType.HEART_RATE -> CleanColors.error
        HealthRecordType.STEPS -> CleanColors.primary
        else -> CleanColors.textSecondary
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
 * 删除确认对话框
 */
@Composable
private fun CleanDeleteConfirmDialog(
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
 * 添加健康记录对话框 - 简洁版本
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanAddHealthRecordDialog(
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

    val isValid = when (type) {
        HealthRecordType.MOOD -> true
        HealthRecordType.BLOOD_PRESSURE -> value.isNotEmpty() && secondaryValue.isNotEmpty()
        else -> value.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getHealthRecordIcon(type),
                    contentDescription = null,
                    tint = CleanColors.primary,
                    modifier = Modifier.size(IconSize.md)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = if (existingRecord != null) "编辑${HealthRecordType.getDisplayName(type)}"
                    else "记录${HealthRecordType.getDisplayName(type)}",
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                when (type) {
                    HealthRecordType.WEIGHT -> {
                        CleanTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                            label = "体重 (kg)",
                            placeholder = "例如: 65.5"
                        )
                    }
                    HealthRecordType.SLEEP -> {
                        CleanTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                            label = "睡眠时长 (小时)",
                            placeholder = "例如: 7.5"
                        )
                        Text(
                            text = "睡眠质量",
                            style = CleanTypography.secondary,
                            color = CleanColors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { quality ->
                                CleanRatingButton(
                                    rating = quality,
                                    label = SleepQuality.getDisplayName(quality),
                                    selected = rating == quality,
                                    onClick = { rating = quality }
                                )
                            }
                        }
                    }
                    HealthRecordType.EXERCISE -> {
                        CleanTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = "运动时长 (分钟)",
                            placeholder = "例如: 30"
                        )
                        ExposedDropdownMenuBox(
                            expanded = showCategoryDropdown,
                            onExpandedChange = { showCategoryDropdown = it }
                        ) {
                            CleanTextField(
                                value = selectedCategory?.let { ExerciseCategory.getDisplayName(it) } ?: "",
                                onValueChange = {},
                                label = "运动类型",
                                placeholder = "选择运动类型",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                                enabled = false,
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                ExerciseCategory.getAllCategories().forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                ExerciseCategory.getDisplayName(category),
                                                style = CleanTypography.body
                                            )
                                        },
                                        onClick = {
                                            selectedCategory = category
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        CleanTextField(
                            value = secondaryValue,
                            onValueChange = { secondaryValue = it.filter { c -> c.isDigit() } },
                            label = "消耗热量 (kcal) - 可选",
                            placeholder = "例如: 200"
                        )
                    }
                    HealthRecordType.MOOD -> {
                        Text(
                            text = "选择你的心情",
                            style = CleanTypography.secondary,
                            color = CleanColors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { mood ->
                                CleanRatingButton(
                                    rating = mood,
                                    label = MoodRating.getDisplayName(mood),
                                    selected = rating == mood,
                                    onClick = {
                                        rating = mood
                                        value = mood.toString()
                                    },
                                    showLabel = false
                                )
                            }
                        }
                        Text(
                            text = MoodRating.getDisplayName(rating),
                            style = CleanTypography.body,
                            color = CleanColors.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HealthRecordType.WATER -> {
                        Text(
                            text = "快速选择",
                            style = CleanTypography.secondary,
                            color = CleanColors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(150, 250, 350, 500).forEach { ml ->
                                CleanQuickSelectChip(
                                    text = "${ml}ml",
                                    selected = value == ml.toString(),
                                    onClick = { value = ml.toString() }
                                )
                            }
                        }
                        CleanTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = "自定义 (ml)",
                            placeholder = "例如: 300"
                        )
                    }
                    HealthRecordType.STEPS -> {
                        CleanTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = "步数",
                            placeholder = "例如: 8000"
                        )
                    }
                    HealthRecordType.BLOOD_PRESSURE -> {
                        CleanTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = "收缩压 (mmHg)",
                            placeholder = "例如: 120"
                        )
                        CleanTextField(
                            value = secondaryValue,
                            onValueChange = { secondaryValue = it.filter { c -> c.isDigit() } },
                            label = "舒张压 (mmHg)",
                            placeholder = "例如: 80"
                        )
                    }
                    HealthRecordType.HEART_RATE -> {
                        CleanTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = "心率 (bpm)",
                            placeholder = "例如: 72"
                        )
                    }
                }

                CleanTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "备注 (可选)",
                    placeholder = "添加一些备注...",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            CleanPrimaryButton(
                text = if (existingRecord != null) "保存" else "记录",
                onClick = {
                    val parsedValue = when (type) {
                        HealthRecordType.MOOD -> rating.toDouble()
                        else -> value.toDoubleOrNull() ?: return@CleanPrimaryButton
                    }
                    val parsedSecondary = secondaryValue.toDoubleOrNull()
                    val parsedRating = when (type) {
                        HealthRecordType.SLEEP, HealthRecordType.MOOD -> rating
                        else -> null
                    }
                    onSave(parsedValue, parsedSecondary, parsedRating, selectedCategory, note)
                },
                enabled = isValid
            )
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

@Composable
private fun CleanRatingButton(
    rating: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showLabel: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(Radius.sm),
            color = if (selected) CleanColors.primary else CleanColors.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = rating.toString(),
                    style = CleanTypography.amountSmall,
                    color = if (selected) CleanColors.onPrimary else CleanColors.textSecondary
                )
            }
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = label,
                style = CleanTypography.caption,
                color = CleanColors.textTertiary
            )
        }
    }
}

@Composable
private fun CleanQuickSelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.sm),
        color = if (selected) CleanColors.primary else CleanColors.surfaceVariant
    ) {
        Text(
            text = text,
            style = CleanTypography.button,
            color = if (selected) CleanColors.onPrimary else CleanColors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
        )
    }
}
