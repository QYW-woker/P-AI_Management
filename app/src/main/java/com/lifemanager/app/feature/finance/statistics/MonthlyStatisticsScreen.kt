package com.lifemanager.app.feature.finance.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifemanager.app.ui.navigation.Screen
import com.lifemanager.app.ui.theme.*

/**
 * 月度统计统一入口页面
 *
 * 包含三个子模块入口：
 * - 月度收支：记录月度收入和支出
 * - 月度资产：记录月度资产变化
 * - 月度定投：记录定投预算和实际投入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToModule: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "月度统计",
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = CleanColors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CleanColors.surface
                )
            )
        },
        containerColor = CleanColors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // 月度收支入口
            item {
                MonthlyModuleCard(
                    icon = Icons.Outlined.SwapVert,
                    iconTint = CleanColors.primary,
                    title = "月度收支",
                    subtitle = "记录每月收入与支出，查看储蓄率",
                    features = listOf("工资/非工资收入", "养老金/定投/日常开销", "储蓄率/开销率分析"),
                    onClick = { onNavigateToModule(Screen.MonthlyIncomeExpense.route) }
                )
            }

            // 月度资产入口
            item {
                MonthlyModuleCard(
                    icon = Icons.Outlined.AccountBalance,
                    iconTint = CleanColors.success,
                    title = "月度资产",
                    subtitle = "记录每月资产变化，跟踪财富增长",
                    features = listOf("银行存款/理财产品", "股票/基金/债券", "房产/车辆等固定资产"),
                    onClick = { onNavigateToModule(Screen.MonthlyAsset.route) }
                )
            }

            // 月度定投入口
            item {
                MonthlyModuleCard(
                    icon = Icons.Outlined.TrendingUp,
                    iconTint = CleanColors.warning,
                    title = "月度定投",
                    subtitle = "记录定投预算与实际投入，跟踪完成率",
                    features = listOf("创业板/科创50/中证500", "沪深300/纳斯达克/标普500", "预算vs实际对比分析"),
                    onClick = { onNavigateToModule(Screen.MonthlyInvestment.route) }
                )
            }

            // 使用说明
            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
                UsageGuideCard()
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * 模块入口卡片
 */
@Composable
private fun MonthlyModuleCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    features: List<String>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.lg),
        color = CleanColors.surface,
        shadowElevation = Elevation.sm
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.Top
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(Radius.md),
                color = iconTint.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(IconSize.lg)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CleanTypography.title,
                    color = CleanColors.textPrimary
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = subtitle,
                    style = CleanTypography.secondary,
                    color = CleanColors.textSecondary
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // 功能列表
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = feature,
                            style = CleanTypography.caption,
                            color = CleanColors.textTertiary
                        )
                    }
                }
            }

            // 箭头
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
 * 使用指南卡片
 */
@Composable
private fun UsageGuideCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = CleanColors.primaryLight
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = CleanColors.primary,
                    modifier = Modifier.size(IconSize.sm)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "使用提示",
                    style = CleanTypography.secondary,
                    color = CleanColors.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "建议每月初记录上月数据，保持记录习惯有助于了解自己的财务状况。",
                style = CleanTypography.caption,
                color = CleanColors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "所有模块都支持导出CSV报表，方便在Excel中进行更深入的分析。",
                style = CleanTypography.caption,
                color = CleanColors.textSecondary
            )
        }
    }
}
