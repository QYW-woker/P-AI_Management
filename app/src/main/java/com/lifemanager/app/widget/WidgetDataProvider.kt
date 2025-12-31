package com.lifemanager.app.widget

import android.content.Context
import com.lifemanager.app.core.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Widget数据提供者
 *
 * 为各类Widget提供数据
 */
@Singleton
class WidgetDataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    // ==================== 财务Widget数据 ====================

    /**
     * 获取今日支出统计
     */
    suspend fun getTodayExpense(): FinanceWidgetData {
        val today = LocalDate.now().toEpochDay().toInt()
        val transactions = database.dailyTransactionDao().getByDate(today).first()

        val expense = transactions
            .filter { it.type == "EXPENSE" }
            .sumOf { it.amount }
        val income = transactions
            .filter { it.type == "INCOME" }
            .sumOf { it.amount }

        return FinanceWidgetData(
            expense = expense,
            income = income,
            transactionCount = transactions.size,
            date = today
        )
    }

    /**
     * 获取月度支出统计
     */
    suspend fun getMonthlyExpense(): FinanceWidgetData {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1).toEpochDay().toInt()
        val monthEnd = today.toEpochDay().toInt()

        val transactions = database.dailyTransactionDao()
            .getByDateRange(monthStart, monthEnd).first()

        val expense = transactions
            .filter { it.type == "EXPENSE" }
            .sumOf { it.amount }
        val income = transactions
            .filter { it.type == "INCOME" }
            .sumOf { it.amount }

        return FinanceWidgetData(
            expense = expense,
            income = income,
            transactionCount = transactions.size,
            date = monthStart
        )
    }

    /**
     * 获取预算进度
     */
    suspend fun getBudgetProgress(): BudgetWidgetData {
        val today = LocalDate.now()
        val yearMonth = today.year * 100 + today.monthValue

        val budget = database.budgetDao().getByYearMonth(yearMonth)

        if (budget == null) {
            return BudgetWidgetData(
                totalBudget = 0.0,
                totalSpent = 0.0,
                percentage = 0,
                remainingDays = today.lengthOfMonth() - today.dayOfMonth + 1
            )
        }

        // 计算本月已花费
        val monthStart = today.withDayOfMonth(1).toEpochDay().toInt()
        val monthEnd = today.toEpochDay().toInt()
        val transactions = database.dailyTransactionDao()
            .getByDateRange(monthStart, monthEnd).first()
        val totalSpent = transactions
            .filter { it.type == "EXPENSE" }
            .sumOf { it.amount }

        val totalBudget = budget.totalBudget
        val percentage = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

        return BudgetWidgetData(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            percentage = percentage,
            remainingDays = today.lengthOfMonth() - today.dayOfMonth + 1
        )
    }

    // ==================== 待办Widget数据 ====================

    /**
     * 获取今日待办
     */
    suspend fun getTodayTodos(): TodoWidgetData {
        val today = LocalDate.now().toEpochDay().toInt()
        val todos = database.todoDao().getByDateSync(today)

        val total = todos.size
        val completed = todos.count { it.status == "COMPLETED" }
        val pending = todos.filter { it.status != "COMPLETED" }
            .sortedBy { it.priority }
            .take(5)

        return TodoWidgetData(
            totalCount = total,
            completedCount = completed,
            pendingItems = pending.map {
                TodoWidgetItem(
                    id = it.id,
                    title = it.title,
                    priority = it.priority,
                    dueTime = it.dueTime
                )
            },
            date = today
        )
    }

    /**
     * 获取待办统计
     */
    suspend fun getTodoStats(): TodoStatsWidgetData {
        val today = LocalDate.now().toEpochDay().toInt()
        val todayTodos = database.todoDao().getByDateSync(today)
        val overdueTodos = database.todoDao().getOverdueCountSync(today)

        return TodoStatsWidgetData(
            todayTotal = todayTodos.size,
            todayCompleted = todayTodos.count { it.status == "COMPLETED" },
            overdueCount = overdueTodos
        )
    }

    // ==================== 习惯Widget数据 ====================

    /**
     * 获取今日习惯打卡状态
     */
    suspend fun getTodayHabits(): HabitWidgetData {
        val today = LocalDate.now().toEpochDay().toInt()
        val habits = database.habitDao().getActiveHabitsSync()
        val records = database.habitRecordDao().getByDateSync(today)

        val checkedIds = records.map { it.habitId }.toSet()
        val habitItems = habits.take(6).map { habit ->
            HabitWidgetItem(
                id = habit.id,
                name = habit.name,
                icon = habit.iconName,
                color = habit.color,
                isChecked = habit.id in checkedIds
            )
        }

        return HabitWidgetData(
            totalCount = habits.size,
            checkedCount = habits.count { it.id in checkedIds },
            habits = habitItems,
            date = today
        )
    }

    // ==================== 健康Widget数据 ====================

    /**
     * 获取健康追踪数据
     */
    suspend fun getHealthData(): HealthWidgetData {
        val today = LocalDate.now().toEpochDay().toInt()

        // 饮水数据
        val waterTotal = database.waterIntakeDao().getDailyTotal(today) ?: 0
        val waterGoal = database.healthGoalDao().getGoalsSync()?.dailyWaterGoal ?: 2000

        // 睡眠数据
        val sleepRecord = database.sleepRecordDao().getByDate(today)
        val sleepGoalHours = database.healthGoalDao().getGoalsSync()?.dailySleepGoal ?: 8.0
        val sleepGoal = (sleepGoalHours * 60).toInt() // 转换为分钟

        return HealthWidgetData(
            waterCurrent = waterTotal,
            waterGoal = waterGoal,
            waterPercentage = minOf(100, waterTotal * 100 / waterGoal),
            sleepDuration = sleepRecord?.duration ?: 0,
            sleepGoal = sleepGoal,
            sleepQuality = sleepRecord?.quality ?: 0,
            date = today
        )
    }

    // ==================== 存钱计划Widget数据 ====================

    /**
     * 获取存钱计划进度
     */
    suspend fun getSavingsProgress(): SavingsWidgetData {
        val plans = database.savingsPlanDao().getActivePlans().first()

        if (plans.isEmpty()) {
            return SavingsWidgetData(
                activePlans = 0,
                totalTarget = 0.0,
                totalSaved = 0.0,
                percentage = 0,
                topPlan = null
            )
        }

        val totalTarget = plans.sumOf { it.targetAmount }
        val totalSaved = plans.sumOf { it.currentAmount }
        val topPlan = plans.maxByOrNull { it.currentAmount / it.targetAmount }

        return SavingsWidgetData(
            activePlans = plans.size,
            totalTarget = totalTarget,
            totalSaved = totalSaved,
            percentage = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt() else 0,
            topPlan = topPlan?.let {
                SavingsPlanWidgetItem(
                    id = it.id,
                    name = it.name,
                    target = it.targetAmount,
                    current = it.currentAmount,
                    percentage = ((it.currentAmount / it.targetAmount) * 100).toInt()
                )
            }
        )
    }

    // ==================== 快捷操作Widget数据 ====================

    /**
     * 获取快捷操作
     */
    fun getQuickActions(): List<QuickActionWidgetItem> = listOf(
        QuickActionWidgetItem("add_expense", "记支出", "💸", "com.lifemanager.app.ADD_EXPENSE"),
        QuickActionWidgetItem("add_income", "记收入", "💰", "com.lifemanager.app.ADD_INCOME"),
        QuickActionWidgetItem("add_todo", "添待办", "📝", "com.lifemanager.app.ADD_TODO"),
        QuickActionWidgetItem("check_habit", "打卡", "✅", "com.lifemanager.app.CHECK_HABIT"),
        QuickActionWidgetItem("add_water", "喝水", "💧", "com.lifemanager.app.ADD_WATER"),
        QuickActionWidgetItem("write_diary", "写日记", "📔", "com.lifemanager.app.WRITE_DIARY")
    )

    // ==================== 综合Widget数据 ====================

    /**
     * 获取仪表盘数据
     */
    suspend fun getDashboardData(): DashboardWidgetData {
        val finance = getTodayExpense()
        val todo = getTodoStats()
        val habit = getTodayHabits()
        val health = getHealthData()

        return DashboardWidgetData(
            todayExpense = finance.expense,
            todoProgress = "${todo.todayCompleted}/${todo.todayTotal}",
            habitProgress = "${habit.checkedCount}/${habit.totalCount}",
            waterProgress = health.waterPercentage
        )
    }
}

// ==================== Widget数据模型 ====================

data class FinanceWidgetData(
    val expense: Double,
    val income: Double,
    val transactionCount: Int,
    val date: Int
)

data class BudgetWidgetData(
    val totalBudget: Double,
    val totalSpent: Double,
    val percentage: Int,
    val remainingDays: Int
)

data class TodoWidgetData(
    val totalCount: Int,
    val completedCount: Int,
    val pendingItems: List<TodoWidgetItem>,
    val date: Int
)

data class TodoWidgetItem(
    val id: Long,
    val title: String,
    val priority: String,
    val dueTime: String?
)

data class TodoStatsWidgetData(
    val todayTotal: Int,
    val todayCompleted: Int,
    val overdueCount: Int
)

data class HabitWidgetData(
    val totalCount: Int,
    val checkedCount: Int,
    val habits: List<HabitWidgetItem>,
    val date: Int
)

data class HabitWidgetItem(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val isChecked: Boolean
)

data class HealthWidgetData(
    val waterCurrent: Int,
    val waterGoal: Int,
    val waterPercentage: Int,
    val sleepDuration: Int,
    val sleepGoal: Int,
    val sleepQuality: Int,
    val date: Int
)

data class SavingsWidgetData(
    val activePlans: Int,
    val totalTarget: Double,
    val totalSaved: Double,
    val percentage: Int,
    val topPlan: SavingsPlanWidgetItem?
)

data class SavingsPlanWidgetItem(
    val id: Long,
    val name: String,
    val target: Double,
    val current: Double,
    val percentage: Int
)

data class QuickActionWidgetItem(
    val id: String,
    val label: String,
    val icon: String,
    val action: String
)

data class DashboardWidgetData(
    val todayExpense: Double,
    val todoProgress: String,
    val habitProgress: String,
    val waterProgress: Int
)
