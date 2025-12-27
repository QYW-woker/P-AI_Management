package com.lifemanager.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToAISettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val showTimePicker by viewModel.showTimePicker.collectAsState()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
    val showClearDataDialog by viewModel.showClearDataDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // 外观设置
            item {
                SettingsSection(title = "外观") {
                    SwitchSettingItem(
                        icon = Icons.Outlined.DarkMode,
                        title = "深色模式",
                        subtitle = "使用深色主题",
                        checked = settings.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    ClickableSettingItem(
                        icon = Icons.Outlined.Language,
                        title = "语言",
                        value = settings.language,
                        onClick = { viewModel.showLanguagePickerDialog() }
                    )
                }
            }

            // 通知设置
            item {
                SettingsSection(title = "通知") {
                    SwitchSettingItem(
                        icon = Icons.Outlined.Notifications,
                        title = "开启通知",
                        subtitle = "接收提醒和通知",
                        checked = settings.enableNotification,
                        onCheckedChange = { viewModel.toggleNotification(it) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    ClickableSettingItem(
                        icon = Icons.Outlined.Schedule,
                        title = "每日提醒时间",
                        value = settings.reminderTime,
                        enabled = settings.enableNotification,
                        onClick = { viewModel.showTimePickerDialog() }
                    )
                }
            }

            // AI功能设置
            item {
                SettingsSection(title = "AI功能") {
                    ClickableSettingItem(
                        icon = Icons.Filled.SmartToy,
                        title = "AI设置",
                        value = "",
                        onClick = onNavigateToAISettings
                    )
                }
            }

            // 数据设置
            item {
                SettingsSection(title = "数据") {
                    SwitchSettingItem(
                        icon = Icons.Outlined.CloudSync,
                        title = "自动备份",
                        subtitle = "定期备份数据到云端",
                        checked = settings.autoBackup,
                        onCheckedChange = { viewModel.toggleAutoBackup(it) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    ClickableSettingItem(
                        icon = Icons.Outlined.CloudUpload,
                        title = "立即备份",
                        value = "",
                        onClick = { /* TODO */ }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    ClickableSettingItem(
                        icon = Icons.Outlined.CloudDownload,
                        title = "恢复数据",
                        value = "",
                        onClick = { /* TODO */ }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    ClickableSettingItem(
                        icon = Icons.Outlined.Delete,
                        title = "清除所有数据",
                        value = "",
                        isDanger = true,
                        onClick = { viewModel.showClearDataConfirmation() }
                    )
                }
            }

            // 关于
            item {
                SettingsSection(title = "关于") {
                    ClickableSettingItem(
                        icon = Icons.Outlined.Info,
                        title = "版本",
                        value = "1.0.0",
                        onClick = { }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    ClickableSettingItem(
                        icon = Icons.Outlined.Description,
                        title = "隐私政策",
                        value = "",
                        onClick = onNavigateToPrivacy
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    ClickableSettingItem(
                        icon = Icons.Outlined.Gavel,
                        title = "用户协议",
                        value = "",
                        onClick = onNavigateToTerms
                    )
                }
            }

            // 账户
            item {
                SettingsSection(title = "账户") {
                    ClickableSettingItem(
                        icon = Icons.Outlined.Login,
                        title = "登录/注册",
                        value = "",
                        onClick = onNavigateToLogin
                    )
                }
            }
        }
    }

    // 语言选择对话框
    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = settings.language,
            onSelect = { viewModel.setLanguage(it) },
            onDismiss = { viewModel.hideLanguagePickerDialog() }
        )
    }

    // 时间选择对话框
    if (showTimePicker) {
        TimePickerDialog(
            currentTime = settings.reminderTime,
            onConfirm = { viewModel.setReminderTime(it) },
            onDismiss = { viewModel.hideTimePickerDialog() }
        )
    }

    // 清除数据确认对话框
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideClearDataConfirmation() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("清除所有数据") },
            text = { Text("确定要清除所有数据吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllData() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确定清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideClearDataConfirmation() }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 设置分组
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            content()
        }
    }
}

/**
 * 开关设置项
 */
@Composable
private fun SwitchSettingItem(
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 可点击设置项
 */
@Composable
private fun ClickableSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDanger) {
                MaterialTheme.colorScheme.error
            } else if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDanger) {
                MaterialTheme.colorScheme.error
            } else if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 1f else 0.5f
            )
        )
    }
}

/**
 * 语言选择对话框
 */
@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf("简体中文", "English")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择语言") },
        text = {
            Column {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = { onSelect(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = language)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 时间选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    currentTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = currentTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提醒时间") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = String.format("%02d", timePickerState.hour)
                    val minute = String.format("%02d", timePickerState.minute)
                    onConfirm("$hour:$minute")
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
