package com.lifemanager.app.core.voice

import com.lifemanager.app.core.ai.model.*
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.DiaryEntity
import com.lifemanager.app.core.database.entity.TransactionSource
import com.lifemanager.app.core.database.entity.Priority
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import com.lifemanager.app.domain.repository.TodoRepository
import com.lifemanager.app.domain.repository.DiaryRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音命令执行器
 * 负责将解析后的命令意图转换为实际的数据库操作
 */
@Singleton
class VoiceCommandExecutor @Inject constructor(
    private val transactionRepository: DailyTransactionRepository,
    private val todoRepository: TodoRepository,
    private val diaryRepository: DiaryRepository
) {

    /**
     * 执行命令意图
     * @param intent 命令意图
     * @return 执行结果
     */
    suspend fun execute(intent: CommandIntent): ExecutionResult {
        return try {
            when (intent) {
                is CommandIntent.Transaction -> executeTransaction(intent)
                is CommandIntent.Todo -> executeTodo(intent)
                is CommandIntent.Diary -> executeDiary(intent)
                is CommandIntent.HabitCheckin -> executeHabitCheckin(intent)
                is CommandIntent.TimeTrack -> executeTimeTrack(intent)
                is CommandIntent.Navigate -> executeNavigate(intent)
                is CommandIntent.Query -> executeQuery(intent)
                is CommandIntent.Goal -> executeGoal(intent)
                is CommandIntent.Savings -> executeSavings(intent)
                is CommandIntent.Unknown -> ExecutionResult.NotRecognized(intent.originalText)
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "执行失败")
        }
    }

    /**
     * 执行记账操作
     */
    private suspend fun executeTransaction(intent: CommandIntent.Transaction): ExecutionResult {
        val amount = intent.amount ?: return ExecutionResult.NeedMoreInfo(
            intent = intent,
            missingFields = listOf("amount"),
            prompt = "请提供金额"
        )

        val date = intent.date?.let { LocalDate.ofEpochDay(it.toLong()) } ?: LocalDate.now()
        val now = LocalTime.now()

        val entity = DailyTransactionEntity(
            id = 0,
            type = if (intent.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME",
            amount = amount,
            categoryId = intent.categoryId,
            date = date.toEpochDay().toInt(),
            time = intent.time ?: now.format(DateTimeFormatter.ofPattern("HH:mm")),
            note = intent.note ?: "",
            source = TransactionSource.VOICE
        )

        transactionRepository.insert(entity)

        val typeStr = if (intent.type == TransactionType.EXPENSE) "支出" else "收入"
        return ExecutionResult.Success(
            message = "已记录${typeStr}: ${intent.note ?: intent.categoryName ?: ""}，金额 ¥${String.format("%.2f", amount)}",
            data = mapOf(
                "type" to intent.type.name,
                "amount" to amount,
                "note" to (intent.note ?: "")
            )
        )
    }

    /**
     * 执行添加待办操作
     */
    private suspend fun executeTodo(intent: CommandIntent.Todo): ExecutionResult {
        // 将优先级字符串转换为Priority枚举
        val priorityEnum = when (intent.priority?.uppercase()) {
            "HIGH", "高" -> Priority.HIGH
            "MEDIUM", "中" -> Priority.MEDIUM
            "LOW", "低" -> Priority.LOW
            else -> Priority.NONE
        }

        // 验证四象限值
        val quadrantValue = when (intent.quadrant?.uppercase()) {
            "IMPORTANT_URGENT" -> "IMPORTANT_URGENT"
            "IMPORTANT_NOT_URGENT" -> "IMPORTANT_NOT_URGENT"
            "NOT_IMPORTANT_URGENT" -> "NOT_IMPORTANT_URGENT"
            "NOT_IMPORTANT_NOT_URGENT" -> "NOT_IMPORTANT_NOT_URGENT"
            else -> null
        }

        val entity = TodoEntity(
            id = 0,
            title = intent.title,
            description = intent.description ?: "",
            priority = priorityEnum,
            quadrant = quadrantValue,
            dueDate = intent.dueDate,
            dueTime = intent.dueTime
        )

        todoRepository.insert(entity)

        val dueDateStr = if (intent.dueDate != null) {
            val date = LocalDate.ofEpochDay(intent.dueDate.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("MM月dd日"))
            if (intent.dueTime != null) "$dateStr ${intent.dueTime}" else dateStr
        } else ""

        return ExecutionResult.Success(
            message = "已添加待办: ${intent.title}" + if (dueDateStr.isNotEmpty()) "，截止时间: $dueDateStr" else "",
            data = mapOf(
                "title" to intent.title,
                "dueDate" to dueDateStr
            )
        )
    }

    /**
     * 执行写日记操作
     */
    private suspend fun executeDiary(intent: CommandIntent.Diary): ExecutionResult {
        val date = LocalDate.now()

        val entity = DiaryEntity(
            id = 0,
            date = date.toEpochDay().toInt(),
            content = intent.content,
            moodScore = intent.mood
        )

        diaryRepository.insert(entity)

        return ExecutionResult.Success(
            message = "已记录日记",
            data = mapOf("content" to intent.content)
        )
    }

    /**
     * 执行习惯打卡
     */
    private suspend fun executeHabitCheckin(intent: CommandIntent.HabitCheckin): ExecutionResult {
        // TODO: 实现习惯打卡逻辑
        return ExecutionResult.NeedMoreInfo(
            intent = intent,
            missingFields = listOf("habitId"),
            prompt = "请确认要打卡的习惯名称"
        )
    }

    /**
     * 执行时间追踪
     */
    private suspend fun executeTimeTrack(intent: CommandIntent.TimeTrack): ExecutionResult {
        val actionStr = when (intent.action) {
            TimeTrackAction.START -> "开始"
            TimeTrackAction.STOP -> "停止"
            TimeTrackAction.PAUSE -> "暂停"
            TimeTrackAction.RESUME -> "继续"
        }

        val taskName = intent.note ?: intent.categoryName ?: "任务"
        return ExecutionResult.Success(
            message = "${actionStr}计时: $taskName",
            data = mapOf(
                "action" to intent.action.name,
                "note" to taskName
            )
        )
    }

    /**
     * 执行页面导航
     */
    private suspend fun executeNavigate(intent: CommandIntent.Navigate): ExecutionResult {
        return ExecutionResult.Success(
            message = "正在打开: ${intent.screen}",
            data = mapOf("screen" to intent.screen)
        )
    }

    /**
     * 执行查询
     */
    private suspend fun executeQuery(intent: CommandIntent.Query): ExecutionResult {
        val timePeriod = intent.params["timePeriod"] as? String

        return when (intent.type) {
            QueryType.TODAY_EXPENSE -> queryExpense("今天")
            QueryType.MONTH_EXPENSE -> queryExpense("本月")
            QueryType.MONTH_INCOME -> queryIncome("本月")
            QueryType.CATEGORY_EXPENSE -> {
                val category = intent.params["category"] as? String ?: "全部"
                queryExpense("本月 $category")
            }
            QueryType.HABIT_STREAK -> queryHabit(timePeriod)
            QueryType.GOAL_PROGRESS -> queryGoal(timePeriod)
            QueryType.SAVINGS_PROGRESS -> ExecutionResult.Success(
                message = "储蓄进度查询功能开发中",
                data = emptyMap<String, Any>()
            )
        }
    }

    /**
     * 查询支出
     */
    private suspend fun queryExpense(timePeriod: String?): ExecutionResult {
        val (startDate, endDate) = parseTimePeriodToEpochDay(timePeriod)
        val expense = transactionRepository.getTotalByTypeInRange(startDate, endDate, "EXPENSE")

        val periodStr = timePeriod ?: "本月"
        return ExecutionResult.Success(
            message = "${periodStr}支出 ¥${String.format("%.2f", expense)}",
            data = mapOf("expense" to expense)
        )
    }

    /**
     * 查询收入
     */
    private suspend fun queryIncome(timePeriod: String?): ExecutionResult {
        val (startDate, endDate) = parseTimePeriodToEpochDay(timePeriod)
        val income = transactionRepository.getTotalByTypeInRange(startDate, endDate, "INCOME")

        val periodStr = timePeriod ?: "本月"
        return ExecutionResult.Success(
            message = "${periodStr}收入 ¥${String.format("%.2f", income)}",
            data = mapOf("income" to income)
        )
    }

    /**
     * 查询目标
     */
    private suspend fun queryGoal(timePeriod: String?): ExecutionResult {
        return ExecutionResult.Success(
            message = "目标查询功能开发中",
            data = emptyMap<String, Any>()
        )
    }

    /**
     * 查询习惯
     */
    private suspend fun queryHabit(timePeriod: String?): ExecutionResult {
        return ExecutionResult.Success(
            message = "习惯查询功能开发中",
            data = emptyMap<String, Any>()
        )
    }

    /**
     * 执行目标操作
     */
    private suspend fun executeGoal(intent: CommandIntent.Goal): ExecutionResult {
        return when (intent.action) {
            GoalAction.CREATE -> ExecutionResult.NeedMoreInfo(
                intent = intent,
                missingFields = listOf("goalName", "targetAmount", "deadline"),
                prompt = "请提供目标详情"
            )
            GoalAction.UPDATE -> ExecutionResult.Success(
                message = "目标已更新",
                data = emptyMap<String, Any>()
            )
            GoalAction.CHECK -> ExecutionResult.Success(
                message = "目标查看功能开发中",
                data = emptyMap<String, Any>()
            )
            GoalAction.DEPOSIT -> {
                val amount = intent.progress
                if (amount != null) {
                    ExecutionResult.Success(
                        message = "已向目标存入 ¥${String.format("%.2f", amount)}",
                        data = mapOf("amount" to amount)
                    )
                } else {
                    ExecutionResult.NeedMoreInfo(
                        intent = intent,
                        missingFields = listOf("amount"),
                        prompt = "请提供存入金额"
                    )
                }
            }
        }
    }

    /**
     * 执行储蓄操作
     */
    private suspend fun executeSavings(intent: CommandIntent.Savings): ExecutionResult {
        return when (intent.action) {
            SavingsAction.DEPOSIT -> {
                val amount = intent.amount
                if (amount != null) {
                    ExecutionResult.Success(
                        message = "已存入 ¥${String.format("%.2f", amount)}",
                        data = mapOf("amount" to amount)
                    )
                } else {
                    ExecutionResult.NeedMoreInfo(
                        intent = intent,
                        missingFields = listOf("amount"),
                        prompt = "请提供存入金额"
                    )
                }
            }
            SavingsAction.WITHDRAW -> {
                val amount = intent.amount
                if (amount != null) {
                    ExecutionResult.Success(
                        message = "已取出 ¥${String.format("%.2f", amount)}",
                        data = mapOf("amount" to amount)
                    )
                } else {
                    ExecutionResult.NeedMoreInfo(
                        intent = intent,
                        missingFields = listOf("amount"),
                        prompt = "请提供取出金额"
                    )
                }
            }
            SavingsAction.CHECK -> ExecutionResult.Success(
                message = "储蓄查看功能开发中",
                data = emptyMap<String, Any>()
            )
        }
    }

    /**
     * 解析时间段到epochDay范围
     */
    private fun parseTimePeriodToEpochDay(period: String?): Pair<Int, Int> {
        val now = LocalDate.now()

        return when {
            period == null -> Pair(
                now.withDayOfMonth(1).toEpochDay().toInt(),
                now.toEpochDay().toInt()
            )
            period.contains("今天") -> Pair(
                now.toEpochDay().toInt(),
                now.toEpochDay().toInt()
            )
            period.contains("本周") || period.contains("这周") -> {
                val startOfWeek = now.with(java.time.DayOfWeek.MONDAY)
                Pair(
                    startOfWeek.toEpochDay().toInt(),
                    now.toEpochDay().toInt()
                )
            }
            period.contains("本月") || period.contains("这个月") -> Pair(
                now.withDayOfMonth(1).toEpochDay().toInt(),
                now.toEpochDay().toInt()
            )
            period.contains("上月") || period.contains("上个月") -> {
                val lastMonth = now.minusMonths(1)
                Pair(
                    lastMonth.withDayOfMonth(1).toEpochDay().toInt(),
                    lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).toEpochDay().toInt()
                )
            }
            period.contains("今年") || period.contains("本年") -> Pair(
                now.withDayOfYear(1).toEpochDay().toInt(),
                now.toEpochDay().toInt()
            )
            else -> Pair(
                now.withDayOfMonth(1).toEpochDay().toInt(),
                now.toEpochDay().toInt()
            )
        }
    }
}
