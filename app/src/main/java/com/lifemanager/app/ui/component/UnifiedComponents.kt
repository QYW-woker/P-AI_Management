package com.lifemanager.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifemanager.app.ui.theme.*

/**
 * 统一UI组件库
 *
 * 提供跨功能模块的统一UI组件
 */

// ==================== 尺寸常量 ====================

/**
 * 应用尺寸常量 - 使用 Pascal Case 命名以兼容现有代码
 */
object AppDimens {
    /** 最小间距 4dp */
    val XS: Dp = Spacing.xs
    /** 小间距 8dp */
    val SM: Dp = Spacing.sm
    /** 中间距 12dp */
    val MD: Dp = Spacing.md
    /** 标准间距 16dp */
    val LG: Dp = Spacing.lg
    /** 大间距 24dp */
    val XL: Dp = Spacing.xl
    /** 超大间距 32dp */
    val XXL: Dp = Spacing.xxl

    /** 页面水平边距 */
    val PageHorizontalPadding: Dp = Spacing.pageHorizontal
    /** 页面垂直边距 */
    val PageVerticalPadding: Dp = Spacing.pageVertical

    /** 间距常量 */
    val SpacingSmall: Dp = Spacing.sm        // 8.dp
    val SpacingNormal: Dp = Spacing.md       // 12.dp
    val SpacingMedium: Dp = Spacing.lg       // 16.dp
    val SpacingLarge: Dp = Spacing.xl        // 24.dp
    val SpacingXLarge: Dp = Spacing.xxl      // 32.dp
    val SpacingXXLarge: Dp = 40.dp           // 超大间距

    /** 卡片内边距 */
    val CardPadding: Dp = Spacing.cardPadding

    /** 进度条高度 */
    val ProgressBarHeight: Dp = 8.dp

    /** 图标尺寸 */
    val IconSmall: Dp = IconSize.sm          // 20.dp
    val IconMedium: Dp = IconSize.md         // 24.dp
    val IconLarge: Dp = IconSize.lg          // 32.dp
    val IconXLarge: Dp = IconSize.xl         // 40.dp
    val IconXXLarge: Dp = 48.dp
}

// ==================== 形状常量 ====================

/**
 * 应用形状常量 - 使用 Pascal Case 命名
 */
object AppShapes {
    /** 小圆角 8dp */
    val Small: RoundedCornerShape = RoundedCornerShape(Radius.sm)
    /** 中圆角 12dp */
    val Medium: RoundedCornerShape = RoundedCornerShape(Radius.md)
    /** 大圆角 16dp */
    val Large: RoundedCornerShape = RoundedCornerShape(Radius.lg)
    /** 超大圆角 24dp */
    val ExtraLarge: RoundedCornerShape = RoundedCornerShape(Radius.xl)
    /** 胶囊形 */
    val Capsule: RoundedCornerShape = RoundedCornerShape(Radius.full)
}

// ==================== 顶部应用栏 ====================

/**
 * 统一顶部应用栏
 *
 * @param title 标题
 * @param onNavigateBack 返回回调，null则不显示返回按钮
 * @param actions 右侧操作按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = CleanTypography.title,
                color = CleanColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = CleanColors.textPrimary
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = CleanColors.surface,
            scrolledContainerColor = CleanColors.surface,
            titleContentColor = CleanColors.textPrimary
        )
    )
}

// ==================== 区块标题 ====================

/**
 * 区块标题组件
 *
 * @param title 标题文本
 * @param action 右侧操作文本
 * @param onActionClick 操作点击回调
 * @param centered 是否居中显示标题
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
    centered: Boolean = false
) {
    if (centered) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = CleanTypography.title,
                color = CleanColors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
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
                    contentPadding = PaddingValues(horizontal = Spacing.sm)
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
}

// ==================== 统一卡片 ====================

/**
 * 统一卡片组件
 *
 * @param modifier Modifier
 * @param onClick 点击回调
 * @param content 内容
 */
@Composable
fun UnifiedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.Medium,
        color = CleanColors.surface,
        shadowElevation = Elevation.xs,
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            content = content
        )
    }
}

// ==================== 统计卡片 ====================

/**
 * 统一统计卡片 - 带标题和值参数版本
 *
 * @param title 标题
 * @param value 值
 * @param subtitle 副标题
 * @param icon 图标
 * @param iconTint 图标颜色
 * @param onClick 点击回调
 */
@Composable
fun UnifiedStatsCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = CleanColors.primary,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.Medium,
        color = CleanColors.surface,
        shadowElevation = Elevation.xs,
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标区域
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AppShapes.Medium)
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(IconSize.md)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
            }

            // 文本区域
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = value,
                    style = CleanTypography.amountMedium,
                    color = CleanColors.textPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = subtitle,
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                }
            }
        }
    }
}

/**
 * 统一统计卡片 - 自定义内容版本 (作为容器使用)
 *
 * @param modifier Modifier
 * @param onClick 点击回调
 * @param content 自定义内容
 */
@Composable
fun UnifiedStatsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.Medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shadowElevation = Elevation.xs,
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            content = content
        )
    }
}

// ==================== 进度条 ====================

/**
 * 统一进度条
 *
 * @param progress 进度 0-1
 * @param color 进度条颜色
 * @param trackColor 轨道颜色
 */
@Composable
fun UnifiedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CleanColors.primary,
    trackColor: Color = CleanColors.borderLight,
    height: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(AppShapes.Capsule)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(AppShapes.Capsule)
                .background(color)
        )
    }
}

// ==================== 数据项 ====================

/**
 * 统一数据项 - 用于显示标签+值的组合
 *
 * @param label 标签
 * @param value 值
 */
@Composable
fun UnifiedDataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = CleanColors.textPrimary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = CleanTypography.secondary,
            color = CleanColors.textSecondary
        )
        Text(
            text = value,
            style = CleanTypography.body,
            color = valueColor
        )
    }
}
