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
        shape = AppShapes.ExtraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.PageHorizontalPadding)
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = "选择目标类型",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "选择最适合您需求的目标结构",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 单级目标选项
            GoalTypeOption(
                icon = Icons.Default.Flag,
                title = "单级目标",
                description = "适合简单、独立的目标，如"存款5万元"、"读完10本书"",
                onClick = {
                    onTypeSelected(GoalStructureType.SINGLE)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 多级目标选项
            GoalTypeOption(
                icon = Icons.Default.AccountTree,
                title = "多级目标",
                description = "适合复杂目标，可拆分为多个子目标，如"健康生活"下包含"每日运动"、"均衡饮食"等",
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.Medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.SpacingNormal),
            verticalAlignment = Alignment.Top
        ) {
            // 图标
            Surface(
                shape = AppShapes.Medium,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPrimary) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppDimens.SpacingNormal))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = AppShapes.Small,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "推荐",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}
