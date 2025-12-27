package com.lifemanager.app.core.voice

import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.ExecutionResult
import com.lifemanager.app.core.ai.service.AIService
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音命令处理状态
 */
sealed class CommandProcessState {
    object Idle : CommandProcessState()
    object Processing : CommandProcessState()
    data class Parsed(val intent: CommandIntent) : CommandProcessState()
    data class NeedConfirmation(val intent: CommandIntent, val description: String) : CommandProcessState()
    data class Executed(val result: ExecutionResult) : CommandProcessState()
    data class Error(val message: String) : CommandProcessState()
}

/**
 * 语音命令处理器
 * 负责解析语音文本并转换为可执行的命令意图
 */
@Singleton
class VoiceCommandProcessor @Inject constructor(
    private val aiService: AIService
) {
    private val _state = MutableStateFlow<CommandProcessState>(CommandProcessState.Idle)
    val state: StateFlow<CommandProcessState> = _state.asStateFlow()

    private var pendingIntent: CommandIntent? = null
    private var autoConfirmEnabled: Boolean = false

    /**
     * 设置自动确认模式
     */
    fun setAutoConfirm(enabled: Boolean) {
        autoConfirmEnabled = enabled
    }

    /**
     * 处理语音文本
     * @param text 识别到的语音文本
     * @param categories 可用的分类列表（用于智能分类）
     * @return 解析后的命令意图
     */
    suspend fun processVoiceText(
        text: String,
        categories: List<CustomFieldEntity> = emptyList()
    ): Result<CommandIntent> {
        if (text.isBlank()) {
            _state.value = CommandProcessState.Error("未识别到有效内容")
            return Result.failure(IllegalArgumentException("空文本"))
        }

        _state.value = CommandProcessState.Processing

        return try {
            val result = aiService.parseCommand(text, categories)

            result.fold(
                onSuccess = { intent ->
                    if (autoConfirmEnabled) {
                        // 自动确认模式，直接标记为已解析
                        _state.value = CommandProcessState.Parsed(intent)
                    } else {
                        // 需要用户确认
                        pendingIntent = intent
                        _state.value = CommandProcessState.NeedConfirmation(
                            intent = intent,
                            description = generateConfirmationMessage(intent)
                        )
                    }
                    Result.success(intent)
                },
                onFailure = { error ->
                    _state.value = CommandProcessState.Error(error.message ?: "解析失败")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            _state.value = CommandProcessState.Error(e.message ?: "处理异常")
            Result.failure(e)
        }
    }

    /**
     * 确认执行待处理的命令
     */
    fun confirmExecution(): CommandIntent? {
        val intent = pendingIntent
        if (intent != null) {
            _state.value = CommandProcessState.Parsed(intent)
            pendingIntent = null
        }
        return intent
    }

    /**
     * 取消待处理的命令
     */
    fun cancelExecution() {
        pendingIntent = null
        _state.value = CommandProcessState.Idle
    }

    /**
     * 标记执行完成
     */
    fun markExecuted(result: ExecutionResult) {
        _state.value = CommandProcessState.Executed(result)
    }

    /**
     * 重置状态
     */
    fun reset() {
        pendingIntent = null
        _state.value = CommandProcessState.Idle
    }

    /**
     * 生成确认消息
     */
    private fun generateConfirmationMessage(intent: CommandIntent): String {
        return when (intent) {
            is CommandIntent.Transaction -> {
                val typeStr = if (intent.type == com.lifemanager.app.core.ai.model.TransactionType.EXPENSE) "支出" else "收入"
                val amount = intent.amount ?: 0.0
                "记录${typeStr}: ${intent.note ?: intent.categoryName ?: ""}，金额 ¥${String.format("%.2f", amount)}"
            }

            is CommandIntent.Todo -> {
                buildString {
                    append("添加待办: ${intent.title}")
                    intent.dueDate?.let { append("，截止时间: $it") }
                }
            }

            is CommandIntent.Diary -> {
                val preview = if (intent.content.length > 20) {
                    intent.content.take(20) + "..."
                } else {
                    intent.content
                }
                "记录日记: $preview"
            }

            is CommandIntent.HabitCheckin -> {
                "习惯打卡: ${intent.habitName}"
            }

            is CommandIntent.TimeTrack -> {
                val actionStr = when (intent.action) {
                    com.lifemanager.app.core.ai.model.TimeTrackAction.START -> "开始"
                    com.lifemanager.app.core.ai.model.TimeTrackAction.STOP -> "停止"
                    com.lifemanager.app.core.ai.model.TimeTrackAction.PAUSE -> "暂停"
                    com.lifemanager.app.core.ai.model.TimeTrackAction.RESUME -> "继续"
                }
                "${actionStr}计时: ${intent.note ?: intent.categoryName ?: "任务"}"
            }

            is CommandIntent.Navigate -> {
                "打开: ${intent.screen}"
            }

            is CommandIntent.Query -> {
                val typeStr = when (intent.type) {
                    com.lifemanager.app.core.ai.model.QueryType.TODAY_EXPENSE -> "查询今日支出"
                    com.lifemanager.app.core.ai.model.QueryType.MONTH_EXPENSE -> "查询本月支出"
                    com.lifemanager.app.core.ai.model.QueryType.MONTH_INCOME -> "查询本月收入"
                    com.lifemanager.app.core.ai.model.QueryType.CATEGORY_EXPENSE -> "查询分类支出"
                    com.lifemanager.app.core.ai.model.QueryType.HABIT_STREAK -> "查询习惯"
                    com.lifemanager.app.core.ai.model.QueryType.GOAL_PROGRESS -> "查询目标"
                    com.lifemanager.app.core.ai.model.QueryType.SAVINGS_PROGRESS -> "查询储蓄"
                }
                val timePeriod = intent.params["timePeriod"] as? String
                timePeriod?.let { "$typeStr ($it)" } ?: typeStr
            }

            is CommandIntent.Goal -> {
                val actionStr = when (intent.action) {
                    com.lifemanager.app.core.ai.model.GoalAction.CREATE -> "创建目标"
                    com.lifemanager.app.core.ai.model.GoalAction.UPDATE -> "更新目标"
                    com.lifemanager.app.core.ai.model.GoalAction.CHECK -> "查看目标"
                    com.lifemanager.app.core.ai.model.GoalAction.DEPOSIT -> "目标存款"
                }
                intent.goalName?.let { "$actionStr: $it" } ?: actionStr
            }

            is CommandIntent.Savings -> {
                val actionStr = when (intent.action) {
                    com.lifemanager.app.core.ai.model.SavingsAction.DEPOSIT -> "存入"
                    com.lifemanager.app.core.ai.model.SavingsAction.WITHDRAW -> "取出"
                    com.lifemanager.app.core.ai.model.SavingsAction.CHECK -> "查看"
                }
                intent.amount?.let {
                    "$actionStr ¥${String.format("%.2f", it)}"
                } ?: actionStr
            }

            is CommandIntent.Unknown -> {
                "未能识别指令，请重新说明"
            }
        }
    }

    companion object {
        /**
         * 检查命令是否需要确认
         * 某些低风险操作可以跳过确认
         */
        fun shouldRequireConfirmation(intent: CommandIntent): Boolean {
            return when (intent) {
                is CommandIntent.Navigate -> false  // 导航不需要确认
                is CommandIntent.Query -> false     // 查询不需要确认
                is CommandIntent.Unknown -> false   // 未知不需要确认
                else -> true                        // 其他操作需要确认
            }
        }
    }
}
