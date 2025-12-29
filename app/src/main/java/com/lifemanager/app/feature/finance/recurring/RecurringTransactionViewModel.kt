package com.lifemanager.app.feature.finance.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.dao.CustomFieldDao
import com.lifemanager.app.core.database.dao.DailyTransactionDao
import com.lifemanager.app.core.database.dao.RecurringTransactionDao
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.core.database.entity.RecurringFrequency
import com.lifemanager.app.core.database.entity.RecurringTransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 周期记账ViewModel
 */
@HiltViewModel
class RecurringTransactionViewModel @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val dailyTransactionDao: DailyTransactionDao,
    private val customFieldDao: CustomFieldDao
) : ViewModel() {

    // 所有周期记账列表
    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> =
        recurringTransactionDao.getAllRecurringTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 显示编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 当前编辑的记录
    private val _editingTransaction = MutableStateFlow<RecurringTransactionEntity?>(null)
    val editingTransaction: StateFlow<RecurringTransactionEntity?> = _editingTransaction.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(RecurringEditState())
    val editState: StateFlow<RecurringEditState> = _editState.asStateFlow()

    // 显示删除确认对话框
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 待删除的记录
    private val _deletingTransaction = MutableStateFlow<RecurringTransactionEntity?>(null)
    val deletingTransaction: StateFlow<RecurringTransactionEntity?> = _deletingTransaction.asStateFlow()

    // 收入分类列表
    private val _incomeCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val incomeCategories: StateFlow<List<CustomFieldEntity>> = _incomeCategories.asStateFlow()

    // 支出分类列表
    private val _expenseCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val expenseCategories: StateFlow<List<CustomFieldEntity>> = _expenseCategories.asStateFlow()

    init {
        loadCategories()
        viewModelScope.launch {
            recurringTransactions.collectLatest {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载分类
     */
    private fun loadCategories() {
        viewModelScope.launch {
            customFieldDao.getFieldsByModuleType("INCOME_CATEGORY")
                .collectLatest { _incomeCategories.value = it }
        }
        viewModelScope.launch {
            customFieldDao.getFieldsByModuleType("EXPENSE_CATEGORY")
                .collectLatest { _expenseCategories.value = it }
        }
    }

    /**
     * 显示创建对话框
     */
    fun showCreateDialog() {
        _editingTransaction.value = null
        _editState.value = RecurringEditState(
            startDate = LocalDate.now().toEpochDay().toInt()
        )
        _showEditDialog.value = true
    }

    /**
     * 显示编辑对话框
     */
    fun showEditDialog(transaction: RecurringTransactionEntity) {
        _editingTransaction.value = transaction
        _editState.value = RecurringEditState(
            name = transaction.name,
            type = transaction.type,
            amount = transaction.amount.toString(),
            categoryId = transaction.categoryId ?: 0,
            note = transaction.note,
            frequency = transaction.frequency,
            interval = transaction.interval,
            dayOfWeek = transaction.dayOfWeek,
            dayOfMonth = transaction.dayOfMonth,
            monthOfYear = transaction.monthOfYear,
            startDate = transaction.startDate,
            endDate = transaction.endDate,
            maxOccurrences = transaction.maxOccurrences,
            autoExecute = transaction.autoExecute,
            reminderDaysBefore = transaction.reminderDaysBefore
        )
        _showEditDialog.value = true
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editingTransaction.value = null
        _editState.value = RecurringEditState()
    }

    /**
     * 更新名称
     */
    fun updateName(name: String) {
        _editState.value = _editState.value.copy(name = name)
    }

    /**
     * 更新类型
     */
    fun updateType(type: String) {
        _editState.value = _editState.value.copy(type = type, categoryId = 0)
    }

    /**
     * 更新金额
     */
    fun updateAmount(amount: String) {
        _editState.value = _editState.value.copy(amount = amount)
    }

    /**
     * 更新分类
     */
    fun updateCategory(categoryId: Long) {
        _editState.value = _editState.value.copy(categoryId = categoryId)
    }

    /**
     * 更新备注
     */
    fun updateNote(note: String) {
        _editState.value = _editState.value.copy(note = note)
    }

    /**
     * 更新周期类型
     */
    fun updateFrequency(frequency: String) {
        _editState.value = _editState.value.copy(frequency = frequency)
    }

    /**
     * 更新周期间隔
     */
    fun updateInterval(interval: Int) {
        _editState.value = _editState.value.copy(interval = interval)
    }

    /**
     * 更新周几
     */
    fun updateDayOfWeek(day: Int?) {
        _editState.value = _editState.value.copy(dayOfWeek = day)
    }

    /**
     * 更新月份中的日期
     */
    fun updateDayOfMonth(day: Int?) {
        _editState.value = _editState.value.copy(dayOfMonth = day)
    }

    /**
     * 更新月份
     */
    fun updateMonthOfYear(month: Int?) {
        _editState.value = _editState.value.copy(monthOfYear = month)
    }

    /**
     * 更新开始日期
     */
    fun updateStartDate(date: Int) {
        _editState.value = _editState.value.copy(startDate = date)
    }

    /**
     * 更新结束日期
     */
    fun updateEndDate(date: Int?) {
        _editState.value = _editState.value.copy(endDate = date)
    }

    /**
     * 更新最大执行次数
     */
    fun updateMaxOccurrences(max: Int?) {
        _editState.value = _editState.value.copy(maxOccurrences = max)
    }

    /**
     * 更新自动执行
     */
    fun updateAutoExecute(auto: Boolean) {
        _editState.value = _editState.value.copy(autoExecute = auto)
    }

    /**
     * 更新提前提醒天数
     */
    fun updateReminderDays(days: Int) {
        _editState.value = _editState.value.copy(reminderDaysBefore = days)
    }

    /**
     * 保存周期记账
     */
    fun saveRecurring() {
        val state = _editState.value

        if (state.name.isBlank()) {
            _editState.value = state.copy(error = "请输入名称")
            return
        }

        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _editState.value = state.copy(error = "请输入有效金额")
            return
        }

        viewModelScope.launch {
            try {
                _editState.value = state.copy(isSaving = true, error = null)

                val nextDueDate = calculateNextDueDate(
                    startDate = state.startDate,
                    frequency = state.frequency,
                    interval = state.interval,
                    dayOfWeek = state.dayOfWeek,
                    dayOfMonth = state.dayOfMonth
                )

                val existingTransaction = _editingTransaction.value
                val transaction = if (existingTransaction != null) {
                    existingTransaction.copy(
                        name = state.name,
                        type = state.type,
                        amount = amount,
                        categoryId = if (state.categoryId > 0) state.categoryId else null,
                        note = state.note,
                        frequency = state.frequency,
                        interval = state.interval,
                        dayOfWeek = state.dayOfWeek,
                        dayOfMonth = state.dayOfMonth,
                        monthOfYear = state.monthOfYear,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        maxOccurrences = state.maxOccurrences,
                        autoExecute = state.autoExecute,
                        reminderDaysBefore = state.reminderDaysBefore,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    RecurringTransactionEntity(
                        name = state.name,
                        type = state.type,
                        amount = amount,
                        categoryId = if (state.categoryId > 0) state.categoryId else null,
                        note = state.note,
                        frequency = state.frequency,
                        interval = state.interval,
                        dayOfWeek = state.dayOfWeek,
                        dayOfMonth = state.dayOfMonth,
                        monthOfYear = state.monthOfYear,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        nextDueDate = nextDueDate,
                        maxOccurrences = state.maxOccurrences,
                        autoExecute = state.autoExecute,
                        reminderDaysBefore = state.reminderDaysBefore
                    )
                }

                if (existingTransaction != null) {
                    recurringTransactionDao.update(transaction)
                } else {
                    recurringTransactionDao.insert(transaction)
                }

                hideEditDialog()
            } catch (e: Exception) {
                _editState.value = state.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 计算下次执行日期
     */
    private fun calculateNextDueDate(
        startDate: Int,
        frequency: String,
        interval: Int,
        dayOfWeek: Int?,
        dayOfMonth: Int?
    ): Int {
        val today = LocalDate.now().toEpochDay().toInt()
        var nextDate = startDate

        if (nextDate <= today) {
            nextDate = today + 1
        }

        val localDate = LocalDate.ofEpochDay(nextDate.toLong())

        return when (frequency) {
            RecurringFrequency.WEEKLY -> {
                val targetDay = dayOfWeek ?: localDate.dayOfWeek.value
                var date = localDate
                while (date.dayOfWeek.value != targetDay) {
                    date = date.plusDays(1)
                }
                date.toEpochDay().toInt()
            }
            RecurringFrequency.MONTHLY -> {
                val targetDay = dayOfMonth ?: localDate.dayOfMonth
                var date = localDate.withDayOfMonth(minOf(targetDay, localDate.lengthOfMonth()))
                if (date.toEpochDay().toInt() <= today) {
                    date = date.plusMonths(1).withDayOfMonth(minOf(targetDay, date.plusMonths(1).lengthOfMonth()))
                }
                date.toEpochDay().toInt()
            }
            else -> nextDate
        }
    }

    /**
     * 切换启用状态
     */
    fun toggleEnabled(transaction: RecurringTransactionEntity) {
        viewModelScope.launch {
            try {
                recurringTransactionDao.setEnabled(transaction.id, !transaction.isEnabled)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirmDialog(transaction: RecurringTransactionEntity) {
        _deletingTransaction.value = transaction
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除对话框
     */
    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
        _deletingTransaction.value = null
    }

    /**
     * 删除周期记账
     */
    fun deleteRecurring() {
        val transaction = _deletingTransaction.value ?: return

        viewModelScope.launch {
            try {
                recurringTransactionDao.delete(transaction.id)
                hideDeleteDialog()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * 立即执行一次
     */
    fun executeNow(transaction: RecurringTransactionEntity) {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toEpochDay().toInt()

                // 创建日常交易记录
                val dailyTransaction = DailyTransactionEntity(
                    date = today,
                    type = transaction.type,
                    amount = transaction.amount,
                    categoryId = transaction.categoryId,
                    note = "自动记账: ${transaction.name}",
                    paymentMethod = null,
                    ledgerId = transaction.ledgerId
                )
                dailyTransactionDao.insert(dailyTransaction)

                // 更新下次执行日期
                val nextDueDate = calculateNextDueDateAfterExecution(transaction)
                recurringTransactionDao.updateAfterExecution(
                    id = transaction.id,
                    nextDueDate = nextDueDate,
                    lastExecutedDate = today
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * 执行后计算下次日期
     */
    private fun calculateNextDueDateAfterExecution(transaction: RecurringTransactionEntity): Int {
        val currentDue = LocalDate.ofEpochDay(transaction.nextDueDate.toLong())

        val nextDate = when (transaction.frequency) {
            RecurringFrequency.DAILY -> currentDue.plusDays(transaction.interval.toLong())
            RecurringFrequency.WEEKLY -> currentDue.plusWeeks(transaction.interval.toLong())
            RecurringFrequency.BIWEEKLY -> currentDue.plusWeeks(2L * transaction.interval)
            RecurringFrequency.MONTHLY -> currentDue.plusMonths(transaction.interval.toLong())
            RecurringFrequency.QUARTERLY -> currentDue.plusMonths(3L * transaction.interval)
            RecurringFrequency.YEARLY -> currentDue.plusYears(transaction.interval.toLong())
            else -> currentDue.plusMonths(1)
        }

        return nextDate.toEpochDay().toInt()
    }

    /**
     * 获取分类名称
     */
    fun getCategoryName(categoryId: Long?, type: String): String {
        if (categoryId == null) return ""
        val categories = if (type == "INCOME") _incomeCategories.value else _expenseCategories.value
        return categories.find { it.id == categoryId }?.name ?: ""
    }

    /**
     * 格式化周期描述
     */
    fun formatFrequencyDescription(transaction: RecurringTransactionEntity): String {
        val intervalStr = if (transaction.interval > 1) "每${transaction.interval}" else "每"

        return when (transaction.frequency) {
            RecurringFrequency.DAILY -> "${intervalStr}天"
            RecurringFrequency.WEEKLY -> {
                val dayName = when (transaction.dayOfWeek) {
                    1 -> "周一"
                    2 -> "周二"
                    3 -> "周三"
                    4 -> "周四"
                    5 -> "周五"
                    6 -> "周六"
                    7 -> "周日"
                    else -> ""
                }
                "${intervalStr}周$dayName"
            }
            RecurringFrequency.BIWEEKLY -> "每两周"
            RecurringFrequency.MONTHLY -> {
                val day = transaction.dayOfMonth ?: 1
                "${intervalStr}月${day}日"
            }
            RecurringFrequency.QUARTERLY -> "每季度"
            RecurringFrequency.YEARLY -> {
                val month = transaction.monthOfYear ?: 1
                val day = transaction.dayOfMonth ?: 1
                "${intervalStr}年${month}月${day}日"
            }
            else -> transaction.frequency
        }
    }

    /**
     * 格式化日期
     */
    fun formatDate(epochDay: Int): String {
        val date = LocalDate.ofEpochDay(epochDay.toLong())
        return "${date.year}/${date.monthValue}/${date.dayOfMonth}"
    }
}

/**
 * 周期记账编辑状态
 */
data class RecurringEditState(
    val name: String = "",
    val type: String = "EXPENSE",
    val amount: String = "",
    val categoryId: Long = 0,
    val note: String = "",
    val frequency: String = RecurringFrequency.MONTHLY,
    val interval: Int = 1,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = 1,
    val monthOfYear: Int? = null,
    val startDate: Int = 0,
    val endDate: Int? = null,
    val maxOccurrences: Int? = null,
    val autoExecute: Boolean = true,
    val reminderDaysBefore: Int = 1,
    val isSaving: Boolean = false,
    val error: String? = null
)
