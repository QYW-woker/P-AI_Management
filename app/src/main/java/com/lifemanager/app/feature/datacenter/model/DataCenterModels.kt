package com.lifemanager.app.feature.datacenter.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

/**
 * 日期范围类型枚举
 */
enum class DateRangeType(val displayName: String) {
    WEEK("本周"),
    MONTH("本月"),
    YEAR("本年"),
    CUSTOM("自定义"),
    ALL("全部")
}

/**
 * 图表类型枚举
 */
enum class ChartType(val displayName: String) {
    PIE("饼图"),
    BAR("柱状图"),
    LINE("折线图"),
    STACKED_BAR("堆叠柱状图"),
    AREA("面积图"),
    RADAR("雷达图"),
    HEATMAP("热力图"),
    TREEMAP("树状图"),
    WATERFALL("瀑布图"),
    DONUT("环形图"),
    SCATTER("散点图"),
    FUNNEL("漏斗图")
}

/**
 * 可视化模块类型
 */
enum class DataModule(val displayName: String, val description: String) {
    FINANCE("财务", "收入、支出、存钱"),
    TODO("待办", "任务完成情况"),
    HABIT("习惯", "打卡统计"),
    TIME("时间", "专注时长"),
    DIARY("日记", "心情记录"),
    GOAL("目标", "目标进度"),
    ASSET("资产", "资产负债"),
    ALL("全部", "综合数据")
}

/**
 * 筛选比较模式
 */
enum class CompareMode(val displayName: String) {
    NONE("不比较"),
    PREVIOUS_PERIOD("环比（上一周期）"),
    SAME_PERIOD_LAST_YEAR("同比（去年同期）"),
    CUSTOM("自定义比较")
}

/**
 * 聚合粒度
 */
enum class AggregateGranularity(val displayName: String) {
    DAY("按天"),
    WEEK("按周"),
    MONTH("按月"),
    QUARTER("按季度"),
    YEAR("按年")
}

/**
 * 排序方式
 */
enum class SortMode(val displayName: String) {
    VALUE_DESC("金额从高到低"),
    VALUE_ASC("金额从低到高"),
    DATE_DESC("日期从新到旧"),
    DATE_ASC("日期从旧到新"),
    NAME_ASC("名称A-Z"),
    NAME_DESC("名称Z-A"),
    PERCENTAGE_DESC("占比从高到低")
}

/**
 * 数据中心筛选状态
 */
data class DataCenterFilterState(
    val dateRangeType: DateRangeType = DateRangeType.MONTH,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val selectedCategoryIds: Set<Long> = emptySet(),  // 空表示全选
    val chartType: ChartType = ChartType.PIE,
    // 高级筛选
    val selectedModules: Set<DataModule> = setOf(DataModule.ALL),
    val compareMode: CompareMode = CompareMode.NONE,
    val compareStartDate: LocalDate? = null,
    val compareEndDate: LocalDate? = null,
    val aggregateGranularity: AggregateGranularity = AggregateGranularity.DAY,
    val sortMode: SortMode = SortMode.VALUE_DESC,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val searchKeyword: String = "",
    val showTopN: Int = 10  // 显示前N项
)

/**
 * 筛选预设
 */
data class FilterPreset(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val filterState: DataCenterFilterState,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)

/**
 * 预定义的筛选预设模板
 */
val defaultFilterPresets = listOf(
    FilterPreset(
        id = 1,
        name = "本月财务概览",
        description = "查看本月收支分布",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.MONTH,
            selectedModules = setOf(DataModule.FINANCE),
            chartType = ChartType.PIE
        ),
        isDefault = true
    ),
    FilterPreset(
        id = 2,
        name = "年度支出趋势",
        description = "本年每月支出变化",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.YEAR,
            selectedModules = setOf(DataModule.FINANCE),
            chartType = ChartType.LINE,
            aggregateGranularity = AggregateGranularity.MONTH
        ),
        isDefault = true
    ),
    FilterPreset(
        id = 3,
        name = "习惯完成热力图",
        description = "查看习惯打卡热力分布",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.MONTH,
            selectedModules = setOf(DataModule.HABIT),
            chartType = ChartType.HEATMAP
        ),
        isDefault = true
    ),
    FilterPreset(
        id = 4,
        name = "时间分配雷达图",
        description = "各类活动时间分配",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.WEEK,
            selectedModules = setOf(DataModule.TIME),
            chartType = ChartType.RADAR
        ),
        isDefault = true
    ),
    FilterPreset(
        id = 5,
        name = "月度环比分析",
        description = "与上月数据对比",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.MONTH,
            selectedModules = setOf(DataModule.ALL),
            compareMode = CompareMode.PREVIOUS_PERIOD
        ),
        isDefault = true
    ),
    FilterPreset(
        id = 6,
        name = "年度同比分析",
        description = "与去年同期对比",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.MONTH,
            selectedModules = setOf(DataModule.FINANCE),
            compareMode = CompareMode.SAME_PERIOD_LAST_YEAR
        ),
        isDefault = true
    ),
    FilterPreset(
        id = 7,
        name = "大额支出分析",
        description = "筛选100元以上支出",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.MONTH,
            selectedModules = setOf(DataModule.FINANCE),
            minAmount = 100.0,
            sortMode = SortMode.VALUE_DESC
        ),
        isDefault = true
    ),
    FilterPreset(
        id = 8,
        name = "目标完成漏斗",
        description = "目标各阶段完成情况",
        filterState = DataCenterFilterState(
            dateRangeType = DateRangeType.YEAR,
            selectedModules = setOf(DataModule.GOAL),
            chartType = ChartType.FUNNEL
        ),
        isDefault = true
    )
)

/**
 * 分类图表数据项
 */
data class CategoryChartItem(
    val fieldId: Long,
    val name: String,
    val value: Double,
    val percentage: Float,
    val color: Color,
    val iconName: String
)

/**
 * 每日财务趋势数据
 */
data class DailyFinanceTrend(
    val date: Int,  // epochDay
    val income: Double,
    val expense: Double
)

/**
 * 每月财务趋势数据
 */
data class MonthlyFinanceTrend(
    val yearMonth: Int,
    val income: Double,
    val expense: Double
)

/**
 * 财务图表数据
 */
data class FinanceChartData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val incomeByCategory: List<CategoryChartItem> = emptyList(),
    val expenseByCategory: List<CategoryChartItem> = emptyList(),
    val dailyTrend: List<DailyFinanceTrend> = emptyList(),
    val monthlyTrend: List<MonthlyFinanceTrend> = emptyList()
)

/**
 * 每日计数数据
 */
data class DailyCount(
    val date: Int,  // epochDay
    val count: Int
)

/**
 * 待办图表数据
 */
data class TodoChartData(
    val completed: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    val completionRate: Float = 0f,
    val dailyCompletions: List<DailyCount> = emptyList()
)

/**
 * 习惯完成项
 */
data class HabitCompletionItem(
    val habitId: Long,
    val name: String,
    val completionRate: Float,
    val color: Color
)

/**
 * 习惯图表数据
 */
data class HabitChartData(
    val activeHabits: Int = 0,
    val todayCheckedIn: Int = 0,
    val overallRate: Float = 0f,
    val habitCompletionRates: List<HabitCompletionItem> = emptyList(),
    val weeklyCheckins: List<DailyCount> = emptyList()
)

/**
 * 每日时长数据
 */
data class DailyDuration(
    val date: Int,  // epochDay
    val minutes: Int
)

/**
 * 时间统计图表数据
 */
data class TimeChartData(
    val totalMinutes: Int = 0,
    val categoryBreakdown: List<CategoryChartItem> = emptyList(),
    val dailyDurations: List<DailyDuration> = emptyList()
)

/**
 * 效率统计图表数据
 */
data class ProductivityChartData(
    val todoStats: TodoChartData = TodoChartData(),
    val habitStats: HabitChartData = HabitChartData(),
    val timeStats: TimeChartData = TimeChartData()
)

/**
 * 心情分布项
 */
data class MoodDistributionItem(
    val moodScore: Int,
    val count: Int,
    val percentage: Float,
    val color: Color
)

/**
 * 每日心情数据
 */
data class DailyMood(
    val date: Int,  // epochDay
    val moodScore: Float
)

/**
 * 日记图表数据
 */
data class DiaryChartData(
    val totalEntries: Int = 0,
    val averageMood: Float = 0f,
    val moodDistribution: List<MoodDistributionItem> = emptyList(),
    val dailyMoodTrend: List<DailyMood> = emptyList()
)

/**
 * 存钱计划进度
 */
data class SavingsPlanProgress(
    val planId: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val progress: Float,
    val color: Color
)

/**
 * 存钱统计图表数据
 */
data class SavingsChartData(
    val activePlans: Int = 0,
    val totalTarget: Double = 0.0,
    val totalCurrent: Double = 0.0,
    val overallProgress: Float = 0f,
    val planProgress: List<SavingsPlanProgress> = emptyList()
)

/**
 * 生活方式图表数据
 */
data class LifestyleChartData(
    val diaryStats: DiaryChartData = DiaryChartData(),
    val savingsStats: SavingsChartData = SavingsChartData()
)

/**
 * 资产趋势数据点
 */
data class AssetTrendPoint(
    val yearMonth: Int,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double
)

/**
 * 资产趋势数据
 */
data class AssetTrendData(
    val trendPoints: List<AssetTrendPoint> = emptyList(),
    val latestNetWorth: Double = 0.0,
    val netWorthChange: Double = 0.0,
    val netWorthChangePercentage: Float = 0f
)

/**
 * 账单查询项
 */
data class BillQueryItem(
    val id: Long,
    val date: Int,
    val type: String,
    val amount: Double,
    val categoryName: String,
    val categoryColor: String,
    val note: String
)

/**
 * 分类排名项
 */
data class CategoryRankingItem(
    val fieldId: Long,
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val rank: Int
)

// ============== 高级图表数据模型 ==============

/**
 * 热力图数据项
 */
data class HeatmapCell(
    val row: Int,       // 行索引（如：周几 0-6）
    val column: Int,    // 列索引（如：周数 0-4）
    val value: Float,   // 值（0-1范围的强度）
    val label: String = "",   // 显示标签
    val date: Int? = null     // 对应日期
)

/**
 * 热力图数据
 */
data class HeatmapData(
    val cells: List<HeatmapCell> = emptyList(),
    val rowLabels: List<String> = emptyList(),  // 行标签（如：周一-周日）
    val columnLabels: List<String> = emptyList(), // 列标签（如：第1周-第5周）
    val title: String = "",
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val colorStart: Color = Color(0xFFE3F2FD),
    val colorEnd: Color = Color(0xFF1565C0)
)

/**
 * 雷达图数据项
 */
data class RadarDataPoint(
    val axis: String,       // 轴名称（如：工作、学习、运动）
    val value: Float,       // 值（0-1范围）
    val maxValue: Float = 1f,
    val color: Color = Color(0xFF2196F3)
)

/**
 * 雷达图数据集
 */
data class RadarDataSet(
    val label: String,
    val points: List<RadarDataPoint>,
    val color: Color,
    val fillAlpha: Float = 0.3f
)

/**
 * 雷达图数据
 */
data class RadarChartData(
    val axes: List<String>,     // 各轴名称
    val dataSets: List<RadarDataSet> = emptyList(),  // 可支持多数据集对比
    val maxValue: Float = 1f
)

/**
 * 树状图节点
 */
data class TreemapNode(
    val id: String,
    val name: String,
    val value: Double,
    val color: Color,
    val percentage: Float = 0f,
    val children: List<TreemapNode> = emptyList()
)

/**
 * 树状图数据
 */
data class TreemapData(
    val rootNodes: List<TreemapNode> = emptyList(),
    val totalValue: Double = 0.0,
    val title: String = ""
)

/**
 * 瀑布图数据项
 */
data class WaterfallItem(
    val label: String,
    val value: Double,
    val isTotal: Boolean = false,  // 是否为合计项
    val isPositive: Boolean = true,
    val startValue: Double = 0.0,  // 起始位置
    val endValue: Double = 0.0     // 结束位置
)

/**
 * 瀑布图数据
 */
data class WaterfallData(
    val items: List<WaterfallItem> = emptyList(),
    val title: String = "",
    val positiveColor: Color = Color(0xFF4CAF50),
    val negativeColor: Color = Color(0xFFF44336),
    val totalColor: Color = Color(0xFF2196F3)
)

/**
 * 漏斗图数据项
 */
data class FunnelItem(
    val label: String,
    val value: Double,
    val percentage: Float,  // 占首项的百分比
    val color: Color,
    val conversionRate: Float = 0f  // 转化率（相对于上一项）
)

/**
 * 漏斗图数据
 */
data class FunnelData(
    val items: List<FunnelItem> = emptyList(),
    val title: String = ""
)

/**
 * 散点图数据点
 */
data class ScatterPoint(
    val x: Float,
    val y: Float,
    val label: String = "",
    val size: Float = 8f,
    val color: Color = Color(0xFF2196F3)
)

/**
 * 散点图数据
 */
data class ScatterData(
    val points: List<ScatterPoint> = emptyList(),
    val xAxisLabel: String = "",
    val yAxisLabel: String = "",
    val title: String = ""
)

/**
 * 对比数据项
 */
data class CompareDataItem(
    val label: String,
    val currentValue: Double,
    val compareValue: Double,
    val changeValue: Double,
    val changePercentage: Float,
    val color: Color = Color.Gray
)

/**
 * 对比数据
 */
data class CompareData(
    val items: List<CompareDataItem> = emptyList(),
    val currentPeriodLabel: String = "本期",
    val comparePeriodLabel: String = "对比期",
    val totalCurrentValue: Double = 0.0,
    val totalCompareValue: Double = 0.0,
    val totalChangePercentage: Float = 0f
)

/**
 * 聚合数据项
 */
data class AggregateDataItem(
    val periodLabel: String,    // 时段标签（如：2024-01、第1周）
    val periodStart: Int,       // 时段开始（epochDay）
    val periodEnd: Int,         // 时段结束
    val value: Double,
    val count: Int = 0,
    val avgValue: Double = 0.0
)

/**
 * 聚合数据
 */
data class AggregateData(
    val items: List<AggregateDataItem> = emptyList(),
    val granularity: AggregateGranularity = AggregateGranularity.DAY,
    val totalValue: Double = 0.0,
    val avgValue: Double = 0.0
)

/**
 * 数据导出格式
 */
enum class ExportFormat(val displayName: String, val extension: String) {
    CSV("CSV表格", "csv"),
    JSON("JSON数据", "json"),
    PDF("PDF报告", "pdf"),
    IMAGE("图片", "png")
}

/**
 * 数据导出配置
 */
data class ExportConfig(
    val format: ExportFormat = ExportFormat.CSV,
    val includeCharts: Boolean = true,
    val includeRawData: Boolean = true,
    val includeSummary: Boolean = true,
    val dateRange: String = "",
    val modules: Set<DataModule> = emptySet()
)

/**
 * 综合数据视图
 * 用于展示多模块数据的汇总
 */
data class UnifiedDataView(
    val financeData: FinanceChartData? = null,
    val productivityData: ProductivityChartData? = null,
    val lifestyleData: LifestyleChartData? = null,
    val assetData: AssetTrendData? = null,
    val compareData: CompareData? = null,
    val heatmapData: HeatmapData? = null,
    val radarData: RadarChartData? = null,
    val treemapData: TreemapData? = null,
    val waterfallData: WaterfallData? = null,
    val funnelData: FunnelData? = null,
    val scatterData: ScatterData? = null,
    val aggregateData: AggregateData? = null
)

/**
 * 数据统计摘要
 */
data class DataSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val todoCompletionRate: Float = 0f,
    val habitCompletionRate: Float = 0f,
    val totalFocusMinutes: Int = 0,
    val avgMoodScore: Float = 0f,
    val goalCompletionRate: Float = 0f,
    val savingsProgress: Float = 0f,
    val netWorthChange: Double = 0.0,
    val healthScore: Int = 0  // 综合健康分
)

/**
 * 数据洞察项
 */
data class DataInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: InsightType,
    val importance: InsightImportance,
    val relatedModule: DataModule,
    val actionSuggestion: String = ""
)

/**
 * 洞察类型
 */
enum class InsightType {
    TREND,          // 趋势洞察
    ANOMALY,        // 异常检测
    ACHIEVEMENT,    // 成就达成
    WARNING,        // 预警提醒
    SUGGESTION      // 建议推荐
}

/**
 * 洞察重要性
 */
enum class InsightImportance {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * 自定义视图配置
 */
data class CustomViewConfig(
    val id: Long = 0,
    val name: String,
    val modules: Set<DataModule>,
    val chartTypes: List<ChartType>,
    val layout: ViewLayout,
    val filterState: DataCenterFilterState,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 视图布局
 */
enum class ViewLayout(val displayName: String, val columns: Int) {
    SINGLE("单列", 1),
    DOUBLE("双列", 2),
    DASHBOARD("仪表盘", 3),
    COMPACT("紧凑", 4)
}
