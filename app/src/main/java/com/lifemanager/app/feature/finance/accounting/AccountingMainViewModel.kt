package com.lifemanager.app.feature.finance.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.dao.CustomFieldDao
import com.lifemanager.app.core.database.dao.DailyTransactionDao
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.domain.model.DailyStats
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
 * 记账主界面ViewModel
 */
@HiltViewModel
class AccountingMainViewModel @Inject constructor(
    private val transactionDao: DailyTransactionDao,
    private val customFieldDao: CustomFieldDao
) : ViewModel() {

    // UI状态
    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 今日统计
    private val _todayStats = MutableStateFlow(DailyStats())
    val todayStats: StateFlow<DailyStats> = _todayStats.asStateFlow()

    // 月度统计
    private val _monthStats = MutableStateFlow(PeriodStats())
    val monthStats: StateFlow<PeriodStats> = _monthStats.asStateFlow()

    // 最近交易
    private val _recentTransactions = MutableStateFlow<List<DailyTransactionWithCategory>>(emptyList())
    val recentTransactions: StateFlow<List<DailyTransactionWithCategory>> = _recentTransactions.asStateFlow()

    // 当前账本
    private val _currentLedger = MutableStateFlow<LedgerInfo?>(null)
    val currentLedger: StateFlow<LedgerInfo?> = _currentLedger.asStateFlow()

    // 分类列表
    private val _categories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val categories: StateFlow<List<CustomFieldEntity>> = _categories.asStateFlow()

    // 快速记账对话框显示状态
    private val _showQuickAddDialog = MutableStateFlow(false)
    val showQuickAddDialog: StateFlow<Boolean> = _showQuickAddDialog.asStateFlow()

    init {
        loadData()
        loadCategories()
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        _uiState.value = UiState.Loading

        val today = LocalDate.now().toEpochDay().toInt()
        val yearMonth = YearMonth.now()
        val monthStart = yearMonth.atDay(1).toEpochDay().toInt()
        val monthEnd = yearMonth.atEndOfMonth().toEpochDay().toInt()

        // 加载今日统计
        viewModelScope.launch {
            try {
                transactionDao.getTransactionsByDate(today).collectLatest { transactions ->
                    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    _todayStats.value = DailyStats(
                        date = today,
                        totalIncome = income,
                        totalExpense = expense,
                        transactionCount = transactions.size
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

        // 加载月度统计
        viewModelScope.launch {
            try {
                transactionDao.getTransactionsInRange(monthStart, monthEnd).collectLatest { transactions ->
                    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    _monthStats.value = PeriodStats(
                        totalIncome = income,
                        totalExpense = expense
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

        // 加载最近交易
        viewModelScope.launch {
            try {
                transactionDao.getRecentTransactions(10).collectLatest { transactions ->
                    val transactionsWithCategory = transactions.map { entity ->
                        val category = entity.categoryId?.let { id ->
                            customFieldDao.getFieldById(id)
                        }
                        DailyTransactionWithCategory(
                            transaction = com.lifemanager.app.domain.model.DailyTransaction(
                                id = entity.id,
                                date = entity.date,
                                time = entity.time,
                                type = if (entity.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
                                amount = entity.amount,
                                categoryId = entity.categoryId,
                                note = entity.note,
                                createdAt = entity.createdAt,
                                updatedAt = entity.updatedAt
                            ),
                            category = category
                        )
                    }
                    _recentTransactions.value = transactionsWithCategory
                    _uiState.value = UiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "加载失败")
            }
        }

        // 设置默认账本
        _currentLedger.value = LedgerInfo(
            id = 1,
            name = "默认账本",
            isDefault = true
        )
    }

    /**
     * 加载分类
     */
    private fun loadCategories() {
        viewModelScope.launch {
            customFieldDao.getFieldsByModuleTypes(
                listOf("EXPENSE_CATEGORY", "INCOME_CATEGORY")
            ).collectLatest { fields ->
                _categories.value = fields
            }
        }
    }

    /**
     * 显示快速记账对话框
     */
    fun showQuickAdd() {
        _showQuickAddDialog.value = true
    }

    /**
     * 隐藏快速记账对话框
     */
    fun hideQuickAdd() {
        _showQuickAddDialog.value = false
    }

    /**
     * 快速添加交易
     */
    fun quickAddTransaction(
        type: TransactionType,
        amount: Double,
        categoryId: Long?,
        note: String
    ) {
        val today = LocalDate.now()
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                val transaction = DailyTransactionEntity(
                    date = today.toEpochDay().toInt(),
                    time = String.format("%02d:%02d", today.atStartOfDay().hour, today.atStartOfDay().minute),
                    type = if (type == TransactionType.INCOME) "INCOME" else "EXPENSE",
                    amount = amount,
                    categoryId = categoryId,
                    note = note,
                    createdAt = now,
                    updatedAt = now
                )
                transactionDao.insert(transaction)
                hideQuickAdd()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }
}
