package com.lifemanager.app.feature.finance.search

import com.lifemanager.app.core.database.entity.TransactionFilter
import com.lifemanager.app.core.database.entity.TransactionSortBy
import java.time.LocalDate

/**
 * 搜索界面状态
 */
data class TransactionSearchState(
    // 基础筛选
    val keyword: String = "",
    val selectedTypes: List<String> = emptyList(),

    // 日期范围
    val dateRangeType: DateRangeType = DateRangeType.ALL,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,

    // 金额范围
    val minAmount: Double? = null,
    val maxAmount: Double? = null,

    // 分类筛选
    val selectedCategoryIds: List<Long> = emptyList(),

    // 账户筛选
    val selectedAccountIds: List<Long> = emptyList(),

    // 标签筛选
    val selectedTags: List<String> = emptyList(),

    // 来源筛选
    val selectedSources: List<String> = emptyList(),

    // 其他筛选
    val hasAttachments: Boolean? = null,
    val hasRefund: Boolean? = null,

    // 排序
    val sortBy: TransactionSortBy = TransactionSortBy.DATE_DESC,

    // 界面状态
    val isExpanded: Boolean = false,
    val activeFilterCount: Int = 0
) {
    /**
     * 转换为DAO查询过滤器
     */
    fun toFilter(): TransactionFilter {
        val (startDate, endDate) = getDateRange()
        return TransactionFilter(
            startDate = startDate,
            endDate = endDate,
            minAmount = minAmount,
            maxAmount = maxAmount,
            types = selectedTypes.takeIf { it.isNotEmpty() },
            categoryIds = selectedCategoryIds.takeIf { it.isNotEmpty() },
            accountIds = selectedAccountIds.takeIf { it.isNotEmpty() },
            keyword = keyword.takeIf { it.isNotBlank() },
            tags = selectedTags.takeIf { it.isNotEmpty() },
            sources = selectedSources.takeIf { it.isNotEmpty() },
            hasAttachments = hasAttachments,
            hasRefund = hasRefund,
            sortBy = sortBy
        )
    }

    private fun getDateRange(): Pair<Int?, Int?> {
        val today = LocalDate.now()
        return when (dateRangeType) {
            DateRangeType.ALL -> null to null
            DateRangeType.TODAY -> today.toEpochDay().toInt() to today.toEpochDay().toInt()
            DateRangeType.YESTERDAY -> today.minusDays(1).toEpochDay().toInt() to today.minusDays(1).toEpochDay().toInt()
            DateRangeType.THIS_WEEK -> today.minusDays(today.dayOfWeek.value.toLong() - 1).toEpochDay().toInt() to today.toEpochDay().toInt()
            DateRangeType.LAST_WEEK -> {
                val lastWeekEnd = today.minusDays(today.dayOfWeek.value.toLong())
                val lastWeekStart = lastWeekEnd.minusDays(6)
                lastWeekStart.toEpochDay().toInt() to lastWeekEnd.toEpochDay().toInt()
            }
            DateRangeType.THIS_MONTH -> today.withDayOfMonth(1).toEpochDay().toInt() to today.toEpochDay().toInt()
            DateRangeType.LAST_MONTH -> {
                val lastMonth = today.minusMonths(1)
                lastMonth.withDayOfMonth(1).toEpochDay().toInt() to lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).toEpochDay().toInt()
            }
            DateRangeType.LAST_3_MONTHS -> today.minusMonths(3).toEpochDay().toInt() to today.toEpochDay().toInt()
            DateRangeType.THIS_YEAR -> today.withDayOfYear(1).toEpochDay().toInt() to today.toEpochDay().toInt()
            DateRangeType.CUSTOM -> {
                customStartDate?.toEpochDay()?.toInt() to customEndDate?.toEpochDay()?.toInt()
            }
        }
    }

    fun calculateActiveFilterCount(): Int {
        var count = 0
        if (keyword.isNotBlank()) count++
        if (selectedTypes.isNotEmpty()) count++
        if (dateRangeType != DateRangeType.ALL) count++
        if (minAmount != null || maxAmount != null) count++
        if (selectedCategoryIds.isNotEmpty()) count++
        if (selectedAccountIds.isNotEmpty()) count++
        if (selectedTags.isNotEmpty()) count++
        if (selectedSources.isNotEmpty()) count++
        if (hasAttachments != null) count++
        if (hasRefund != null) count++
        return count
    }
}

/**
 * 日期范围类型
 */
enum class DateRangeType(val displayName: String) {
    ALL("全部时间"),
    TODAY("今天"),
    YESTERDAY("昨天"),
    THIS_WEEK("本周"),
    LAST_WEEK("上周"),
    THIS_MONTH("本月"),
    LAST_MONTH("上月"),
    LAST_3_MONTHS("近三个月"),
    THIS_YEAR("今年"),
    CUSTOM("自定义")
}

/**
 * 快速金额筛选
 */
object AmountPresets {
    val presets = listOf(
        "10元以下" to (null to 10.0),
        "10-50元" to (10.0 to 50.0),
        "50-100元" to (50.0 to 100.0),
        "100-500元" to (100.0 to 500.0),
        "500-1000元" to (500.0 to 1000.0),
        "1000元以上" to (1000.0 to null)
    )
}

/**
 * 搜索结果
 */
data class SearchResult(
    val totalCount: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val transactions: List<TransactionSearchItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
)

/**
 * 搜索结果项
 */
data class TransactionSearchItem(
    val id: Long,
    val type: String,
    val amount: Double,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val accountName: String?,
    val date: Int,
    val time: String,
    val note: String,
    val hasAttachments: Boolean,
    val hasSplits: Boolean,
    val hasRefund: Boolean,
    val merchantName: String? = null
)

/**
 * 搜索历史
 */
data class SearchHistory(
    val id: Long = 0,
    val keyword: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 快捷筛选标签
 */
data class QuickFilterChip(
    val id: String,
    val label: String,
    val filter: TransactionSearchState.() -> TransactionSearchState,
    val isSelected: Boolean = false
)

/**
 * 预设快捷筛选
 */
object QuickFilters {
    fun getDefaultFilters(): List<QuickFilterChip> = listOf(
        QuickFilterChip(
            id = "today",
            label = "今日",
            filter = { copy(dateRangeType = DateRangeType.TODAY) }
        ),
        QuickFilterChip(
            id = "this_week",
            label = "本周",
            filter = { copy(dateRangeType = DateRangeType.THIS_WEEK) }
        ),
        QuickFilterChip(
            id = "this_month",
            label = "本月",
            filter = { copy(dateRangeType = DateRangeType.THIS_MONTH) }
        ),
        QuickFilterChip(
            id = "expense_only",
            label = "仅支出",
            filter = { copy(selectedTypes = listOf("EXPENSE")) }
        ),
        QuickFilterChip(
            id = "income_only",
            label = "仅收入",
            filter = { copy(selectedTypes = listOf("INCOME")) }
        ),
        QuickFilterChip(
            id = "large_expense",
            label = "大额消费",
            filter = { copy(minAmount = 500.0, selectedTypes = listOf("EXPENSE")) }
        ),
        QuickFilterChip(
            id = "has_attachments",
            label = "有附件",
            filter = { copy(hasAttachments = true) }
        )
    )
}
