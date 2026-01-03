package com.lifemanager.app.feature.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifemanager.app.domain.model.SleepDuration
import com.lifemanager.app.domain.model.moodList
import com.lifemanager.app.domain.model.quickSleepOptions
import com.lifemanager.app.domain.model.weatherList
import com.lifemanager.app.ui.component.PremiumTextField
import kotlin.math.roundToInt
import com.lifemanager.app.ui.theme.AppColors

/**
 * 编辑日记对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDiaryDialog(
    viewModel: DiaryViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()

    // 动画效果
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialogScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .scale(scale)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = AppColors.Primary.copy(alpha = 0.2f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f),
                            AppColors.Primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = viewModel.formatDate(editState.date) + " " +
                                    viewModel.getDayOfWeek(editState.date)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveDiary() },
                            enabled = !editState.isSaving
                        ) {
                            if (editState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("保存")
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 错误提示
                    editState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 心情选择
                    Text(
                        text = "今天的心情",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(moodList, key = { it.score }) { mood ->
                            MoodChip(
                                emoji = mood.emoji,
                                name = mood.name,
                                color = Color(mood.color),
                                selected = editState.moodScore == mood.score,
                                onClick = {
                                    viewModel.updateEditMood(
                                        if (editState.moodScore == mood.score) null else mood.score
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 天气选择
                    Text(
                        text = "今天的天气",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(weatherList, key = { it.code }) { weather ->
                            WeatherChip(
                                emoji = weather.emoji,
                                name = weather.name,
                                selected = editState.weather == weather.code,
                                onClick = {
                                    viewModel.updateEditWeather(
                                        if (editState.weather == weather.code) null else weather.code
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 睡眠时长选择
                    SleepDurationSection(
                        sleepMinutes = editState.sleepMinutes,
                        onSleepMinutesChange = { viewModel.updateEditSleep(it) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 图片/视频附件
                    Text(
                        text = "图片/视频",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AttachmentSection(
                        attachments = editState.attachments,
                        onAddAttachment = { uri -> viewModel.addAttachment(uri) },
                        onRemoveAttachment = { uri -> viewModel.removeAttachment(uri) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 日记内容
                    Text(
                        text = "日记内容",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.content,
                        onValueChange = { viewModel.updateEditContent(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        placeholder = "记录今天的点滴...",
                        singleLine = false,
                        maxLines = Int.MAX_VALUE
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentSection(
    attachments: List<String>,
    onAddAttachment: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit
) {
    val context = LocalContext.current

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        uris.forEach { uri ->
            // 请求持久化URI权限
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onAddAttachment(uri.toString())
        }
    }

    // 视频选择器
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onAddAttachment(it.toString())
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 添加图片按钮
        item {
            AddMediaButton(
                icon = Icons.Filled.Image,
                label = "图片",
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // 添加视频按钮
        item {
            AddMediaButton(
                icon = Icons.Filled.Videocam,
                label = "视频",
                onClick = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )
        }

        // 已添加的附件
        items(attachments, key = { it }) { attachment ->
            AttachmentItem(
                uri = attachment,
                onRemove = { onRemoveAttachment(attachment) }
            )
        }
    }
}

@Composable
private fun AddMediaButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttachmentItem(
    uri: String,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = uri.contains("video") || uri.endsWith(".mp4") || uri.endsWith(".mov")

    Box(
        modifier = Modifier.size(80.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(uri))
                        .crossfade(true)
                        .build(),
                    contentDescription = "附件",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 视频标识
                if (isVideo) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "视频",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 删除按钮 - 调小尺寸
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "删除",
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun MoodChip(
    emoji: String,
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherChip(
    emoji: String,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = name)
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = name)
            }
        }
    }
}

/**
 * 睡眠时长选择组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepDurationSection(
    sleepMinutes: Int?,
    onSleepMinutesChange: (Int?) -> Unit
) {
    val sleepDuration = SleepDuration.fromMinutes(sleepMinutes)
    var sliderValue by remember(sleepMinutes) {
        mutableFloatStateOf((sleepMinutes ?: 0).toFloat() / 60f)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "昨晚睡眠",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            // 显示当前睡眠时长
            if (sleepDuration != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌙",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sleepDuration.formatDisplay(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 快捷选择按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSleepOptions.forEach { hours ->
                val minutes = hours * 60
                val isSelected = sleepMinutes == minutes

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onSleepMinutesChange(null)
                            sliderValue = 0f
                        } else {
                            onSleepMinutesChange(minutes)
                            sliderValue = hours.toFloat()
                        }
                    },
                    label = {
                        Text("${hours}h")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 滑动条
        Column {
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                    // 四舍五入到15分钟
                    val totalMinutes = (newValue * 60).roundToInt()
                    val roundedMinutes = ((totalMinutes + 7) / 15) * 15
                    if (roundedMinutes > 0) {
                        onSleepMinutesChange(roundedMinutes)
                    } else {
                        onSleepMinutesChange(null)
                    }
                },
                valueRange = 0f..12f,
                steps = 47, // 0-12小时，每15分钟一个步进 (12*4 - 1 = 47)
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "6h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "12h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
