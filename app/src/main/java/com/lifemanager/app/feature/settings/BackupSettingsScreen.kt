package com.lifemanager.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.backup.BackupInfo
import com.lifemanager.app.core.backup.BackupState
import com.lifemanager.app.core.backup.CloudProvider
import com.lifemanager.app.ui.theme.AppColors

/**
 * 数据备份设置页面
 *
 * 支持:
 * - 自动备份到云端 (百度网盘/阿里云盘)
 * - 自定义备份周期
 * - 立即备份到本地
 * - 从本地/云端恢复数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupSettingsViewModel = hiltViewModel()
) {
    val backupState by viewModel.backupState.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val backupInterval by viewModel.backupInterval.collectAsState()
    val currentProvider by viewModel.currentProvider.collectAsState()
    val baiduConnected by viewModel.baiduConnected.collectAsState()
    val aliyunConnected by viewModel.aliyunConnected.collectAsState()
    val localBackups by viewModel.localBackups.collectAsState()
    val cloudBackups by viewModel.cloudBackups.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

    val showIntervalPicker by viewModel.showIntervalPicker.collectAsState()
    val showProviderPicker by viewModel.showProviderPicker.collectAsState()
    val showRestoreDialog by viewModel.showRestoreDialog.collectAsState()
    val showCloudConnectDialog by viewModel.showCloudConnectDialog.collectAsState()
    val connectingProvider by viewModel.connectingProvider.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 处理备份状态变化
    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearState()
            }
            is BackupState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景渐变
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
                        } else {
                            listOf(Color(0xFFF5F7FF), Color(0xFFFFF5F8), Color(0xFFF0F9FF))
                        }
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💾", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "数据备份",
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
                // 自动备份设置
                item {
                    BackupSettingsSection(
                        title = "自动备份",
                        icon = "⏰",
                        gradientColors = AppColors.GradientCosmic
                    ) {
                        BackupSwitchItem(
                            icon = Icons.Outlined.CloudSync,
                            title = "启用自动备份",
                            subtitle = "定期自动备份数据到云端",
                            checked = autoBackupEnabled,
                            onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        BackupClickableItem(
                            icon = Icons.Outlined.Schedule,
                            title = "备份周期",
                            value = getIntervalDisplayName(backupInterval),
                            enabled = autoBackupEnabled,
                            onClick = { viewModel.showIntervalPickerDialog() }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        BackupClickableItem(
                            icon = Icons.Outlined.Cloud,
                            title = "备份位置",
                            value = currentProvider.displayName,
                            enabled = autoBackupEnabled,
                            onClick = { viewModel.showProviderPickerDialog() }
                        )
                        if (lastBackupTime > 0) {
                            Divider(modifier = Modifier.padding(start = 56.dp))
                            BackupInfoItem(
                                icon = Icons.Outlined.History,
                                title = "上次备份",
                                value = formatLastBackupTime(lastBackupTime)
                            )
                        }
                    }
                }

                // 云存储账号
                item {
                    BackupSettingsSection(
                        title = "云存储账号",
                        icon = "☁️",
                        gradientColors = AppColors.GradientSky
                    ) {
                        // 百度网盘
                        CloudAccountItem(
                            icon = "🅱️",
                            title = "百度网盘",
                            connected = baiduConnected,
                            onConnect = { viewModel.showConnectDialog(CloudProvider.BAIDU) },
                            onDisconnect = { viewModel.disconnectBaidu() }
                        )
                        Divider(modifier = Modifier.padding(start = 56.dp))
                        // 阿里云盘
                        CloudAccountItem(
                            icon = "🅰️",
                            title = "阿里云盘",
                            connected = aliyunConnected,
                            onConnect = { viewModel.showConnectDialog(CloudProvider.ALIYUN) },
                            onDisconnect = { viewModel.disconnectAliyun() }
                        )
                    }
                }

                // 手动备份
                item {
                    BackupSettingsSection(
                        title = "手动备份",
                        icon = "📦",
                        gradientColors = AppColors.GradientEmerald
                    ) {
                        BackupActionItem(
                            icon = Icons.Outlined.Save,
                            title = "立即备份",
                            subtitle = "备份数据到本地存储",
                            onClick = { viewModel.backupNow() },
                            isLoading = backupState is BackupState.BackingUp
                        )
                        if (currentProvider != CloudProvider.LOCAL &&
                            (baiduConnected || aliyunConnected)) {
                            Divider(modifier = Modifier.padding(start = 56.dp))
                            BackupActionItem(
                                icon = Icons.Outlined.CloudUpload,
                                title = "备份到云端",
                                subtitle = "将数据同步到${currentProvider.displayName}",
                                onClick = { viewModel.backupToCloud() },
                                isLoading = backupState is BackupState.BackingUp
                            )
                        }
                    }
                }

                // 数据恢复
                item {
                    BackupSettingsSection(
                        title = "数据恢复",
                        icon = "♻️",
                        gradientColors = AppColors.GradientGold
                    ) {
                        BackupClickableItem(
                            icon = Icons.Outlined.RestorePage,
                            title = "从本地恢复",
                            value = "${localBackups.size}个备份",
                            onClick = { viewModel.showRestoreDialogFromLocal() }
                        )
                        if (cloudBackups.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(start = 56.dp))
                            BackupClickableItem(
                                icon = Icons.Outlined.CloudDownload,
                                title = "从云端恢复",
                                value = "${cloudBackups.size}个备份",
                                onClick = { viewModel.showRestoreDialogFromCloud() }
                            )
                        }
                    }
                }

                // 本地备份列表
                if (localBackups.isNotEmpty()) {
                    item {
                        Text(
                            text = "本地备份记录",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    items(localBackups.take(5)) { backup ->
                        BackupListItem(
                            backup = backup,
                            onRestore = { viewModel.restoreFromBackup(backup) },
                            onDelete = { viewModel.deleteBackup(backup) }
                        )
                    }
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // 加载状态遮罩
        if (backupState is BackupState.BackingUp ||
            backupState is BackupState.Restoring ||
            backupState is BackupState.Connecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.Primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (val state = backupState) {
                                is BackupState.BackingUp -> state.message
                                is BackupState.Restoring -> state.message
                                is BackupState.Connecting -> state.message
                                else -> "处理中..."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // 备份周期选择对话框
    if (showIntervalPicker) {
        IntervalPickerDialog(
            currentInterval = backupInterval,
            onSelect = { viewModel.setBackupInterval(it) },
            onDismiss = { viewModel.hideIntervalPickerDialog() }
        )
    }

    // 备份位置选择对话框
    if (showProviderPicker) {
        ProviderPickerDialog(
            currentProvider = currentProvider,
            baiduConnected = baiduConnected,
            aliyunConnected = aliyunConnected,
            onSelect = { viewModel.setCloudProvider(it) },
            onDismiss = { viewModel.hideProviderPickerDialog() }
        )
    }

    // 恢复数据对话框
    if (showRestoreDialog) {
        RestoreDialog(
            backups = if (viewModel.isRestoringFromCloud) cloudBackups else localBackups,
            isCloud = viewModel.isRestoringFromCloud,
            onRestore = { viewModel.restoreFromBackup(it) },
            onDismiss = { viewModel.hideRestoreDialog() }
        )
    }

    // 云存储连接对话框
    if (showCloudConnectDialog && connectingProvider != null) {
        CloudConnectDialog(
            provider = connectingProvider!!,
            onConnect = { authCode -> viewModel.connectCloud(authCode) },
            onDismiss = { viewModel.hideConnectDialog() }
        )
    }
}

@Composable
private fun BackupSettingsSection(
    title: String,
    icon: String,
    gradientColors: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
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
private fun BackupSwitchItem(
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
private fun BackupClickableItem(
    icon: ImageVector,
    title: String,
    value: String,
    enabled: Boolean = true,
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
            tint = if (enabled) AppColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.6f else 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BackupInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CloudAccountItem(
    icon: String,
    title: String,
    connected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (connected) "已连接" else "未连接",
                style = MaterialTheme.typography.bodySmall,
                color = if (connected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (connected) {
            OutlinedButton(
                onClick = onDisconnect,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("断开")
            }
        } else {
            Button(
                onClick = onConnect,
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text("连接")
            }
        }
    }
}

@Composable
private fun BackupActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
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
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = AppColors.Primary
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BackupListItem(
    backup: BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backup.formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${backup.provider.icon} ${backup.formattedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("恢复") },
                        onClick = {
                            showMenu = false
                            onRestore()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Restore, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalPickerDialog(
    currentInterval: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val intervals = listOf(
        24 to "每天",
        168 to "每周",
        336 to "每两周",
        720 to "每月"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("选择备份周期", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                intervals.forEach { (interval, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(interval) }
                            .background(
                                if (interval == currentInterval)
                                    AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = interval == currentInterval,
                            onClick = { onSelect(interval) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            fontWeight = if (interval == currentInterval) FontWeight.Medium else FontWeight.Normal
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
private fun ProviderPickerDialog(
    currentProvider: CloudProvider,
    baiduConnected: Boolean,
    aliyunConnected: Boolean,
    onSelect: (CloudProvider) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("选择备份位置", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                CloudProvider.entries.forEach { provider ->
                    val isConnected = when (provider) {
                        CloudProvider.BAIDU -> baiduConnected
                        CloudProvider.ALIYUN -> aliyunConnected
                        CloudProvider.LOCAL -> true
                    }
                    val isEnabled = isConnected || provider == CloudProvider.LOCAL

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = isEnabled) { onSelect(provider) }
                            .background(
                                if (provider == currentProvider)
                                    AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(provider.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.displayName,
                                fontWeight = if (provider == currentProvider) FontWeight.Medium else FontWeight.Normal,
                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            if (!isEnabled) {
                                Text(
                                    text = "未连接",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        RadioButton(
                            selected = provider == currentProvider,
                            onClick = { if (isEnabled) onSelect(provider) },
                            enabled = isEnabled,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreDialog(
    backups: List<BackupInfo>,
    isCloud: Boolean,
    onRestore: (BackupInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (isCloud) "从云端恢复" else "从本地恢复",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (backups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无备份",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "选择要恢复的备份:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    backups.take(10).forEach { backup ->
                        OutlinedCard(
                            onClick = { onRestore(backup) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(backup.provider.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = backup.formattedDate,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = backup.formattedSize,
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
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = AppColors.Primary)
            }
        }
    )
}

@Composable
private fun CloudConnectDialog(
    provider: CloudProvider,
    onConnect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var authCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Text(provider.icon, fontSize = 48.sp)
        },
        title = {
            Text(
                text = "连接${provider.displayName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "请输入授权码以连接您的${provider.displayName}账号",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = authCode,
                    onValueChange = { authCode = it },
                    label = { Text("授权码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "提示: 请先在${provider.displayName}APP或网页端授权本应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(authCode) },
                enabled = authCode.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text("连接")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun getIntervalDisplayName(intervalHours: Int): String {
    return when (intervalHours) {
        24 -> "每天"
        168 -> "每周"
        336 -> "每两周"
        720 -> "每月"
        else -> "每天"
    }
}

private fun formatLastBackupTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        days < 30 -> "${days}天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

// 扩展函数
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
