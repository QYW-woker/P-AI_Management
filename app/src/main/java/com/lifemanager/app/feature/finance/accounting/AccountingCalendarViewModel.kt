package com.lifemanager.app.feature.finance.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.dao.CustomFieldDao
import com.lifemanager.app.core.database.dao.DailyTransactionDao
import com.lifemanager.app.domain.model.DailyTransactionWithCategory
import com.lifemanager.app.domain.model.PeriodStats
import com.lifemanager.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 记账日历ViewModel
 */
@HiltViewModel
class AccountingCalendarViewModel @Inject constructor(
    private val transactionDao: DailyTransactionDao,
    private val customFieldDao: CustomFieldDao
) : ViewModel() {

    // 当前年月（YYYYMM格式）
    private val _currentYearMonth = MutableStateFlow(
        YearMonth.now().let { it.year * 100 + it.monthValue }
    )
    val currentYearMonth: StateFlow<Int> = _currentYearMonth.asStateFlow()

    // 选中的日期（epochDay）
    private val _selectedDate = MutableStateFlow(LocalDate.now().toEpochDay().toInt())
    val selectedDate: StateFlow<Int> = _selectedDate.asStateFlow()

    // 日历数据（日期 -> 收支数据）
    private val _calendarData = MutableStateFlow<Map<Int, DayData>>(emptyMap())
    val calendarData: StateFlow<Map<Int, DayData>> = _calendarData.asStateFlow()

    // 选中日期的交易列表
    private val _selectedDateTransactions = MutableStateFlow<List<DailyTransactionWithCategory>>(emptyList())
    val selectedDateTransactions: StateFlow<List<DailyTransactionWithCategory>> = _selectedDateTransactions.asStateFlow()

    // 月度统计
    private val ym = YearMonth.now()
    private val defaultStartDate = ym.atDay(1).toEpochDay().toInt()
    private val defaultEndDate = ym.atEndOfMonth().toEpochDay().toInt()
    private val _monthStats = MutableStateFlow(PeriodStats(startDate = defaultStartDate, endDate = defaultEndDate))
    val monthStats: StateFlow<PeriodStats> = _monthStats.asStateFlow()

    init {
        loadMonthData()
        loadSelectedDateTransactions()
    }

    /**
     * 加载月度数据
     */
    private fun loadMonthData() {
        viewModelScope.launch {
            val yearMonth = _currentYearMonth.value
            val year = yearMonth / 100
            val month = yearMonth % 100
            val currentYM = YearMonth.of(year, month)
            val startDate = currentYM.atDay(1).toEpochDay().toInt()
            val endDate = currentYM.atEndOfMonth().toEpochDay().toInt()

            // 加载日历数据
            transactionDao.getDailyIncomeExpenseTotals(startDate, endDate).collectLatest { dailyData ->
                val dataMap = dailyData.associate {
                    it.date to DayData(income = it.income, expense = it.expense)
                }
                _calendarData.value = dataMap

                // 计算月度统计
                val totalIncome = dailyData.sumOf { it.income }
                val totalExpense = dailyData.sumOf { it.expense }
                _monthStats.value = PeriodStats(
                    startDate = startDate,
                    endDate = endDate,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense
                )
            }
        }
    }

    /**
     * 加载选中日期的交易
     */
    private fun loadSelectedDateTransactions() {
        viewModelScope.launch {
            transactionDao.getTransactionsByDate(_selectedDate.value).collectLatest { transactions ->
                val transactionsWithCategory = transactions.map { entity ->
                    val category = entity.categoryId?.let { id ->
                        customFieldDao.getFieldById(id)
                    }
                    DailyTransactionWithCategory(
                        transaction = entity,
                        category = category
                    )
                }
                _selectedDateTransactions.value = transactionsWithCategory
            }
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(epochDay: Int) {
        _selectedDate.value = epochDay
        loadSelectedDateTransactions()
    }

    /**
     * 上个月
     */
    fun previousMonth() {
        val current = _currentYearMonth.value
        val year = current / 100
        val month = current % 100

        val newYearMonth = if (month == 1) {
            (year - 1) * 100 + 12
        } else {
            year * 100 + (month - 1)
        }
        _currentYearMonth.value = newYearMonth
        loadMonthData()
    }

    /**
     * 下个月
     */
    fun nextMonth() {
        val current = _currentYearMonth.value
        val year = current / 100
        val month = current % 100

        val newYearMonth = if (month == 12) {
            (year + 1) * 100 + 1
        } else {
            year * 100 + (month + 1)
        }
        _currentYearMonth.value = newYearMonth
        loadMonthData()
    }

    /**
     * 跳转到今天
     */
    fun goToToday() {
        val today = LocalDate.now()
        _currentYearMonth.value = today.year * 100 + today.monthValue
        _selectedDate.value = today.toEpochDay().toInt()
        loadMonthData()
        loadSelectedDateTransactions()
    }

    /**
     * 跳转到指定日期
     */
    fun goToDate(date: LocalDate) {
        _currentYearMonth.value = date.year * 100 + date.monthValue
        _selectedDate.value = date.toEpochDay().toInt()
        loadMonthData()
        loadSelectedDateTransactions()
    }
}
