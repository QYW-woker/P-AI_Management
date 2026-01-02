package com.lifemanager.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.ripple.rememberRipple
import com.lifemanager.app.ui.theme.*

/**
 * 干净设计组件库
 *
 * 遵循设计原则:
 * - 克制的颜色使用
 * - 统一的间距
 * - 清晰的层级
 * - 轻量不花哨
 */

// ==================== 卡片组件 ====================

/**
 * 简洁卡片 - 无阴影，仅边框
 */
@Composable
fun CleanCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Radius.md)

    Surface(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = shape,
        color = CleanColors.surface,
        tonalElevation = Elevation.none,
        shadowElevation = Elevation.xs
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            content = content
        )
    }
}

/**
 * 简洁卡片 - 带边框版本
 */
@Composable
fun CleanCardBordered(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Radius.md)

    Surface(
        modifier = modifier
            .border(1.dp, CleanColors.borderLight, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = shape,
        color = CleanColors.surface,
        tonalElevation = Elevation.none
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            content = content
        )
    }
}

// ==================== 列表项组件 ====================

/**
 * 简洁列表项 - 可点击进入详情
 */
@Composable
fun CleanListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    caption: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) CleanColors.surfaceVariant else CleanColors.surface,
        animationSpec = tween(Duration.fast),
        label = "bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = Spacing.listItemHorizontal,
                vertical = Spacing.listItemVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标
        if (leadingIcon != null) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                leadingIcon()
            }
            Spacer(modifier = Modifier.width(Spacing.md))
        }

        // 中间文本区域
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CleanTypography.body,
                color = CleanColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (caption != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = caption,
                    style = CleanTypography.caption,
                    color = CleanColors.textTertiary
                )
            }
        }

        // 右侧内容
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            trailingContent()
        }

        // 箭头指示
        if (showArrow) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CleanColors.textTertiary,
                modifier = Modifier.size(IconSize.sm)
            )
        }
    }
}

/**
 * 简洁分隔线
 */
@Composable
fun CleanDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp
) {
    Divider(
        modifier = modifier.padding(start = startIndent),
        thickness = 1.dp,
        color = CleanColors.divider
    )
}

// ==================== 区块标题 ====================

/**
 * 区块标题
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageHorizontal, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = CleanTypography.title,
            color = CleanColors.textPrimary
        )
        if (action != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
            ) {
                Text(
                    text = action,
                    style = CleanTypography.button,
                    color = CleanColors.primary
                )
            }
        }
    }
}

// ==================== 按钮组件 ====================

/**
 * 主要按钮 - 用于关键操作
 */
@Composable
fun CleanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(TouchTarget.button),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = CleanColors.primary,
            contentColor = CleanColors.onPrimary,
            disabledContainerColor = CleanColors.border,
            disabledContentColor = CleanColors.textDisabled
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Text(text = text, style = CleanTypography.button)
    }
}

/**
 * 次要按钮 - 用于非关键操作
 */
@Composable
fun CleanSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(TouchTarget.button),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.sm),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = CleanColors.primary,
            disabledContentColor = CleanColors.textDisabled
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Text(text = text, style = CleanTypography.button)
    }
}

/**
 * 文字按钮 - 用于最轻量的操作
 */
@Composable
fun CleanTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = CleanColors.primary
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            text = text,
            style = CleanTypography.button,
            color = if (enabled) color else CleanColors.textDisabled
        )
    }
}

// ==================== 状态指示器 ====================

/**
 * 状态标签
 */
@Composable
fun StatusTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.sm),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = CleanTypography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = color,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        )
    }
}

/**
 * 优先级指示点
 */
@Composable
fun PriorityDot(
    priority: String,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    val color = when (priority.uppercase()) {
        "HIGH" -> CleanColors.priorityHigh
        "MEDIUM" -> CleanColors.priorityMedium
        "LOW" -> CleanColors.priorityLow
        else -> CleanColors.priorityNone
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

// ==================== 空状态 ====================

/**
 * 空状态视图
 */
@Composable
fun EmptyStateView(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = CleanColors.textTertiary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
        }

        Text(
            text = message,
            style = CleanTypography.secondary,
            color = CleanColors.textTertiary
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            CleanTextButton(text = actionText, onClick = onActionClick)
        }
    }
}

// ==================== 加载状态 ====================

/**
 * 加载指示器
 */
@Composable
fun CleanLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = 2.dp,
        color = CleanColors.primary,
        trackColor = CleanColors.borderLight
    )
}

/**
 * 页面加载状态
 */
@Composable
fun PageLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CleanLoadingIndicator(size = 32.dp)
    }
}

// ==================== 输入框 ====================

/**
 * 简洁输入框
 */
@Composable
fun CleanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it, style = CleanTypography.secondary) } },
        placeholder = placeholder?.let { { Text(it, style = CleanTypography.body, color = CleanColors.textPlaceholder) } },
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { { Text(it, style = CleanTypography.caption) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(Radius.sm),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CleanColors.primary,
            unfocusedBorderColor = CleanColors.border,
            errorBorderColor = CleanColors.error,
            focusedContainerColor = CleanColors.surface,
            unfocusedContainerColor = CleanColors.surface,
            cursorColor = CleanColors.primary
        ),
        textStyle = CleanTypography.body.copy(color = CleanColors.textPrimary)
    )
}

// ==================== 金额显示 ====================

/**
 * 金额文本
 */
@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = CleanTypography.amountMedium,
    showSign: Boolean = false,
    isExpense: Boolean = false,
    prefix: String = "¥"
) {
    val color = when {
        isExpense -> CleanColors.error
        amount > 0 -> CleanColors.success
        else -> CleanColors.textPrimary
    }

    val sign = when {
        !showSign -> ""
        isExpense || amount < 0 -> "-"
        amount > 0 -> "+"
        else -> ""
    }

    val displayAmount = kotlin.math.abs(amount)
    val formattedAmount = if (displayAmount % 1.0 == 0.0) {
        displayAmount.toLong().toString()
    } else {
        String.format("%.2f", displayAmount)
    }

    Text(
        text = "$sign$prefix$formattedAmount",
        style = style,
        color = color,
        modifier = modifier
    )
}

// ==================== 动画工具 ====================

/**
 * 带涟漪效果的可点击修饰符
 */
@Composable
fun Modifier.cleanClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .clip(RoundedCornerShape(Radius.sm))
        .clickable(
            interactionSource = interactionSource,
            indication = rememberRipple(
                bounded = true,
                color = CleanColors.primary
            ),
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * 带涟漪效果的可点击容器
 */
@Composable
fun CleanClickableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.cleanClickable(enabled = enabled, onClick = onClick),
        content = content
    )
}

/**
 * 状态转换动画规格
 */
object CleanAnimations {
    /** 快速淡入淡出 */
    fun fadeSpec() = androidx.compose.animation.core.tween<Float>(
        durationMillis = Duration.fast
    )

    /** 标准淡入淡出 */
    fun standardFadeSpec() = androidx.compose.animation.core.tween<Float>(
        durationMillis = Duration.standard
    )

    /** 标准滑入滑出 */
    @Composable
    fun slideEnterTransition() = androidx.compose.animation.slideInVertically(
        initialOffsetY = { it / 10 },
        animationSpec = androidx.compose.animation.core.tween(Duration.enter)
    ) + androidx.compose.animation.fadeIn(
        animationSpec = androidx.compose.animation.core.tween(Duration.enter)
    )

    @Composable
    fun slideExitTransition() = androidx.compose.animation.slideOutVertically(
        targetOffsetY = { -it / 10 },
        animationSpec = androidx.compose.animation.core.tween(Duration.exit)
    ) + androidx.compose.animation.fadeOut(
        animationSpec = androidx.compose.animation.core.tween(Duration.exit)
    )
}
