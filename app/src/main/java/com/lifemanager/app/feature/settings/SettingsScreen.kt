package com.lifemanager.app.feature.settings

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.BuildConfig
import com.lifemanager.app.core.data.repository.CurrencySymbol
import com.lifemanager.app.core.data.repository.DateFormat
import com.lifemanager.app.core.data.repository.WeekStartDay
import com.lifemanager.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 设置页面 - Premium Design
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
    val uiState by viewModel.uiState.collectAsState()
    val showTimePicker by viewModel.showTimePicker.collectAsState()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
    val showClearDataDialog by viewModel.showClearDataDialog.collectAsState()
    val showBackupSuccessDialog by viewModel.showBackupSuccessDialog.collectAsState()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val showExportSuccessDialog by viewModel.showExportSuccessDialog.collectAsState()
    val exportStartDate by viewModel.exportStartDate.collectAsState()
    val exportEndDate by viewModel.exportEndDate.collectAsState()

    // 新增对话框状态
    val showCurrencyPicker by viewModel.showCurrencyPicker.collectAsState()
    val showDateFormatPicker by viewModel.showDateFormatPicker.collectAsState()
    val showWeekStartPicker by viewModel.showWeekStartPicker.collectAsState()
    val showDecimalPlacesPicker by viewModel.showDecimalPlacesPicker.collectAsState()
    val showHomeCardSettings by viewModel.showHomeCardSettings.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 判断主题
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 处理UI状态变化
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SettingsUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearUiState()
            }
            is SettingsUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearUiState()
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
                            listOf(
                                Color(0xFF1a1a2e),
                                Color(0xFF16213e),
                                Color(0xFF0f3460)
                            )
                        } else {
                            listOf(
                                Color(0xFFF5F7FF),
                                Color(0xFFFFF5F8),
                                Color(0xFFF0F9FF)
                            )
                        }
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                PremiumSettingsTopBar(onNavigateBack = onNavigateBack)
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 用户头像区域（如果已登录）
                if (isLoggedIn && currentUser != null) {
                    item {
                        UserProfileCard(
                            nickname = currentUser?.nickname ?: currentUser?.username ?: "用户",
                            email = currentUser?.email ?: "",
                            onLogoutClick = { viewModel.showLogoutConfirmation() }
                        )
                    }
                }

                // 外观设置
                item {
                    PremiumSettingsSection(
                        title = "外观",
                        icon = "🎨",
                        gradientColors = AppColors.GradientAurora
                    ) {
                        PremiumSwitchItem(
                            icon = Icons.Outlined.DarkMode,
                            title = "深色模式",
                            subtitle = "使用深色主题",
                            checked = settings.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.Language,
                            title = "语言",
                            value = settings.language,
                            onClick = { viewModel.showLanguagePickerDialog() }
                        )
                    }
                }

                // 显示格式设置
                item {
                    PremiumSettingsSection(
                        title = "显示格式",
                        icon = "📐",
                        gradientColors = AppColors.GradientEmerald
                    ) {
                        PremiumClickableItem(
                            icon = Icons.Outlined.AttachMoney,
                            title = "货币符号",
                            value = settings.currencySymbol.displayName,
                            onClick = { viewModel.showCurrencyPickerDialog() }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.Pin,
                            title = "金额小数位",
                            value = "${settings.decimalPlaces}位",
                            onClick = { viewModel.showDecimalPlacesPickerDialog() }
                        )
                        PremiumDivider()
                        PremiumSwitchItem(
                            icon = Icons.Outlined.FormatListNumbered,
                            title = "千位分隔符",
                            subtitle = "使用逗号分隔 (1,000)",
                            checked = settings.useThousandSeparator,
                            onCheckedChange = { viewModel.toggleThousandSeparator(it) }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "日期格式",
                            value = settings.dateFormat.displayName,
                            onClick = { viewModel.showDateFormatPickerDialog() }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.DateRange,
                            title = "周起始日",
                            value = settings.weekStartDay.displayName,
                            onClick = { viewModel.showWeekStartPickerDialog() }
                        )
                    }
                }

                // 首页布局设置
                item {
                    PremiumSettingsSection(
                        title = "首页布局",
                        icon = "🏠",
                        gradientColors = AppColors.GradientGold
                    ) {
                        PremiumClickableItem(
                            icon = Icons.Outlined.Dashboard,
                            title = "自定义首页卡片",
                            value = "显示/隐藏",
                            onClick = { viewModel.showHomeCardSettingsDialog() }
                        )
                    }
                }

                // 通知设置
                item {
                    PremiumSettingsSection(
                        title = "通知",
                        icon = "🔔",
                        gradientColors = AppColors.GradientRose
                    ) {
                        PremiumSwitchItem(
                            icon = Icons.Outlined.Notifications,
                            title = "开启通知",
                            subtitle = "接收提醒和通知",
                            checked = settings.enableNotification,
                            onCheckedChange = { viewModel.toggleNotification(it) }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
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
                    PremiumSettingsSection(
                        title = "AI功能",
                        icon = "🤖",
                        gradientColors = AppColors.GradientCosmic
                    ) {
                        PremiumClickableItem(
                            icon = Icons.Filled.SmartToy,
                            title = "AI设置",
                            value = "",
                            onClick = onNavigateToAISettings
                        )
                    }
                }

                // 数据设置
                item {
                    PremiumSettingsSection(
                        title = "数据",
                        icon = "💾",
                        gradientColors = AppColors.GradientSky
                    ) {
                        PremiumSwitchItem(
                            icon = Icons.Outlined.CloudSync,
                            title = "自动备份",
                            subtitle = "定期备份到云端",
                            checked = settings.autoBackup,
                            onCheckedChange = { viewModel.toggleAutoBackup(it) }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.CloudUpload,
                            title = "立即备份",
                            value = "",
                            onClick = { viewModel.backupNow() }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.CloudDownload,
                            title = "恢复数据",
                            value = "",
                            onClick = { viewModel.restoreData() }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.FileDownload,
                            title = "导出记账数据",
                            value = "",
                            onClick = { viewModel.showExportDialog() }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
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
                    PremiumSettingsSection(
                        title = "关于",
                        icon = "ℹ️",
                        gradientColors = AppColors.GradientMint
                    ) {
                        PremiumClickableItem(
                            icon = Icons.Outlined.Info,
                            title = "版本",
                            value = BuildConfig.VERSION_NAME,
                            onClick = { }
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.Description,
                            title = "隐私政策",
                            value = "",
                            onClick = onNavigateToPrivacy
                        )
                        PremiumDivider()
                        PremiumClickableItem(
                            icon = Icons.Outlined.Gavel,
                            title = "用户协议",
                            value = "",
                            onClick = onNavigateToTerms
                        )
                    }
                }

                // 账户（未登录时显示登录按钮）
                if (!isLoggedIn) {
                    item {
                        PremiumSettingsSection(
                            title = "账户",
                            icon = "👤",
                            gradientColors = AppColors.GradientPurpleHaze
                        ) {
                            PremiumClickableItem(
                                icon = Icons.Outlined.Login,
                                title = "登录/注册",
                                value = "",
                                onClick = onNavigateToLogin
                            )
                        }
                    }
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // 对话框
    if (showLanguagePicker) {
        PremiumPickerDialog(
            title = "选择语言",
            options = listOf("简体中文", "English"),
            currentValue = settings.language,
            onSelect = { viewModel.setLanguage(it) },
            onDismiss = { viewModel.hideLanguagePickerDialog() }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            currentTime = settings.reminderTime,
            onConfirm = { viewModel.setReminderTime(it) },
            onDismiss = { viewModel.hideTimePickerDialog() }
        )
    }

    if (showClearDataDialog) {
        PremiumAlertDialog(
            icon = "⚠️",
            title = "清除所有数据",
            message = "确定要清除所有数据吗？此操作不可撤销。",
            confirmText = "确定清除",
            isDanger = true,
            onConfirm = { viewModel.clearAllData() },
            onDismiss = { viewModel.hideClearDataConfirmation() }
        )
    }

    showBackupSuccessDialog?.let { backupPath ->
        PremiumAlertDialog(
            icon = "✅",
            title = "备份成功",
            message = "数据已备份到:\n$backupPath",
            confirmText = "确定",
            onConfirm = { viewModel.hideBackupSuccessDialog() },
            onDismiss = { viewModel.hideBackupSuccessDialog() }
        )
    }

    if (uiState is SettingsUiState.Loading) {
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
                        text = (uiState as SettingsUiState.Loading).message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        PremiumAlertDialog(
            icon = "👋",
            title = "退出登录",
            message = "确定要退出当前账号吗？",
            confirmText = "退出",
            isDanger = true,
            onConfirm = { viewModel.confirmLogout() },
            onDismiss = { viewModel.hideLogoutConfirmation() }
        )
    }

    if (showExportDialog) {
        ExportDataDialog(
            startDate = exportStartDate,
            endDate = exportEndDate,
            onStartDateChange = { viewModel.setExportStartDate(it) },
            onEndDateChange = { viewModel.setExportEndDate(it) },
            onConfirm = { viewModel.exportFinanceData() },
            onDismiss = { viewModel.hideExportDialog() }
        )
    }

    showExportSuccessDialog?.let { exportPath ->
        PremiumAlertDialog(
            icon = "✅",
            title = "导出成功",
            message = "数据已导出到:\n$exportPath",
            confirmText = "确定",
            onConfirm = { viewModel.hideExportSuccessDialog() },
            onDismiss = { viewModel.hideExportSuccessDialog() }
        )
    }

    if (showCurrencyPicker) {
        PremiumEnumPickerDialog(
            title = "选择货币符号",
            options = CurrencySymbol.entries,
            currentValue = settings.currencySymbol,
            displayName = { it.displayName },
            onSelect = { viewModel.setCurrencySymbol(it) },
            onDismiss = { viewModel.hideCurrencyPickerDialog() }
        )
    }

    if (showDateFormatPicker) {
        PremiumEnumPickerDialog(
            title = "选择日期格式",
            options = DateFormat.entries,
            currentValue = settings.dateFormat,
            displayName = { it.displayName },
            onSelect = { viewModel.setDateFormat(it) },
            onDismiss = { viewModel.hideDateFormatPickerDialog() }
        )
    }

    if (showWeekStartPicker) {
        PremiumEnumPickerDialog(
            title = "选择周起始日",
            options = WeekStartDay.entries,
            currentValue = settings.weekStartDay,
            displayName = { it.displayName },
            onSelect = { viewModel.setWeekStartDay(it) },
            onDismiss = { viewModel.hideWeekStartPickerDialog() }
        )
    }

    if (showDecimalPlacesPicker) {
        PremiumPickerDialog(
            title = "选择小数位数",
            options = listOf("0位小数", "1位小数", "2位小数", "3位小数", "4位小数"),
            currentValue = "${settings.decimalPlaces}位小数",
            onSelect = { selected ->
                val places = selected.replace("位小数", "").toIntOrNull() ?: 2
                viewModel.setDecimalPlaces(places)
            },
            onDismiss = { viewModel.hideDecimalPlacesPickerDialog() }
        )
    }

    if (showHomeCardSettings) {
        HomeCardSettingsDialog(
            config = settings.homeCardConfig,
            onCardVisibilityChange = { key, visible -> viewModel.setHomeCardVisibility(key, visible) },
            onReset = { viewModel.resetHomeCardConfig() },
            onDismiss = { viewModel.hideHomeCardSettingsDialog() }
        )
    }
}

/**
 * 高级顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSettingsTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * 用户头像卡片
 */
@Composable
private fun UserProfileCard(
    nickname: String,
    email: String,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = AppColors.GradientCosmic.first().copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = AppColors.GradientCosmic.map { it.copy(alpha = 0.9f) }
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nickname.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (email.isNotEmpty()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Outlined.Logout,
                    contentDescription = "退出",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 高级设置分组
 */
@Composable
private fun PremiumSettingsSection(
    title: String,
    icon: String,
    gradientColors: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = gradientColors.first().copy(alpha = 0.2f)
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
    ) {
        // 标题栏
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

/**
 * 高级开关设置项
 */
@Composable
private fun PremiumSwitchItem(
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
                checkedTrackColor = AppColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * 高级可点击设置项
 */
@Composable
private fun PremiumClickableItem(
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
            tint = when {
                isDanger -> MaterialTheme.colorScheme.error
                enabled -> AppColors.Primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = when {
                isDanger -> MaterialTheme.colorScheme.error
                enabled -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 0.6f else 0.3f
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 分隔线
 */
@Composable
private fun PremiumDivider() {
    Divider(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * 高级选择对话框
 */
@Composable
private fun PremiumPickerDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(option) }
                            .background(
                                if (option == currentValue)
                                    AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == currentValue,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppColors.Primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            fontWeight = if (option == currentValue) FontWeight.Medium else FontWeight.Normal
                        )
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

/**
 * 泛型枚举选择对话框
 */
@Composable
private fun <T> PremiumEnumPickerDialog(
    title: String,
    options: List<T>,
    currentValue: T,
    displayName: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(option) }
                            .background(
                                if (option == currentValue)
                                    AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == currentValue,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppColors.Primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = displayName(option),
                            fontWeight = if (option == currentValue) FontWeight.Medium else FontWeight.Normal
                        )
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

/**
 * 高级警告对话框
 */
@Composable
private fun PremiumAlertDialog(
    icon: String,
    title: String,
    message: String,
    confirmText: String,
    isDanger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Text(icon, fontSize = 40.sp)
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDanger) MaterialTheme.colorScheme.error else AppColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
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
        shape = RoundedCornerShape(24.dp),
        title = { Text("选择提醒时间", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour = String.format("%02d", timePickerState.hour)
                    val minute = String.format("%02d", timePickerState.minute)
                    onConfirm("$hour:$minute")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
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

/**
 * 数据导出对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDataDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("📊", fontSize = 40.sp) },
        title = { Text("导出记账数据", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "选择导出的日期范围，数据将导出为CSV格式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 开始日期
                OutlinedCard(
                    onClick = { showStartDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "开始日期",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = startDate.format(dateFormatter),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    }
                }

                // 结束日期
                OutlinedCard(
                    onClick = { showEndDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "结束日期",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = endDate.format(dateFormatter),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    }
                }

                // 快捷选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("本月" to {
                        val now = LocalDate.now()
                        onStartDateChange(now.withDayOfMonth(1))
                        onEndDateChange(now)
                    }, "近3月" to {
                        val now = LocalDate.now()
                        onStartDateChange(now.minusMonths(3))
                        onEndDateChange(now)
                    }, "今年" to {
                        val now = LocalDate.now()
                        onStartDateChange(now.withDayOfYear(1))
                        onEndDateChange(now)
                    }).forEach { (label, action) ->
                        AssistChip(
                            onClick = action,
                            label = { Text(label) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 日期选择器
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            onStartDateChange(selectedDate)
                        }
                        showStartDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            onEndDateChange(selectedDate)
                        }
                        showEndDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

/**
 * 首页卡片设置对话框
 */
@Composable
private fun HomeCardSettingsDialog(
    config: com.lifemanager.app.core.data.repository.HomeCardConfig,
    onCardVisibilityChange: (String, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val cardItems = listOf(
        Triple("todayStats", "今日统计", "📊"),
        Triple("monthlyFinance", "月度财务", "💰"),
        Triple("topGoals", "目标进度", "🎯"),
        Triple("habitProgress", "习惯打卡", "⭐"),
        Triple("aiInsight", "AI 洞察", "🤖"),
        Triple("quickActions", "快捷操作", "⚡")
    )

    val getVisibility: (String) -> Boolean = { key ->
        when (key) {
            "todayStats" -> config.showTodayStats
            "monthlyFinance" -> config.showMonthlyFinance
            "topGoals" -> config.showTopGoals
            "habitProgress" -> config.showHabitProgress
            "aiInsight" -> config.showAIInsight
            "quickActions" -> config.showQuickActions
            else -> true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("🏠", fontSize = 40.sp) },
        title = { Text("自定义首页卡片", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "选择要在首页显示的卡片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                cardItems.forEach { (key, title, emoji) ->
                    val checked = getVisibility(key)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCardVisibilityChange(key, !checked) }
                            .background(
                                if (checked) AppColors.Primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { onCardVisibilityChange(key, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.Primary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onReset) {
                Text("重置")
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
