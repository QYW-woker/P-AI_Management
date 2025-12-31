package com.lifemanager.app.feature.finance.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.FundAccountEntity
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.domain.repository.FundAccountRepository
import com.lifemanager.app.domain.usecase.CustomFieldUseCase
import com.lifemanager.app.domain.usecase.DailyTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 日常记账ViewModel
 */
@HiltViewModel
class DailyTransactionViewModel @Inject constructor(
    private val transactionUseCase: DailyTransactionUseCase,
    private val fieldUseCase: CustomFieldUseCase,
    private val fundAccountRepository: FundAccountRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<TransactionUiState>(TransactionUiState.Loading)
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    // 当前年月
    private val _currentYearMonth = MutableStateFlow(
        YearMonth.now().let { it.year * 100 + it.monthValue }
    )
    val currentYearMonth: StateFlow<Int> = _currentYearMonth.asStateFlow()

    // 选中日期
    private val _selectedDate = MutableStateFlow(LocalDate.now().toEpochDay().toInt())
    val selectedDate: StateFlow<Int> = _selectedDate.asStateFlow()

    // 交易分组列表
    private val _transactionGroups = MutableStateFlow<List<DailyTransactionGroup>>(emptyList())
    val transactionGroups: StateFlow<List<DailyTransactionGroup>> = _transactionGroups.asStateFlow()

    // 本月统计
    private val _monthStats = MutableStateFlow(PeriodStats(0, 0))
    val monthStats: StateFlow<PeriodStats> = _monthStats.asStateFlow()

    // 今日统计
    private val _todayStats = MutableStateFlow(DailyStats(0))
    val todayStats: StateFlow<DailyStats> = _todayStats.asStateFlow()

    // 日历日支出数据
    private val _calendarData = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val calendarData: StateFlow<Map<Int, Double>> = _calendarData.asStateFlow()

    // 可用分类
    private val _categories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val categories: StateFlow<List<CustomFieldEntity>> = _categories.asStateFlow()

    // 可用账户
    private val _accounts = MutableStateFlow<List<FundAccountEntity>>(emptyList())
    val accounts: StateFlow<List<FundAccountEntity>> = _accounts.asStateFlow()

    // 显示添加/编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 显示删除确认
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(TransactionEditState())
    val editState: StateFlow<TransactionEditState> = _editState.asStateFlow()

    // 待删除的交易ID
    private var deleteTransactionId: Long? = null

    // 视图模式：LIST, CALENDAR
    private val _viewMode = MutableStateFlow("LIST")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    // ============ 批量删除功能 ============
    // 选择模式
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // 已选中的交易ID
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // 显示批量删除确认对话框
    private val _showBatchDeleteDialog = MutableStateFlow(false)
    val showBatchDeleteDialog: StateFlow<Boolean> = _showBatchDeleteDialog.asStateFlow()

    /**
     * 进入选择模式
     */
    fun enterSelectionMode() {
        _isSelectionMode.value = true
        _selectedIds.value = emptySet()
    }

    /**
     * 退出选择模式
     */
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    /**
     * 切换选中状态
     */
    fun toggleSelection(id: Long) {
        val currentSet = _selectedIds.value.toMutableSet()
        if (currentSet.contains(id)) {
            currentSet.remove(id)
        } else {
            currentSet.add(id)
        }
        _selectedIds.value = currentSet
    }

    /**
     * 全选当前列表
     */
    fun selectAll() {
        val allIds = _transactionGroups.value.flatMap { group ->
            group.transactions.map { it.transaction.id }
        }.toSet()
        _selectedIds.value = allIds
    }

    /**
     * 取消全选
     */
    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    /**
     * 显示批量删除确认
     */
    fun showBatchDeleteConfirm() {
        if (_selectedIds.value.isNotEmpty()) {
            _showBatchDeleteDialog.value = true
        }
    }

    /**
     * 隐藏批量删除确认
     */
    fun hideBatchDeleteConfirm() {
        _showBatchDeleteDialog.value = false
    }

    /**
     * 确认批量删除
     */
    fun confirmBatchDelete() {
        val idsToDelete = _selectedIds.value.toList()
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                transactionUseCase.deleteTransactions(idsToDelete)
                hideBatchDeleteConfirm()
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    init {
        loadData()
        observeCategories()
        loadAccounts()
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = TransactionUiState.Loading

                // 加载今日统计
                _todayStats.value = transactionUseCase.getTodayStats()

                // 加载本月统计
                _monthStats.value = transactionUseCase.getCurrentMonthStats()

                _uiState.value = TransactionUiState.Success
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error(e.message ?: "加载失败")
            }
        }

        // 观察最近交易
        viewModelScope.launch {
            transactionUseCase.getRecentTransactionGroups(100)
                .catch { e ->
                    _uiState.value = TransactionUiState.Error(e.message ?: "加载失败")
                }
                .collect { groups ->
                    _transactionGroups.value = groups
                    if (_uiState.value is TransactionUiState.Loading) {
                        _uiState.value = TransactionUiState.Success
                    }
                }
        }

        // 观察日历数据
        viewModelScope.launch {
            _currentYearMonth.flatMapLatest { yearMonth ->
                transactionUseCase.getCalendarExpenseData(yearMonth)
            }.collect { data ->
                _calendarData.value = data
            }
        }
    }

    // 收入分类
    private val _incomeCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val incomeCategories: StateFlow<List<CustomFieldEntity>> = _incomeCategories.asStateFlow()

    // 支出分类
    private val _expenseCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val expenseCategories: StateFlow<List<CustomFieldEntity>> = _expenseCategories.asStateFlow()

    /**
     * 观察分类（分别观察收入和支出分类）
     */
    private fun observeCategories() {
        // 观察收入分类
        viewModelScope.launch {
            fieldUseCase.getFieldsByModule("INCOME")
                .collect { fields ->
                    _incomeCategories.value = fields
                    // 如果当前是收入类型，更新categories
                    if (_editState.value.type == TransactionType.INCOME) {
                        _categories.value = fields
                    }
                }
        }
        // 观察支出分类
        viewModelScope.launch {
            fieldUseCase.getFieldsByModule("EXPENSE")
                .collect { fields ->
                    _expenseCategories.value = fields
                    // 如果当前是支出类型，更新categories
                    if (_editState.value.type == TransactionType.EXPENSE) {
                        _categories.value = fields
                    }
                }
        }
    }

    /**
     * 加载可用账户
     */
    private fun loadAccounts() {
        viewModelScope.launch {
            fundAccountRepository.getAllEnabled()
                .catch { /* 忽略错误 */ }
                .collect { accountList ->
                    _accounts.value = accountList
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
     * 切换视图模式
     */
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == "LIST") "CALENDAR" else "LIST"
    }

    /**
     * 上个月
     */
    fun previousMonth() {
        val current = _currentYearMonth.value
        val year = current / 100
        val month = current % 100
        _currentYearMonth.value = if (month == 1) {
            (year - 1) * 100 + 12
        } else {
            year * 100 + (month - 1)
        }
    }

    /**
     * 下个月
     */
    fun nextMonth() {
        val current = _currentYearMonth.value
        val year = current / 100
        val month = current % 100
        _currentYearMonth.value = if (month == 12) {
            (year + 1) * 100 + 1
        } else {
            year * 100 + (month + 1)
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(date: Int) {
        _selectedDate.value = date
    }

    /**
     * 格式化年月
     */
    fun formatYearMonth(yearMonth: Int): String {
        val year = yearMonth / 100
        val month = yearMonth % 100
        return "${year}年${month}月"
    }

    /**
     * 显示添加对话框
     */
    fun showAddDialog(type: String = TransactionType.EXPENSE) {
        _editState.value = TransactionEditState(
            type = type,
            date = LocalDate.now().toEpochDay().toInt(),  // 默认今天，用户可以修改
            time = java.time.LocalTime.now().let {
                String.format("%02d:%02d", it.hour, it.minute)
            }
        )
        // 根据类型加载对应分类
        _categories.value = if (type == TransactionType.INCOME) {
            _incomeCategories.value
        } else {
            _expenseCategories.value
        }
        _showEditDialog.value = true
    }

    /**
     * 显示编辑对话框
     */
    fun showEditDialog(id: Long) {
        viewModelScope.launch {
            try {
                val transaction = transactionUseCase.getTransactionById(id)
                if (transaction != null) {
                    _editState.value = TransactionEditState(
                        id = transaction.transaction.id,
                        isEditing = true,
                        type = transaction.transaction.type,
                        amount = transaction.transaction.amount,
                        categoryId = transaction.transaction.categoryId,
                        accountId = transaction.transaction.accountId,
                        date = transaction.transaction.date,
                        time = transaction.transaction.time,
                        note = transaction.transaction.note
                    )
                    // 根据类型加载对应分类
                    _categories.value = if (transaction.transaction.type == TransactionType.INCOME) {
                        _incomeCategories.value
                    } else {
                        _expenseCategories.value
                    }
                    _showEditDialog.value = true
                }
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(error = e.message)
            }
        }
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editState.value = TransactionEditState()
    }

    /**
     * 更新编辑类型
     */
    fun updateEditType(type: String) {
        _editState.value = _editState.value.copy(type = type, categoryId = null)
        // 根据类型切换分类列表
        _categories.value = if (type == TransactionType.INCOME) {
            _incomeCategories.value
        } else {
            _expenseCategories.value
        }
    }

    /**
     * 更新编辑金额
     */
    fun updateEditAmount(amount: Double) {
        _editState.value = _editState.value.copy(amount = amount)
    }

    /**
     * 更新编辑分类
     */
    fun updateEditCategory(categoryId: Long?) {
        _editState.value = _editState.value.copy(categoryId = categoryId)
    }

    /**
     * 更新编辑账户
     */
    fun updateEditAccount(accountId: Long?) {
        _editState.value = _editState.value.copy(accountId = accountId)
    }

    /**
     * 更新编辑日期
     */
    fun updateEditDate(date: Int) {
        _editState.value = _editState.value.copy(date = date)
    }

    /**
     * 更新编辑时间
     */
    fun updateEditTime(time: String) {
        _editState.value = _editState.value.copy(time = time)
    }

    /**
     * 更新编辑备注
     */
    fun updateEditNote(note: String) {
        _editState.value = _editState.value.copy(note = note)
    }

    /**
     * 保存交易
     */
    fun saveTransaction() {
        val state = _editState.value
        if (state.amount <= 0) {
            _editState.value = state.copy(error = "请输入金额")
            return
        }

        viewModelScope.launch {
            try {
                _editState.value = state.copy(isSaving = true, error = null)

                if (state.isEditing) {
                    transactionUseCase.updateTransaction(
                        id = state.id,
                        type = state.type,
                        amount = state.amount,
                        categoryId = state.categoryId,
                        date = state.date,
                        time = state.time,
                        note = state.note,
                        accountId = state.accountId
                    )
                } else {
                    transactionUseCase.addTransaction(
                        type = state.type,
                        amount = state.amount,
                        categoryId = state.categoryId,
                        date = state.date,
                        time = state.time,
                        note = state.note,
                        accountId = state.accountId
                    )
                }

                hideEditDialog()
                refresh()
            } catch (e: Exception) {
                _editState.value = state.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 显示删除确认
     */
    fun showDeleteConfirm(id: Long) {
        deleteTransactionId = id
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
        deleteTransactionId = null
    }

    /**
     * 确认删除
     */
    fun confirmDelete() {
        val id = deleteTransactionId ?: return

        viewModelScope.launch {
            try {
                transactionUseCase.deleteTransaction(id)
                hideDeleteConfirm()
                refresh()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}
