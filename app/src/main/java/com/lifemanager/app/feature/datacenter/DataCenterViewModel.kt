package com.lifemanager.app.feature.datacenter

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.ai.service.AIDataAnalysisService
import com.lifemanager.app.core.database.entity.AIAnalysisEntity
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.domain.model.MonthlyBudgetAnalysis
import com.lifemanager.app.domain.repository.*
import com.lifemanager.app.domain.usecase.BudgetUseCase
import com.lifemanager.app.feature.datacenter.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
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
    val avgMoodScore: Float = 0f,
    // 预算相关
    val budgetAmount: Double = 0.0,
    val budgetSpent: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetUsagePercentage: Int = 0,
    val hasBudget: Boolean = false
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
    private val customFieldRepository: CustomFieldRepository,
    private val budgetUseCase: BudgetUseCase,
    private val monthlyAssetRepository: MonthlyAssetRepository,
    private val aiDataAnalysisService: AIDataAnalysisService
) : ViewModel() {

    // 数据加载防抖任务
    private var loadDataJob: Job? = null
    private val DEBOUNCE_DELAY = 300L // 防抖延迟毫秒

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

    // 预算历史分析
    private val _budgetAnalysis = MutableStateFlow<List<MonthlyBudgetAnalysis>>(emptyList())
    val budgetAnalysis: StateFlow<List<MonthlyBudgetAnalysis>> = _budgetAnalysis.asStateFlow()

    // 预算AI建议
    private val _budgetAIAdvice = MutableStateFlow("")
    val budgetAIAdvice: StateFlow<String> = _budgetAIAdvice.asStateFlow()

    // 资产趋势数据
    private val _assetTrendData = MutableStateFlow<AssetTrendData?>(null)
    val assetTrendData: StateFlow<AssetTrendData?> = _assetTrendData.asStateFlow()

    // 账单列表
    private val _billList = MutableStateFlow<List<BillQueryItem>>(emptyList())
    val billList: StateFlow<List<BillQueryItem>> = _billList.asStateFlow()

    // 支出分类排名
    private val _expenseRanking = MutableStateFlow<List<CategoryRankingItem>>(emptyList())
    val expenseRanking: StateFlow<List<CategoryRankingItem>> = _expenseRanking.asStateFlow()

    // AI综合健康评分
    private val _overallHealthScore = MutableStateFlow<AIAnalysisEntity?>(null)
    val overallHealthScore: StateFlow<AIAnalysisEntity?> = _overallHealthScore.asStateFlow()

    // AI分析加载状态
    private val _isAIAnalyzing = MutableStateFlow(false)
    val isAIAnalyzing: StateFlow<Boolean> = _isAIAnalyzing.asStateFlow()

    // 各模块AI分析
    private val _financeAnalysis = MutableStateFlow<AIAnalysisEntity?>(null)
    val financeAnalysis: StateFlow<AIAnalysisEntity?> = _financeAnalysis.asStateFlow()

    private val _goalAnalysis = MutableStateFlow<AIAnalysisEntity?>(null)
    val goalAnalysis: StateFlow<AIAnalysisEntity?> = _goalAnalysis.asStateFlow()

    private val _habitAnalysis = MutableStateFlow<AIAnalysisEntity?>(null)
    val habitAnalysis: StateFlow<AIAnalysisEntity?> = _habitAnalysis.asStateFlow()

    // ============== 高级筛选和图表状态 ==============

    // 筛选预设列表
    private val _filterPresets = MutableStateFlow<List<FilterPreset>>(defaultFilterPresets)
    val filterPresets: StateFlow<List<FilterPreset>> = _filterPresets.asStateFlow()

    // 当前选中的预设
    private val _selectedPreset = MutableStateFlow<FilterPreset?>(null)
    val selectedPreset: StateFlow<FilterPreset?> = _selectedPreset.asStateFlow()

    // 高级筛选面板展开状态
    private val _isAdvancedFilterExpanded = MutableStateFlow(false)
    val isAdvancedFilterExpanded: StateFlow<Boolean> = _isAdvancedFilterExpanded.asStateFlow()

    // 对比数据
    private val _compareData = MutableStateFlow<CompareData?>(null)
    val compareData: StateFlow<CompareData?> = _compareData.asStateFlow()

    // 热力图数据
    private val _heatmapData = MutableStateFlow<HeatmapData?>(null)
    val heatmapData: StateFlow<HeatmapData?> = _heatmapData.asStateFlow()

    // 雷达图数据
    private val _radarData = MutableStateFlow<RadarChartData?>(null)
    val radarData: StateFlow<RadarChartData?> = _radarData.asStateFlow()

    // 树状图数据
    private val _treemapData = MutableStateFlow<TreemapData?>(null)
    val treemapData: StateFlow<TreemapData?> = _treemapData.asStateFlow()

    // 瀑布图数据
    private val _waterfallData = MutableStateFlow<WaterfallData?>(null)
    val waterfallData: StateFlow<WaterfallData?> = _waterfallData.asStateFlow()

    // 漏斗图数据
    private val _funnelData = MutableStateFlow<FunnelData?>(null)
    val funnelData: StateFlow<FunnelData?> = _funnelData.asStateFlow()

    // 散点图数据
    private val _scatterData = MutableStateFlow<ScatterData?>(null)
    val scatterData: StateFlow<ScatterData?> = _scatterData.asStateFlow()

    // 聚合数据
    private val _aggregateData = MutableStateFlow<AggregateData?>(null)
    val aggregateData: StateFlow<AggregateData?> = _aggregateData.asStateFlow()

    // 数据摘要
    private val _dataSummary = MutableStateFlow<DataSummary?>(null)
    val dataSummary: StateFlow<DataSummary?> = _dataSummary.asStateFlow()

    // 数据洞察
    private val _dataInsights = MutableStateFlow<List<DataInsight>>(emptyList())
    val dataInsights: StateFlow<List<DataInsight>> = _dataInsights.asStateFlow()

    // 导出配置
    private val _exportConfig = MutableStateFlow(ExportConfig())
    val exportConfig: StateFlow<ExportConfig> = _exportConfig.asStateFlow()

    // 导出状态
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // 自定义视图配置列表
    private val _customViews = MutableStateFlow<List<CustomViewConfig>>(emptyList())
    val customViews: StateFlow<List<CustomViewConfig>> = _customViews.asStateFlow()

    init {
        loadCategories()
        loadData()
        loadAIAnalysis()
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
        loadDataDebounced()
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
        loadDataDebounced()
    }

    /**
     * 更新收入分类选择
     */
    fun updateIncomeSelection(ids: Set<Long>) {
        _selectedIncomeIds.value = ids
        loadFinanceDataDebounced()
    }

    /**
     * 更新支出分类选择
     */
    fun updateExpenseSelection(ids: Set<Long>) {
        _selectedExpenseIds.value = ids
        loadFinanceDataDebounced()
    }

    // 财务数据加载防抖任务
    private var loadFinanceJob: Job? = null

    /**
     * 防抖加载财务数据
     */
    private fun loadFinanceDataDebounced() {
        loadFinanceJob?.cancel()
        loadFinanceJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            loadFinanceData()
        }
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
        loadDataDebounced()
    }

    /**
     * 防抖加载数据
     */
    private fun loadDataDebounced() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            loadData()
        }
    }

    /**
     * 加载AI分析数据
     */
    private fun loadAIAnalysis() {
        viewModelScope.launch {
            // 收集综合健康评分
            aiDataAnalysisService.getOverallHealthScore().collect { analysis ->
                _overallHealthScore.value = analysis
            }
        }
        viewModelScope.launch {
            aiDataAnalysisService.getFinanceAnalysis().collect { analyses ->
                _financeAnalysis.value = analyses.firstOrNull()
            }
        }
        viewModelScope.launch {
            aiDataAnalysisService.getGoalAnalysis().collect { analyses ->
                _goalAnalysis.value = analyses.firstOrNull()
            }
        }
        viewModelScope.launch {
            aiDataAnalysisService.getHabitAnalysis().collect { analyses ->
                _habitAnalysis.value = analyses.firstOrNull()
            }
        }
    }

    /**
     * 刷新AI分析
     */
    fun refreshAIAnalysis() {
        viewModelScope.launch {
            _isAIAnalyzing.value = true
            try {
                // 先分析各模块
                aiDataAnalysisService.analyzeFinanceData(forceRefresh = true)
                aiDataAnalysisService.analyzeGoalData(forceRefresh = true)
                aiDataAnalysisService.analyzeHabitData(forceRefresh = true)
                // 然后生成综合评分
                aiDataAnalysisService.generateOverallHealthScore(forceRefresh = true)
            } catch (e: Exception) {
                // 静默失败
            } finally {
                _isAIAnalyzing.value = false
            }
        }
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
                loadBudgetData()
                loadAssetTrendData()
                loadBillList()
                loadExpenseRanking()

                _uiState.value = DataCenterUiState.Success
            } catch (e: Exception) {
                _uiState.value = DataCenterUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 加载预算数据
     */
    private suspend fun loadBudgetData() {
        try {
            // 加载预算历史分析
            _budgetAnalysis.value = budgetUseCase.getMonthlyBudgetAnalysis(6)

            // 加载当月预算AI建议
            val currentYearMonth = LocalDate.now().let { it.year * 100 + it.monthValue }
            _budgetAIAdvice.value = budgetUseCase.generateAIBudgetAdvice(currentYearMonth)
        } catch (e: Exception) {
            // 忽略预算加载错误
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

        // 预算统计
        val budgetWithSpending = budgetUseCase.getBudgetWithSpending(yearMonth).first()
        val budgetAmount = budgetWithSpending?.budget?.totalBudget ?: 0.0
        val budgetSpent = budgetWithSpending?.totalSpent ?: 0.0
        val budgetRemaining = budgetWithSpending?.remaining ?: 0.0
        val budgetUsagePercentage = budgetWithSpending?.usagePercentage ?: 0
        val hasBudget = budgetWithSpending != null

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
            avgMoodScore = avgMood,
            budgetAmount = budgetAmount,
            budgetSpent = budgetSpent,
            budgetRemaining = budgetRemaining,
            budgetUsagePercentage = budgetUsagePercentage,
            hasBudget = hasBudget
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

    /**
     * 加载资产趋势数据
     */
    private suspend fun loadAssetTrendData() {
        try {
            val today = LocalDate.now()
            val endMonth = today.year * 100 + today.monthValue
            // 获取最近12个月的数据
            val startMonth = today.minusMonths(11).let { it.year * 100 + it.monthValue }

            val netWorthTrend = monthlyAssetRepository.getNetWorthTrend(startMonth, endMonth).first()

            val trendPoints = mutableListOf<AssetTrendPoint>()

            // 为每个月获取资产和负债数据
            for (month in generateMonthRange(startMonth, endMonth)) {
                val totalAssets = monthlyAssetRepository.getTotalAssets(month)
                val totalLiabilities = monthlyAssetRepository.getTotalLiabilities(month)
                val netWorth = totalAssets - totalLiabilities

                if (totalAssets > 0 || totalLiabilities > 0) {
                    trendPoints.add(
                        AssetTrendPoint(
                            yearMonth = month,
                            totalAssets = totalAssets,
                            totalLiabilities = totalLiabilities,
                            netWorth = netWorth
                        )
                    )
                }
            }

            // 计算变化
            val latestNetWorth = trendPoints.lastOrNull()?.netWorth ?: 0.0
            val previousNetWorth = if (trendPoints.size >= 2) trendPoints[trendPoints.size - 2].netWorth else 0.0
            val netWorthChange = latestNetWorth - previousNetWorth
            val netWorthChangePercentage = if (previousNetWorth != 0.0) {
                ((netWorthChange / previousNetWorth) * 100).toFloat()
            } else 0f

            _assetTrendData.value = AssetTrendData(
                trendPoints = trendPoints,
                latestNetWorth = latestNetWorth,
                netWorthChange = netWorthChange,
                netWorthChangePercentage = netWorthChangePercentage
            )
        } catch (e: Exception) {
            // 忽略资产趋势加载错误
        }
    }

    /**
     * 生成月份范围
     */
    private fun generateMonthRange(startMonth: Int, endMonth: Int): List<Int> {
        val months = mutableListOf<Int>()
        var current = startMonth
        while (current <= endMonth) {
            months.add(current)
            val year = current / 100
            val month = current % 100
            if (month == 12) {
                current = (year + 1) * 100 + 1
            } else {
                current = year * 100 + month + 1
            }
        }
        return months
    }

    /**
     * 加载账单列表
     */
    private suspend fun loadBillList() {
        try {
            val (startDate, endDate) = getDateRange()
            val transactions = dailyTransactionRepository.getByDateRange(startDate, endDate).first()

            val categoryMap = (_incomeCategories.value + _expenseCategories.value).associateBy { it.id }

            _billList.value = transactions.map { tx ->
                val category = tx.categoryId?.let { categoryMap[it] }
                BillQueryItem(
                    id = tx.id,
                    date = tx.date,
                    type = tx.type,
                    amount = tx.amount,
                    categoryName = category?.name ?: "未分类",
                    categoryColor = category?.color ?: "#808080",
                    note = tx.note
                )
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            // 忽略账单列表加载错误
        }
    }

    /**
     * 加载支出分类排名
     */
    private suspend fun loadExpenseRanking() {
        try {
            val (startDate, endDate) = getDateRange()

            val expenseTotals = dailyTransactionRepository
                .getCategoryTotalsInRange(startDate, endDate, "EXPENSE")
                .first()

            val categoryMap = _expenseCategories.value.associateBy { it.id }
            val totalExpense = expenseTotals.sumOf { it.total }

            _expenseRanking.value = expenseTotals
                .mapNotNull { total ->
                    val fieldId = total.fieldId ?: return@mapNotNull null
                    val category = categoryMap[fieldId] ?: return@mapNotNull null
                    CategoryRankingItem(
                        fieldId = fieldId,
                        name = category.name,
                        amount = total.total,
                        percentage = if (totalExpense > 0) (total.total / totalExpense * 100).toFloat() else 0f,
                        color = parseHexColor(category.color),
                        rank = 0 // 临时值，下面会更新
                    )
                }
                .sortedByDescending { it.amount }
                .mapIndexed { index, item ->
                    item.copy(rank = index + 1)
                }
        } catch (e: Exception) {
            // 忽略支出排名加载错误
        }
    }

    // ============== 高级筛选和图表方法 ==============

    /**
     * 切换高级筛选面板展开状态
     */
    fun toggleAdvancedFilter() {
        _isAdvancedFilterExpanded.value = !_isAdvancedFilterExpanded.value
    }

    /**
     * 应用筛选预设
     */
    fun applyPreset(preset: FilterPreset) {
        _selectedPreset.value = preset
        _filterState.value = preset.filterState
        loadDataDebounced()
        loadAdvancedChartData()
    }

    /**
     * 保存自定义筛选预设
     */
    fun saveCustomPreset(name: String, description: String = "") {
        val newPreset = FilterPreset(
            id = System.currentTimeMillis(),
            name = name,
            description = description,
            filterState = _filterState.value,
            isDefault = false
        )
        _filterPresets.value = _filterPresets.value + newPreset
    }

    /**
     * 删除自定义筛选预设
     */
    fun deletePreset(presetId: Long) {
        _filterPresets.value = _filterPresets.value.filter { it.id != presetId || it.isDefault }
        if (_selectedPreset.value?.id == presetId) {
            _selectedPreset.value = null
        }
    }

    /**
     * 更新选中的模块
     */
    fun updateSelectedModules(modules: Set<DataModule>) {
        _filterState.value = _filterState.value.copy(selectedModules = modules)
        loadDataDebounced()
    }

    /**
     * 更新对比模式
     */
    fun updateCompareMode(mode: CompareMode) {
        _filterState.value = _filterState.value.copy(compareMode = mode)
        if (mode != CompareMode.NONE) {
            loadCompareData()
        } else {
            _compareData.value = null
        }
    }

    /**
     * 更新聚合粒度
     */
    fun updateAggregateGranularity(granularity: AggregateGranularity) {
        _filterState.value = _filterState.value.copy(aggregateGranularity = granularity)
        loadAggregateData()
    }

    /**
     * 更新排序方式
     */
    fun updateSortMode(sortMode: SortMode) {
        _filterState.value = _filterState.value.copy(sortMode = sortMode)
        loadDataDebounced()
    }

    /**
     * 更新金额范围筛选
     */
    fun updateAmountRange(minAmount: Double?, maxAmount: Double?) {
        _filterState.value = _filterState.value.copy(
            minAmount = minAmount,
            maxAmount = maxAmount
        )
        loadDataDebounced()
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchKeyword(keyword: String) {
        _filterState.value = _filterState.value.copy(searchKeyword = keyword)
        loadDataDebounced()
    }

    /**
     * 更新显示前N项
     */
    fun updateShowTopN(n: Int) {
        _filterState.value = _filterState.value.copy(showTopN = n)
        loadDataDebounced()
    }

    /**
     * 重置筛选条件
     */
    fun resetFilters() {
        _filterState.value = DataCenterFilterState()
        _selectedPreset.value = null
        _selectedIncomeIds.value = emptySet()
        _selectedExpenseIds.value = emptySet()
        loadDataDebounced()
    }

    /**
     * 加载高级图表数据
     */
    fun loadAdvancedChartData() {
        viewModelScope.launch {
            when (_filterState.value.chartType) {
                ChartType.HEATMAP -> loadHeatmapData()
                ChartType.RADAR -> loadRadarData()
                ChartType.TREEMAP -> loadTreemapData()
                ChartType.WATERFALL -> loadWaterfallData()
                ChartType.FUNNEL -> loadFunnelData()
                ChartType.SCATTER -> loadScatterData()
                else -> { /* 其他图表类型在常规加载中处理 */ }
            }
            loadDataSummary()
            generateDataInsights()
        }
    }

    /**
     * 加载热力图数据 - 习惯打卡热力图
     */
    private suspend fun loadHeatmapData() {
        try {
            val (startDate, endDate) = getDateRange()
            val habits = habitRepository.getActiveHabits().first()

            if (habits.isEmpty()) {
                _heatmapData.value = null
                return
            }

            // 生成日期到周几和周数的映射
            val cells = mutableListOf<HeatmapCell>()
            val weekFields = WeekFields.of(Locale.getDefault())

            var currentDate = LocalDate.ofEpochDay(startDate.toLong())
            val endLocalDate = LocalDate.ofEpochDay(endDate.toLong())
            val startWeek = currentDate.get(weekFields.weekOfMonth())

            while (!currentDate.isAfter(endLocalDate)) {
                val epochDay = currentDate.toEpochDay().toInt()
                val dayOfWeek = currentDate.dayOfWeek.value - 1  // 0-6
                val weekOfMonth = currentDate.get(weekFields.weekOfMonth()) - startWeek

                // 统计当天所有习惯的完成情况
                var completedCount = 0
                habits.forEach { habit ->
                    val record = habitRepository.getRecordByHabitAndDate(habit.id, epochDay)
                    if (record?.isCompleted == true) {
                        completedCount++
                    }
                }

                val completionRate = if (habits.isNotEmpty()) {
                    completedCount.toFloat() / habits.size
                } else 0f

                cells.add(HeatmapCell(
                    row = dayOfWeek,
                    column = weekOfMonth.coerceAtLeast(0),
                    value = completionRate,
                    label = "${(completionRate * 100).toInt()}%",
                    date = epochDay
                ))

                currentDate = currentDate.plusDays(1)
            }

            _heatmapData.value = HeatmapData(
                cells = cells,
                rowLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
                columnLabels = (1..(cells.maxOfOrNull { it.column }?.plus(1) ?: 1)).map { "第${it}周" },
                title = "习惯完成热力图",
                colorStart = Color(0xFFE3F2FD),
                colorEnd = Color(0xFF1565C0)
            )
        } catch (e: Exception) {
            _heatmapData.value = null
        }
    }

    /**
     * 加载雷达图数据 - 多维度生活质量评估
     */
    private suspend fun loadRadarData() {
        try {
            val (startDate, endDate) = getDateRange()

            // 计算各维度的得分（0-1范围）
            val axes = listOf("财务健康", "习惯坚持", "任务效率", "时间管理", "心情状态", "目标进度")

            // 财务健康：收入支出比
            val yearMonth = LocalDate.now().let { it.year * 100 + it.monthValue }
            val totalIncome = monthlyIncomeExpenseRepository.getTotalIncome(yearMonth)
            val totalExpense = monthlyIncomeExpenseRepository.getTotalExpense(yearMonth)
            val financeScore = if (totalIncome > 0) {
                ((totalIncome - totalExpense) / totalIncome).toFloat().coerceIn(0f, 1f)
            } else 0.5f

            // 习惯坚持率
            val habits = habitRepository.getActiveHabits().first()
            val habitScore = if (habits.isNotEmpty()) {
                val today = LocalDate.now().toEpochDay().toInt()
                val checkedIn = habits.count { habit ->
                    habitRepository.getRecordByHabitAndDate(habit.id, today)?.isCompleted == true
                }
                checkedIn.toFloat() / habits.size
            } else 0f

            // 任务效率
            val today = LocalDate.now().toEpochDay().toInt()
            val todoStats = todoRepository.getTodayStats(today)
            val todoScore = if (todoStats.total > 0) {
                todoStats.completed.toFloat() / todoStats.total
            } else 0.5f

            // 时间管理（专注时长占比）
            val timeRecords = timeTrackRepository.getRecordsByDateRange(startDate, endDate).first()
            val totalMinutes = timeRecords.sumOf { it.durationMinutes }
            val dayCount = (endDate - startDate + 1).coerceAtLeast(1)
            val avgMinutesPerDay = totalMinutes.toFloat() / dayCount
            val timeScore = (avgMinutesPerDay / 480f).coerceIn(0f, 1f)  // 假设8小时为满分

            // 心情状态
            val diaries = diaryRepository.getByDateRange(startDate, endDate).first()
            val moodScore = if (diaries.isNotEmpty()) {
                diaries.mapNotNull { it.moodScore }.average().toFloat() / 5f
            } else 0.5f

            // 目标进度
            val goals = goalRepository.getAllGoals().first()
            val activeGoals = goals.filter { it.status == "ACTIVE" }
            val goalScore = if (activeGoals.isNotEmpty()) {
                activeGoals.map { goal ->
                    when {
                        goal.targetValue != null && goal.targetValue > 0 ->
                            (goal.currentValue / goal.targetValue).toFloat().coerceIn(0f, 1f)
                        else -> (goal.currentValue / 100.0).toFloat().coerceIn(0f, 1f)
                    }
                }.average().toFloat()
            } else 0.5f

            val scores = listOf(financeScore, habitScore, todoScore, timeScore, moodScore, goalScore)

            val dataSet = RadarDataSet(
                label = "本期表现",
                points = axes.mapIndexed { index, axis ->
                    RadarDataPoint(
                        axis = axis,
                        value = scores[index],
                        color = Color(0xFF2196F3)
                    )
                },
                color = Color(0xFF2196F3),
                fillAlpha = 0.3f
            )

            _radarData.value = RadarChartData(
                axes = axes,
                dataSets = listOf(dataSet),
                maxValue = 1f
            )
        } catch (e: Exception) {
            _radarData.value = null
        }
    }

    /**
     * 加载树状图数据 - 支出分类层级
     */
    private suspend fun loadTreemapData() {
        try {
            val (startDate, endDate) = getDateRange()

            val expenseTotals = dailyTransactionRepository
                .getCategoryTotalsInRange(startDate, endDate, "EXPENSE")
                .first()

            val categoryMap = _expenseCategories.value.associateBy { it.id }
            val totalExpense = expenseTotals.sumOf { it.total }

            val nodes = expenseTotals.mapNotNull { total ->
                val fieldId = total.fieldId ?: return@mapNotNull null
                val category = categoryMap[fieldId] ?: return@mapNotNull null
                TreemapNode(
                    id = fieldId.toString(),
                    name = category.name,
                    value = total.total,
                    color = parseHexColor(category.color),
                    percentage = if (totalExpense > 0) (total.total / totalExpense * 100).toFloat() else 0f
                )
            }.sortedByDescending { it.value }

            _treemapData.value = TreemapData(
                rootNodes = nodes,
                totalValue = totalExpense,
                title = "支出分类占比"
            )
        } catch (e: Exception) {
            _treemapData.value = null
        }
    }

    /**
     * 加载瀑布图数据 - 收支流水
     */
    private suspend fun loadWaterfallData() {
        try {
            val yearMonth = LocalDate.now().let { it.year * 100 + it.monthValue }

            // 获取上月结余
            val previousMonth = if (yearMonth % 100 == 1) {
                (yearMonth / 100 - 1) * 100 + 12
            } else {
                yearMonth - 1
            }
            val previousIncome = monthlyIncomeExpenseRepository.getTotalIncome(previousMonth)
            val previousExpense = monthlyIncomeExpenseRepository.getTotalExpense(previousMonth)
            val openingBalance = previousIncome - previousExpense

            // 获取本月收入分类
            val (startDate, endDate) = getDateRange()
            val incomeTotals = dailyTransactionRepository
                .getCategoryTotalsInRange(startDate, endDate, "INCOME")
                .first()
            val expenseTotals = dailyTransactionRepository
                .getCategoryTotalsInRange(startDate, endDate, "EXPENSE")
                .first()

            val incomeMap = _incomeCategories.value.associateBy { it.id }
            val expenseMap = _expenseCategories.value.associateBy { it.id }

            val items = mutableListOf<WaterfallItem>()
            var runningTotal = openingBalance

            // 期初余额
            items.add(WaterfallItem(
                label = "期初余额",
                value = openingBalance,
                isTotal = true,
                isPositive = openingBalance >= 0,
                startValue = 0.0,
                endValue = openingBalance
            ))

            // 收入项目
            incomeTotals.forEach { total ->
                val fieldId = total.fieldId ?: return@forEach
                val category = incomeMap[fieldId] ?: return@forEach
                val startVal = runningTotal
                runningTotal += total.total
                items.add(WaterfallItem(
                    label = category.name,
                    value = total.total,
                    isPositive = true,
                    startValue = startVal,
                    endValue = runningTotal
                ))
            }

            // 支出项目
            expenseTotals.forEach { total ->
                val fieldId = total.fieldId ?: return@forEach
                val category = expenseMap[fieldId] ?: return@forEach
                val startVal = runningTotal
                runningTotal -= total.total
                items.add(WaterfallItem(
                    label = category.name,
                    value = total.total,
                    isPositive = false,
                    startValue = startVal,
                    endValue = runningTotal
                ))
            }

            // 期末余额
            items.add(WaterfallItem(
                label = "期末余额",
                value = runningTotal,
                isTotal = true,
                isPositive = runningTotal >= 0,
                startValue = 0.0,
                endValue = runningTotal
            ))

            _waterfallData.value = WaterfallData(
                items = items,
                title = "收支流水瀑布图"
            )
        } catch (e: Exception) {
            _waterfallData.value = null
        }
    }

    /**
     * 加载漏斗图数据 - 目标完成阶段
     */
    private suspend fun loadFunnelData() {
        try {
            val goals = goalRepository.getAllGoals().first()

            val totalGoals = goals.size
            val activeGoals = goals.count { it.status == "ACTIVE" }
            val inProgressGoals = goals.count { it.status == "ACTIVE" && it.currentValue > 0 }
            val nearCompletionGoals = goals.count {
                it.status == "ACTIVE" &&
                it.targetValue != null &&
                it.currentValue / it.targetValue >= 0.8
            }
            val completedGoals = goals.count { it.status == "COMPLETED" }

            val items = listOf(
                FunnelItem(
                    label = "全部目标",
                    value = totalGoals.toDouble(),
                    percentage = 100f,
                    color = Color(0xFF2196F3),
                    conversionRate = 100f
                ),
                FunnelItem(
                    label = "进行中",
                    value = activeGoals.toDouble(),
                    percentage = if (totalGoals > 0) activeGoals.toFloat() / totalGoals * 100 else 0f,
                    color = Color(0xFF4CAF50),
                    conversionRate = if (totalGoals > 0) activeGoals.toFloat() / totalGoals * 100 else 0f
                ),
                FunnelItem(
                    label = "已有进展",
                    value = inProgressGoals.toDouble(),
                    percentage = if (totalGoals > 0) inProgressGoals.toFloat() / totalGoals * 100 else 0f,
                    color = Color(0xFFFF9800),
                    conversionRate = if (activeGoals > 0) inProgressGoals.toFloat() / activeGoals * 100 else 0f
                ),
                FunnelItem(
                    label = "即将完成",
                    value = nearCompletionGoals.toDouble(),
                    percentage = if (totalGoals > 0) nearCompletionGoals.toFloat() / totalGoals * 100 else 0f,
                    color = Color(0xFF9C27B0),
                    conversionRate = if (inProgressGoals > 0) nearCompletionGoals.toFloat() / inProgressGoals * 100 else 0f
                ),
                FunnelItem(
                    label = "已完成",
                    value = completedGoals.toDouble(),
                    percentage = if (totalGoals > 0) completedGoals.toFloat() / totalGoals * 100 else 0f,
                    color = Color(0xFF4CAF50),
                    conversionRate = if (nearCompletionGoals > 0) completedGoals.toFloat() / nearCompletionGoals * 100 else 0f
                )
            )

            _funnelData.value = FunnelData(
                items = items,
                title = "目标完成漏斗"
            )
        } catch (e: Exception) {
            _funnelData.value = null
        }
    }

    /**
     * 加载散点图数据 - 支出金额与日期分布
     */
    private suspend fun loadScatterData() {
        try {
            val (startDate, endDate) = getDateRange()
            val transactions = dailyTransactionRepository.getByDateRange(startDate, endDate).first()

            val categoryMap = _expenseCategories.value.associateBy { it.id }

            val points = transactions
                .filter { it.type == "EXPENSE" }
                .map { tx ->
                    val category = tx.categoryId?.let { categoryMap[it] }
                    ScatterPoint(
                        x = tx.date.toFloat(),
                        y = tx.amount.toFloat(),
                        label = category?.name ?: "未分类",
                        color = category?.let { parseHexColor(it.color) } ?: Color.Gray
                    )
                }

            _scatterData.value = ScatterData(
                points = points,
                xAxisLabel = "日期",
                yAxisLabel = "金额",
                title = "支出分布散点图"
            )
        } catch (e: Exception) {
            _scatterData.value = null
        }
    }

    /**
     * 加载对比数据
     */
    private fun loadCompareData() {
        viewModelScope.launch {
            try {
                val filter = _filterState.value
                val (currentStart, currentEnd) = getDateRange()

                val (compareStart, compareEnd) = when (filter.compareMode) {
                    CompareMode.PREVIOUS_PERIOD -> {
                        val duration = currentEnd - currentStart
                        Pair(currentStart - duration - 1, currentStart - 1)
                    }
                    CompareMode.SAME_PERIOD_LAST_YEAR -> {
                        val startDate = LocalDate.ofEpochDay(currentStart.toLong())
                        val endDate = LocalDate.ofEpochDay(currentEnd.toLong())
                        Pair(
                            startDate.minusYears(1).toEpochDay().toInt(),
                            endDate.minusYears(1).toEpochDay().toInt()
                        )
                    }
                    CompareMode.CUSTOM -> {
                        filter.compareStartDate?.toEpochDay()?.toInt()?.let { start ->
                            filter.compareEndDate?.toEpochDay()?.toInt()?.let { end ->
                                Pair(start, end)
                            }
                        } ?: return@launch
                    }
                    else -> return@launch
                }

                // 获取当期数据
                val currentExpense = dailyTransactionRepository
                    .getCategoryTotalsInRange(currentStart, currentEnd, "EXPENSE")
                    .first()
                val currentIncome = dailyTransactionRepository
                    .getCategoryTotalsInRange(currentStart, currentEnd, "INCOME")
                    .first()

                // 获取对比期数据
                val compareExpense = dailyTransactionRepository
                    .getCategoryTotalsInRange(compareStart, compareEnd, "EXPENSE")
                    .first()
                val compareIncome = dailyTransactionRepository
                    .getCategoryTotalsInRange(compareStart, compareEnd, "INCOME")
                    .first()

                val categoryMap = (_incomeCategories.value + _expenseCategories.value).associateBy { it.id }

                // 构建对比数据
                val allCategoryIds = (currentExpense.mapNotNull { it.fieldId } +
                    compareExpense.mapNotNull { it.fieldId }).toSet()

                val items = allCategoryIds.mapNotNull { fieldId ->
                    val category = categoryMap[fieldId] ?: return@mapNotNull null
                    val currentVal = currentExpense.find { it.fieldId == fieldId }?.total ?: 0.0
                    val compareVal = compareExpense.find { it.fieldId == fieldId }?.total ?: 0.0
                    val change = currentVal - compareVal
                    val changePercent = if (compareVal > 0) {
                        ((currentVal - compareVal) / compareVal * 100).toFloat()
                    } else if (currentVal > 0) 100f else 0f

                    CompareDataItem(
                        label = category.name,
                        currentValue = currentVal,
                        compareValue = compareVal,
                        changeValue = change,
                        changePercentage = changePercent,
                        color = parseHexColor(category.color)
                    )
                }

                val totalCurrent = currentExpense.sumOf { it.total }
                val totalCompare = compareExpense.sumOf { it.total }
                val totalChange = if (totalCompare > 0) {
                    ((totalCurrent - totalCompare) / totalCompare * 100).toFloat()
                } else 0f

                _compareData.value = CompareData(
                    items = items.sortedByDescending { kotlin.math.abs(it.changeValue) },
                    currentPeriodLabel = "本期",
                    comparePeriodLabel = when (filter.compareMode) {
                        CompareMode.PREVIOUS_PERIOD -> "上期"
                        CompareMode.SAME_PERIOD_LAST_YEAR -> "去年同期"
                        else -> "对比期"
                    },
                    totalCurrentValue = totalCurrent,
                    totalCompareValue = totalCompare,
                    totalChangePercentage = totalChange
                )
            } catch (e: Exception) {
                _compareData.value = null
            }
        }
    }

    /**
     * 加载聚合数据
     */
    private fun loadAggregateData() {
        viewModelScope.launch {
            try {
                val (startDate, endDate) = getDateRange()
                val granularity = _filterState.value.aggregateGranularity

                val transactions = dailyTransactionRepository.getByDateRange(startDate, endDate).first()

                val items = when (granularity) {
                    AggregateGranularity.DAY -> {
                        transactions.groupBy { it.date }.map { (date, txs) ->
                            val dateStr = LocalDate.ofEpochDay(date.toLong())
                                .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                            AggregateDataItem(
                                periodLabel = dateStr,
                                periodStart = date,
                                periodEnd = date,
                                value = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount },
                                count = txs.size,
                                avgValue = txs.map { it.amount }.average()
                            )
                        }
                    }
                    AggregateGranularity.WEEK -> {
                        val weekFields = WeekFields.of(Locale.getDefault())
                        transactions.groupBy { tx ->
                            val date = LocalDate.ofEpochDay(tx.date.toLong())
                            date.get(weekFields.weekOfYear())
                        }.map { (week, txs) ->
                            AggregateDataItem(
                                periodLabel = "第${week}周",
                                periodStart = txs.minOf { it.date },
                                periodEnd = txs.maxOf { it.date },
                                value = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount },
                                count = txs.size
                            )
                        }
                    }
                    AggregateGranularity.MONTH -> {
                        transactions.groupBy { tx ->
                            val date = LocalDate.ofEpochDay(tx.date.toLong())
                            date.year * 100 + date.monthValue
                        }.map { (yearMonth, txs) ->
                            val year = yearMonth / 100
                            val month = yearMonth % 100
                            AggregateDataItem(
                                periodLabel = "${year}年${month}月",
                                periodStart = txs.minOf { it.date },
                                periodEnd = txs.maxOf { it.date },
                                value = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount },
                                count = txs.size
                            )
                        }
                    }
                    AggregateGranularity.QUARTER -> {
                        transactions.groupBy { tx ->
                            val date = LocalDate.ofEpochDay(tx.date.toLong())
                            date.year * 10 + (date.monthValue - 1) / 3 + 1
                        }.map { (yearQuarter, txs) ->
                            val year = yearQuarter / 10
                            val quarter = yearQuarter % 10
                            AggregateDataItem(
                                periodLabel = "${year}年Q${quarter}",
                                periodStart = txs.minOf { it.date },
                                periodEnd = txs.maxOf { it.date },
                                value = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount },
                                count = txs.size
                            )
                        }
                    }
                    AggregateGranularity.YEAR -> {
                        transactions.groupBy { tx ->
                            LocalDate.ofEpochDay(tx.date.toLong()).year
                        }.map { (year, txs) ->
                            AggregateDataItem(
                                periodLabel = "${year}年",
                                periodStart = txs.minOf { it.date },
                                periodEnd = txs.maxOf { it.date },
                                value = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount },
                                count = txs.size
                            )
                        }
                    }
                }.sortedBy { it.periodStart }

                _aggregateData.value = AggregateData(
                    items = items,
                    granularity = granularity,
                    totalValue = items.sumOf { it.value },
                    avgValue = if (items.isNotEmpty()) items.sumOf { it.value } / items.size else 0.0
                )
            } catch (e: Exception) {
                _aggregateData.value = null
            }
        }
    }

    /**
     * 加载数据摘要
     */
    private suspend fun loadDataSummary() {
        try {
            val (startDate, endDate) = getDateRange()
            val today = LocalDate.now().toEpochDay().toInt()
            val yearMonth = LocalDate.now().let { it.year * 100 + it.monthValue }

            val totalIncome = monthlyIncomeExpenseRepository.getTotalIncome(yearMonth)
            val totalExpense = monthlyIncomeExpenseRepository.getTotalExpense(yearMonth)

            val todoStats = todoRepository.getTodayStats(today)
            val habits = habitRepository.getActiveHabits().first()
            val habitCheckedIn = habits.count { habit ->
                habitRepository.getRecordByHabitAndDate(habit.id, today)?.isCompleted == true
            }

            val timeRecords = timeTrackRepository.getRecordsByDateRange(startDate, endDate).first()
            val diaries = diaryRepository.getByDateRange(startDate, endDate).first()

            val goals = goalRepository.getAllGoals().first()
            val completedGoals = goals.count { it.status == "COMPLETED" }
            val totalGoals = goals.size

            val savingsPlans = savingsPlanRepository.getActivePlans().first()
            val savingsProgress = if (savingsPlans.isNotEmpty()) {
                savingsPlans.map {
                    if (it.targetAmount > 0) (it.currentAmount / it.targetAmount).toFloat() else 0f
                }.average().toFloat()
            } else 0f

            _dataSummary.value = DataSummary(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = totalIncome - totalExpense,
                todoCompletionRate = if (todoStats.total > 0) todoStats.completed.toFloat() / todoStats.total else 0f,
                habitCompletionRate = if (habits.isNotEmpty()) habitCheckedIn.toFloat() / habits.size else 0f,
                totalFocusMinutes = timeRecords.sumOf { it.durationMinutes },
                avgMoodScore = if (diaries.isNotEmpty()) diaries.mapNotNull { it.moodScore }.average().toFloat() else 0f,
                goalCompletionRate = if (totalGoals > 0) completedGoals.toFloat() / totalGoals else 0f,
                savingsProgress = savingsProgress,
                healthScore = calculateHealthScore()
            )
        } catch (e: Exception) {
            _dataSummary.value = null
        }
    }

    /**
     * 计算综合健康分
     */
    private suspend fun calculateHealthScore(): Int {
        return try {
            val summary = _dataSummary.value ?: return 50

            var score = 0

            // 财务健康 (25分)
            val financeScore = if (summary.netBalance >= 0) 25 else
                (25 * (1 + summary.netBalance / (summary.totalExpense.coerceAtLeast(1.0)))).toInt().coerceIn(0, 25)
            score += financeScore

            // 任务效率 (20分)
            score += (summary.todoCompletionRate * 20).toInt()

            // 习惯坚持 (20分)
            score += (summary.habitCompletionRate * 20).toInt()

            // 时间管理 (15分)
            val avgFocusPerDay = summary.totalFocusMinutes.toFloat() / 30  // 假设30天
            score += (minOf(avgFocusPerDay / 120f, 1f) * 15).toInt()  // 2小时为满分

            // 心情状态 (10分)
            score += (summary.avgMoodScore / 5f * 10).toInt()

            // 目标进度 (10分)
            score += (summary.goalCompletionRate * 10).toInt()

            score.coerceIn(0, 100)
        } catch (e: Exception) {
            50
        }
    }

    /**
     * 生成数据洞察
     */
    private suspend fun generateDataInsights() {
        try {
            val insights = mutableListOf<DataInsight>()
            val summary = _dataSummary.value
            val (startDate, endDate) = getDateRange()

            // 财务洞察
            if (summary != null && summary.netBalance < 0) {
                insights.add(DataInsight(
                    id = "finance_deficit",
                    title = "财务预警",
                    description = "本月支出超过收入¥${String.format("%.0f", -summary.netBalance)}",
                    type = InsightType.WARNING,
                    importance = InsightImportance.HIGH,
                    relatedModule = DataModule.FINANCE,
                    actionSuggestion = "建议检查支出情况，控制非必要开支"
                ))
            }

            // 习惯洞察
            if (summary != null && summary.habitCompletionRate < 0.5f) {
                insights.add(DataInsight(
                    id = "habit_low",
                    title = "习惯坚持率偏低",
                    description = "当前习惯完成率仅${(summary.habitCompletionRate * 100).toInt()}%",
                    type = InsightType.SUGGESTION,
                    importance = InsightImportance.MEDIUM,
                    relatedModule = DataModule.HABIT,
                    actionSuggestion = "尝试减少习惯数量，专注培养核心习惯"
                ))
            }

            // 支出趋势洞察
            val expenseRanking = _expenseRanking.value
            if (expenseRanking.isNotEmpty()) {
                val topCategory = expenseRanking.first()
                if (topCategory.percentage > 40) {
                    insights.add(DataInsight(
                        id = "expense_concentrated",
                        title = "支出集中",
                        description = "${topCategory.name}占比${String.format("%.0f", topCategory.percentage)}%，支出较为集中",
                        type = InsightType.TREND,
                        importance = InsightImportance.LOW,
                        relatedModule = DataModule.FINANCE
                    ))
                }
            }

            // 目标洞察
            val goals = goalRepository.getAllGoals().first()
            val overdueGoals = goals.filter {
                it.status == "ACTIVE" &&
                it.endDate != null &&
                it.endDate < LocalDate.now().toEpochDay().toInt()
            }
            if (overdueGoals.isNotEmpty()) {
                insights.add(DataInsight(
                    id = "goals_overdue",
                    title = "目标逾期提醒",
                    description = "有${overdueGoals.size}个目标已超过截止日期",
                    type = InsightType.WARNING,
                    importance = InsightImportance.HIGH,
                    relatedModule = DataModule.GOAL,
                    actionSuggestion = "建议检查逾期目标，调整计划或标记放弃"
                ))
            }

            // 成就洞察
            if (summary != null && summary.healthScore >= 80) {
                insights.add(DataInsight(
                    id = "health_score_high",
                    title = "生活质量优秀",
                    description = "综合健康分达到${summary.healthScore}分，继续保持！",
                    type = InsightType.ACHIEVEMENT,
                    importance = InsightImportance.LOW,
                    relatedModule = DataModule.ALL
                ))
            }

            _dataInsights.value = insights.sortedByDescending {
                when (it.importance) {
                    InsightImportance.HIGH -> 3
                    InsightImportance.MEDIUM -> 2
                    InsightImportance.LOW -> 1
                }
            }
        } catch (e: Exception) {
            _dataInsights.value = emptyList()
        }
    }

    /**
     * 更新导出配置
     */
    fun updateExportConfig(config: ExportConfig) {
        _exportConfig.value = config
    }

    /**
     * 导出数据
     */
    fun exportData(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                // 实际导出逻辑需要根据平台实现
                // 这里只是模拟导出过程
                delay(1000)
                onComplete(true, "导出成功")
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * 保存自定义视图配置
     */
    fun saveCustomView(name: String) {
        val config = CustomViewConfig(
            id = System.currentTimeMillis(),
            name = name,
            modules = _filterState.value.selectedModules,
            chartTypes = listOf(_filterState.value.chartType),
            layout = ViewLayout.SINGLE,
            filterState = _filterState.value
        )
        _customViews.value = _customViews.value + config
    }

    /**
     * 加载自定义视图配置
     */
    fun loadCustomView(config: CustomViewConfig) {
        _filterState.value = config.filterState
        loadDataDebounced()
        loadAdvancedChartData()
    }

    /**
     * 删除自定义视图配置
     */
    fun deleteCustomView(id: Long) {
        _customViews.value = _customViews.value.filter { it.id != id }
    }
}
