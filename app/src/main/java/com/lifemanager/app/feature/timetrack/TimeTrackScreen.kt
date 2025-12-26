package com.lifemanager.app.feature.timetrack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.TimeCategoryEntity
import com.lifemanager.app.domain.model.*

/**
 * 时间统计主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTrackScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimeTrackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val categoryDurations by viewModel.categoryDurations.collectAsState()
    val showCategoryDialog by viewModel.showCategoryDialog.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时间统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Filled.Add, contentDescription = "手动添加")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 计时器卡片
            TimerCard(
                timerState = timerState,
                onStart = { viewModel.showCategoryPicker() },
                onStop = { viewModel.stopTimer() },
                formatTime = { viewModel.formatElapsedTime(it) }
            )

            // 今日统计
            TodayStatsCard(
                stats = todayStats,
                formatDuration = { viewModel.formatDuration(it) }
            )

            when (uiState) {
                is TimeTrackUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TimeTrackUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as TimeTrackUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is TimeTrackUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 分类时长
                        if (categoryDurations.isNotEmpty()) {
                            item {
                                Text(
                                    text = "今日分类",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            item {
                                CategoryDurationsRow(
                                    durations = categoryDurations,
                                    formatDuration = { viewModel.formatDuration(it) }
                                )
                            }
                        }

                        // 今日记录
                        item {
                            Text(
                                text = "今日记录",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (todayRecords.isEmpty()) {
                            item {
                                EmptyState()
                            }
                        } else {
                            items(todayRecords, key = { it.record.id }) { record ->
                                RecordItem(
                                    record = record,
                                    formatDuration = { viewModel.formatDuration(it) },
                                    onDelete = { viewModel.deleteRecord(record.record.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 分类选择对话框
    if (showCategoryDialog) {
        CategoryPickerDialog(
            categories = categories,
            onSelect = { viewModel.startTimer(it) },
            onDismiss = { viewModel.hideCategoryPicker() }
        )
    }

    // 手动添加对话框
    if (showAddDialog) {
        AddRecordDialog(
            viewModel = viewModel,
            categories = categories,
            onDismiss = { viewModel.hideAddDialog() }
        )
    }
}

@Composable
private fun TimerCard(
    timerState: TimerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    formatTime: (Long) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (timerState.isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (timerState.isRunning) {
                Text(
                    text = timerState.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = formatTime(timerState.elapsedSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (timerState.isRunning)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledIconButton(
                onClick = if (timerState.isRunning) onStop else onStart,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (timerState.isRunning)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (timerState.isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (timerState.isRunning) "停止" else "开始",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayStatsCard(
    stats: TodayTimeStats,
    formatDuration: (Int) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDuration(stats.totalMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "今日总时长",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stats.recordCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "记录数",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryDurationsRow(
    durations: List<CategoryDuration>,
    formatDuration: (Int) -> String
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(durations, key = { it.categoryId ?: 0 }) { duration ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = parseColor(duration.categoryColor).copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = duration.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = parseColor(duration.categoryColor)
                    )
                    Text(
                        text = formatDuration(duration.durationMinutes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordItem(
    record: TimeRecordWithCategory,
    formatDuration: (Int) -> String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        record.category?.let { parseColor(it.color) }
                            ?: MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.category?.name ?: "未分类",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (record.record.note.isNotBlank()) {
                    Text(
                        text = record.record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = formatDuration(record.record.durationMinutes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<TimeCategoryEntity>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择分类") },
        text = {
            Column {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(category.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(parseColor(category.color))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = category.name)
                    }
                }
                if (categories.isEmpty()) {
                    Text(
                        text = "暂无分类，将使用默认分类",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(null) }) {
                Text("不选择分类")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordDialog(
    viewModel: TimeTrackViewModel,
    categories: List<TimeCategoryEntity>,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动添加记录") },
        text = {
            Column {
                // 时长输入
                OutlinedTextField(
                    value = if (editState.durationMinutes > 0) editState.durationMinutes.toString() else "",
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { viewModel.updateEditDuration(it) }
                    },
                    label = { Text("时长（分钟）") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 备注
                OutlinedTextField(
                    value = editState.note,
                    onValueChange = { viewModel.updateEditNote(it) },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )

                editState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.saveManualRecord() },
                enabled = !editState.isSaving
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "今日暂无记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
