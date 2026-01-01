package com.lifemanager.app.feature.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.dao.DailyTransactionDao
import com.lifemanager.app.core.database.dao.GoalDao
import com.lifemanager.app.core.database.dao.TodoDao
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 首页ViewModel - 提供真实数据并优化性能
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionDao: DailyTransactionDao,
    private val todoDao: TodoDao,
    private val goalDao: GoalDao,
    private val habitRepository: HabitRepository
) : ViewModel() {

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 今日统计
    private val _todayStats = MutableStateFlow(TodayStatsData())
    val todayStats: StateFlow<TodayStatsData> = _todayStats.asStateFlow()

    // 本月财务
    private val _monthlyFinance = MutableStateFlow(MonthlyFinanceData())
    val monthlyFinance: StateFlow<MonthlyFinanceData> = _monthlyFinance.asStateFlow()

    // 目标进度
    private val _topGoals = MutableStateFlow<List<GoalProgressData>>(emptyList())
    val topGoals: StateFlow<List<GoalProgressData>> = _topGoals.asStateFlow()

    // 缓存日期参数，避免重复计算
    private val today = LocalDate.now().toEpochDay().toInt()
    private val yearMonth = YearMonth.now()
    private val monthStartDate = yearMonth.atDay(1).toEpochDay().toInt()
    private val monthEndDate = yearMonth.atEndOfMonth().toEpochDay().toInt()

    init {
        loadInitialData()
        observeDataChanges()
    }

    /**
     * 快速加载初始数据（一次性查询）
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true

            withContext(Dispatchers.IO) {
                // 并行加载所有初始数据
                val todayStatsDeferred = async { loadTodayStatsOnce() }
                val monthlyFinanceDeferred = async { loadMonthlyFinanceOnce() }
                val goalsDeferred = async { loadTopGoalsOnce() }

                // 等待所有数据加载完成
                _todayStats.value = todayStatsDeferred.await()
                _monthlyFinance.value = monthlyFinanceDeferred.await()
                _topGoals.value = goalsDeferred.await()
            }

            _isLoading.value = false
        }
    }

    /**
     * 观察数据变化（后台更新）
     */
    private fun observeDataChanges() {
        // 观察今日交易变化
        viewModelScope.launch {
            transactionDao.getTransactionsByDate(today)
                .drop(1) // 跳过初始值，因为已经加载过
                .debounce(300) // 防抖，避免频繁更新
                .collectLatest { transactions ->
                    val todayExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val todayIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    _todayStats.value = _todayStats.value.copy(
                        todayExpense = todayExpense,
                        todayIncome = todayIncome
                    )
                }
        }

        // 观察本月交易变化
        viewModelScope.launch {
            transactionDao.getTransactionsInRange(monthStartDate, monthEndDate)
                .drop(1)
                .debounce(300)
                .collectLatest { transactions ->
                    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    _monthlyFinance.value = MonthlyFinanceData(
                        totalIncome = income,
                        totalExpense = expense,
                        balance = income - expense
                    )
                }
        }

        // 观察目标变化
        viewModelScope.launch {
            goalDao.getActiveGoals()
                .drop(1)
                .debounce(300)
                .collectLatest { goals ->
                    _topGoals.value = goals.take(3).map { goal ->
                        val progress = calculateGoalProgress(goal)
                        GoalProgressData(
                            id = goal.id,
                            title = goal.title,
                            progress = progress,
                            progressText = formatGoalProgress(goal, progress)
                        )
                    }
                }
        }

        // 观察今日待办变化
        viewModelScope.launch {
            todoDao.getTodayTodos(today)
                .drop(1) // 跳过初始值
                .debounce(300)
                .collectLatest {
                    // 当今日待办变化时，重新获取统计数据
                    val todoStats = todoDao.getTodayStats(today)
                    _todayStats.value = _todayStats.value.copy(
                        completedTodos = todoStats.completed,
                        totalTodos = todoStats.total
                    )
                }
        }

        // 观察习惯打卡变化
        viewModelScope.launch {
            habitRepository.getActiveHabits()
                .drop(1)
                .debounce(300)
                .collectLatest { habits ->
                    val totalHabits = habits.size
                    // 统计今日已打卡的习惯数量
                    val completedHabits = habitRepository.countTodayCheckins(today)
                    _todayStats.value = _todayStats.value.copy(
                        completedHabits = completedHabits,
                        totalHabits = totalHabits
                    )
                }
        }

        // 观察今日打卡记录变化
        viewModelScope.launch {
            habitRepository.getRecordsByDate(today)
                .drop(1)
                .debounce(300)
                .collectLatest {
                    // 当打卡记录变化时，重新统计
                    val totalHabits = habitRepository.countActiveHabits()
                    val completedHabits = habitRepository.countTodayCheckins(today)
                    _todayStats.value = _todayStats.value.copy(
                        completedHabits = completedHabits,
                        totalHabits = totalHabits
                    )
                }
        }
    }

    /**
     * 一次性加载今日统计
     */
    private suspend fun loadTodayStatsOnce(): TodayStatsData {
        return try {
            val transactions = transactionDao.getTransactionsByDate(today).first()
            val todayExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val todayIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val todoStats = todoDao.getTodayStats(today)
            val totalHabits = habitRepository.countActiveHabits()
            val completedHabits = habitRepository.countTodayCheckins(today)

            TodayStatsData(
                completedTodos = todoStats.completed,
                totalTodos = todoStats.total,
                todayExpense = todayExpense,
                todayIncome = todayIncome,
                completedHabits = completedHabits,
                totalHabits = totalHabits,
                focusMinutes = 0
            )
        } catch (e: Exception) {
            TodayStatsData()
        }
    }

    /**
     * 一次性加载本月财务
     */
    private suspend fun loadMonthlyFinanceOnce(): MonthlyFinanceData {
        return try {
            val transactions = transactionDao.getTransactionsInRange(monthStartDate, monthEndDate).first()
            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            MonthlyFinanceData(
                totalIncome = income,
                totalExpense = expense,
                balance = income - expense
            )
        } catch (e: Exception) {
            MonthlyFinanceData()
        }
    }

    /**
     * 一次性加载目标进度
     */
    private suspend fun loadTopGoalsOnce(): List<GoalProgressData> {
        return try {
            val goals = goalDao.getActiveGoals().first()
            goals.take(3).map { goal ->
                val progress = calculateGoalProgress(goal)
                GoalProgressData(
                    id = goal.id,
                    title = goal.title,
                    progress = progress,
                    progressText = formatGoalProgress(goal, progress)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 计算目标进度
     */
    private fun calculateGoalProgress(goal: GoalEntity): Float {
        return when (goal.progressType) {
            "NUMERIC" -> {
                val target = goal.targetValue ?: 100.0
                if (target > 0) (goal.currentValue / target).toFloat().coerceIn(0f, 1f) else 0f
            }
            else -> (goal.currentValue / 100.0).toFloat().coerceIn(0f, 1f)
        }
    }

    /**
     * 格式化目标进度文本
     */
    private fun formatGoalProgress(goal: GoalEntity, progress: Float): String {
        return when (goal.progressType) {
            "NUMERIC" -> {
                val current = goal.currentValue.toInt()
                val target = goal.targetValue?.toInt() ?: 100
                "$current${goal.unit} / $target${goal.unit}"
            }
            else -> "${(progress * 100).toInt()}%"
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadInitialData()
    }
}

/**
 * 今日统计数据
 */
@Stable
data class TodayStatsData(
    val completedTodos: Int = 0,
    val totalTodos: Int = 0,
    val todayExpense: Double = 0.0,
    val todayIncome: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val focusMinutes: Int = 0
)

/**
 * 本月财务数据
 */
@Stable
data class MonthlyFinanceData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0
)

/**
 * 目标进度数据
 */
@Stable
data class GoalProgressData(
    val id: Long,
    val title: String,
    val progress: Float,
    val progressText: String
)
