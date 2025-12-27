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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifemanager.app.core.ai.model.*

/**
 * 命令确认对话框
 * 在执行语音命令前显示确认
 */
@Composable
fun CommandConfirmationDialog(
    intent: CommandIntent,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标
                val (icon, iconTint) = getIntentIconAndColor(intent)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = iconTint
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                    text = getIntentTitle(intent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 内容详情
                IntentDetailContent(intent)

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }

                    // 编辑按钮（可选）
                    if (onEdit != null) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("编辑")
                        }
                    }

                    // 确认按钮
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("确认")
                    }
                }
            }
        }
    }
}

/**
 * 获取意图的图标和颜色
 */
@Composable
private fun getIntentIconAndColor(intent: CommandIntent): Pair<ImageVector, androidx.compose.ui.graphics.Color> {
    return when (intent) {
        is CommandIntent.Transaction -> {
            if (intent.type == TransactionType.EXPENSE) {
                Icons.Default.RemoveCircle to MaterialTheme.colorScheme.error
            } else {
                Icons.Default.AddCircle to MaterialTheme.colorScheme.primary
            }
        }
        is CommandIntent.Todo -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.secondary
        is CommandIntent.Diary -> Icons.Default.Book to MaterialTheme.colorScheme.tertiary
        is CommandIntent.HabitCheckin -> Icons.Default.Done to MaterialTheme.colorScheme.primary
        is CommandIntent.TimeTrack -> Icons.Default.Timer to MaterialTheme.colorScheme.secondary
        is CommandIntent.Navigate -> Icons.Default.Navigation to MaterialTheme.colorScheme.tertiary
        is CommandIntent.Query -> Icons.Default.Search to MaterialTheme.colorScheme.primary
        is CommandIntent.Goal -> Icons.Default.Flag to MaterialTheme.colorScheme.secondary
        is CommandIntent.Savings -> Icons.Default.Savings to MaterialTheme.colorScheme.primary
        is CommandIntent.Unknown -> Icons.Default.QuestionMark to MaterialTheme.colorScheme.error
    }
}

/**
 * 获取意图标题
 */
private fun getIntentTitle(intent: CommandIntent): String {
    return when (intent) {
        is CommandIntent.Transaction -> {
            if (intent.type == TransactionType.EXPENSE) "记录支出" else "记录收入"
        }
        is CommandIntent.Todo -> "添加待办"
        is CommandIntent.Diary -> "记录日记"
        is CommandIntent.HabitCheckin -> "习惯打卡"
        is CommandIntent.TimeTrack -> {
            when (intent.action) {
                TimeTrackAction.START -> "开始计时"
                TimeTrackAction.STOP -> "停止计时"
                TimeTrackAction.PAUSE -> "暂停计时"
                TimeTrackAction.RESUME -> "继续计时"
            }
        }
        is CommandIntent.Navigate -> "页面导航"
        is CommandIntent.Query -> "数据查询"
        is CommandIntent.Goal -> {
            when (intent.action) {
                GoalAction.CREATE -> "创建目标"
                GoalAction.UPDATE -> "更新目标"
                GoalAction.CHECK -> "查看目标"
                GoalAction.DEPOSIT -> "目标存款"
            }
        }
        is CommandIntent.Savings -> {
            when (intent.action) {
                SavingsAction.DEPOSIT -> "存入存款"
                SavingsAction.WITHDRAW -> "取出存款"
                SavingsAction.CHECK -> "查看存款"
            }
        }
        is CommandIntent.Unknown -> "未识别命令"
    }
}

/**
 * 意图详情内容
 */
@Composable
private fun IntentDetailContent(intent: CommandIntent) {
    when (intent) {
        is CommandIntent.Transaction -> TransactionDetail(intent)
        is CommandIntent.Todo -> TodoDetail(intent)
        is CommandIntent.Diary -> DiaryDetail(intent)
        is CommandIntent.HabitCheckin -> HabitCheckinDetail(intent)
        is CommandIntent.TimeTrack -> TimeTrackDetail(intent)
        is CommandIntent.Navigate -> NavigateDetail(intent)
        is CommandIntent.Query -> QueryDetail(intent)
        is CommandIntent.Goal -> GoalDetail(intent)
        is CommandIntent.Savings -> SavingsDetail(intent)
        is CommandIntent.Unknown -> UnknownDetail(intent)
    }
}

@Composable
private fun TransactionDetail(intent: CommandIntent.Transaction) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 金额
        val amountValue = intent.amount ?: 0.0
        Text(
            text = "¥${String.format("%.2f", amountValue)}",
            style = MaterialTheme.typography.headlineMedium,
            color = if (intent.type == TransactionType.EXPENSE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 备注
        intent.note?.let { note ->
            DetailRow(label = "备注", value = note)
        }

        // 分类
        intent.categoryName?.let { cat ->
            DetailRow(label = "分类", value = cat)
        }

        // 商家
        intent.payee?.let { payee ->
            DetailRow(label = "商家", value = payee)
        }
    }
}

@Composable
private fun TodoDetail(intent: CommandIntent.Todo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = intent.title,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        intent.dueDate?.let { date ->
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "截止日期", value = date.toString())
        }

        intent.dueTime?.let { time ->
            DetailRow(label = "截止时间", value = time)
        }

        intent.priority?.let { priority ->
            DetailRow(label = "优先级", value = priority)
        }
    }
}

@Composable
private fun DiaryDetail(intent: CommandIntent.Diary) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val preview = if (intent.content.length > 100) {
            intent.content.take(100) + "..."
        } else {
            intent.content
        }

        Text(
            text = preview,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        intent.mood?.let { mood ->
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "心情", value = getMoodText(mood))
        }
    }
}

@Composable
private fun HabitCheckinDetail(intent: CommandIntent.HabitCheckin) {
    Text(
        text = intent.habitName,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TimeTrackDetail(intent: CommandIntent.TimeTrack) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        intent.note?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        intent.categoryName?.let { cat ->
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "分类", value = cat)
        }
    }
}

@Composable
private fun NavigateDetail(intent: CommandIntent.Navigate) {
    Text(
        text = "打开「${intent.screen}」",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun QueryDetail(intent: CommandIntent.Query) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val queryTypeText = when (intent.type) {
            QueryType.TODAY_EXPENSE -> "查询今日支出"
            QueryType.MONTH_EXPENSE -> "查询本月支出"
            QueryType.MONTH_INCOME -> "查询本月收入"
            QueryType.CATEGORY_EXPENSE -> "查询分类支出"
            QueryType.HABIT_STREAK -> "查询习惯连续天数"
            QueryType.GOAL_PROGRESS -> "查询目标进度"
            QueryType.SAVINGS_PROGRESS -> "查询存钱进度"
        }

        Text(
            text = queryTypeText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        val timePeriod = intent.params["timePeriod"] as? String
        timePeriod?.let { period ->
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "时间范围", value = period)
        }
    }
}

@Composable
private fun GoalDetail(intent: CommandIntent.Goal) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        intent.goalName?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        intent.progress?.let { progress ->
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "进度", value = "${String.format("%.1f", progress * 100)}%")
        }
    }
}

@Composable
private fun SavingsDetail(intent: CommandIntent.Savings) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        intent.planName?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        intent.amount?.let { amount ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¥${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UnknownDetail(intent: CommandIntent.Unknown) {
    Text(
        text = "原始内容: ${intent.originalText}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$label: ",
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
 * 执行结果对话框
 */
@Composable
fun ExecutionResultDialog(
    result: ExecutionResult,
    onDismiss: () -> Unit,
    onAction: (() -> Unit)? = null,
    actionText: String = "查看",
    modifier: Modifier = Modifier
) {
    val isSuccess = result is ExecutionResult.Success
    val message = when (result) {
        is ExecutionResult.Success -> result.message
        is ExecutionResult.Failure -> result.message
        is ExecutionResult.NeedConfirmation -> result.previewMessage
        is ExecutionResult.NeedMoreInfo -> result.prompt
        is ExecutionResult.NotRecognized -> "未能识别: ${result.originalText}"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 结果图标
                val (icon, color) = if (isSuccess) {
                    Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
                } else {
                    Icons.Default.Error to MaterialTheme.colorScheme.error
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = color
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 消息
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isSuccess && onAction != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("关闭")
                        }
                        Button(
                            onClick = onAction,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(actionText)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
    }
}
