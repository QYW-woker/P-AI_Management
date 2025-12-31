package com.lifemanager.app.feature.datacenter

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import com.lifemanager.app.feature.datacenter.component.*
import com.lifemanager.app.feature.datacenter.model.*
import com.lifemanager.app.feature.datacenter.tab.FinanceTab
import com.lifemanager.app.feature.datacenter.tab.LifestyleTab
import com.lifemanager.app.feature.datacenter.tab.OverviewTab
import com.lifemanager.app.feature.datacenter.tab.ProductivityTab

/**
 * 数据中心页面
 *
 * 展示用户的多维度数据统计和图表分析
 * 支持：
 * - 可选字段筛选（来源于收入/支出分类名称）
 * - 自定义时间筛选（支持日历选择器）
 * - 多种图表展示（折线图/饼状图/趋势图等）
 * - 多模块数据：支出、收入、待办、计时、打卡、日记、存钱
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    // 数据状态
    val overviewStats by viewModel.overviewStats.collectAsState()
    val financeChartData by viewModel.financeChartData.collectAsState()
    val productivityChartData by viewModel.productivityChartData.collectAsState()
    val lifestyleChartData by viewModel.lifestyleChartData.collectAsState()

    // 分类数据
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val selectedIncomeIds by viewModel.selectedIncomeIds.collectAsState()
    val selectedExpenseIds by viewModel.selectedExpenseIds.collectAsState()

    // 预算数据
    val budgetAnalysis by viewModel.budgetAnalysis.collectAsState()
    val budgetAIAdvice by viewModel.budgetAIAdvice.collectAsState()

    // 资产趋势数据
    val assetTrendData by viewModel.assetTrendData.collectAsState()

    // 账单列表
    val billList by viewModel.billList.collectAsState()

    // 支出排名
    val expenseRanking by viewModel.expenseRanking.collectAsState()

    // AI分析数据
    val overallHealthScore by viewModel.overallHealthScore.collectAsState()
    val isAIAnalyzing by viewModel.isAIAnalyzing.collectAsState()
    val financeAIAnalysis by viewModel.financeAnalysis.collectAsState()
    val goalAIAnalysis by viewModel.goalAnalysis.collectAsState()
    val habitAIAnalysis by viewModel.habitAnalysis.collectAsState()

    // 高级筛选数据
    val filterPresets by viewModel.filterPresets.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val isAdvancedFilterExpanded by viewModel.isAdvancedFilterExpanded.collectAsState()
    val compareData by viewModel.compareData.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val radarData by viewModel.radarData.collectAsState()
    val treemapData by viewModel.treemapData.collectAsState()
    val waterfallData by viewModel.waterfallData.collectAsState()
    val funnelData by viewModel.funnelData.collectAsState()
    val dataInsights by viewModel.dataInsights.collectAsState()

    // Tab配置
    val tabs = listOf("总览", "财务", "效率", "生活", "分析")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据中心") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 日期范围选择器
            DateRangeSelector(
                selectedType = filterState.dateRangeType,
                customStartDate = filterState.customStartDate,
                customEndDate = filterState.customEndDate,
                onTypeChange = { type ->
                    viewModel.updateDateRangeType(type)
                },
                onCustomRangeChange = { start, end ->
                    viewModel.updateCustomDateRange(start, end)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 高级筛选面板
            AdvancedFilterPanel(
                filterState = filterState,
                isExpanded = isAdvancedFilterExpanded,
                filterPresets = filterPresets,
                selectedPreset = selectedPreset,
                onToggleExpand = { viewModel.toggleAdvancedFilter() },
                onApplyPreset = { viewModel.applyPreset(it) },
                onSavePreset = { name, desc -> viewModel.saveCustomPreset(name, desc) },
                onDeletePreset = { viewModel.deletePreset(it) },
                onUpdateModules = { viewModel.updateSelectedModules(it) },
                onUpdateCompareMode = { viewModel.updateCompareMode(it) },
                onUpdateGranularity = { viewModel.updateAggregateGranularity(it) },
                onUpdateSortMode = { viewModel.updateSortMode(it) },
                onUpdateAmountRange = { min, max -> viewModel.updateAmountRange(min, max) },
                onUpdateSearchKeyword = { viewModel.updateSearchKeyword(it) },
                onUpdateShowTopN = { viewModel.updateShowTopN(it) },
                onResetFilters = { viewModel.resetFilters() }
            )

            // Tab导航
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) }
                    )
                }
            }

            // 内容区域
            when (uiState) {
                is DataCenterUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DataCenterUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (uiState as DataCenterUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                is DataCenterUiState.Success -> {
                    // 根据选中的Tab显示对应内容
                    when (selectedTab) {
                        0 -> OverviewTab(
                            overviewStats = overviewStats,
                            financeData = financeChartData,
                            productivityData = productivityChartData,
                            lifestyleData = lifestyleChartData,
                            assetTrendData = assetTrendData,
                            overallHealthScore = overallHealthScore,
                            isAIAnalyzing = isAIAnalyzing,
                            financeAnalysis = financeAIAnalysis,
                            goalAnalysis = goalAIAnalysis,
                            habitAnalysis = habitAIAnalysis,
                            onRefreshAI = { viewModel.refreshAIAnalysis() }
                        )

                        1 -> FinanceTab(
                            financeData = financeChartData,
                            incomeCategories = incomeCategories,
                            expenseCategories = expenseCategories,
                            selectedIncomeIds = selectedIncomeIds,
                            selectedExpenseIds = selectedExpenseIds,
                            onIncomeSelectionChange = { viewModel.updateIncomeSelection(it) },
                            onExpenseSelectionChange = { viewModel.updateExpenseSelection(it) },
                            chartType = filterState.chartType,
                            onChartTypeChange = { viewModel.updateChartType(it) },
                            budgetAnalysis = budgetAnalysis,
                            budgetAIAdvice = budgetAIAdvice,
                            billList = billList,
                            expenseRanking = expenseRanking
                        )

                        2 -> ProductivityTab(
                            data = productivityChartData
                        )

                        3 -> LifestyleTab(
                            data = lifestyleChartData
                        )

                        4 -> AdvancedAnalysisTab(
                            filterState = filterState,
                            compareData = compareData,
                            heatmapData = heatmapData,
                            radarData = radarData,
                            treemapData = treemapData,
                            waterfallData = waterfallData,
                            funnelData = funnelData,
                            dataInsights = dataInsights,
                            onChartTypeChange = { viewModel.updateChartType(it) },
                            onLoadAdvancedData = { viewModel.loadAdvancedChartData() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 高级分析标签页
 */
@Composable
private fun AdvancedAnalysisTab(
    filterState: DataCenterFilterState,
    compareData: CompareData?,
    heatmapData: HeatmapData?,
    radarData: RadarChartData?,
    treemapData: TreemapData?,
    waterfallData: WaterfallData?,
    funnelData: FunnelData?,
    dataInsights: List<DataInsight>,
    onChartTypeChange: (ChartType) -> Unit,
    onLoadAdvancedData: () -> Unit
) {
    // 首次加载时触发数据加载
    LaunchedEffect(Unit) {
        onLoadAdvancedData()
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 图表类型选择器
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "选择图表类型",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GroupedChartTypeSelector(
                        selected = filterState.chartType,
                        onSelect = {
                            onChartTypeChange(it)
                            onLoadAdvancedData()
                        }
                    )
                }
            }
        }

        // 数据洞察
        if (dataInsights.isNotEmpty()) {
            item {
                DataInsightsCard(insights = dataInsights)
            }
        }

        // 根据选中的图表类型显示对应图表
        item {
            when (filterState.chartType) {
                ChartType.HEATMAP -> {
                    heatmapData?.let { HeatmapChartView(data = it) }
                        ?: EmptyChartCard(chartType = "热力图")
                }
                ChartType.RADAR -> {
                    radarData?.let { RadarChartView(data = it) }
                        ?: EmptyChartCard(chartType = "雷达图")
                }
                ChartType.TREEMAP -> {
                    treemapData?.let { TreemapChartView(data = it) }
                        ?: EmptyChartCard(chartType = "树状图")
                }
                ChartType.WATERFALL -> {
                    waterfallData?.let { WaterfallChartView(data = it) }
                        ?: EmptyChartCard(chartType = "瀑布图")
                }
                ChartType.FUNNEL -> {
                    funnelData?.let { FunnelChartView(data = it) }
                        ?: EmptyChartCard(chartType = "漏斗图")
                }
                else -> {
                    // 其他图表类型的提示
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${filterState.chartType.displayName}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "此图表类型可在财务标签页中查看详细数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 对比分析
        if (compareData != null && filterState.compareMode != CompareMode.NONE) {
            item {
                CompareDataCard(data = compareData)
            }
        }

        // 多维度雷达图（始终显示作为生活质量概览）
        if (filterState.chartType != ChartType.RADAR && radarData != null) {
            item {
                RadarChartView(data = radarData)
            }
        }
    }
}

@Composable
private fun EmptyChartCard(chartType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "暂无${chartType}数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "请确保有相关数据后再查看",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
