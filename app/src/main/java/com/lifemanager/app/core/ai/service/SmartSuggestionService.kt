package com.lifemanager.app.core.ai.service

import com.google.gson.Gson
import com.lifemanager.app.core.ai.model.*
import com.lifemanager.app.core.ai.service.api.ChatMessage
import com.lifemanager.app.core.ai.service.api.ChatRequest
import com.lifemanager.app.core.ai.service.api.DeepSeekApi
import com.lifemanager.app.core.database.dao.*
import com.lifemanager.app.data.repository.AIConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能建议服务
 *
 * 分析用户数据，生成个性化建议和洞察
 */
@Singleton
class SmartSuggestionService @Inject constructor(
    private val api: DeepSeekApi,
    private val configRepository: AIConfigRepository,
    private val transactionDao: DailyTransactionDao,
    private val todoDao: TodoDao,
    private val habitDao: HabitDao,
    private val habitRecordDao: HabitRecordDao,
    private val goalDao: GoalDao,
    private val budgetDao: BudgetDao,
    private val customFieldDao: CustomFieldDao,
    private val gson: Gson
) {
    private val _suggestions = MutableStateFlow<List<AISuggestion>>(emptyList())
    val suggestions: Flow<List<AISuggestion>> = _suggestions.asStateFlow()

    private val _quickActions = MutableStateFlow<List<QuickAction>>(getDefaultQuickActions())
    val quickActions: Flow<List<QuickAction>> = _quickActions.asStateFlow()

    /**
     * 生成今日智能建议
     */
    suspend fun generateDailySuggestions(): List<AISuggestion> = withContext(Dispatchers.IO) {
        val suggestions = mutableListOf<AISuggestion>()
        val today = LocalDate.now()
        val currentHour = LocalTime.now().hour

        // 1. 检查预算情况
        suggestions.addAll(checkBudgetStatus())

        // 2. 检查待办提醒
        suggestions.addAll(checkTodoReminders(today))

        // 3. 检查习惯打卡
        suggestions.addAll(checkHabitReminders(today, currentHour))

        // 4. 检查目标进度
        suggestions.addAll(checkGoalProgress())

        // 5. 生成消费洞察
        suggestions.addAll(generateSpendingInsights())

        // 6. 根据时间段生成建议
        suggestions.addAll(getTimeBasedSuggestions(currentHour))

        // 排序并更新
        val sortedSuggestions = suggestions.sortedByDescending { it.priority }
        _suggestions.value = sortedSuggestions
        sortedSuggestions
    }

    /**
     * 检查预算状态
     */
    private suspend fun checkBudgetStatus(): List<AISuggestion> {
        val suggestions = mutableListOf<AISuggestion>()
        val today = LocalDate.now()
        val currentYearMonth = today.year * 100 + today.monthValue
        val budget = budgetDao.getByYearMonth(currentYearMonth) ?: return suggestions

        val monthStart = YearMonth.now().atDay(1).toEpochDay().toInt()
        val todayEpoch = today.toEpochDay().toInt()

        val transactions = transactionDao.getTransactionsBetweenDatesSync(monthStart, todayEpoch)
            .filter { it.type == "EXPENSE" }

        val spent = transactions.sumOf { it.amount }
        val usageRate = if (budget.totalBudget > 0) (spent / budget.totalBudget * 100) else 0.0
        val daysInMonth = today.lengthOfMonth()
        val daysPassed = today.dayOfMonth
        val expectedRate = (daysPassed.toDouble() / daysInMonth) * 100

        when {
            usageRate >= 100 -> {
                suggestions.add(
                    AISuggestion(
                        id = "budget_exceeded_${budget.id}",
                        type = SuggestionType.BUDGET_WARNING,
                        title = "预算已超支",
                        description = "本月预算已使用${String.format("%.0f", usageRate)}%，超出预算¥${String.format("%.2f", spent - budget.totalBudget)}",
                        priority = 95,
                        action = SuggestionAction.ViewReport("budget")
                    )
                )
            }
            usageRate >= 80 -> {
                suggestions.add(
                    AISuggestion(
                        id = "budget_warning_${budget.id}",
                        type = SuggestionType.BUDGET_WARNING,
                        title = "预算即将用尽",
                        description = "本月预算已使用${String.format("%.0f", usageRate)}%，剩余¥${String.format("%.2f", budget.totalBudget - spent)}",
                        priority = 80,
                        action = SuggestionAction.ViewReport("budget")
                    )
                )
            }
            usageRate > expectedRate + 15 -> {
                suggestions.add(
                    AISuggestion(
                        id = "budget_ahead_${budget.id}",
                        type = SuggestionType.SPENDING_ALERT,
                        title = "消费进度超前",
                        description = "本月消费进度比预期快${String.format("%.0f", usageRate - expectedRate)}%，建议适当控制",
                        priority = 60
                    )
                )
            }
        }
        return suggestions
    }

    /**
     * 检查待办提醒
     */
    private suspend fun checkTodoReminders(today: LocalDate): List<AISuggestion> {
        val suggestions = mutableListOf<AISuggestion>()
        val todayEpoch = today.toEpochDay().toInt()
        val tomorrowEpoch = today.plusDays(1).toEpochDay().toInt()

        // 今日待办
        val todayTodos = todoDao.getByDateSync(todayEpoch).filter { it.status == "PENDING" }
        if (todayTodos.isNotEmpty()) {
            val importantCount = todayTodos.count { it.quadrant?.contains("IMPORTANT") == true }
            suggestions.add(
                AISuggestion(
                    id = "today_todos",
                    type = SuggestionType.TIME_SENSITIVE,
                    title = "今日待办",
                    description = "您有${todayTodos.size}项待办事项" +
                            if (importantCount > 0) "，其中${importantCount}项重要" else "",
                    priority = 85,
                    action = SuggestionAction.Navigate("todo")
                )
            )
        }

        // 过期未完成
        val overdueTodos = todoDao.getOverdueSync(todayEpoch).filter { it.status == "PENDING" }
        if (overdueTodos.isNotEmpty()) {
            suggestions.add(
                AISuggestion(
                    id = "overdue_todos",
                    type = SuggestionType.TIME_SENSITIVE,
                    title = "逾期待办",
                    description = "有${overdueTodos.size}项待办已过期，点击处理",
                    priority = 90,
                    action = SuggestionAction.Navigate("todo")
                )
            )
        }

        // 明日重要事项预告
        val tomorrowTodos = todoDao.getByDateSync(tomorrowEpoch)
            .filter { it.quadrant?.contains("IMPORTANT") == true }
        if (tomorrowTodos.isNotEmpty()) {
            suggestions.add(
                AISuggestion(
                    id = "tomorrow_preview",
                    type = SuggestionType.QUICK_ACTION,
                    title = "明日预告",
                    description = "明天有${tomorrowTodos.size}项重要事项：${tomorrowTodos.first().title}${if (tomorrowTodos.size > 1) "等" else ""}",
                    priority = 50
                )
            )
        }

        return suggestions
    }

    /**
     * 检查习惯打卡提醒
     */
    private suspend fun checkHabitReminders(today: LocalDate, currentHour: Int): List<AISuggestion> {
        val suggestions = mutableListOf<AISuggestion>()
        val todayEpoch = today.toEpochDay().toInt()
        val habits = habitDao.getEnabledSync()

        // 获取今日已打卡的习惯ID
        val todayRecords = habitRecordDao.getByDateSync(todayEpoch)
        val checkedHabitIds = todayRecords.map { it.habitId }.toSet()

        // 找出未打卡的习惯
        val uncheckedHabits = habits.filter { it.id !in checkedHabitIds }

        if (uncheckedHabits.isNotEmpty()) {
            // 晚上提醒未打卡的习惯
            if (currentHour >= 20) {
                suggestions.add(
                    AISuggestion(
                        id = "habit_reminder_evening",
                        type = SuggestionType.HABIT_REMINDER,
                        title = "习惯打卡提醒",
                        description = "今日还有${uncheckedHabits.size}个习惯未打卡：${uncheckedHabits.take(2).joinToString("、") { it.name }}",
                        priority = 75,
                        action = SuggestionAction.Navigate("habit")
                    )
                )
            }
            // 早上提示打卡目标
            else if (currentHour in 6..9) {
                suggestions.add(
                    AISuggestion(
                        id = "habit_morning",
                        type = SuggestionType.HABIT_REMINDER,
                        title = "早安打卡",
                        description = "新的一天，记得完成${habits.size}个习惯哦",
                        priority = 40,
                        action = SuggestionAction.Navigate("habit")
                    )
                )
            }
        }

        // 检查连续打卡成就
        for (habit in habits) {
            val streak = calculateStreak(habit.id, todayEpoch)
            if (streak > 0 && streak % 7 == 0) {
                suggestions.add(
                    AISuggestion(
                        id = "habit_streak_${habit.id}",
                        type = SuggestionType.GOAL_MILESTONE,
                        title = "坚持成就",
                        description = "「${habit.name}」已连续打卡${streak}天，继续保持！",
                        priority = 55
                    )
                )
            }
        }

        return suggestions
    }

    private suspend fun calculateStreak(habitId: Long, today: Int): Int {
        var streak = 0
        var checkDate = today
        while (habitRecordDao.getByHabitAndDateSync(habitId, checkDate) != null) {
            streak++
            checkDate--
        }
        return streak
    }

    /**
     * 检查目标进度
     */
    private suspend fun checkGoalProgress(): List<AISuggestion> {
        val suggestions = mutableListOf<AISuggestion>()
        val goals = goalDao.getActiveGoalsSync()
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay().toInt()

        for (goal in goals) {
            val progress = if ((goal.targetValue ?: 0.0) > 0) {
                (goal.currentValue / goal.targetValue!! * 100).toInt()
            } else 0

            // 即将到期的目标
            goal.endDate?.let { endDate ->
                val daysLeft = endDate - todayEpoch
                when {
                    daysLeft <= 0 && progress < 100 -> {
                        suggestions.add(
                            AISuggestion(
                                id = "goal_expired_${goal.id}",
                                type = SuggestionType.TIME_SENSITIVE,
                                title = "目标已到期",
                                description = "「${goal.title}」已到期，完成度${progress}%",
                                priority = 85,
                                action = SuggestionAction.GoalUpdate(goal.id)
                            )
                        )
                    }
                    daysLeft in 1..7 && progress < 80 -> {
                        suggestions.add(
                            AISuggestion(
                                id = "goal_deadline_${goal.id}",
                                type = SuggestionType.TIME_SENSITIVE,
                                title = "目标即将到期",
                                description = "「${goal.title}」还剩${daysLeft}天，当前进度${progress}%",
                                priority = 70,
                                action = SuggestionAction.GoalUpdate(goal.id)
                            )
                        )
                    }
                    else -> { /* No action needed for other cases */ }
                }
            }

            // 里程碑达成
            if (progress >= 50 && progress < 60) {
                suggestions.add(
                    AISuggestion(
                        id = "goal_milestone_${goal.id}_50",
                        type = SuggestionType.GOAL_MILESTONE,
                        title = "目标过半",
                        description = "「${goal.title}」已完成50%，加油！",
                        priority = 45
                    )
                )
            } else if (progress >= 90 && progress < 100) {
                suggestions.add(
                    AISuggestion(
                        id = "goal_milestone_${goal.id}_90",
                        type = SuggestionType.GOAL_MILESTONE,
                        title = "目标即将达成",
                        description = "「${goal.title}」已完成${progress}%，冲刺！",
                        priority = 60
                    )
                )
            }
        }

        return suggestions
    }

    /**
     * 生成消费洞察
     */
    private suspend fun generateSpendingInsights(): List<AISuggestion> {
        val suggestions = mutableListOf<AISuggestion>()
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay().toInt()

        // 获取近7天消费
        val weekStart = today.minusDays(6).toEpochDay().toInt()
        val weekTransactions = transactionDao.getTransactionsBetweenDatesSync(weekStart, todayEpoch)
            .filter { it.type == "EXPENSE" }

        if (weekTransactions.isNotEmpty()) {
            val totalWeek = weekTransactions.sumOf { it.amount }
            val avgDaily = totalWeek / 7

            // 找出消费最高的分类
            val topCategory = weekTransactions
                .groupBy { it.categoryId }
                .maxByOrNull { it.value.sumOf { t -> t.amount } }

            if (topCategory != null) {
                val categoryAmount = topCategory.value.sumOf { it.amount }
                val categoryName = topCategory.key?.let {
                    customFieldDao.getFieldById(it)?.name
                } ?: "未分类"

                if (categoryAmount > totalWeek * 0.4) {
                    suggestions.add(
                        AISuggestion(
                            id = "spending_concentration",
                            type = SuggestionType.SPENDING_ALERT,
                            title = "消费集中提示",
                            description = "本周「$categoryName」消费占比${String.format("%.0f", categoryAmount / totalWeek * 100)}%",
                            priority = 35,
                            action = SuggestionAction.ViewReport("category")
                        )
                    )
                }
            }

            // 对比上周
            val lastWeekStart = today.minusDays(13).toEpochDay().toInt()
            val lastWeekEnd = today.minusDays(7).toEpochDay().toInt()
            val lastWeekTransactions = transactionDao.getTransactionsBetweenDatesSync(lastWeekStart, lastWeekEnd)
                .filter { it.type == "EXPENSE" }

            if (lastWeekTransactions.isNotEmpty()) {
                val lastWeekTotal = lastWeekTransactions.sumOf { it.amount }
                val changeRate = ((totalWeek - lastWeekTotal) / lastWeekTotal * 100).toInt()

                if (changeRate > 30) {
                    suggestions.add(
                        AISuggestion(
                            id = "spending_increase",
                            type = SuggestionType.SPENDING_ALERT,
                            title = "消费上涨",
                            description = "本周消费比上周增加${changeRate}%",
                            priority = 50,
                            action = SuggestionAction.ViewReport("trend")
                        )
                    )
                } else if (changeRate < -20) {
                    suggestions.add(
                        AISuggestion(
                            id = "spending_decrease",
                            type = SuggestionType.PERSONALIZED,
                            title = "节省有道",
                            description = "本周消费比上周减少${-changeRate}%，继续保持！",
                            priority = 30
                        )
                    )
                }
            }
        }

        return suggestions
    }

    /**
     * 根据时间段生成建议
     */
    private fun getTimeBasedSuggestions(currentHour: Int): List<AISuggestion> {
        val suggestions = mutableListOf<AISuggestion>()

        when (currentHour) {
            in 6..8 -> {
                suggestions.add(
                    AISuggestion(
                        id = "morning_greeting",
                        type = SuggestionType.PERSONALIZED,
                        title = "早安",
                        description = "新的一天，记录今天的第一笔开支吧",
                        priority = 20,
                        action = SuggestionAction.CreateTransaction("expense")
                    )
                )
            }
            in 12..13 -> {
                suggestions.add(
                    AISuggestion(
                        id = "lunch_record",
                        type = SuggestionType.QUICK_ACTION,
                        title = "记录午餐",
                        description = "点击快速记录午餐消费",
                        priority = 25,
                        action = SuggestionAction.CreateTransaction("expense", "餐饮")
                    )
                )
            }
            in 21..23 -> {
                suggestions.add(
                    AISuggestion(
                        id = "evening_review",
                        type = SuggestionType.WEEKLY_INSIGHT,
                        title = "今日回顾",
                        description = "看看今天的收支情况",
                        priority = 30,
                        action = SuggestionAction.ViewReport("daily")
                    )
                )
            }
        }

        // 周末特别建议
        val dayOfWeek = LocalDate.now().dayOfWeek.value
        if (dayOfWeek >= 6) {
            suggestions.add(
                AISuggestion(
                    id = "weekend_review",
                    type = SuggestionType.WEEKLY_INSIGHT,
                    title = "周末小结",
                    description = "周末是回顾本周财务状况的好时机",
                    priority = 25,
                    action = SuggestionAction.ViewReport("weekly")
                )
            )
        }

        return suggestions
    }

    /**
     * 获取默认快捷操作
     */
    private fun getDefaultQuickActions(): List<QuickAction> {
        return listOf(
            QuickAction(
                id = "quick_expense",
                icon = "ShoppingCart",
                title = "记支出",
                subtitle = "快速记录消费",
                command = "记一笔支出",
                category = QuickActionCategory.FINANCE
            ),
            QuickAction(
                id = "quick_income",
                icon = "AccountBalance",
                title = "记收入",
                subtitle = "快速记录收入",
                command = "记一笔收入",
                category = QuickActionCategory.FINANCE
            ),
            QuickAction(
                id = "quick_todo",
                icon = "CheckCircle",
                title = "添加待办",
                subtitle = "创建新待办事项",
                command = "添加一个待办",
                category = QuickActionCategory.TODO
            ),
            QuickAction(
                id = "quick_habit",
                icon = "Favorite",
                title = "习惯打卡",
                subtitle = "快速完成打卡",
                command = "打开习惯",
                category = QuickActionCategory.HABIT
            ),
            QuickAction(
                id = "query_today",
                icon = "Today",
                title = "今日支出",
                subtitle = "查看今天花了多少",
                command = "今天花了多少钱",
                category = QuickActionCategory.QUERY
            ),
            QuickAction(
                id = "query_month",
                icon = "CalendarMonth",
                title = "本月支出",
                subtitle = "查看月度消费",
                command = "这个月花了多少钱",
                category = QuickActionCategory.QUERY
            ),
            QuickAction(
                id = "query_budget",
                icon = "Savings",
                title = "预算情况",
                subtitle = "查看预算使用",
                command = "预算还剩多少",
                category = QuickActionCategory.QUERY
            ),
            QuickAction(
                id = "open_stats",
                icon = "BarChart",
                title = "统计报表",
                subtitle = "查看详细报表",
                command = "打开统计",
                category = QuickActionCategory.NAVIGATION
            )
        )
    }

    /**
     * 使用AI生成个性化建议
     */
    suspend fun generateAIPersonalizedSuggestions(): Result<List<AISuggestion>> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("AI未配置"))
            }

            val today = LocalDate.now()
            val todayEpoch = today.toEpochDay().toInt()
            val monthStart = YearMonth.now().atDay(1).toEpochDay().toInt()

            // 收集用户数据摘要
            val transactions = transactionDao.getTransactionsBetweenDatesSync(monthStart, todayEpoch)
            val goals = goalDao.getActiveGoalsSync()
            val habits = habitDao.getEnabledSync()
            val budgets = budgetDao.getAllSync()

            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val dataSummary = """
用户数据摘要：
- 本月收入：¥${String.format("%.2f", income)}
- 本月支出：¥${String.format("%.2f", expense)}
- 活跃目标数：${goals.size}个
- 跟踪习惯数：${habits.size}个
- 设置预算数：${budgets.size}个
- 今日日期：$today（星期${today.dayOfWeek.value}）
- 当前时间：${LocalTime.now().hour}点
""".trimIndent()

            val prompt = """
作为智能生活助手，请根据用户数据生成3-5条个性化建议。

$dataSummary

请按以下JSON格式返回建议列表：
[
  {
    "type": "SPENDING_ALERT/HABIT_REMINDER/GOAL_MILESTONE/SAVINGS_TIP/HEALTH_TIP/PERSONALIZED",
    "title": "简短标题(10字内)",
    "description": "具体建议内容(30字内)",
    "priority": 优先级0-100
  }
]

要求：
1. 建议要具体、可行
2. 根据时间和数据特点给出针对性建议
3. 语气亲切友好
4. 只返回JSON数组，不要其他文字
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(ChatMessage("user", prompt)),
                temperature = 0.5,
                maxTokens = 500
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
                ?: return@withContext Result.failure(Exception("AI响应为空"))

            val suggestions = parseAISuggestions(content)
            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAISuggestions(json: String): List<AISuggestion> {
        return try {
            val jsonStr = extractJsonArray(json)
            val list = gson.fromJson(jsonStr, List::class.java) as? List<Map<String, Any>>
                ?: return emptyList()

            list.mapIndexed { index, item ->
                val typeStr = item["type"] as? String ?: "PERSONALIZED"
                AISuggestion(
                    id = "ai_suggestion_${UUID.randomUUID()}",
                    type = try {
                        SuggestionType.valueOf(typeStr)
                    } catch (e: Exception) {
                        SuggestionType.PERSONALIZED
                    },
                    title = item["title"] as? String ?: "建议",
                    description = item["description"] as? String ?: "",
                    priority = (item["priority"] as? Number)?.toInt() ?: (50 - index * 5)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractJsonArray(text: String): String {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else {
            "[]"
        }
    }

    /**
     * 忽略建议
     */
    fun dismissSuggestion(suggestionId: String) {
        _suggestions.value = _suggestions.value.filter { it.id != suggestionId }
    }
}
