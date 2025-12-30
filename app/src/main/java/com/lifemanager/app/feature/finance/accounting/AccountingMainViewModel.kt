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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val todayEpochDay = LocalDate.now().toEpochDay().toInt()
    private val _todayStats = MutableStateFlow(DailyStats(date = todayEpochDay))
    val todayStats: StateFlow<DailyStats> = _todayStats.asStateFlow()

    // 月度统计
    private val yearMonth = YearMonth.now()
    private val monthStartDate = yearMonth.atDay(1).toEpochDay().toInt()
    private val monthEndDate = yearMonth.atEndOfMonth().toEpochDay().toInt()
    private val _monthStats = MutableStateFlow(PeriodStats(startDate = monthStartDate, endDate = monthEndDate))
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

    // 编辑交易对话框状态
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _editingTransaction = MutableStateFlow<DailyTransactionWithCategory?>(null)
    val editingTransaction: StateFlow<DailyTransactionWithCategory?> = _editingTransaction.asStateFlow()

    init {
        loadData()
        loadCategories()
    }

    /**
     * 加载数据
     * 使用debounce避免频繁更新，并优化分类查询
     */
    @OptIn(FlowPreview::class)
    private fun loadData() {
        _uiState.value = UiState.Loading

        // 加载今日统计
        viewModelScope.launch {
            try {
                transactionDao.getTransactionsByDate(todayEpochDay)
                    .debounce(200) // 防抖避免频繁更新
                    .collectLatest { transactions ->
                        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                        _todayStats.value = DailyStats(
                            date = todayEpochDay,
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
                transactionDao.getTransactionsInRange(monthStartDate, monthEndDate)
                    .debounce(200)
                    .collectLatest { transactions ->
                        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                        _monthStats.value = PeriodStats(
                            startDate = monthStartDate,
                            endDate = monthEndDate,
                            totalIncome = income,
                            totalExpense = expense
                        )
                    }
            } catch (e: Exception) {
                // Handle error
            }
        }

        // 加载最近交易 - 优化：批量查询分类避免N+1问题
        viewModelScope.launch {
            try {
                transactionDao.getRecentTransactions(10)
                    .debounce(200)
                    .collectLatest { transactions ->
                        // 批量获取所有需要的分类ID
                        val categoryIds = transactions.mapNotNull { it.categoryId }.distinct()

                        // 一次性查询所有分类（在IO线程）
                        val categoriesMap = withContext(Dispatchers.IO) {
                            if (categoryIds.isNotEmpty()) {
                                categoryIds.mapNotNull { id ->
                                    customFieldDao.getFieldById(id)?.let { id to it }
                                }.toMap()
                            } else {
                                emptyMap()
                            }
                        }

                        // 组装数据
                        val transactionsWithCategory = transactions.map { entity ->
                            DailyTransactionWithCategory(
                                transaction = entity,
                                category = entity.categoryId?.let { categoriesMap[it] }
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
        type: String,
        amount: Double,
        categoryId: Long?,
        note: String,
        date: LocalDate = LocalDate.now(),
        time: String? = null
    ) {
        val now = System.currentTimeMillis()
        val transactionTime = time ?: String.format("%02d:%02d", java.time.LocalTime.now().hour, java.time.LocalTime.now().minute)

        viewModelScope.launch {
            try {
                val transaction = DailyTransactionEntity(
                    date = date.toEpochDay().toInt(),
                    time = transactionTime,
                    type = type,
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

    /**
     * 显示编辑交易对话框
     */
    fun showEditTransaction(transactionId: Long) {
        viewModelScope.launch {
            val transaction = _recentTransactions.value.find { it.transaction.id == transactionId }
            if (transaction != null) {
                _editingTransaction.value = transaction
                _showEditDialog.value = true
            }
        }
    }

    /**
     * 隐藏编辑交易对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editingTransaction.value = null
    }

    /**
     * 更新交易
     */
    fun updateTransaction(
        id: Long,
        type: String,
        amount: Double,
        categoryId: Long?,
        note: String,
        date: LocalDate,
        time: String?
    ) {
        val now = System.currentTimeMillis()
        val transactionTime = time ?: String.format("%02d:%02d", java.time.LocalTime.now().hour, java.time.LocalTime.now().minute)

        viewModelScope.launch {
            try {
                val existingTransaction = transactionDao.getById(id)
                if (existingTransaction != null) {
                    val updated = existingTransaction.copy(
                        date = date.toEpochDay().toInt(),
                        time = transactionTime,
                        type = type,
                        amount = amount,
                        categoryId = categoryId,
                        note = note,
                        updatedAt = now
                    )
                    transactionDao.update(updated)
                    hideEditDialog()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * 删除交易
     */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            try {
                transactionDao.deleteById(id)
                hideEditDialog()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
