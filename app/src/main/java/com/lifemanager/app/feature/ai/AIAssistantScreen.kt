package com.lifemanager.app.feature.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
 * AI助手页面
 * 语音输入和命令执行的主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onExecuteIntent: (CommandIntent) -> Unit,
    viewModel: VoiceInputViewModel = hiltViewModel()
) {
    val recognitionState by viewModel.recognitionState.collectAsState()
    val volumeLevel by viewModel.volumeLevel.collectAsState()
    val commandState by viewModel.commandState.collectAsState()
    val isVoiceAvailable by viewModel.isVoiceAvailable.collectAsState()
    val lastRecognizedText by viewModel.lastRecognizedText.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val pendingIntent by viewModel.pendingIntent.collectAsState()
    val resultMessage by viewModel.resultMessage.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // 显示结果消息
    LaunchedEffect(resultMessage) {
        resultMessage?.let { (message, isSuccess) ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearResultMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI助手") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
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
        ) {
            // 主内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 状态显示
                when {
                    !isVoiceAvailable -> {
                        // 语音识别不可用
                        VoiceUnavailableContent(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    recognitionState is VoiceRecognitionState.Idle &&
                            commandState is CommandProcessState.Idle -> {
                        // 空闲状态，显示引导
                        IdleStateContent(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        // 显示语音输入面板
                        VoiceInputPanel(
                            state = recognitionState,
                            volumeLevel = volumeLevel,
                            onStartListening = { viewModel.startListening() },
                            onStopListening = { viewModel.stopListening() },
                            onCancel = { viewModel.cancelListening() },
                            onRetry = { viewModel.startListening() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                // 处理中的指示器
                if (commandState is CommandProcessState.Processing) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在分析命令...")
                        }
                    }
                }
            }

            // 底部输入区域
            BottomInputArea(
                textInput = textInput,
                onTextChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.processTextInput(textInput)
                        textInput = ""
                    }
                },
                recognitionState = recognitionState,
                volumeLevel = volumeLevel,
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() },
                enabled = isVoiceAvailable
            )
        }
    }

    // 确认对话框
    if (showConfirmDialog && pendingIntent != null) {
        CommandConfirmationDialog(
            intent = pendingIntent!!,
            onConfirm = {
                val intent = viewModel.confirmExecution()
                intent?.let { onExecuteIntent(it) }
            },
            onCancel = { viewModel.cancelExecution() }
        )
    }
}

/**
 * 语音不可用的内容
 */
@Composable
private fun VoiceUnavailableContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "语音识别服务不可用",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请检查设备是否支持语音识别，或尝试安装Google语音服务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 空闲状态内容
 */
@Composable
private fun IdleStateContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "点击麦克风开始语音输入",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "或在下方输入文字命令",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 示例命令
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "试试这样说:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                CommandExample("今天午饭花了25元")
                CommandExample("明天下午3点开会")
                CommandExample("记日记今天很开心")
                CommandExample("这个月花了多少钱")
                CommandExample("打开记账")
            }
        }
    }
}

@Composable
private fun CommandExample(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardVoice,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "\"$text\"",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 底部输入区域
 */
@Composable
private fun BottomInputArea(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    recognitionState: VoiceRecognitionState,
    volumeLevel: Float,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文本输入框
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入命令...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    // 语音按钮
                    CompactVoiceButton(
                        state = recognitionState,
                        onStartListening = onStartListening,
                        onStopListening = onStopListening,
                        enabled = enabled
                    )
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 发送按钮
            FilledIconButton(
                onClick = onSend,
                enabled = textInput.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送"
                )
            }
        }
    }
}
