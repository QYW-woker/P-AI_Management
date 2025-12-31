package com.lifemanager.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

// ==================== Premium 输入框组件 ====================

/**
 * Premium 风格输入框
 * 带有渐变边框、聚焦动画和玻璃态背景
 */
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    // 聚焦时的动画
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.5f,
        animationSpec = tween(300),
        label = "borderAlpha"
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 8.dp else 2.dp,
        animationSpec = tween(300),
        label = "elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> AppColors.Primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val gradientBorder = if (isFocused && !isError) {
        Brush.linearGradient(
            colors = listOf(
                AppColors.Primary.copy(alpha = borderAlpha),
                AppColors.Secondary.copy(alpha = borderAlpha),
                AppColors.Primary.copy(alpha = borderAlpha)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(borderColor, borderColor)
        )
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = if (isFocused) AppColors.Primary.copy(alpha = 0.3f) else Color.Transparent
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    brush = gradientBorder,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = label?.let { { Text(it) } },
                placeholder = placeholder?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) } },
                leadingIcon = leadingIcon,
                trailingIcon = if (isPassword) {
                    {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else trailingIcon,
                isError = isError,
                enabled = enabled,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else visualTransformation,
                interactionSource = interactionSource,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    cursorColor = AppColors.Primary,
                    focusedLabelColor = AppColors.Primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 错误提示
        if (isError && !errorMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

// ==================== Premium 弹框组件 ====================

/**
 * Premium 风格弹框
 * 带有玻璃态背景、渐变边框和动画效果
 */
@Composable
fun PremiumDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconBackgroundColor: Color = AppColors.Primary.copy(alpha = 0.1f),
    title: String? = null,
    titleAlign: TextAlign = TextAlign.Center,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        // 动画效果
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }

        val scale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "dialogScale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(200),
            label = "dialogAlpha"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .alpha(alpha)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = AppColors.Primary.copy(alpha = 0.2f),
                    ambientColor = AppColors.GlowBlue.copy(alpha = 0.1f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f),
                            AppColors.Primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标
                icon?.let {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(8.dp, CircleShape, spotColor = iconBackgroundColor)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        iconBackgroundColor,
                                        iconBackgroundColor.copy(alpha = 0.5f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(it, fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 标题
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = titleAlign,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 内容
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

/**
 * Premium 确认按钮
 */
@Composable
fun PremiumConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientColors: List<Color> = listOf(AppColors.Primary, AppColors.Secondary)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = gradientColors.first().copy(alpha = 0.4f)
            ),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (enabled) Brush.horizontalGradient(gradientColors)
                    else Brush.horizontalGradient(listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    ))
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Premium 取消按钮
 */
@Composable
fun PremiumDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    TextButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(14.dp),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Premium 删除确认弹框
 */
@Composable
fun PremiumDeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "确认删除",
    message: String = "确定要删除吗？此操作无法撤销。",
    confirmText: String = "删除",
    dismissText: String = "取消"
) {
    PremiumDialog(
        onDismissRequest = onDismissRequest,
        icon = "🗑️",
        iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
        title = title,
        confirmButton = {
            PremiumConfirmButton(
                text = confirmText,
                onClick = onConfirm,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            )
        },
        dismissButton = {
            PremiumDismissButton(text = dismissText, onClick = onDismissRequest)
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Premium 信息提示弹框
 */
@Composable
fun PremiumInfoDialog(
    onDismissRequest: () -> Unit,
    icon: String = "ℹ️",
    title: String,
    message: String,
    confirmText: String = "确定"
) {
    PremiumDialog(
        onDismissRequest = onDismissRequest,
        icon = icon,
        iconBackgroundColor = AppColors.Primary.copy(alpha = 0.1f),
        title = title,
        confirmButton = {
            PremiumConfirmButton(
                text = confirmText,
                onClick = onDismissRequest
            )
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Premium 输入弹框
 */
@Composable
fun PremiumInputDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    icon: String? = null,
    title: String,
    initialValue: String = "",
    label: String? = null,
    placeholder: String? = null,
    confirmText: String = "确定",
    dismissText: String = "取消",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var inputValue by remember { mutableStateOf(initialValue) }

    PremiumDialog(
        onDismissRequest = onDismissRequest,
        icon = icon,
        title = title,
        confirmButton = {
            PremiumConfirmButton(
                text = confirmText,
                onClick = { onConfirm(inputValue) },
                enabled = inputValue.isNotBlank()
            )
        },
        dismissButton = {
            PremiumDismissButton(text = dismissText, onClick = onDismissRequest)
        }
    ) {
        PremiumTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = label,
            placeholder = placeholder,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
