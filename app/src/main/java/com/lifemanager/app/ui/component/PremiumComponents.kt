package com.lifemanager.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifemanager.app.ui.theme.AppColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * 高级UI组件集合 - Premium Design System
 *
 * 包含玻璃态、光泽效果、高级动画等视觉特效
 */

// ==================== 玻璃态卡片组件 ====================

/**
 * 玻璃态卡片 - Glassmorphism Card
 * 带有模糊背景、边框光泽和微妙阴影
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradientColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.9f),
        Color.White.copy(alpha = 0.7f)
    ),
    borderGradient: List<Color> = listOf(
        Color.White.copy(alpha = 0.8f),
        Color.White.copy(alpha = 0.2f)
    ),
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                spotColor = AppColors.GlowPurple.copy(alpha = 0.15f),
                ambientColor = AppColors.GlowBlue.copy(alpha = 0.1f)
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = borderGradient,
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = shape
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * 高级渐变卡片 - Premium Gradient Card
 */
@Composable
fun PremiumGradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradientColors: List<Color> = AppColors.GradientAurora,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                spotColor = gradientColors.first().copy(alpha = 0.4f)
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            ) {
                // 添加光泽叠加层
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.1f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        content = content
                    )
                }
            }
        }
    }
}

// ==================== 闪光效果组件 ====================

/**
 * 闪光效果修饰符
 */
@Composable
fun Modifier.shimmerEffect(
    durationMillis: Int = 1500,
    shimmerColor: Color = AppColors.Shimmer
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    shimmerColor,
                    Color.Transparent
                ),
                start = Offset(size.width * shimmerOffset, 0f),
                end = Offset(size.width * (shimmerOffset + 0.5f), size.height)
            ),
            blendMode = BlendMode.SrcAtop
        )
    }
}

/**
 * 带闪光效果的卡片
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = AppColors.GradientCosmic,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .shadow(16.dp, shape, spotColor = gradientColors.first().copy(alpha = 0.3f))
            .shimmerEffect(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.linearGradient(colors = gradientColors))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}

// ==================== 动态光晕效果 ====================

/**
 * 发光按钮组件
 */
@Composable
fun GlowingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = AppColors.GradientAurora,
    glowColor: Color = gradientColors.first()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // 发光动画
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = glowColor.copy(alpha = glowAlpha)
            )
    ) {
        Surface(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(brush = Brush.linearGradient(colors = gradientColors))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ==================== 动态进度指示器 ====================

/**
 * 高级圆形进度指示器
 */
@Composable
fun PremiumCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = Color.White.copy(alpha = 0.2f),
    gradientColors: List<Color> = AppColors.GradientEmerald,
    centerContent: @Composable () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val sweep = animatedProgress * 360f
            val strokeWidthPx = strokeWidth.toPx()

            // 背景轨道
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )

            // 进度弧
            drawArc(
                brush = Brush.sweepGradient(colors = gradientColors),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )
        }

        centerContent()
    }
}

/**
 * 高级线性进度条
 */
@Composable
fun PremiumLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    trackColor: Color = Color.White.copy(alpha = 0.2f),
    gradientColors: List<Color> = AppColors.GradientAurora,
    showShimmer: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val shape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(shape)
                .background(brush = Brush.horizontalGradient(colors = gradientColors))
                .then(if (showShimmer) Modifier.shimmerEffect() else Modifier)
        )
    }
}

// ==================== 粒子动画背景 ====================

/**
 * 浮动粒子背景
 */
@Composable
fun FloatingParticlesBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    particleColors: List<Color> = listOf(
        AppColors.GlowPurple,
        AppColors.GlowBlue,
        AppColors.GlowPink,
        AppColors.CandyMint.copy(alpha = 0.5f)
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        repeat(particleCount) { index ->
            val seed = index * 137.5f
            val x = ((sin((time + seed) * 0.01f) + 1) / 2 * width)
            val y = ((cos((time + seed * 0.7f) * 0.008f) + 1) / 2 * height)
            val radius = 4f + (index % 4) * 3f
            val color = particleColors[index % particleColors.size]
            val alpha = 0.3f + (sin((time + seed) * 0.02f) + 1) / 4

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

// ==================== 脉冲动画圆 ====================

/**
 * 脉冲动画效果
 */
@Composable
fun PulsingCircle(
    modifier: Modifier = Modifier,
    color: Color = AppColors.Primary,
    size: Dp = 60.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // 外圈脉冲
        Box(
            modifier = Modifier
                .size(size * scale)
                .background(
                    color = color.copy(alpha = alpha),
                    shape = CircleShape
                )
        )
        // 内圈
        Box(
            modifier = Modifier
                .size(size)
                .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

// ==================== 数字动画显示 ====================

/**
 * 动画数字显示
 */
@Composable
fun AnimatedNumber(
    targetNumber: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    durationMillis: Int = 1000
) {
    var animatedNumber by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetNumber) {
        val startNumber = animatedNumber
        val diff = targetNumber - startNumber
        val steps = 60
        val stepDelay = durationMillis / steps.toLong()

        repeat(steps) { step ->
            animatedNumber = startNumber + (diff * (step + 1) / steps)
            kotlinx.coroutines.delay(stepDelay)
        }
        animatedNumber = targetNumber
    }

    Text(
        text = "$prefix${java.text.NumberFormat.getNumberInstance().format(animatedNumber)}$suffix",
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

// ==================== 3D卡片倾斜效果 ====================

/**
 * 3D倾斜卡片（hover效果）
 */
@Composable
fun TiltCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradientColors: List<Color> = listOf(
        AppColors.Primary,
        AppColors.Secondary
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rotationX by animateFloatAsState(
        targetValue = if (isPressed) 5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rotationX"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 8f else 16f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                this.rotationX = rotationX
                cameraDistance = 12f * density
            }
            .shadow(elevation.dp, shape, spotColor = gradientColors.first().copy(alpha = 0.4f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.linearGradient(colors = gradientColors))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    content = content
                )
            }
        }
    }
}

// ==================== 标签胶囊组件 ====================

/**
 * 渐变胶囊标签
 */
@Composable
fun GradientChip(
    text: String,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = AppColors.GradientAurora,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(50),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(brush = Brush.horizontalGradient(colors = gradientColors))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==================== 高级统计项 ====================

/**
 * 高级统计显示项
 */
@Composable
fun PremiumStatItem(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconBackground: Color = AppColors.CandyLavender.copy(alpha = 0.3f),
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = iconBackground,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
