package com.lifemanager.app.feature.ai.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.GoalAction
import com.lifemanager.app.core.ai.model.QueryType
import com.lifemanager.app.core.ai.model.SavingsAction
import com.lifemanager.app.core.ai.model.TimeTrackAction
import com.lifemanager.app.core.ai.model.TransactionType

/**
 * 语音命令确认对话框
 * 在执行语音命令前显示确认信息
 */
@Composable
fun VoiceConfirmationDialog(
    intent: CommandIntent,
    description: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = getIntentIcon(intent),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = getIntentTitle(intent),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                // 命令描述
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 详细信息卡片
                IntentDetailCard(intent = intent)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("确认执行")
            }
        },
        dismissButton = {
            Row {
                if (onEdit != null) {
                    OutlinedButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("编辑")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
            }
        }
    )
}

/**
 * 命令详情卡片
 */
@Composable
private fun IntentDetailCard(intent: CommandIntent) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (intent) {
                is CommandIntent.Transaction -> {
                    DetailRow(
                        label = "类型",
                        value = if (intent.type == TransactionType.EXPENSE) "支出" else "收入"
                    )
                    intent.amount?.let { amount ->
                        DetailRow(
                            label = "金额",
                            value = "¥${String.format("%.2f", amount)}"
                        )
                    }
                    intent.note?.let {
                        DetailRow(label = "备注", value = it)
                    }
                    intent.categoryName?.let {
                        DetailRow(label = "分类", value = it)
                    }
                    intent.payee?.let {
                        DetailRow(label = "商家", value = it)
                    }
                }

                is CommandIntent.Todo -> {
                    DetailRow(label = "标题", value = intent.title)
                    intent.dueDate?.let {
                        DetailRow(label = "截止日期", value = it.toString())
                    }
                    intent.dueTime?.let {
                        DetailRow(label = "截止时间", value = it)
                    }
                    intent.priority?.let {
                        DetailRow(label = "优先级", value = it)
                    }
                }

                is CommandIntent.Diary -> {
                    DetailRow(label = "内容", value = intent.content)
                    intent.mood?.let {
                        DetailRow(label = "心情", value = getMoodText(it))
                    }
                    intent.weather?.let {
                        DetailRow(label = "天气", value = it)
                    }
                }

                is CommandIntent.HabitCheckin -> {
                    DetailRow(label = "习惯", value = intent.habitName)
                    intent.value?.let {
                        DetailRow(label = "数值", value = it.toString())
                    }
                }

                is CommandIntent.TimeTrack -> {
                    DetailRow(
                        label = "操作",
                        value = when (intent.action) {
                            TimeTrackAction.START -> "开始计时"
                            TimeTrackAction.STOP -> "停止计时"
                            TimeTrackAction.PAUSE -> "暂停计时"
                            TimeTrackAction.RESUME -> "继续计时"
                        }
                    )
                    intent.categoryName?.let {
                        DetailRow(label = "分类", value = it)
                    }
                    intent.note?.let {
                        DetailRow(label = "备注", value = it)
                    }
                }

                is CommandIntent.Navigate -> {
                    DetailRow(label = "目标页面", value = intent.screen)
                }

                is CommandIntent.Query -> {
                    DetailRow(
                        label = "查询类型",
                        value = when (intent.type) {
                            QueryType.TODAY_EXPENSE -> "今日支出"
                            QueryType.MONTH_EXPENSE -> "本月支出"
                            QueryType.MONTH_INCOME -> "本月收入"
                            QueryType.CATEGORY_EXPENSE -> "分类支出"
                            QueryType.HABIT_STREAK -> "习惯连续天数"
                            QueryType.GOAL_PROGRESS -> "目标进度"
                            QueryType.SAVINGS_PROGRESS -> "存钱进度"
                        }
                    )
                    (intent.params["timePeriod"] as? String)?.let {
                        DetailRow(label = "时间范围", value = it)
                    }
                }

                is CommandIntent.Goal -> {
                    DetailRow(
                        label = "操作",
                        value = when (intent.action) {
                            GoalAction.CREATE -> "创建目标"
                            GoalAction.UPDATE -> "更新目标"
                            GoalAction.CHECK -> "查看目标"
                            GoalAction.DEPOSIT -> "目标存款"
                        }
                    )
                    intent.goalName?.let {
                        DetailRow(label = "目标名称", value = it)
                    }
                    intent.progress?.let {
                        DetailRow(label = "进度", value = "${String.format("%.1f", it * 100)}%")
                    }
                }

                is CommandIntent.Savings -> {
                    DetailRow(
                        label = "操作",
                        value = when (intent.action) {
                            SavingsAction.DEPOSIT -> "存入"
                            SavingsAction.WITHDRAW -> "取出"
                            SavingsAction.CHECK -> "查看"
                        }
                    )
                    intent.amount?.let {
                        DetailRow(label = "金额", value = "¥${String.format("%.2f", it)}")
                    }
                    intent.planName?.let {
                        DetailRow(label = "计划", value = it)
                    }
                }

                is CommandIntent.Unknown -> {
                    Text(
                        text = "原始输入: ${intent.originalText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 详情行
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 获取命令意图对应的图标
 */
private fun getIntentIcon(intent: CommandIntent): ImageVector {
    return when (intent) {
        is CommandIntent.Transaction -> {
            if (intent.type == TransactionType.EXPENSE) Icons.Default.RemoveCircle
            else Icons.Default.AddCircle
        }
        is CommandIntent.Todo -> Icons.Default.CheckCircle
        is CommandIntent.Diary -> Icons.Default.Book
        is CommandIntent.HabitCheckin -> Icons.Default.Stars
        is CommandIntent.TimeTrack -> Icons.Default.Timer
        is CommandIntent.Navigate -> Icons.Default.Navigation
        is CommandIntent.Query -> Icons.Default.Search
        is CommandIntent.Goal -> Icons.Default.Flag
        is CommandIntent.Savings -> Icons.Default.Savings
        is CommandIntent.Unknown -> Icons.Default.HelpOutline
    }
}

/**
 * 获取命令意图对应的标题
 */
private fun getIntentTitle(intent: CommandIntent): String {
    return when (intent) {
        is CommandIntent.Transaction -> {
            if (intent.type == TransactionType.EXPENSE) "记录支出" else "记录收入"
        }
        is CommandIntent.Todo -> "添加待办"
        is CommandIntent.Diary -> "写日记"
        is CommandIntent.HabitCheckin -> "习惯打卡"
        is CommandIntent.TimeTrack -> "时间追踪"
        is CommandIntent.Navigate -> "页面导航"
        is CommandIntent.Query -> "查询"
        is CommandIntent.Goal -> "目标管理"
        is CommandIntent.Savings -> "储蓄操作"
        is CommandIntent.Unknown -> "未识别命令"
    }
}

/**
 * 获取心情文本
 */
private fun getMoodText(mood: Int): String {
    return when (mood) {
        1 -> "很差"
        2 -> "较差"
        3 -> "一般"
        4 -> "不错"
        5 -> "很好"
        else -> "未知"
    }
}

/**
 * 命令执行结果提示
 */
@Composable
fun CommandResultSnackbar(
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier.padding(16.dp),
        containerColor = if (isSuccess) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (isSuccess) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        action = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(message)
        }
    }
}
