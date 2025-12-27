package com.lifemanager.app.feature.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.ExecutionResult
import com.lifemanager.app.core.voice.CommandProcessState
import com.lifemanager.app.core.voice.VoiceRecognitionState
import com.lifemanager.app.feature.ai.component.*

/**
 * 语音输入主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputScreen(
    onNavigateBack: () -> Unit,
    onExecuteIntent: (CommandIntent) -> Unit,
    viewModel: VoiceInputViewModel = hiltViewModel()
) {
    val recognitionState by viewModel.recognitionState.collectAsState()
    val volumeLevel by viewModel.volumeLevel.collectAsState()
    val commandState by viewModel.commandState.collectAsState()
    val isVoiceAvailable by viewModel.isVoiceAvailable.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val pendingIntent by viewModel.pendingIntent.collectAsState()
    val pendingDescription by viewModel.pendingDescription.collectAsState()
    val resultMessage by viewModel.resultMessage.collectAsState()
    val lastRecognizedText by viewModel.lastRecognizedText.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 处理结果消息
    LaunchedEffect(resultMessage) {
        resultMessage?.let { (message, _) ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearResultMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音输入") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 打开帮助 */ }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "帮助")
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 状态提示
            if (!isVoiceAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "语音识别服务不可用，请检查设备设置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 语音输入面板
            VoiceInputPanel(
                state = recognitionState,
                volumeLevel = volumeLevel,
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() },
                onCancel = { viewModel.cancelListening() },
                onRetry = { viewModel.startListening() },
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = isVoiceAvailable
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 处理状态显示
            AnimatedVisibility(
                visible = commandState is CommandProcessState.Processing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "正在解析命令...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 已解析的命令显示
            AnimatedVisibility(
                visible = commandState is CommandProcessState.Parsed,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val parsedIntent = (commandState as? CommandProcessState.Parsed)?.intent
                parsedIntent?.let { intent ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "命令已识别",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onExecuteIntent(intent) }
                            ) {
                                Text("执行")
                            }
                        }
                    }
                }
            }

            // 使用说明
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "语音命令示例",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    VoiceCommandExample(
                        icon = Icons.Default.ShoppingCart,
                        title = "记账",
                        examples = listOf(
                            "今天午饭花了25元",
                            "收到工资5000块",
                            "买水果花了38.5"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VoiceCommandExample(
                        icon = Icons.Default.CheckCircle,
                        title = "待办",
                        examples = listOf(
                            "明天下午3点开会",
                            "提醒我周五交报告",
                            "添加待办买牛奶"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VoiceCommandExample(
                        icon = Icons.Default.Book,
                        title = "日记",
                        examples = listOf(
                            "记日记今天很开心",
                            "写日记和朋友聚餐了"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VoiceCommandExample(
                        icon = Icons.Default.Search,
                        title = "查询",
                        examples = listOf(
                            "这个月花了多少钱",
                            "查看今天的待办",
                            "本周收入多少"
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 确认对话框
    if (showConfirmDialog && pendingIntent != null && pendingDescription != null) {
        VoiceConfirmationDialog(
            intent = pendingIntent!!,
            description = pendingDescription!!,
            onConfirm = {
                viewModel.confirmExecution()?.let { intent ->
                    onExecuteIntent(intent)
                }
            },
            onCancel = { viewModel.cancelExecution() }
        )
    }
}

/**
 * 语音命令示例
 */
@Composable
private fun VoiceCommandExample(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    examples: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            examples.forEach { example ->
                Text(
                    text = "• $example",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 快捷语音输入组件
 * 可嵌入到其他页面使用
 */
@Composable
fun QuickVoiceInput(
    onIntentRecognized: (CommandIntent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceInputViewModel = hiltViewModel()
) {
    val recognitionState by viewModel.recognitionState.collectAsState()
    val volumeLevel by viewModel.volumeLevel.collectAsState()
    val commandState by viewModel.commandState.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val pendingIntent by viewModel.pendingIntent.collectAsState()
    val pendingDescription by viewModel.pendingDescription.collectAsState()

    // 监听已解析的命令
    LaunchedEffect(commandState) {
        if (commandState is CommandProcessState.Parsed) {
            val intent = (commandState as CommandProcessState.Parsed).intent
            onIntentRecognized(intent)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 波形动画
        VoiceWaveAnimation(
            isRecording = recognitionState is VoiceRecognitionState.Listening,
            volumeLevel = volumeLevel,
            modifier = Modifier.height(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 语音按钮
        VoiceInputButton(
            state = recognitionState,
            volumeLevel = volumeLevel,
            onStartListening = { viewModel.startListening() },
            onStopListening = { viewModel.stopListening() },
            onCancel = { viewModel.cancelListening() }
        )

        // 状态文本
        val stateText = when (recognitionState) {
            is VoiceRecognitionState.Idle -> "点击开始语音输入"
            is VoiceRecognitionState.Listening -> "正在聆听..."
            is VoiceRecognitionState.Processing -> "识别中..."
            is VoiceRecognitionState.PartialResult ->
                (recognitionState as VoiceRecognitionState.PartialResult).text
            is VoiceRecognitionState.Result ->
                (recognitionState as VoiceRecognitionState.Result).text
            is VoiceRecognitionState.Error ->
                (recognitionState as VoiceRecognitionState.Error).message
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stateText,
            style = MaterialTheme.typography.bodySmall,
            color = if (recognitionState is VoiceRecognitionState.Error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }

    // 确认对话框
    if (showConfirmDialog && pendingIntent != null && pendingDescription != null) {
        VoiceConfirmationDialog(
            intent = pendingIntent!!,
            description = pendingDescription!!,
            onConfirm = {
                viewModel.confirmExecution()?.let { intent ->
                    onIntentRecognized(intent)
                }
            },
            onCancel = { viewModel.cancelExecution() }
        )
    }
}
