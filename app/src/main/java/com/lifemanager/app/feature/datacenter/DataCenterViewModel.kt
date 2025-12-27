package com.lifemanager.app.feature.datacenter

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.domain.repository.*
import com.lifemanager.app.feature.datacenter.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 数据中心UI状态
 */
sealed class DataCenterUiState {
    object Loading : DataCenterUiState()
    object Success : DataCenterUiState()
    data class Error(val message: String) : DataCenterUiState()
}

/**
 * 总览统计数据
 */
data class OverviewStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalSavings: Double = 0.0,
    val todoCompleted: Int = 0,
    val todoTotal: Int = 0,
    val habitCheckedIn: Int = 0,
    val habitTotal: Int = 0,
    val focusMinutes: Int = 0,
    val activeGoals: Int = 0,
    val completedGoals: Int = 0,
    val avgGoalProgress: Float = 0f,
    val diaryCount: Int = 0,
    val avgMoodScore: Float = 0f
)

/**
 * 数据中心ViewModel
 */
@HiltViewModel
class DataCenterViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val habitRepository: HabitRepository,
    private val diaryRepository: DiaryRepository,
    private val timeTrackRepository: TimeTrackRepository,
    private val savingsPlanRepository: SavingsPlanRepository,
    private val goalRepository: GoalRepository,
    private val monthlyIncomeExpenseRepository: MonthlyIncomeExpenseRepository,
    private val dailyTransactionRepository: DailyTransactionRepository,
    private val customFieldRepository: CustomFieldRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<DataCenterUiState>(DataCenterUiState.Loading)
    val uiState: StateFlow<DataCenterUiState> = _uiState.asStateFlow()

    // 选中的标签页
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // 筛选状态
    private val _filterState = MutableStateFlow(DataCenterFilterState())
    val filterState: StateFlow<DataCenterFilterState> = _filterState.asStateFlow()

    // 收入分类选中状态
    private val _selectedIncomeIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIncomeIds: StateFlow<Set<Long>> = _selectedIncomeIds.asStateFlow()

    // 支出分类选中状态
    private val _selectedExpenseIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedExpenseIds: StateFlow<Set<Long>> = _selectedExpenseIds.asStateFlow()

    // 总览统计
    private val _overviewStats = MutableStateFlow(OverviewStats())
    val overviewStats: StateFlow<OverviewStats> = _overviewStats.asStateFlow()

    // 收入分类列表
    private val _incomeCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val incomeCategories: StateFlow<List<CustomFieldEntity>> = _incomeCategories.asStateFlow()

    // 支出分类列表
    private val _expenseCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val expenseCategories: StateFlow<List<CustomFieldEntity>> = _expenseCategories.asStateFlow()

    // 财务图表数据
    private val _financeChartData = MutableStateFlow<FinanceChartData?>(null)
    val financeChartData: StateFlow<FinanceChartData?> = _financeChartData.asStateFlow()

    // 效率图表数据
    private val _productivityChartData = MutableStateFlow<ProductivityChartData?>(null)
    val productivityChartData: StateFlow<ProductivityChartData?> = _productivityChartData.asStateFlow()

    // 生活图表数据
    private val _lifestyleChartData = MutableStateFlow<LifestyleChartData?>(null)
    val lifestyleChartData: StateFlow<LifestyleChartData?> = _lifestyleChartData.asStateFlow()

    init {
        loadCategories()
        loadData()
    }

    /**
     * 选择标签页
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * 更新日期范围类型
     */
    fun updateDateRangeType(type: DateRangeType) {
        _filterState.value = _filterState.value.copy(
            dateRangeType = type,
            customStartDate = null,
            customEndDate = null
        )
        loadData()
    }

    /**
     * 更新自定义日期范围
     */
    fun updateCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        _filterState.value = _filterState.value.copy(
            dateRangeType = DateRangeType.CUSTOM,
            customStartDate = startDate,
            customEndDate = endDate
        )
        loadData()
    }

    /**
     * 更新收入分类选择
     */
    fun updateIncomeSelection(ids: Set<Long>) {
        _selectedIncomeIds.value = ids
        loadFinanceData()
    }

    /**
     * 更新支出分类选择
     */
    fun updateExpenseSelection(ids: Set<Long>) {
        _selectedExpenseIds.value = ids
        loadFinanceData()
    }

    /**
     * 更新图表类型
     */
    fun updateChartType(type: ChartType) {
        _filterState.value = _filterState.value.copy(chartType = type)
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }

    /**
     * 加载分类列表
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                customFieldRepository.getFieldsByModule(ModuleType.INCOME).collect {
                    _incomeCategories.value = it
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }
        viewModelScope.launch {
            try {
                customFieldRepository.getFieldsByModule(ModuleType.EXPENSE).collect {
                    _expenseCategories.value = it
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }

    /**
     * 加载所有数据
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = DataCenterUiState.Loading
            try {
                val stats = calculateStats()
                _overviewStats.value = stats

                loadFinanceDataInternal()
                loadProductivityData()
                loadLifestyleData()

                _uiState.value = DataCenterUiState.Success
            } catch (e: Exception) {
                _uiState.value = DataCenterUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 仅加载财务数据
     */
    private fun loadFinanceData() {
        viewModelScope.launch {
            try {
                loadFinanceDataInternal()
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }

    /**
     * 加载财务图表数据
     */
    private suspend fun loadFinanceDataInternal() {
        val (startDate, endDate) = getDateRange()

        // 获取收入分类汇总
        val incomeTotals = dailyTransactionRepository
            .getCategoryTotalsInRange(startDate, endDate, "INCOME")
            .first()

        // 获取支出分类汇总
        val expenseTotals = dailyTransactionRepository
            .getCategoryTotalsInRange(startDate, endDate, "EXPENSE")
            .first()

        // 获取每日趋势
        val dailyTotals = dailyTransactionRepository
            .getDailyExpenseTotals(startDate, endDate)
            .first()

        // 转换为图表数据
        val incomeItems = mapToChartItems(incomeTotals, _incomeCategories.value, _selectedIncomeIds.value)
        val expenseItems = mapToChartItems(expenseTotals, _expenseCategories.value, _selectedExpenseIds.value)

        // 构建每日趋势数据
        val dailyTrendMap = dailyTotals.associate { it.date to it.total }
        val allDates = generateDateRange(startDate, endDate)
        val dailyTrend = allDates.map { date ->
            DailyFinanceTrend(
                date = date,
                income = 0.0,  // 暂时只有支出数据
                expense = dailyTrendMap[date] ?: 0.0
            )
        }

        val totalIncome = incomeItems.sumOf { it.value }
        val totalExpense = expenseItems.sumOf { it.value }

        _financeChartData.value = FinanceChartData(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = totalIncome - totalExpense,
            incomeByCategory = incomeItems,
            expenseByCategory = expenseItems,
            dailyTrend = dailyTrend
        )
    }

    /**
     * 加载效率图表数据
     */
    private suspend fun loadProductivityData() {
        val today = LocalDate.now().toEpochDay().toInt()
        val (startDate, endDate) = getDateRange()

        // 待办统计
        val todoStats = todoRepository.getTodayStats(today)
        val overdueTodos = todoRepository.getOverdueTodos(today).first()

        // 习惯统计
        val habits = habitRepository.getActiveHabits().first()
        val habitCheckedIn = habits.count { habit ->
            habitRepository.getRecordByHabitAndDate(habit.id, today) != null
        }

        // 计算各习惯完成率
        val habitRates = habits.map { habit ->
            val records = habitRepository.getRecordsByHabitAndDateRange(habit.id, startDate, endDate).first()
            val totalDays = (endDate - startDate + 1).coerceAtLeast(1)
            val completedDays = records.count { it.isCompleted }
            HabitCompletionItem(
                habitId = habit.id,
                name = habit.name,
                completionRate = completedDays.toFloat() / totalDays,
                color = parseHexColor(habit.color)
            )
        }

        // 时间追踪统计
        val timeRecords = timeTrackRepository.getRecordsByDateRange(startDate, endDate).first()
        val totalMinutes = timeRecords.sumOf { it.durationMinutes }

        // 时间分类分布
        val timeCategoryTotals = timeRecords
            .groupBy { it.categoryId }
            .mapValues { (_, records) -> records.sumOf { it.durationMinutes } }

        val timeCategories = timeTrackRepository.getAllCategories().first()
        val timeBreakdown = timeCategoryTotals.mapNotNull { (categoryId, minutes) ->
            val category = timeCategories.find { it.id == categoryId }
            category?.let {
                CategoryChartItem(
                    fieldId = it.id,
                    name = it.name,
                    value = minutes.toDouble(),
                    percentage = if (totalMinutes > 0) minutes.toFloat() / totalMinutes else 0f,
                    color = parseHexColor(it.color),
                    iconName = it.iconName
                )
            }
        }

        // 每日专注时长
        val dailyDurations = timeRecords
            .groupBy { it.date }
            .map { (date, records) ->
                DailyDuration(date, records.sumOf { it.durationMinutes })
            }
            .sortedBy { it.date }

        _productivityChartData.value = ProductivityChartData(
            todoStats = TodoChartData(
                completed = todoStats.completed,
                pending = todoStats.total - todoStats.completed,
                overdue = overdueTodos.size,
                completionRate = if (todoStats.total > 0) todoStats.completed.toFloat() / todoStats.total else 0f
            ),
            habitStats = HabitChartData(
                activeHabits = habits.size,
                todayCheckedIn = habitCheckedIn,
                overallRate = if (habits.isNotEmpty()) habitCheckedIn.toFloat() / habits.size else 0f,
                habitCompletionRates = habitRates
            ),
            timeStats = TimeChartData(
                totalMinutes = totalMinutes,
                categoryBreakdown = timeBreakdown,
                dailyDurations = dailyDurations
            )
        )
    }

    /**
     * 加载生活图表数据
     */
    private suspend fun loadLifestyleData() {
        val (startDate, endDate) = getDateRange()

        // 日记统计
        val diaries = diaryRepository.getByDateRange(startDate, endDate).first()
        val avgMood = if (diaries.isNotEmpty()) {
            diaries.mapNotNull { it.moodScore }.average().toFloat()
        } else 0f

        // 心情分布
        val moodCounts = diaries.groupBy { it.moodScore ?: 0 }
            .mapValues { (_, list) -> list.size }
        val totalDiaries = diaries.size.coerceAtLeast(1)
        val moodDistribution = (1..5).map { score ->
            val count = moodCounts[score] ?: 0
            MoodDistributionItem(
                moodScore = score,
                count = count,
                percentage = count.toFloat() / totalDiaries,
                color = getMoodColorByScore(score)
            )
        }

        // 每日心情趋势
        val dailyMood = diaries
            .filter { it.moodScore != null }
            .map { DailyMood(it.date, it.moodScore!!.toFloat()) }
            .sortedBy { it.date }

        // 存钱计划统计
        val savingsPlans = savingsPlanRepository.getActivePlans().first()
        val totalTarget = savingsPlans.sumOf { it.targetAmount }
        val totalCurrent = savingsPlans.sumOf { it.currentAmount }

        val planProgress = savingsPlans.map { plan ->
            SavingsPlanProgress(
                planId = plan.id,
                name = plan.name,
                targetAmount = plan.targetAmount,
                currentAmount = plan.currentAmount,
                progress = if (plan.targetAmount > 0)
                    (plan.currentAmount / plan.targetAmount).toFloat().coerceIn(0f, 1f)
                else 0f,
                color = parseHexColor(plan.color)
            )
        }

        _lifestyleChartData.value = LifestyleChartData(
            diaryStats = DiaryChartData(
                totalEntries = diaries.size,
                averageMood = avgMood,
                moodDistribution = moodDistribution,
                dailyMoodTrend = dailyMood
            ),
            savingsStats = SavingsChartData(
                activePlans = savingsPlans.size,
                totalTarget = totalTarget,
                totalCurrent = totalCurrent,
                overallProgress = if (totalTarget > 0) (totalCurrent / totalTarget).toFloat().coerceIn(0f, 1f) else 0f,
                planProgress = planProgress
            )
        )
    }

    /**
     * 计算总览统计数据
     */
    private suspend fun calculateStats(): OverviewStats {
        val today = LocalDate.now().toEpochDay().toInt()
        val (startDate, endDate) = getDateRange()

        // 待办统计
        val todoStats = todoRepository.getTodayStats(today)

        // 习惯统计
        val habits = habitRepository.getActiveHabits().first()
        val habitCheckedIn = habits.count { habit ->
            habitRepository.getRecordByHabitAndDate(habit.id, today) != null
        }

        // 日记统计
        val diaries = diaryRepository.getByDateRange(startDate, endDate).first()
        val avgMood = if (diaries.isNotEmpty()) {
            diaries.mapNotNull { it.moodScore }.average().toFloat()
        } else 0f

        // 时间统计
        val timeRecords = timeTrackRepository.getRecordsByDateRange(startDate, endDate).first()
        val focusMinutes = timeRecords.sumOf { it.durationMinutes }

        // 存钱统计
        val savingsPlans = savingsPlanRepository.getActivePlans().first()
        val totalSavings = savingsPlans.sumOf { it.currentAmount }

        // 目标统计
        val goals = goalRepository.getAllGoals().first()
        val activeGoals = goals.count { it.status == "ACTIVE" }
        val completedGoals = goals.count { it.status == "COMPLETED" }
        val avgProgress = if (goals.isNotEmpty()) {
            goals.filter { it.status == "ACTIVE" }.map { goal ->
                when {
                    goal.targetValue != null && goal.targetValue > 0 ->
                        (goal.currentValue / goal.targetValue).toFloat().coerceIn(0f, 1f)
                    else -> (goal.currentValue / 100.0).toFloat().coerceIn(0f, 1f)
                }
            }.average().toFloat()
        } else 0f

        // 财务统计
        val yearMonth = LocalDate.now().let { it.year * 100 + it.monthValue }
        val totalIncome = monthlyIncomeExpenseRepository.getTotalIncome(yearMonth)
        val totalExpense = monthlyIncomeExpenseRepository.getTotalExpense(yearMonth)

        return OverviewStats(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalSavings = totalSavings,
            todoCompleted = todoStats.completed,
            todoTotal = todoStats.total,
            habitCheckedIn = habitCheckedIn,
            habitTotal = habits.size,
            focusMinutes = focusMinutes,
            activeGoals = activeGoals,
            completedGoals = completedGoals,
            avgGoalProgress = avgProgress,
            diaryCount = diaries.size,
            avgMoodScore = avgMood
        )
    }

    /**
     * 获取日期范围
     */
    private fun getDateRange(): Pair<Int, Int> {
        val today = LocalDate.now().toEpochDay().toInt()
        val filter = _filterState.value

        return when (filter.dateRangeType) {
            DateRangeType.WEEK -> {
                val todayDate = LocalDate.ofEpochDay(today.toLong())
                val startOfWeek = todayDate.minusDays(todayDate.dayOfWeek.value.toLong() - 1)
                Pair(startOfWeek.toEpochDay().toInt(), today)
            }
            DateRangeType.MONTH -> {
                val todayDate = LocalDate.ofEpochDay(today.toLong())
                val startOfMonth = todayDate.withDayOfMonth(1)
                Pair(startOfMonth.toEpochDay().toInt(), today)
            }
            DateRangeType.YEAR -> {
                val todayDate = LocalDate.ofEpochDay(today.toLong())
                val startOfYear = todayDate.withDayOfYear(1)
                Pair(startOfYear.toEpochDay().toInt(), today)
            }
            DateRangeType.CUSTOM -> {
                val startDate = filter.customStartDate?.toEpochDay()?.toInt() ?: 0
                val endDate = filter.customEndDate?.toEpochDay()?.toInt() ?: today
                Pair(startDate, endDate)
            }
            DateRangeType.ALL -> Pair(0, today)
        }
    }

    /**
     * 将分类汇总转换为图表数据
     */
    private fun mapToChartItems(
        totals: List<com.lifemanager.app.core.database.dao.FieldTotal>,
        categories: List<CustomFieldEntity>,
        selectedIds: Set<Long>
    ): List<CategoryChartItem> {
        val categoryMap = categories.associateBy { it.id }
        val totalValue = totals.sumOf { it.total }

        return totals
            .filter { total ->
                val fieldId = total.fieldId ?: return@filter false
                // 如果没有选中任何分类，显示全部；否则只显示选中的
                selectedIds.isEmpty() || selectedIds.contains(fieldId)
            }
            .mapNotNull { total ->
                val fieldId = total.fieldId ?: return@mapNotNull null
                val category = categoryMap[fieldId] ?: return@mapNotNull null
                CategoryChartItem(
                    fieldId = fieldId,
                    name = category.name,
                    value = total.total,
                    percentage = if (totalValue > 0) (total.total / totalValue).toFloat() else 0f,
                    color = parseHexColor(category.color),
                    iconName = category.iconName
                )
            }
            .sortedByDescending { it.value }
    }

    /**
     * 生成日期范围内的所有日期
     */
    private fun generateDateRange(startDate: Int, endDate: Int): List<Int> {
        return (startDate..endDate).toList()
    }

    /**
     * 解析十六进制颜色
     */
    private fun parseHexColor(hexColor: String): Color {
        return try {
            val colorString = if (hexColor.startsWith("#")) hexColor.substring(1) else hexColor
            Color(android.graphics.Color.parseColor("#$colorString"))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    /**
     * 根据心情分数获取颜色
     */
    private fun getMoodColorByScore(score: Int): Color {
        return when (score) {
            5 -> Color(0xFF4CAF50)
            4 -> Color(0xFF8BC34A)
            3 -> Color(0xFFFFEB3B)
            2 -> Color(0xFFFF9800)
            1 -> Color(0xFFF44336)
            else -> Color.Gray
        }
    }
}
