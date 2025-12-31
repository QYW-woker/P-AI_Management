package com.lifemanager.app.feature.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.ai.model.*
import com.lifemanager.app.feature.ai.component.CommandConfirmationDialog
import com.lifemanager.app.ui.component.PremiumTextField
import kotlinx.coroutines.launch

/**
 * AI对话界面
 *
 * 多轮对话 + 智能建议 + 快捷操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onExecuteIntent: (CommandIntent) -> Unit,
    viewModel: AIAssistantViewModel = hiltViewModel()
) {
    val conversationContext by viewModel.conversationContext.collectAsState()
    val aiMode by viewModel.aiMode.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState(initial = emptyList())
    val quickActions by viewModel.quickActions.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val pendingIntent by viewModel.pendingIntent.collectAsState()
    val queryResult by viewModel.queryResult.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 自动滚动到最新消息
    LaunchedEffect(conversationContext.messages.size) {
        if (conversationContext.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversationContext.messages.size - 1)
        }
    }

    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("小管家")
                        Spacer(modifier = Modifier.width(8.dp))
                        // AI模式指示器
                        Surface(
                            color = when (aiMode) {
                                AIMode.CHAT -> MaterialTheme.colorScheme.primary
                                AIMode.COMMAND -> MaterialTheme.colorScheme.tertiary
                                AIMode.ASSISTANT -> MaterialTheme.colorScheme.secondary
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = when (aiMode) {
                                    AIMode.CHAT -> "对话"
                                    AIMode.COMMAND -> "命令"
                                    AIMode.ASSISTANT -> "助手"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 刷新建议
                    IconButton(
                        onClick = { viewModel.refreshSuggestions() },
                        enabled = !uiState.isRefreshingSuggestions
                    ) {
                        if (uiState.isRefreshingSuggestions) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                    // 清除对话
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清除对话")
                    }
                    // 设置
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 智能建议区域（可折叠）
            AnimatedVisibility(
                visible = showSuggestions && suggestions.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SuggestionsSection(
                    suggestions = suggestions.take(3),
                    onDismiss = { viewModel.dismissSuggestion(it) },
                    onAction = { suggestion ->
                        suggestion.action?.let { action ->
                            handleSuggestionAction(action, viewModel, onExecuteIntent)
                        }
                    },
                    onHide = { showSuggestions = false }
                )
            }

            // 隐藏建议时的小按钮
            if (!showSuggestions && suggestions.isNotEmpty()) {
                TextButton(
                    onClick = { showSuggestions = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("查看${suggestions.size}条建议")
                }
            }

            // 对话区域
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 欢迎消息
                uiState.welcomeMessage?.let { welcome ->
                    item {
                        MessageBubble(
                            message = welcome,
                            onQuickReply = { viewModel.sendQuickReply(it) }
                        )
                    }
                }

                // 对话消息
                items(conversationContext.messages) { message ->
                    MessageBubble(
                        message = message,
                        onQuickReply = { viewModel.sendQuickReply(it) }
                    )
                }

                // 处理中指示器
                if (isProcessing) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // 快捷操作区域
            QuickActionsRow(
                actions = quickActions,
                onAction = { viewModel.executeQuickAction(it) }
            )

            // 输入区域
            ChatInputArea(
                textInput = textInput,
                onTextChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                    }
                },
                isProcessing = isProcessing
            )
        }
    }

    // 确认对话框
    if (uiState.showConfirmDialog && pendingIntent != null) {
        CommandConfirmationDialog(
            intent = pendingIntent!!,
            onConfirm = {
                val intent = viewModel.confirmIntent()
                intent?.let { onExecuteIntent(it) }
            },
            onCancel = { viewModel.cancelIntent() }
        )
    }

    // 查询结果对话框
    if (uiState.showQueryResult && queryResult != null) {
        QueryResultDialog(
            result = queryResult!!,
            onDismiss = { viewModel.dismissQueryResult() }
        )
    }
}

/**
 * 智能建议区域
 */
@Composable
private fun SuggestionsSection(
    suggestions: List<AISuggestion>,
    onDismiss: (String) -> Unit,
    onAction: (AISuggestion) -> Unit,
    onHide: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "智能建议",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = onHide,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.ExpandLess,
                    contentDescription = "收起",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        suggestions.forEach { suggestion ->
            SuggestionCard(
                suggestion = suggestion,
                onDismiss = { onDismiss(suggestion.id) },
                onAction = { onAction(suggestion) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 建议卡片
 */
@Composable
private fun SuggestionCard(
    suggestion: AISuggestion,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction() },
        colors = CardDefaults.cardColors(
            containerColor = when (suggestion.type) {
                SuggestionType.BUDGET_WARNING -> MaterialTheme.colorScheme.errorContainer
                SuggestionType.TIME_SENSITIVE -> MaterialTheme.colorScheme.tertiaryContainer
                SuggestionType.GOAL_MILESTONE -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (suggestion.type) {
                    SuggestionType.BUDGET_WARNING -> Icons.Default.Warning
                    SuggestionType.SPENDING_ALERT -> Icons.Default.TrendingUp
                    SuggestionType.HABIT_REMINDER -> Icons.Default.Favorite
                    SuggestionType.GOAL_MILESTONE -> Icons.Default.EmojiEvents
                    SuggestionType.TIME_SENSITIVE -> Icons.Default.Schedule
                    else -> Icons.Default.Lightbulb
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "忽略",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 消息气泡
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    onQuickReply: (QuickReply) -> Unit
) {
    val isUser = message.role == ChatRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }

        // 快捷回复按钮
        if (!isUser && message.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(message.suggestions) { reply ->
                    SuggestionChip(
                        onClick = { onQuickReply(reply) },
                        label = { Text(reply.text, maxLines = 1) }
                    )
                }
            }
        }
    }
}

/**
 * 正在输入指示器
 */
@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    val alpha by animateFloatAsState(
                        targetValue = 1f,
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.6f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * 快捷操作行
 */
@Composable
private fun QuickActionsRow(
    actions: List<QuickAction>,
    onAction: (QuickAction) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions.take(6)) { action ->
            AssistChip(
                onClick = { onAction(action) },
                label = { Text(action.title, maxLines = 1) },
                leadingIcon = {
                    Icon(
                        imageVector = getIconForAction(action),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

/**
 * 获取快捷操作图标
 */
@Composable
private fun getIconForAction(action: QuickAction) = when (action.icon) {
    "ShoppingCart" -> Icons.Default.ShoppingCart
    "AccountBalance" -> Icons.Default.AccountBalance
    "CheckCircle" -> Icons.Default.CheckCircle
    "Favorite" -> Icons.Default.Favorite
    "Today" -> Icons.Default.Today
    "CalendarMonth" -> Icons.Default.CalendarMonth
    "Savings" -> Icons.Default.Savings
    "BarChart" -> Icons.Default.BarChart
    else -> Icons.Default.TouchApp
}

/**
 * 聊天输入区域
 */
@Composable
private fun ChatInputArea(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumTextField(
                value = textInput,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = "说点什么...",
                singleLine = true,
                enabled = !isProcessing
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = textInput.isNotBlank() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "发送"
                    )
                }
            }
        }
    }
}

/**
 * 查询结果对话框
 */
@Composable
private fun QueryResultDialog(
    result: DataQueryResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("查询结果") },
        text = {
            Column {
                Text(
                    text = result.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (result.details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    result.details.forEach { detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = detail.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = detail.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                detail.trend?.let { trend ->
                                    Icon(
                                        imageVector = when (trend) {
                                            TrendDirection.UP -> Icons.Default.TrendingUp
                                            TrendDirection.DOWN -> Icons.Default.TrendingDown
                                            TrendDirection.STABLE -> Icons.Default.TrendingFlat
                                        },
                                        contentDescription = null,
                                        tint = when (trend) {
                                            TrendDirection.UP -> Color.Red
                                            TrendDirection.DOWN -> Color.Green
                                            TrendDirection.STABLE -> Color.Gray
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (result.suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "建议",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    result.suggestions.forEach { suggestion ->
                        Text(
                            text = "• $suggestion",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

/**
 * 处理建议动作
 */
private fun handleSuggestionAction(
    action: SuggestionAction,
    viewModel: AIAssistantViewModel,
    onExecuteIntent: (CommandIntent) -> Unit
) {
    when (action) {
        is SuggestionAction.Navigate -> {
            viewModel.sendMessage("打开${action.screen}")
        }
        is SuggestionAction.CreateTransaction -> {
            val typeText = if (action.type == "income") "收入" else "支出"
            val categoryText = action.category?.let { "，分类$it" } ?: ""
            viewModel.sendMessage("记一笔$typeText$categoryText")
        }
        is SuggestionAction.CreateTodo -> {
            viewModel.sendMessage("添加待办：${action.title}")
        }
        is SuggestionAction.ViewReport -> {
            viewModel.sendMessage("查看${action.reportType}报表")
        }
        else -> {}
    }
}
