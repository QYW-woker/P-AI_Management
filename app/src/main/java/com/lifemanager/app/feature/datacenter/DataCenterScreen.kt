package com.lifemanager.app.feature.datacenter

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.feature.datacenter.component.DateRangeSelector
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

    // Tab配置
    val tabs = listOf("总览", "财务", "效率", "生活")

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
                    }
                }
            }
        }
    }
}
