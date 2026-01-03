package com.lifemanager.app.feature.diary

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifemanager.app.core.database.entity.DiaryEntity
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*

/**
 * 日记主界面 - 简洁设计版本
 *
 * 设计原则:
 * - 干净、克制、有呼吸感
 * - 轻灵不花哨
 * - 使用统一的设计系统
 * - 不使用emoji，使用Material图标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanDiaryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit = {},
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val diaries by viewModel.diaries.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    // 获取今日日期用于新建日记
    val todayDate = remember {
        java.time.LocalDate.now().toEpochDay().toInt()
    }

    Scaffold(
        containerColor = CleanColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "日记",
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
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == "LIST") Icons.Outlined.CalendarMonth else Icons.Outlined.ViewList,
                            contentDescription = if (viewMode == "LIST") "日历视图" else "列表视图",
                            tint = CleanColors.textSecondary
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
                onClick = { onNavigateToEdit(todayDate) },
                containerColor = CleanColors.primary,
                contentColor = CleanColors.onPrimary,
                shape = RoundedCornerShape(Radius.md)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "写日记")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 简洁统计卡片
            CleanDiaryStatsCard(statistics = statistics)

            // 月份选择器
            CleanMonthSelector(
                yearMonth = currentYearMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                formatYearMonth = { viewModel.formatYearMonth(it) }
            )

            when (uiState) {
                is DiaryUiState.Loading -> {
                    PageLoadingState()
                }

                is DiaryUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as DiaryUiState.Error).message,
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

                is DiaryUiState.Success -> {
                    if (diaries.isEmpty()) {
                        EmptyStateView(
                            message = "本月还没有日记",
                            icon = Icons.Outlined.Book,
                            actionText = "开始记录",
                            onActionClick = { onNavigateToEdit(todayDate) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Spacing.pageHorizontal,
                                vertical = Spacing.md
                            ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            items(diaries, key = { it.id }) { diary ->
                                CleanDiaryItem(
                                    diary = diary,
                                    formatDate = { viewModel.formatDate(it) },
                                    getDayOfWeek = { viewModel.getDayOfWeek(it) },
                                    onClick = { onNavigateToEdit(diary.date) }
                                )
                            }

                            // 底部安全间距
                            item {
                                Spacer(modifier = Modifier.height(Spacing.bottomSafe + 56.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        CleanDeleteDialog(
            title = "确认删除",
            message = "确定要删除这篇日记吗？",
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.hideDeleteConfirm() }
        )
    }
}

/**
 * 简洁统计卡片 - 使用图标代替emoji
 */
@Composable
private fun CleanDiaryStatsCard(statistics: DiaryStatistics) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageHorizontal, vertical = Spacing.md),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CleanStatItem(
                label = "总篇数",
                value = statistics.totalCount.toString(),
                icon = Icons.Outlined.Book
            )
            CleanStatItem(
                label = "连续",
                value = "${statistics.currentStreak}天",
                icon = Icons.Outlined.LocalFireDepartment
            )
            CleanStatItem(
                label = "平均心情",
                value = String.format("%.1f", statistics.averageMood),
                icon = getMoodIcon(statistics.averageMood.toInt())
            )
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
            modifier = Modifier.size(IconSize.lg)
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
 * 简洁月份选择器
 */
@Composable
private fun CleanMonthSelector(
    yearMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    formatYearMonth: (Int) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CleanColors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "上个月",
                    tint = CleanColors.textSecondary
                )
            }

            Text(
                text = formatYearMonth(yearMonth),
                style = CleanTypography.title,
                color = CleanColors.textPrimary
            )

            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "下个月",
                    tint = CleanColors.textSecondary
                )
            }
        }
    }
}

/**
 * 简洁日记项
 */
@Composable
private fun CleanDiaryItem(
    diary: DiaryEntity,
    formatDate: (Int) -> String,
    getDayOfWeek: (Int) -> String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val attachments = remember(diary.attachments) {
        parseAttachments(diary.attachments)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.surface,
        shadowElevation = Elevation.xs
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // 日期和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDate(diary.date),
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = getDayOfWeek(diary.date),
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // 附件数量指示
                    if (attachments.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = "附件",
                                modifier = Modifier.size(16.dp),
                                tint = CleanColors.textTertiary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = attachments.size.toString(),
                                style = CleanTypography.caption,
                                color = CleanColors.textTertiary
                            )
                        }
                    }

                    // 天气图标
                    diary.weather?.let { weather ->
                        Icon(
                            imageVector = getWeatherIcon(weather),
                            contentDescription = weather,
                            modifier = Modifier.size(18.dp),
                            tint = CleanColors.textSecondary
                        )
                    }

                    // 心情图标
                    diary.moodScore?.let { score ->
                        Icon(
                            imageVector = getMoodIcon(score),
                            contentDescription = "心情",
                            modifier = Modifier.size(20.dp),
                            tint = getMoodColor(score)
                        )
                    }
                }
            }

            // 附件预览
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(attachments.take(4), key = { it }) { attachment ->
                        val isVideo = attachment.contains("video") ||
                                attachment.endsWith(".mp4") ||
                                attachment.endsWith(".mov")
                        Box(modifier = Modifier.size(56.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.parse(attachment))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "附件",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(Radius.sm)),
                                contentScale = ContentScale.Crop
                            )
                            if (isVideo) {
                                Icon(
                                    imageVector = Icons.Filled.PlayCircle,
                                    contentDescription = "视频",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp),
                                    tint = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                    if (attachments.size > 4) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(Radius.sm))
                                    .background(CleanColors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${attachments.size - 4}",
                                    style = CleanTypography.secondary,
                                    color = CleanColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 内容预览
            Text(
                text = diary.content,
                style = CleanTypography.body,
                color = CleanColors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // 箭头指示 - 表明可以点击查看详情
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CleanColors.textTertiary,
                    modifier = Modifier.size(IconSize.sm)
                )
            }
        }
    }
}

/**
 * 解析附件JSON
 */
private fun parseAttachments(attachmentsJson: String): List<String> {
    if (attachmentsJson.isBlank() || attachmentsJson == "[]") return emptyList()
    return try {
        attachmentsJson
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * 获取心情图标
 */
private fun getMoodIcon(score: Int): ImageVector {
    return when (score) {
        1 -> Icons.Outlined.SentimentVeryDissatisfied
        2 -> Icons.Outlined.SentimentDissatisfied
        3 -> Icons.Outlined.SentimentNeutral
        4 -> Icons.Outlined.SentimentSatisfied
        5 -> Icons.Outlined.SentimentVerySatisfied
        else -> Icons.Outlined.SentimentNeutral
    }
}

/**
 * 获取心情颜色
 */
private fun getMoodColor(score: Int): Color {
    return when (score) {
        1, 2 -> CleanColors.textTertiary
        3 -> CleanColors.warning
        4, 5 -> CleanColors.success
        else -> CleanColors.textSecondary
    }
}

/**
 * 获取天气图标
 */
private fun getWeatherIcon(weather: String): ImageVector {
    return when (weather.uppercase()) {
        "SUNNY" -> Icons.Outlined.WbSunny
        "CLOUDY" -> Icons.Outlined.Cloud
        "OVERCAST" -> Icons.Outlined.CloudQueue
        "LIGHT_RAIN" -> Icons.Outlined.Grain
        "RAINY", "HEAVY_RAIN" -> Icons.Outlined.Umbrella
        "THUNDERSTORM" -> Icons.Outlined.Thunderstorm
        "SNOWY" -> Icons.Outlined.AcUnit
        "WINDY" -> Icons.Outlined.Air
        "FOGGY" -> Icons.Outlined.Cloud
        else -> Icons.Outlined.Cloud
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
