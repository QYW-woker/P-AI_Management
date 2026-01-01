package com.lifemanager.app.core.voice

import com.lifemanager.app.core.ai.model.*
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.core.database.entity.TodoEntity
import com.lifemanager.app.core.database.entity.DiaryEntity
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalType
import com.lifemanager.app.core.database.entity.GoalCategory
import com.lifemanager.app.core.database.entity.ProgressType
import com.lifemanager.app.core.database.entity.TransactionSource
import com.lifemanager.app.core.database.entity.Priority
import com.lifemanager.app.core.database.entity.HabitRecordEntity
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import com.lifemanager.app.domain.repository.TodoRepository
import com.lifemanager.app.domain.repository.DiaryRepository
import com.lifemanager.app.domain.repository.GoalRepository
import com.lifemanager.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
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
    private val diaryRepository: DiaryRepository,
    private val goalRepository: GoalRepository,
    private val habitRepository: HabitRepository
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
                is CommandIntent.Multiple -> executeMultiple(intent)
                is CommandIntent.Unknown -> ExecutionResult.NotRecognized(intent.originalText)
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "执行失败")
        }
    }

    /**
     * 执行多条记录
     */
    private suspend fun executeMultiple(intent: CommandIntent.Multiple): ExecutionResult {
        val results = mutableListOf<String>()
        var successCount = 0

        for (childIntent in intent.intents) {
            val result = execute(childIntent)
            when (result) {
                is ExecutionResult.Success -> {
                    successCount++
                    results.add(result.message)
                }
                else -> {}
            }
        }

        return if (successCount > 0) {
            ExecutionResult.MultipleAdded(
                count = successCount,
                summary = "成功执行 $successCount 条记录:\n${results.joinToString("\n")}"
            )
        } else {
            ExecutionResult.Failure("批量执行失败")
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
        val habitName = intent.habitName.trim()

        // 获取所有活跃习惯
        val activeHabits = habitRepository.getActiveHabits().first()

        if (activeHabits.isEmpty()) {
            return ExecutionResult.Failure("您还没有添加任何习惯，请先在习惯页面添加习惯")
        }

        // 查找匹配的习惯（支持模糊匹配）
        val matchedHabit = activeHabits.find { habit ->
            habit.name.equals(habitName, ignoreCase = true) ||
            habit.name.contains(habitName, ignoreCase = true) ||
            habitName.contains(habit.name, ignoreCase = true)
        }

        if (matchedHabit == null) {
            // 返回可用的习惯列表提示
            val habitList = activeHabits.take(5).joinToString("、") { it.name }
            return ExecutionResult.NeedMoreInfo(
                intent = intent,
                missingFields = listOf("habitName"),
                prompt = "未找到习惯「$habitName」，您的习惯有: $habitList"
            )
        }

        val today = LocalDate.now().toEpochDay().toInt()

        // 检查今日是否已打卡
        val existingRecord = habitRepository.getRecordByHabitAndDate(matchedHabit.id, today)

        if (existingRecord != null && existingRecord.isCompleted) {
            return ExecutionResult.Success(
                message = "「${matchedHabit.name}」今日已打卡",
                data = mapOf(
                    "habitId" to matchedHabit.id,
                    "habitName" to matchedHabit.name,
                    "alreadyChecked" to true
                )
            )
        }

        // 执行打卡
        val record = HabitRecordEntity(
            habitId = matchedHabit.id,
            date = today,
            isCompleted = true,
            value = intent.value,
            note = "语音打卡"
        )
        habitRepository.saveRecord(record)

        // 计算连续打卡天数
        val streak = calculateHabitStreak(matchedHabit.id, today)

        val valueMsg = if (intent.value != null) "，数值: ${intent.value}" else ""
        val streakMsg = if (streak > 1) "，已连续 $streak 天" else ""

        return ExecutionResult.Success(
            message = "「${matchedHabit.name}」打卡成功$valueMsg$streakMsg",
            data = mapOf(
                "habitId" to matchedHabit.id,
                "habitName" to matchedHabit.name,
                "streak" to streak,
                "value" to (intent.value ?: 0.0)
            )
        )
    }

    /**
     * 计算习惯连续打卡天数
     */
    private suspend fun calculateHabitStreak(habitId: Long, today: Int): Int {
        var streak = 0
        var checkDate = today

        while (true) {
            val isChecked = habitRepository.isCheckedIn(habitId, checkDate)
            if (isChecked) {
                streak++
                checkDate--
            } else {
                break
            }
        }
        return streak
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
        val today = LocalDate.now().toEpochDay().toInt()
        val activeHabits = habitRepository.getActiveHabits().first()

        if (activeHabits.isEmpty()) {
            return ExecutionResult.Success(
                message = "您还没有添加任何习惯",
                data = emptyMap<String, Any>()
            )
        }

        // 统计今日打卡情况
        var todayCheckedCount = 0
        val habitStatusList = mutableListOf<String>()

        for (habit in activeHabits) {
            val isChecked = habitRepository.isCheckedIn(habit.id, today)
            if (isChecked) {
                todayCheckedCount++
                val streak = calculateHabitStreak(habit.id, today)
                habitStatusList.add("${habit.name}(连续${streak}天)")
            }
        }

        val totalHabits = activeHabits.size
        val uncheckedCount = totalHabits - todayCheckedCount

        val message = if (todayCheckedCount == 0) {
            "今日${totalHabits}个习惯都还未打卡"
        } else if (uncheckedCount == 0) {
            "太棒了！今日${totalHabits}个习惯已全部打卡: ${habitStatusList.joinToString("、")}"
        } else {
            "今日已打卡${todayCheckedCount}/${totalHabits}个习惯: ${habitStatusList.joinToString("、")}"
        }

        return ExecutionResult.Success(
            message = message,
            data = mapOf(
                "totalHabits" to totalHabits,
                "checkedCount" to todayCheckedCount,
                "habits" to habitStatusList
            )
        )
    }

    /**
     * 执行目标操作
     */
    private suspend fun executeGoal(intent: CommandIntent.Goal): ExecutionResult {
        return when (intent.action) {
            GoalAction.CREATE -> {
                val goalName = intent.goalName
                if (goalName.isNullOrBlank()) {
                    return ExecutionResult.NeedMoreInfo(
                        intent = intent,
                        missingFields = listOf("goalName"),
                        prompt = "请提供目标名称"
                    )
                }

                // 解析目标类型和周期
                val (goalType, endDate) = parseGoalTypeAndEndDate(goalName)

                // 解析目标分类
                val category = parseGoalCategory(goalName)

                // 解析目标数值
                val (targetValue, unit) = parseGoalTarget(goalName)

                val now = LocalDate.now()
                val entity = GoalEntity(
                    id = 0,
                    title = goalName,
                    description = "",
                    goalType = goalType,
                    category = category,
                    startDate = now.toEpochDay().toInt(),
                    endDate = endDate,
                    progressType = if (targetValue != null) ProgressType.NUMERIC else ProgressType.PERCENTAGE,
                    targetValue = targetValue,
                    currentValue = 0.0,
                    unit = unit
                )

                goalRepository.insert(entity)

                ExecutionResult.Success(
                    message = "已创建目标: $goalName",
                    data = mapOf(
                        "goalName" to goalName,
                        "targetValue" to (targetValue ?: 0.0),
                        "unit" to unit
                    )
                )
            }
            GoalAction.UPDATE -> {
                val progress = intent.progress
                if (progress != null) {
                    ExecutionResult.Success(
                        message = "目标进度已更新为 ${progress.toInt()}%",
                        data = mapOf("progress" to progress)
                    )
                } else {
                    ExecutionResult.Success(
                        message = "目标已更新",
                        data = emptyMap<String, Any>()
                    )
                }
            }
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
     * 解析目标类型和结束日期
     */
    private fun parseGoalTypeAndEndDate(goalName: String): Pair<String, Int?> {
        val now = LocalDate.now()
        return when {
            goalName.contains("这个月") || goalName.contains("本月") -> {
                val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                GoalType.MONTHLY to endOfMonth.toEpochDay().toInt()
            }
            goalName.contains("这个季度") || goalName.contains("本季度") -> {
                val endOfQuarter = now.plusMonths((3 - now.monthValue % 3).toLong())
                    .withDayOfMonth(1).minusDays(1)
                GoalType.QUARTERLY to endOfQuarter.toEpochDay().toInt()
            }
            goalName.contains("今年") || goalName.contains("本年") -> {
                val endOfYear = now.withDayOfYear(now.lengthOfYear())
                GoalType.YEARLY to endOfYear.toEpochDay().toInt()
            }
            else -> GoalType.LONG_TERM to null
        }
    }

    /**
     * 解析目标分类
     */
    private fun parseGoalCategory(goalName: String): String {
        return when {
            goalName.contains("减肥") || goalName.contains("健身") ||
            goalName.contains("运动") || goalName.contains("体重") ||
            goalName.contains("锻炼") || goalName.contains("跑步") -> GoalCategory.HEALTH

            goalName.contains("存钱") || goalName.contains("赚") ||
            goalName.contains("收入") || goalName.contains("储蓄") ||
            goalName.contains("万元") || goalName.contains("元") -> GoalCategory.FINANCE

            goalName.contains("学习") || goalName.contains("读书") ||
            goalName.contains("看书") || goalName.contains("课程") ||
            goalName.contains("考试") || goalName.contains("证书") -> GoalCategory.LEARNING

            goalName.contains("工作") || goalName.contains("升职") ||
            goalName.contains("项目") || goalName.contains("业绩") -> GoalCategory.CAREER

            else -> GoalCategory.LIFESTYLE
        }
    }

    /**
     * 解析目标数值和单位
     */
    private fun parseGoalTarget(goalName: String): Pair<Double?, String> {
        // 匹配数字+单位的模式
        val patterns = listOf(
            Regex("(\\d+(?:\\.\\d+)?)(斤|公斤|kg|KG)") to { v: Double -> v to "斤" },
            Regex("(\\d+(?:\\.\\d+)?)(公里|km|KM)") to { v: Double -> v to "公里" },
            Regex("(\\d+(?:\\.\\d+)?)(万元|万)") to { v: Double -> v * 10000 to "元" },
            Regex("(\\d+(?:\\.\\d+)?)(元|块)") to { v: Double -> v to "元" },
            Regex("(\\d+(?:\\.\\d+)?)(本|篇|个|次|天)") to { v: Double -> v to goalName.let {
                when {
                    it.contains("书") || it.contains("本") -> "本"
                    it.contains("天") -> "天"
                    it.contains("次") -> "次"
                    else -> "个"
                }
            }}
        )

        for ((regex, transform) in patterns) {
            val match = regex.find(goalName)
            if (match != null) {
                val value = match.groupValues[1].toDoubleOrNull() ?: continue
                return transform(value)
            }
        }

        return null to ""
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
