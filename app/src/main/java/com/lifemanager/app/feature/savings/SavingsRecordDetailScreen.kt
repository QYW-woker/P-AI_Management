package com.lifemanager.app.feature.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import com.lifemanager.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 存钱记录详情页面
 *
 * 展示单笔存款的完整信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsRecordDetailScreen(
    recordId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SavingsPlanViewModel = hiltViewModel()
) {
    val deposit by viewModel.getDepositById(recordId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = "存款详情",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (deposit != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val currentDeposit = deposit
        if (currentDeposit != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(AppDimens.PageHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
            ) {
                // 金额卡片
                AmountCard(
                    deposit = currentDeposit,
                    numberFormat = numberFormat
                )

                // 详细信息卡片
                DetailInfoCard(deposit = currentDeposit)

                Spacer(modifier = Modifier.weight(1f))

                // 删除按钮
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text("删除此记录")
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))
            }
        } else {
            // 加载状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条存款记录吗？删除后计划的存款金额将相应减少。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDeposit(recordId)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AmountCard(
    deposit: SavingsRecordEntity,
    numberFormat: NumberFormat
) {
    UnifiedCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Savings,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            // 金额
            Text(
                text = "+¥${numberFormat.format(deposit.amount)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

            // 存款日期
            Text(
                text = formatDateFromInt(deposit.depositDate),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailInfoCard(deposit: SavingsRecordEntity) {
    UnifiedCard {
        Column {
            SectionTitle(title = "详细信息", centered = true)

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            // 存款日期
            DetailRow(
                icon = Icons.Default.CalendarMonth,
                label = "存款日期",
                value = formatDateFromInt(deposit.date)
            )

            Divider(modifier = Modifier.padding(vertical = AppDimens.SpacingMedium))

            // 创建时间
            DetailRow(
                icon = Icons.Default.Schedule,
                label = "创建时间",
                value = formatTimestamp(deposit.createdAt)
            )

            // 备注
            if (deposit.note?.isNotBlank() == true) {
                Divider(modifier = Modifier.padding(vertical = AppDimens.SpacingMedium))

                DetailRow(
                    icon = Icons.Default.Notes,
                    label = "备注",
                    value = deposit.note
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDateFromInt(dateInt: Int): String {
    val year = dateInt / 10000
    val month = (dateInt % 10000) / 100
    val day = dateInt % 100
    return "${year}年${month}月${day}日"
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
