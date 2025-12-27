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
        val date = parseDate(intent.date) ?: LocalDate.now()
        val now = LocalTime.now()

        val entity = DailyTransactionEntity(
            id = 0,
            type = if (intent.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME",
            amount = intent.amount,
            categoryId = intent.categoryId,
            date = date.toEpochDay().toInt(),
            time = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            note = intent.description ?: "",
            source = TransactionSource.VOICE
        )

        transactionRepository.insert(entity)

        val typeStr = if (intent.type == TransactionType.EXPENSE) "支出" else "收入"
        return ExecutionResult.Success(
            message = "已记录${typeStr}: ${intent.description ?: ""}，金额 ¥${String.format("%.2f", intent.amount)}",
            data = mapOf(
                "type" to intent.type.name,
                "amount" to intent.amount,
                "description" to (intent.description ?: "")
            )
        )
    }

    /**
     * 执行添加待办操作
     */
    private suspend fun executeTodo(intent: CommandIntent.Todo): ExecutionResult {
        val dueDateTime = intent.dueDate?.let { parseDateTime(it) }
        val dueDate = dueDateTime?.first
        val dueTime = dueDateTime?.second

        // 将优先级数字转换为字符串
        val priorityStr = when (intent.priority) {
            3 -> Priority.HIGH
            2 -> Priority.MEDIUM
            1 -> Priority.LOW
            else -> Priority.NONE
        }

        val entity = TodoEntity(
            id = 0,
            title = intent.title,
            description = intent.description ?: "",
            priority = priorityStr,
            dueDate = dueDate,
            dueTime = dueTime
        )

        todoRepository.insert(entity)

        val dueDateStr = if (dueDate != null) {
            val date = LocalDate.ofEpochDay(dueDate.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("MM月dd日"))
            if (dueTime != null) "$dateStr $dueTime" else dateStr
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
        val date = parseDate(intent.date) ?: LocalDate.now()

        // 解析心情为评分
        val moodScore = intent.mood?.let { parseMoodScore(it) }

        val entity = DiaryEntity(
            id = 0,
            date = date.toEpochDay().toInt(),
            content = intent.content,
            moodScore = moodScore
        )

        diaryRepository.insert(entity)

        return ExecutionResult.Success(
            message = "已记录日记",
            data = mapOf("content" to intent.content)
        )
    }

    /**
     * 解析心情文字为评分
     */
    private fun parseMoodScore(mood: String): Int {
        return when {
            mood.contains("非常开心") || mood.contains("很开心") || mood.contains("超级") -> 5
            mood.contains("开心") || mood.contains("高兴") || mood.contains("快乐") -> 4
            mood.contains("一般") || mood.contains("普通") -> 3
            mood.contains("不开心") || mood.contains("难过") || mood.contains("伤心") -> 2
            mood.contains("很难过") || mood.contains("崩溃") || mood.contains("糟糕") -> 1
            else -> 3
        }
    }

    /**
     * 执行习惯打卡
     */
    private suspend fun executeHabitCheckin(intent: CommandIntent.HabitCheckin): ExecutionResult {
        // TODO: 实现习惯打卡逻辑
        return ExecutionResult.NeedMoreInfo(
            question = "请确认要打卡的习惯名称",
            suggestions = listOf()
        )
    }

    /**
     * 执行时间追踪
     */
    private suspend fun executeTimeTrack(intent: CommandIntent.TimeTrack): ExecutionResult {
        // TODO: 实现时间追踪逻辑
        val actionStr = when (intent.action) {
            TimeTrackAction.START -> "开始"
            TimeTrackAction.STOP -> "停止"
            TimeTrackAction.PAUSE -> "暂停"
            TimeTrackAction.RESUME -> "继续"
        }

        return ExecutionResult.Success(
            message = "${actionStr}计时: ${intent.taskName}",
            data = mapOf(
                "action" to intent.action.name,
                "taskName" to intent.taskName
            )
        )
    }

    /**
     * 执行页面导航
     */
    private suspend fun executeNavigate(intent: CommandIntent.Navigate): ExecutionResult {
        return ExecutionResult.Success(
            message = "正在打开: ${intent.destination}",
            data = mapOf("destination" to intent.destination)
        )
    }

    /**
     * 执行查询
     */
    private suspend fun executeQuery(intent: CommandIntent.Query): ExecutionResult {
        return when (intent.queryType) {
            QueryType.BALANCE -> queryBalance(intent.timePeriod)
            QueryType.EXPENSE -> queryExpense(intent.timePeriod)
            QueryType.INCOME -> queryIncome(intent.timePeriod)
            QueryType.TODO -> queryTodo(intent.timePeriod)
            QueryType.GOAL -> queryGoal(intent.timePeriod)
            QueryType.HABIT -> queryHabit(intent.timePeriod)
            QueryType.GENERAL -> ExecutionResult.NeedMoreInfo(
                question = "请问您想查询什么？",
                suggestions = listOf("余额", "本月支出", "本月收入", "待办事项")
            )
        }
    }

    /**
     * 查询余额
     */
    private suspend fun queryBalance(timePeriod: String?): ExecutionResult {
        val (startDate, endDate) = parseTimePeriodToEpochDay(timePeriod)

        val income = transactionRepository.getTotalByTypeInRange(startDate, endDate, "INCOME")
        val expense = transactionRepository.getTotalByTypeInRange(startDate, endDate, "EXPENSE")
        val balance = income - expense

        val periodStr = timePeriod ?: "本月"
        return ExecutionResult.Success(
            message = "${periodStr}收入 ¥${String.format("%.2f", income)}，支出 ¥${String.format("%.2f", expense)}，结余 ¥${String.format("%.2f", balance)}",
            data = mapOf(
                "income" to income,
                "expense" to expense,
                "balance" to balance
            )
        )
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
     * 查询待办
     */
    private suspend fun queryTodo(timePeriod: String?): ExecutionResult {
        val count = todoRepository.countPending()

        return ExecutionResult.Success(
            message = "您有 $count 个未完成的待办事项",
            data = mapOf("count" to count)
        )
    }

    /**
     * 查询目标
     */
    private suspend fun queryGoal(timePeriod: String?): ExecutionResult {
        return ExecutionResult.Success(
            message = "目标查询功能开发中",
            data = emptyMap()
        )
    }

    /**
     * 查询习惯
     */
    private suspend fun queryHabit(timePeriod: String?): ExecutionResult {
        return ExecutionResult.Success(
            message = "习惯查询功能开发中",
            data = emptyMap()
        )
    }

    /**
     * 执行目标操作
     */
    private suspend fun executeGoal(intent: CommandIntent.Goal): ExecutionResult {
        return when (intent.action) {
            GoalAction.CREATE -> ExecutionResult.NeedMoreInfo(
                question = "请提供目标详情",
                suggestions = listOf("目标名称", "目标金额", "目标日期")
            )
            GoalAction.UPDATE -> ExecutionResult.Success(
                message = "目标已更新",
                data = emptyMap()
            )
            GoalAction.CHECK -> ExecutionResult.Success(
                message = "目标查看功能开发中",
                data = emptyMap()
            )
            GoalAction.DEPOSIT -> {
                val amount = intent.amount
                if (amount != null) {
                    ExecutionResult.Success(
                        message = "已向目标存入 ¥${String.format("%.2f", amount)}",
                        data = mapOf("amount" to amount)
                    )
                } else {
                    ExecutionResult.NeedMoreInfo(
                        question = "请提供存入金额",
                        suggestions = listOf("100", "500", "1000")
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
                        question = "请提供存入金额",
                        suggestions = listOf()
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
                        question = "请提供取出金额",
                        suggestions = listOf()
                    )
                }
            }
            SavingsAction.CHECK -> ExecutionResult.Success(
                message = "储蓄查看功能开发中",
                data = emptyMap()
            )
        }
    }

    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null

        return try {
            val today = LocalDate.now()
            when {
                dateStr.contains("今天") -> today
                dateStr.contains("昨天") -> today.minusDays(1)
                dateStr.contains("前天") -> today.minusDays(2)
                dateStr.contains("明天") -> today.plusDays(1)
                dateStr.contains("后天") -> today.plusDays(2)
                else -> {
                    // 尝试解析具体日期
                    val formats = listOf(
                        "yyyy-MM-dd",
                        "yyyy年MM月dd日",
                        "MM月dd日",
                        "MM-dd"
                    )
                    for (format in formats) {
                        try {
                            return if (format.contains("yyyy")) {
                                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format))
                            } else {
                                LocalDate.parse("${today.year}年$dateStr",
                                    DateTimeFormatter.ofPattern("yyyy年$format"))
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析日期时间字符串
     * @return Pair<epochDay, timeString>
     */
    private fun parseDateTime(dateTimeStr: String?): Pair<Int, String?>? {
        if (dateTimeStr.isNullOrBlank()) return null

        return try {
            val today = LocalDate.now()

            // 解析时间部分
            val timePattern = Regex("""(\d{1,2})[点时:](\d{1,2})?[分]?""")
            val timeMatch = timePattern.find(dateTimeStr)

            val timeStr = if (timeMatch != null) {
                val h = timeMatch.groupValues[1].toInt()
                val m = timeMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
                String.format("%02d:%02d", h, m)
            } else {
                null
            }

            // 解析日期部分
            val date = when {
                dateTimeStr.contains("今天") -> today
                dateTimeStr.contains("明天") -> today.plusDays(1)
                dateTimeStr.contains("后天") -> today.plusDays(2)
                dateTimeStr.contains("下周") -> {
                    val dayOfWeek = when {
                        dateTimeStr.contains("一") -> 1
                        dateTimeStr.contains("二") -> 2
                        dateTimeStr.contains("三") -> 3
                        dateTimeStr.contains("四") -> 4
                        dateTimeStr.contains("五") -> 5
                        dateTimeStr.contains("六") -> 6
                        dateTimeStr.contains("日") || dateTimeStr.contains("天") -> 7
                        else -> 1
                    }
                    today.plusWeeks(1).with(java.time.DayOfWeek.of(dayOfWeek))
                }
                dateTimeStr.contains("周") -> {
                    val dayOfWeek = when {
                        dateTimeStr.contains("一") -> 1
                        dateTimeStr.contains("二") -> 2
                        dateTimeStr.contains("三") -> 3
                        dateTimeStr.contains("四") -> 4
                        dateTimeStr.contains("五") -> 5
                        dateTimeStr.contains("六") -> 6
                        dateTimeStr.contains("日") || dateTimeStr.contains("天") -> 7
                        else -> today.dayOfWeek.value
                    }
                    val target = today.with(java.time.DayOfWeek.of(dayOfWeek))
                    if (target.isBefore(today)) target.plusWeeks(1) else target
                }
                else -> today
            }

            Pair(date.toEpochDay().toInt(), timeStr)
        } catch (e: Exception) {
            null
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
