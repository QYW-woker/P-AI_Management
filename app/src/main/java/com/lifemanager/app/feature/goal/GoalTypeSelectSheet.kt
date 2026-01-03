package com.lifemanager.app.feature.goal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifemanager.app.domain.model.GoalStructureType
import com.lifemanager.app.ui.component.*
import com.lifemanager.app.ui.theme.*

/**
 * 目标类型选择底部弹窗
 *
 * 用于选择创建单级目标还是多级目标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalTypeSelectSheet(
    onDismiss: () -> Unit,
    onTypeSelected: (GoalStructureType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = AppShapes.ExtraLarge,
        containerColor = CleanColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.pageHorizontal)
                .padding(bottom = Spacing.xxl)
        ) {
            // 标题
            Text(
                text = "选择目标类型",
                style = CleanTypography.title,
                color = CleanColors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "选择最适合您需求的目标结构",
                style = CleanTypography.secondary,
                color = CleanColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // 单级目标选项
            GoalTypeOption(
                icon = Icons.Default.Flag,
                title = "单级目标",
                description = "适合简单、独立的目标，如存款5万元、读完10本书",
                onClick = {
                    onTypeSelected(GoalStructureType.SINGLE)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // 多级目标选项
            GoalTypeOption(
                icon = Icons.Default.AccountTree,
                title = "多级目标",
                description = "适合复杂目标，可拆分为多个子目标，如健康生活下包含每日运动、均衡饮食等",
                isPrimary = true,
                onClick = {
                    onTypeSelected(GoalStructureType.MULTI_LEVEL)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun GoalTypeOption(
    icon: ImageVector,
    title: String,
    description: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.Medium,
        color = if (isPrimary) CleanColors.primaryLight else CleanColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            // 图标
            Surface(
                shape = AppShapes.Medium,
                color = if (isPrimary) CleanColors.primary else CleanColors.surface,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPrimary) CleanColors.onPrimary else CleanColors.textSecondary,
                        modifier = Modifier.size(IconSize.md)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = CleanTypography.body,
                        color = CleanColors.textPrimary
                    )
                    if (isPrimary) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Surface(
                            shape = AppShapes.Small,
                            color = CleanColors.primary
                        ) {
                            Text(
                                text = "推荐",
                                style = CleanTypography.caption,
                                color = CleanColors.onPrimary,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = description,
                    style = CleanTypography.caption,
                    color = CleanColors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CleanColors.textTertiary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}
