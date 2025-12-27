package com.lifemanager.app.feature.ai.component

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifemanager.app.core.voice.VoiceRecognitionState

/**
 * 语音输入按钮组件
 * 支持按住录音或点击录音两种模式
 */
@Composable
fun VoiceInputButton(
    state: VoiceRecognitionState,
    volumeLevel: Float,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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
        hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val isListening = state is VoiceRecognitionState.Listening ||
            state is VoiceRecognitionState.PartialResult

    val isProcessing = state is VoiceRecognitionState.Processing

    // 按钮缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 脉冲动画背景
        VoicePulseAnimation(
            isRecording = isListening,
            modifier = Modifier.size(120.dp),
            color = MaterialTheme.colorScheme.primary
        )

        // 主按钮
        FloatingActionButton(
            onClick = {
                when {
                    !enabled -> {}
                    !hasPermission -> {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    isListening -> onStopListening()
                    isProcessing -> {} // 处理中不响应点击
                    else -> onStartListening()
                }
            },
            modifier = Modifier
                .size(64.dp)
                .scale(scale),
            containerColor = if (isListening) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = Color.White
        ) {
            when {
                isProcessing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                !hasPermission -> {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "需要麦克风权限"
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isListening) "停止录音" else "开始录音"
                    )
                }
            }
        }

        // 取消按钮（录音时显示）
        AnimatedVisibility(
            visible = isListening,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
        ) {
            SmallFloatingActionButton(
                onClick = onCancel,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 语音输入面板
 * 显示录音状态和识别结果
 */
@Composable
fun VoiceInputPanel(
    state: VoiceRecognitionState,
    volumeLevel: Float,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态文本
            Text(
                text = when (state) {
                    is VoiceRecognitionState.Idle -> "点击麦克风开始语音输入"
                    is VoiceRecognitionState.Listening -> "正在聆听..."
                    is VoiceRecognitionState.Processing -> "正在识别..."
                    is VoiceRecognitionState.PartialResult -> state.text
                    is VoiceRecognitionState.Result -> "识别完成"
                    is VoiceRecognitionState.Error -> state.message
                },
                style = MaterialTheme.typography.bodyLarge,
                color = when (state) {
                    is VoiceRecognitionState.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 波形动画
            VoiceWaveAnimation(
                isRecording = state is VoiceRecognitionState.Listening ||
                        state is VoiceRecognitionState.PartialResult,
                volumeLevel = volumeLevel,
                modifier = Modifier.height(48.dp),
                barCount = 7,
                maxBarHeight = 48.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 语音按钮
            VoiceInputButton(
                state = state,
                volumeLevel = volumeLevel,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onCancel = onCancel,
                enabled = enabled
            )

            // 识别结果显示
            AnimatedVisibility(
                visible = state is VoiceRecognitionState.Result ||
                        state is VoiceRecognitionState.PartialResult
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val resultText = when (state) {
                        is VoiceRecognitionState.Result -> state.text
                        is VoiceRecognitionState.PartialResult -> state.text
                        else -> ""
                    }

                    if (resultText.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = resultText,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 错误状态显示重试按钮
            AnimatedVisibility(visible = state is VoiceRecognitionState.Error) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("重试")
                }
            }
        }
    }
}

/**
 * 紧凑的语音输入按钮
 * 用于在输入框等位置显示
 */
@Composable
fun CompactVoiceButton(
    state: VoiceRecognitionState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            onStartListening()
        }
    }

    LaunchedEffect(Unit) {
        hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val isListening = state is VoiceRecognitionState.Listening ||
            state is VoiceRecognitionState.PartialResult
    val isProcessing = state is VoiceRecognitionState.Processing

    IconButton(
        onClick = {
            when {
                !enabled -> {}
                !hasPermission -> {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                isListening -> onStopListening()
                isProcessing -> {}
                else -> onStartListening()
            }
        },
        enabled = enabled && !isProcessing,
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isListening) MaterialTheme.colorScheme.errorContainer
                else Color.Transparent
            )
    ) {
        when {
            isProcessing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            !hasPermission -> {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "需要麦克风权限",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) "停止录音" else "开始录音",
                    tint = if (isListening) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}
