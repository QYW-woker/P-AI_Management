package com.lifemanager.app.feature.ai.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * 语音波形动画
 * 显示麦克风录音时的音量可视化效果
 */
@Composable
fun VoiceWaveAnimation(
    isRecording: Boolean,
    volumeLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    barWidth: Dp = 4.dp,
    barSpacing: Dp = 4.dp,
    minBarHeight: Dp = 8.dp,
    maxBarHeight: Dp = 32.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline
) {
    // 动画过渡
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    // 为每个bar创建相位偏移的动画
    val animatedPhases = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600 + index * 100,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase_$index"
        )
    }

    val color = if (isRecording) activeColor else inactiveColor

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width((barWidth * barCount) + (barSpacing * (barCount - 1)))
                .height(maxBarHeight)
        ) {
            val barWidthPx = barWidth.toPx()
            val barSpacingPx = barSpacing.toPx()
            val minHeightPx = minBarHeight.toPx()
            val maxHeightPx = maxBarHeight.toPx()
            val centerY = size.height / 2

            for (i in 0 until barCount) {
                val phase = animatedPhases[i].value

                // 计算bar的高度
                val heightFactor = if (isRecording) {
                    // 录音时：基于音量和动画相位计算高度
                    val baseHeight = 0.3f + volumeLevel * 0.7f
                    val wave = (sin(phase + i * 0.5f) + 1f) / 2f
                    baseHeight * (0.5f + wave * 0.5f)
                } else {
                    // 非录音时：显示最小高度
                    minHeightPx / maxHeightPx
                }

                val barHeight = (minHeightPx + (maxHeightPx - minHeightPx) * heightFactor)
                    .coerceIn(minHeightPx, maxHeightPx)

                val x = i * (barWidthPx + barSpacingPx) + barWidthPx / 2

                drawLine(
                    color = color,
                    start = Offset(x, centerY - barHeight / 2),
                    end = Offset(x, centerY + barHeight / 2),
                    strokeWidth = barWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * 圆形脉冲动画
 * 用于语音按钮的录音指示
 */
@Composable
fun VoicePulseAnimation(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    pulseCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // 创建多个脉冲动画
    val pulseAnimations = List(pulseCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    easing = LinearEasing,
                    delayMillis = index * 500
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_$index"
        )
    }

    if (isRecording) {
        Canvas(modifier = modifier) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = minOf(size.width, size.height) / 2

            pulseAnimations.forEach { animation ->
                val progress = animation.value
                val radius = maxRadius * progress
                val alpha = (1f - progress) * 0.5f

                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

/**
 * 简单的点动画
 * 用于显示处理中状态
 */
@Composable
fun ProcessingDotsAnimation(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    dotSize: Dp = 8.dp,
    dotSpacing: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dotAnimations = List(dotCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0f at 0
                    1f at 300
                    0f at 600
                    0f at 1200
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(index * 200)
            ),
            label = "dot_$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotAnimations.forEachIndexed { index, animation ->
            Canvas(modifier = Modifier.size(dotSize)) {
                val scale = 0.5f + animation.value * 0.5f
                val radius = size.minDimension / 2 * scale

                drawCircle(
                    color = color.copy(alpha = 0.5f + animation.value * 0.5f),
                    radius = radius
                )
            }
        }
    }
}
