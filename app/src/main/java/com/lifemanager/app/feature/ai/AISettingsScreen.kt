package com.lifemanager.app.feature.ai

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifemanager.app.ui.component.PremiumTextField

/**
 * AI设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val aiConfig by viewModel.aiConfig.collectAsState()
    val featureConfig by viewModel.featureConfig.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val editingApiKey by viewModel.editingApiKey.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 跟踪用户是否正在等待授予悬浮窗权限
    var pendingFloatingBallEnable by remember { mutableStateOf(false) }

    // 监听生命周期，当从设置页面返回时检查权限
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingFloatingBallEnable) {
                // 用户从设置页面返回，检查权限是否已授予
                if (Settings.canDrawOverlays(context)) {
                    viewModel.setFloatingBallEnabled(true)
                }
                pendingFloatingBallEnable = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 处理状态消息
    LaunchedEffect(uiState) {
        when (uiState) {
            is AISettingsUiState.Success -> {
                snackbarHostState.showSnackbar((uiState as AISettingsUiState.Success).message)
                viewModel.clearMessage()
            }
            is AISettingsUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AISettingsUiState.Error).message)
                viewModel.clearMessage()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // API配置卡片
            SettingsSection(title = "API配置") {
                // API Key配置
                SettingsItem(
                    icon = Icons.Default.Key,
                    title = "DeepSeek API Key",
                    subtitle = if (aiConfig.isConfigured) "已配置" else "未配置",
                    onClick = { viewModel.showApiKeyDialog() }
                )

                // 连接状态
                if (aiConfig.isConfigured) {
                    SettingsItem(
                        icon = Icons.Default.CheckCircle,
                        title = "API状态",
                        subtitle = "已配置，点击测试连接",
                        onClick = { viewModel.testConnection() },
                        trailing = {
                            if (uiState is AISettingsUiState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 语音功能
            SettingsSection(title = "语音功能") {
                SwitchSettingsItem(
                    icon = Icons.Default.Mic,
                    title = "语音输入",
                    subtitle = "使用语音输入记账、添加待办等",
                    checked = featureConfig.voiceInputEnabled,
                    onCheckedChange = {
                        viewModel.updateFeatureConfig(featureConfig.copy(voiceInputEnabled = it))
                    }
                )

                SwitchSettingsItem(
                    icon = Icons.Default.BubbleChart,
                    title = "悬浮球",
                    subtitle = "在任意界面显示悬浮球，快速语音输入",
                    checked = featureConfig.floatingBallEnabled,
                    onCheckedChange = {
                        if (it) {
                            // 需要检查悬浮窗权限
                            if (!Settings.canDrawOverlays(context)) {
                                // 标记为等待权限授予
                                pendingFloatingBallEnable = true
                                // 跳转到悬浮窗权限设置页面
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.setFloatingBallEnabled(true)
                            }
                        } else {
                            viewModel.setFloatingBallEnabled(false)
                        }
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 智能功能
            SettingsSection(title = "智能功能") {
                SwitchSettingsItem(
                    icon = Icons.Default.Category,
                    title = "智能分类",
                    subtitle = "AI自动识别交易类别",
                    checked = featureConfig.smartClassifyEnabled,
                    onCheckedChange = {
                        viewModel.updateFeatureConfig(featureConfig.copy(smartClassifyEnabled = it))
                    }
                )

                SwitchSettingsItem(
                    icon = Icons.Default.Image,
                    title = "图片识别",
                    subtitle = "拍照或截图识别账单",
                    checked = featureConfig.imageRecognitionEnabled,
                    onCheckedChange = {
                        viewModel.updateFeatureConfig(featureConfig.copy(imageRecognitionEnabled = it))
                    }
                )

                SwitchSettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "通知自动记账",
                    subtitle = "监听微信/支付宝支付通知自动记账",
                    checked = featureConfig.notificationListenerEnabled,
                    onCheckedChange = {
                        if (it) {
                            // 跳转到通知访问设置
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                        viewModel.setNotificationListenerEnabled(it)
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 确认设置
            SettingsSection(title = "确认设置") {
                SwitchSettingsItem(
                    icon = Icons.Default.TouchApp,
                    title = "自动确认",
                    subtitle = "语音操作无需二次确认（不推荐）",
                    checked = featureConfig.autoConfirmEnabled,
                    onCheckedChange = {
                        viewModel.updateFeatureConfig(featureConfig.copy(autoConfirmEnabled = it))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 说明卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = """
                            1. 获取API Key: 访问 platform.deepseek.com 注册并获取
                            2. 语音命令示例:
                               • "今天午饭花了25元"
                               • "明天下午3点开会"
                               • "记日记今天很开心"
                            3. 支持拍照识别发票和支付截图
                            4. 开启通知监听可自动记录支付
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // API Key输入对话框
    if (showApiKeyDialog) {
        ApiKeyDialog(
            apiKey = editingApiKey,
            testResult = testResult,
            isTesting = uiState is AISettingsUiState.Testing,
            onApiKeyChange = { viewModel.updateApiKey(it) },
            onTest = { viewModel.testConnection() },
            onSave = { viewModel.saveApiKey() },
            onDismiss = { viewModel.hideApiKeyDialog() }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
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

@Composable
private fun ApiKeyDialog(
    apiKey: String,
    testResult: String?,
    isTesting: Boolean,
    onApiKeyChange: (String) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置API Key") },
        text = {
            Column {
                Text(
                    text = "请输入DeepSeek API Key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                PremiumTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = "API Key",
                    placeholder = "sk-...",
                    singleLine = true,
                    visualTransformation = if (showKey)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey)
                                    Icons.Default.VisibilityOff
                                else
                                    Icons.Default.Visibility,
                                contentDescription = if (showKey) "隐藏" else "显示"
                            )
                        }
                    }
                )

                // 测试结果
                testResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.contains("成功"))
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onTest, enabled = !isTesting) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("测试")
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSave) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
