package com.lifemanager.app.feature.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.ExecutionResult
import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.core.ai.model.TransactionType
import com.lifemanager.app.core.voice.CommandProcessState
import com.lifemanager.app.core.voice.VoiceRecognitionState
import com.lifemanager.app.feature.ai.component.*
import com.lifemanager.app.ui.component.PremiumTextField

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
    val featureConfig by viewModel.featureConfig.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showImageRecognition by remember { mutableStateOf(false) }
    var isImageProcessing by remember { mutableStateOf(false) }
    var recognizedPayment by remember { mutableStateOf<PaymentInfo?>(null) }
    var showPaymentEditDialog by remember { mutableStateOf(false) }
    var editablePayment by remember { mutableStateOf<PaymentInfo?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 悬浮球快捷开关 - 使用气泡图标更直观
                    IconButton(
                        onClick = { viewModel.toggleFloatingBall(context) }
                    ) {
                        BadgedBox(
                            badge = {
                                if (featureConfig?.floatingBallEnabled == true) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = "悬浮球",
                                tint = if (featureConfig?.floatingBallEnabled == true)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
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
                .imePadding()  // 让输入框随键盘上移
        ) {
            // 主内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    // 图片识别模式
                    showImageRecognition -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 返回按钮
                            TextButton(
                                onClick = {
                                    showImageRecognition = false
                                    recognizedPayment = null
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("返回语音输入")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "拍照识别",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            ImageRecognitionComponent(
                                isProcessing = isImageProcessing,
                                recognizedPayment = recognizedPayment,
                                onImageSelected = { uri ->
                                    isImageProcessing = true
                                    viewModel.processImageForRecognition(context, uri) { payment ->
                                        recognizedPayment = payment
                                        isImageProcessing = false
                                        // 识别完成后打开编辑对话框
                                        if (payment != null) {
                                            editablePayment = payment
                                            showPaymentEditDialog = true
                                        }
                                    }
                                },
                                onBitmapCaptured = { /* 处理bitmap */ },
                                onConfirm = { payment ->
                                    // 打开编辑对话框让用户确认/修改
                                    editablePayment = payment
                                    showPaymentEditDialog = true
                                },
                                onCancel = {
                                    recognizedPayment = null
                                }
                            )
                        }
                    }

                    // 空闲状态，显示引导（不再预先判断语音不可用）
                    recognitionState is VoiceRecognitionState.Idle &&
                            commandState is CommandProcessState.Idle -> {
                        IdleStateContent(
                            onOpenImageRecognition = {
                                if (featureConfig?.imageRecognitionEnabled == true) {
                                    showImageRecognition = true
                                }
                            },
                            onStartListening = { viewModel.startListening() },
                            imageRecognitionEnabled = featureConfig?.imageRecognitionEnabled == true,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // 显示语音输入面板
                    else -> {
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

            // 底部输入区域 - 始终启用语音按钮，让用户尝试
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
                enabled = true
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

    // 支付信息编辑确认对话框
    if (showPaymentEditDialog && editablePayment != null) {
        PaymentEditDialog(
            payment = editablePayment!!,
            onConfirm = { updatedPayment ->
                viewModel.confirmPaymentRecord(updatedPayment)
                showPaymentEditDialog = false
                editablePayment = null
                showImageRecognition = false
                recognizedPayment = null
            },
            onDismiss = {
                showPaymentEditDialog = false
                editablePayment = null
            }
        )
    }
}

/**
 * 支付信息编辑对话框
 */
@Composable
private fun PaymentEditDialog(
    payment: PaymentInfo,
    onConfirm: (PaymentInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(payment.amount.toString()) }
    var payee by remember { mutableStateOf(payment.payee ?: "") }
    var isExpense by remember { mutableStateOf(payment.type == TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认记账信息") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "请核对并修改识别结果",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 金额输入
                PremiumTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "金额",
                    leadingIcon = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 商户/备注
                PremiumTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = "商户/备注",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isExpense) {
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("支出")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { isExpense = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("支出")
                        }
                    }

                    if (!isExpense) {
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("收入")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { isExpense = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("收入")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    val updatedPayment = payment.copy(
                        amount = parsedAmount,
                        payee = payee.ifBlank { null },
                        type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
                    )
                    onConfirm(updatedPayment)
                },
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
            ) {
                Text("确认记账")
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
 * 语音不可用的内容
 */
@Composable
private fun VoiceUnavailableContent(
    onOpenImageRecognition: () -> Unit,
    imageRecognitionEnabled: Boolean,
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
            text = "请在下方输入文字命令，或使用拍照识别功能",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 提示解决方案
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "如何启用语音识别:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. 安装讯飞输入法或搜狗输入法\n2. 在系统设置中启用语音输入\n3. 授予APP麦克风权限",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 拍照识别按钮
        if (imageRecognitionEnabled) {
            Spacer(modifier = Modifier.height(24.dp))
            FilledTonalButton(onClick = onOpenImageRecognition) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("拍照识别账单")
            }
        }
    }
}

/**
 * 空闲状态内容
 */
@Composable
private fun IdleStateContent(
    onOpenImageRecognition: () -> Unit,
    onStartListening: () -> Unit,
    imageRecognitionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            onStartListening()
        }
    }

    // 检查权限
    LaunchedEffect(Unit) {
        hasPermission = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 大麦克风图标 - 点击开始语音识别（带权限检查）
        FilledIconButton(
            onClick = {
                if (!hasPermission) {
                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                } else {
                    onStartListening()
                }
            },
            modifier = Modifier.size(96.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (hasPermission) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (hasPermission) "点击开始语音输入" else "点击授权麦克风",
                modifier = Modifier.size(48.dp)
            )
        }
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

        // 拍照识别按钮
        if (imageRecognitionEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onOpenImageRecognition) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("拍照识别账单")
            }
        }

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
                CommandExample("昨天开了场会议")
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
            PremiumTextField(
                value = textInput,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = "输入命令...",
                singleLine = true,
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
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送"
                )
            }
        }
    }
}
