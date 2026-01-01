package com.lifemanager.app.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifemanager.app.R
import com.lifemanager.app.core.floatingball.FloatingBallManager
import com.lifemanager.app.ui.theme.AppColors
import java.io.File

/**
 * AI设置页面
 *
 * 提供丰富的AI功能配置选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel(),
    floatingBallManager: FloatingBallManager
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val showVoiceSelector by viewModel.showVoiceSelector.collectAsState()
    val showPersonalitySelector by viewModel.showPersonalitySelector.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()

    // 悬浮球权限状态
    var permissionStatus by remember { mutableStateOf(floatingBallManager.getPermissionStatus()) }
    var showBackgroundRunDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setCustomAvatar(it) }
    }

    // 刷新权限状态
    LaunchedEffect(Unit) {
        permissionStatus = floatingBallManager.getPermissionStatus()
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景渐变
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
                                Color(0xFFF0F4FF),
                                Color(0xFFE8F0FF),
                                Color(0xFFE0ECFF)
                            )
                        }
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AI设置",
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
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // AI助手设置
                item {
                    AISettingsSection(
                        title = "AI助手",
                        icon = "🧠",
                        gradientColors = AppColors.GradientCosmic
                    ) {
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.SmartToy,
                            title = "启用AI助手",
                            subtitle = "开启智能语音助手功能",
                            checked = settings.aiEnabled,
                            onCheckedChange = { viewModel.setAIEnabled(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsClickableItem(
                            icon = Icons.Outlined.Person,
                            title = "AI性格",
                            value = settings.personality.displayName,
                            onClick = { viewModel.showPersonalitySelectorDialog() }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsClickableItem(
                            icon = Icons.Outlined.RecordVoiceOver,
                            title = "语音风格",
                            value = settings.voiceStyle.displayName,
                            onClick = { viewModel.showVoiceSelectorDialog() }
                        )
                    }
                }

                // 悬浮球设置
                item {
                    AISettingsSection(
                        title = "悬浮球",
                        icon = "🎈",
                        gradientColors = AppColors.GradientRose
                    ) {
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Circle,
                            title = "显示悬浮球",
                            subtitle = "在屏幕上显示AI助手悬浮球",
                            checked = settings.floatingBallEnabled,
                            onCheckedChange = { viewModel.setFloatingBallEnabled(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsClickableItem(
                            icon = Icons.Outlined.BatteryChargingFull,
                            title = "后台常驻",
                            value = if (permissionStatus.hasBatteryOptimizationExemption) "已开启" else "未开启",
                            onClick = { showBackgroundRunDialog = true }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.TouchApp,
                            title = "自动隐藏",
                            subtitle = "无操作时自动收起悬浮球",
                            checked = settings.autoHideFloatingBall,
                            onCheckedChange = { viewModel.setAutoHideFloatingBall(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Mood,
                            title = "心情同步",
                            subtitle = "悬浮球表情跟随日记心情变化",
                            checked = settings.moodSync,
                            onCheckedChange = { viewModel.setMoodSync(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSliderItem(
                            icon = Icons.Outlined.Opacity,
                            title = "透明度",
                            value = settings.floatingBallOpacity,
                            onValueChange = { viewModel.setFloatingBallOpacity(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsAvatarItem(
                            customAvatarPath = settings.customAvatarPath,
                            onSelectImage = { showAvatarDialog = true }
                        )
                    }
                }

                // 语音识别设置
                item {
                    AISettingsSection(
                        title = "语音识别",
                        icon = "🎤",
                        gradientColors = AppColors.GradientEmerald
                    ) {
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Mic,
                            title = "语音记账",
                            subtitle = "通过语音快速记账",
                            checked = settings.voiceAccountingEnabled,
                            onCheckedChange = { viewModel.setVoiceAccountingEnabled(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.VolumeUp,
                            title = "语音反馈",
                            subtitle = "操作后播放语音反馈",
                            checked = settings.voiceFeedback,
                            onCheckedChange = { viewModel.setVoiceFeedback(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Hearing,
                            title = "唤醒词",
                            subtitle = "说\"小管家\"唤醒AI助手",
                            checked = settings.wakeWordEnabled,
                            onCheckedChange = { viewModel.setWakeWordEnabled(it) }
                        )
                    }
                }

                // 智能分析设置
                item {
                    AISettingsSection(
                        title = "智能分析",
                        icon = "📊",
                        gradientColors = AppColors.GradientGold
                    ) {
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Analytics,
                            title = "消费分析",
                            subtitle = "智能分析消费习惯并给出建议",
                            checked = settings.expenseAnalysis,
                            onCheckedChange = { viewModel.setExpenseAnalysis(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.TrendingUp,
                            title = "预算预警",
                            subtitle = "预算即将超支时智能提醒",
                            checked = settings.budgetWarning,
                            onCheckedChange = { viewModel.setBudgetWarning(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Lightbulb,
                            title = "省钱建议",
                            subtitle = "根据消费记录提供省钱建议",
                            checked = settings.savingTips,
                            onCheckedChange = { viewModel.setSavingTips(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Psychology,
                            title = "情绪洞察",
                            subtitle = "分析日记情绪并给出关怀",
                            checked = settings.emotionInsight,
                            onCheckedChange = { viewModel.setEmotionInsight(it) }
                        )
                    }
                }

                // 图像识别设置
                item {
                    AISettingsSection(
                        title = "图像识别",
                        icon = "📷",
                        gradientColors = AppColors.GradientSky
                    ) {
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Screenshot,
                            title = "截图识别",
                            subtitle = "自动识别支付截图记账",
                            checked = settings.screenshotRecognition,
                            onCheckedChange = { viewModel.setScreenshotRecognition(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Receipt,
                            title = "发票识别",
                            subtitle = "拍照识别发票自动记账",
                            checked = settings.invoiceRecognition,
                            onCheckedChange = { viewModel.setInvoiceRecognition(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.CreditCard,
                            title = "银行卡识别",
                            subtitle = "拍照识别银行账单",
                            checked = settings.bankCardRecognition,
                            onCheckedChange = { viewModel.setBankCardRecognition(it) }
                        )
                    }
                }

                // 高级设置
                item {
                    AISettingsSection(
                        title = "高级设置",
                        icon = "⚙️",
                        gradientColors = AppColors.GradientMint
                    ) {
                        AISettingsClickableItem(
                            icon = Icons.Outlined.Key,
                            title = "API密钥",
                            value = if (settings.hasApiKey) "已配置" else "未配置",
                            onClick = { viewModel.showApiKeyDialog() }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.Cloud,
                            title = "云端AI",
                            subtitle = "使用云端AI获得更强大功能",
                            checked = settings.cloudAI,
                            onCheckedChange = { viewModel.setCloudAI(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsSwitchItem(
                            icon = Icons.Outlined.History,
                            title = "对话历史",
                            subtitle = "保存与AI的对话记录",
                            checked = settings.saveHistory,
                            onCheckedChange = { viewModel.setSaveHistory(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        AISettingsClickableItem(
                            icon = Icons.Outlined.Delete,
                            title = "清除对话历史",
                            value = "",
                            isDanger = true,
                            onClick = { viewModel.clearHistory() }
                        )
                    }
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // 语音风格选择对话框
    if (showVoiceSelector) {
        VoiceSelectorDialog(
            currentVoice = settings.voiceStyle,
            onSelect = { viewModel.setVoiceStyle(it) },
            onDismiss = { viewModel.hideVoiceSelectorDialog() }
        )
    }

    // AI性格选择对话框
    if (showPersonalitySelector) {
        PersonalitySelectorDialog(
            currentPersonality = settings.personality,
            onSelect = { viewModel.setPersonality(it) },
            onDismiss = { viewModel.hidePersonalitySelectorDialog() }
        )
    }

    // API密钥配置对话框
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = settings.apiKey,
            onSave = { viewModel.setApiKey(it) },
            onDismiss = { viewModel.hideApiKeyDialog() }
        )
    }

    // 后台常驻设置对话框
    if (showBackgroundRunDialog) {
        BackgroundRunDialog(
            permissionStatus = permissionStatus,
            onRequestBatteryOptimization = {
                floatingBallManager.requestDisableBatteryOptimization()?.let { intent ->
                    context.startActivity(intent)
                }
                showBackgroundRunDialog = false
            },
            onOpenAutoStartSettings = {
                floatingBallManager.getAutoStartSettingsIntent()?.let { intent ->
                    context.startActivity(intent)
                }
                showBackgroundRunDialog = false
            },
            onDismiss = {
                showBackgroundRunDialog = false
                // 刷新权限状态
                permissionStatus = floatingBallManager.getPermissionStatus()
            }
        )
    }

    // 悬浮球形象选择对话框
    if (showAvatarDialog) {
        AvatarSelectorDialog(
            hasCustomAvatar = settings.hasCustomAvatar,
            customAvatarPath = settings.customAvatarPath,
            onSelectFromGallery = {
                imagePickerLauncher.launch("image/*")
                showAvatarDialog = false
            },
            onResetToDefault = {
                viewModel.clearCustomAvatar()
                showAvatarDialog = false
            },
            onDismiss = { showAvatarDialog = false }
        )
    }
}

@Composable
private fun AISettingsSection(
    title: String,
    icon: String,
    gradientColors: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.15f) }),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = gradientColors.first()
                )
            }

            content()
        }
    }
}

@Composable
private fun AISettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Primary
            )
        )
    }
}

@Composable
private fun AISettingsClickableItem(
    icon: ImageVector,
    title: String,
    value: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDanger) MaterialTheme.colorScheme.error else AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AISettingsSliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(start = 40.dp),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Primary,
                activeTrackColor = AppColors.Primary
            )
        )
    }
}

@Composable
private fun VoiceSelectorDialog(
    currentVoice: VoiceStyle,
    onSelect: (VoiceStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("选择语音风格", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                VoiceStyle.entries.forEach { voice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(voice) }
                            .background(
                                if (voice == currentVoice)
                                    AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(voice.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = voice.displayName,
                                fontWeight = if (voice == currentVoice) FontWeight.Medium else FontWeight.Normal
                            )
                            Text(
                                text = voice.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = voice == currentVoice,
                            onClick = { onSelect(voice) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = AppColors.Primary)
            }
        }
    )
}

@Composable
private fun PersonalitySelectorDialog(
    currentPersonality: AIPersonality,
    onSelect: (AIPersonality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("选择AI性格", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AIPersonality.entries.forEach { personality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(personality) }
                            .background(
                                if (personality == currentPersonality)
                                    AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(personality.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = personality.displayName,
                                fontWeight = if (personality == currentPersonality) FontWeight.Medium else FontWeight.Normal
                            )
                            Text(
                                text = personality.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = personality == currentPersonality,
                            onClick = { onSelect(personality) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = AppColors.Primary)
            }
        }
    )
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("配置API密钥", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "配置OpenAI或其他AI服务的API密钥，以使用云端AI功能。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API密钥") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKey)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
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
private fun BackgroundRunDialog(
    permissionStatus: com.lifemanager.app.core.floatingball.FloatingBallPermissionStatus,
    onRequestBatteryOptimization: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔋", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("后台常驻设置", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "为确保AI悬浮球在应用退到后台时仍能正常显示，请完成以下设置：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 电池优化设置
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (permissionStatus.hasBatteryOptimizationExemption)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else
                            Color(0xFFFFC107).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !permissionStatus.hasBatteryOptimizationExemption) {
                                onRequestBatteryOptimization()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (permissionStatus.hasBatteryOptimizationExemption)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (permissionStatus.hasBatteryOptimizationExemption)
                                Color(0xFF4CAF50)
                            else
                                Color(0xFFFFC107),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "忽略电池优化",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (permissionStatus.hasBatteryOptimizationExemption)
                                    "已设置"
                                else
                                    "点击设置，防止系统杀死后台服务",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!permissionStatus.hasBatteryOptimizationExemption) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 自启动设置（国产ROM）
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAutoStartSettings() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Autorenew,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自启动权限",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "部分手机需要开启自启动权限",
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

                // 提示信息
                Text(
                    text = "💡 提示：不同品牌手机设置位置可能不同，如小米在\"设置-应用管理-自启动\"，华为在\"设置-应用-应用启动管理\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = AppColors.Primary)
            }
        }
    )
}

/**
 * 悬浮球形象设置项
 */
@Composable
private fun AISettingsAvatarItem(
    customAvatarPath: String?,
    onSelectImage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectImage)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Face,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "悬浮球形象",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (customAvatarPath != null) "已自定义" else "使用默认形象",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 预览图
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (customAvatarPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(customAvatarPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "自定义形象",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_fairy_assistant),
                    contentDescription = "默认形象",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 悬浮球形象选择对话框
 */
@Composable
private fun AvatarSelectorDialog(
    hasCustomAvatar: Boolean,
    customAvatarPath: String?,
    onSelectFromGallery: () -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("悬浮球形象", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 当前形象预览
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, AppColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (customAvatarPath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(customAvatarPath))
                                .crossfade(true)
                                .build(),
                            contentDescription = "当前形象",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fairy_assistant),
                            contentDescription = "默认形象",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }

                Text(
                    text = if (hasCustomAvatar) "当前使用自定义形象" else "当前使用默认形象",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "建议使用正方形PNG图片，推荐尺寸256x256像素",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 选择按钮
                Button(
                    onClick = onSelectFromGallery,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从相册选择")
                }

                // 恢复默认按钮
                if (hasCustomAvatar) {
                    OutlinedButton(
                        onClick = onResetToDefault,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("恢复默认形象")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = AppColors.Primary)
            }
        }
    )
}

/**
 * 语音风格
 */
enum class VoiceStyle(val displayName: String, val icon: String, val description: String) {
    SWEET("甜美女声", "👧", "温柔甜美的女性声音"),
    GENTLE("温柔男声", "👦", "温和亲切的男性声音"),
    PROFESSIONAL("专业播报", "🎙️", "标准新闻播报风格"),
    CUTE("可爱童声", "🧒", "活泼可爱的童声"),
    WISE("睿智长者", "👴", "沉稳睿智的声音")
}

/**
 * AI性格
 */
enum class AIPersonality(val displayName: String, val icon: String, val description: String) {
    FRIENDLY("友好亲切", "😊", "热情友好，像朋友一样交流"),
    PROFESSIONAL("专业严谨", "🧑‍💼", "专业准确，注重效率"),
    HUMOROUS("幽默风趣", "😄", "轻松幽默，让记账更有趣"),
    CARING("温馨关怀", "🤗", "关心体贴，时刻关注你的状态"),
    MOTIVATING("激励鼓舞", "💪", "积极向上，帮你养成好习惯")
}

// 扩展函数
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
