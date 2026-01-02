package com.lifemanager.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一设计规范
 *
 * 定义全应用统一的圆角、间距、尺寸等设计常量
 */
object AppDimens {

    // ==================== 圆角规范 ====================
    /** 小圆角 - 用于标签、小按钮 */
    val CornerSmall: Dp = 8.dp
    /** 中圆角 - 用于卡片、输入框 */
    val CornerMedium: Dp = 12.dp
    /** 大圆角 - 用于大卡片、底部弹窗 */
    val CornerLarge: Dp = 16.dp
    /** 超大圆角 - 用于全屏弹窗 */
    val CornerExtraLarge: Dp = 24.dp

    // ==================== 间距规范 ====================
    /** 极小间距 */
    val SpacingXSmall: Dp = 4.dp
    /** 小间距 */
    val SpacingSmall: Dp = 8.dp
    /** 中间距 */
    val SpacingMedium: Dp = 12.dp
    /** 标准间距 */
    val SpacingNormal: Dp = 16.dp
    /** 大间距 */
    val SpacingLarge: Dp = 20.dp
    /** 超大间距 */
    val SpacingXLarge: Dp = 24.dp
    /** 巨大间距 */
    val SpacingXXLarge: Dp = 32.dp

    // ==================== 页面内边距 ====================
    /** 页面水平内边距 */
    val PageHorizontalPadding: Dp = 16.dp
    /** 页面垂直内边距 */
    val PageVerticalPadding: Dp = 16.dp
    /** 卡片内边距 */
    val CardPadding: Dp = 16.dp

    // ==================== 组件尺寸 ====================
    /** 图标小尺寸 */
    val IconSmall: Dp = 16.dp
    /** 图标中尺寸 */
    val IconMedium: Dp = 24.dp
    /** 图标大尺寸 */
    val IconLarge: Dp = 32.dp
    /** 图标超大尺寸 */
    val IconXLarge: Dp = 48.dp
    /** 图标巨大尺寸 */
    val IconXXLarge: Dp = 64.dp

    /** 头像小尺寸 */
    val AvatarSmall: Dp = 32.dp
    /** 头像中尺寸 */
    val AvatarMedium: Dp = 40.dp
    /** 头像大尺寸 */
    val AvatarLarge: Dp = 56.dp

    /** 按钮最小高度 */
    val ButtonMinHeight: Dp = 48.dp
    /** 快捷按钮尺寸 */
    val QuickButtonSize: Dp = 56.dp

    /** 进度条高度 */
    val ProgressBarHeight: Dp = 8.dp

    /** 分割线厚度 */
    val DividerThickness: Dp = 1.dp

    // ==================== 列表项间距 ====================
    /** 列表项垂直间距 */
    val ListItemSpacing: Dp = 12.dp
    /** 网格项间距 */
    val GridItemSpacing: Dp = 12.dp
}

/**
 * 统一形状规范
 */
object AppShapes {
    val Small = RoundedCornerShape(AppDimens.CornerSmall)
    val Medium = RoundedCornerShape(AppDimens.CornerMedium)
    val Large = RoundedCornerShape(AppDimens.CornerLarge)
    val ExtraLarge = RoundedCornerShape(AppDimens.CornerExtraLarge)
}

/**
 * 统一顶部应用栏
 *
 * 所有页面使用统一的 TopAppBar 样式，标题居中
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopAppBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * 统一页面标题（用于模块标题）
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    centered: Boolean = true
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = modifier.then(
            if (centered) Modifier.fillMaxWidth() else Modifier
        )
    )
}

/**
 * 统一卡片样式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = AppShapes.Large,
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.CardPadding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = AppShapes.Large
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.CardPadding),
                content = content
            )
        }
    }
}

/**
 * 统一统计卡片
 */
@Composable
fun UnifiedStatsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.Large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardPadding),
            content = content
        )
    }
}
