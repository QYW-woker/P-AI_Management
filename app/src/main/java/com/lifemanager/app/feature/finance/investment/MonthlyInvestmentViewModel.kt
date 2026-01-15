package com.lifemanager.app.feature.finance.investment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.domain.model.EditInvestmentState
import com.lifemanager.app.domain.model.InvestmentFieldStats
import com.lifemanager.app.domain.model.InvestmentUiState
import com.lifemanager.app.domain.model.MonthlyInvestmentWithField
import com.lifemanager.app.domain.model.InvestmentMonthlyStats
import com.lifemanager.app.domain.model.MonthlyTrendPoint
import com.lifemanager.app.domain.usecase.MonthlyInvestmentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 月度定投ViewModel
 *
 * 管理月度定投页面的UI状态和业务逻辑
 */
@HiltViewModel
class MonthlyInvestmentViewModel @Inject constructor(
    private val useCase: MonthlyInvestmentUseCase
) : ViewModel() {

    // 当前选中的年月 (格式: YYYYMM)
    private val _currentYearMonth = MutableStateFlow(getCurrentYearMonth())
    val currentYearMonth: StateFlow<Int> = _currentYearMonth.asStateFlow()

    // UI状态
    private val _uiState = MutableStateFlow<InvestmentUiState>(InvestmentUiState.Loading)
    val uiState: StateFlow<InvestmentUiState> = _uiState.asStateFlow()

    // 定投记录列表
    private val _records = MutableStateFlow<List<MonthlyInvestmentWithField>>(emptyList())
    val records: StateFlow<List<MonthlyInvestmentWithField>> = _records.asStateFlow()

    // 月度统计数据
    private val _monthlyStats = MutableStateFlow(InvestmentMonthlyStats(0, 0.0, 0.0))
    val monthlyStats: StateFlow<InvestmentMonthlyStats> = _monthlyStats.asStateFlow()

    // 字段统计
    private val _fieldStats = MutableStateFlow<List<InvestmentFieldStats>>(emptyList())
    val fieldStats: StateFlow<List<InvestmentFieldStats>> = _fieldStats.asStateFlow()

    // 可用的月份列表
    private val _availableMonths = MutableStateFlow<List<Int>>(emptyList())
    val availableMonths: StateFlow<List<Int>> = _availableMonths.asStateFlow()

    // 预算趋势数据
    private val _budgetTrend = MutableStateFlow<List<MonthlyTrendPoint>>(emptyList())
    val budgetTrend: StateFlow<List<MonthlyTrendPoint>> = _budgetTrend.asStateFlow()

    // 实际趋势数据
    private val _actualTrend = MutableStateFlow<List<MonthlyTrendPoint>>(emptyList())
    val actualTrend: StateFlow<List<MonthlyTrendPoint>> = _actualTrend.asStateFlow()

    // 定投类别字段列表
    val investmentFields: StateFlow<List<CustomFieldEntity>> = useCase.getInvestmentFields()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 编辑记录状态
    private val _editState = MutableStateFlow(EditInvestmentState())
    val editState: StateFlow<EditInvestmentState> = _editState.asStateFlow()

    // 是否显示添加/编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 是否显示删除确认对话框
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 待删除的记录ID
    private val _deleteRecordId = MutableStateFlow<Long?>(null)

    init {
        // 初始化预设字段
        viewModelScope.launch {
            useCase.initPresetFieldsIfNeeded()
        }

        // 加载可用月份
        loadAvailableMonths()

        // 加载当前月份数据
        loadMonthData(_currentYearMonth.value)
    }

    /**
     * 获取当前年月
     */
    private fun getCurrentYearMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)
    }

    /**
     * 切换月份
     */
    fun selectMonth(yearMonth: Int) {
        _currentYearMonth.value = yearMonth
        loadMonthData(yearMonth)
    }

    /**
     * 切换到上一个月
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

        selectMonth(newYearMonth)
    }

    /**
     * 切换到下一个月
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

        selectMonth(newYearMonth)
    }

    /**
     * 加载可用的月份列表
     */
    private fun loadAvailableMonths() {
        viewModelScope.launch {
            useCase.getAvailableMonths().collect { months ->
                _availableMonths.value = months
            }
        }
    }

    /**
     * 加载指定月份的数据
     */
    private fun loadMonthData(yearMonth: Int) {
        _uiState.value = InvestmentUiState.Loading

        viewModelScope.launch {
            try {
                // 收集记录数据
                launch {
                    useCase.getRecordsWithFields(yearMonth).collect { records ->
                        _records.value = records
                    }
                }

                // 收集字段统计
                launch {
                    useCase.getFieldStats(yearMonth).collect { stats ->
                        _fieldStats.value = stats
                    }
                }

                // 获取月度统计
                val stats = useCase.getMonthlyStats(yearMonth)
                _monthlyStats.value = stats

                // 加载年度趋势
                val year = yearMonth / 100
                launch {
                    useCase.getYearlyBudgetTrend(year).collect { trend ->
                        _budgetTrend.value = trend
                    }
                }
                launch {
                    useCase.getYearlyActualTrend(year).collect { trend ->
                        _actualTrend.value = trend
                    }
                }

                _uiState.value = InvestmentUiState.Success(
                    records = _records.value,
                    stats = stats,
                    fieldStats = _fieldStats.value
                )

            } catch (e: Exception) {
                _uiState.value = InvestmentUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 刷新当前月份数据
     */
    fun refresh() {
        loadMonthData(_currentYearMonth.value)
    }

    /**
     * 显示添加记录对话框
     */
    fun showAddDialog() {
        _editState.value = EditInvestmentState(
            yearMonth = _currentYearMonth.value,
            isEditing = false
        )
        _showEditDialog.value = true
    }

    /**
     * 显示编辑记录对话框
     */
    fun showEditDialog(recordId: Long) {
        viewModelScope.launch {
            val record = useCase.getRecordById(recordId)
            if (record != null) {
                _editState.value = EditInvestmentState(
                    id = record.id,
                    yearMonth = record.yearMonth,
                    fieldId = record.fieldId ?: 0L,
                    budgetAmount = record.budgetAmount,
                    actualAmount = record.actualAmount,
                    note = record.note,
                    isEditing = true
                )
                _showEditDialog.value = true
            }
        }
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editState.value = EditInvestmentState()
    }

    /**
     * 更新编辑状态 - 字段
     */
    fun updateEditField(fieldId: Long) {
        _editState.value = _editState.value.copy(fieldId = fieldId)
    }

    /**
     * 更新编辑状态 - 预算金额
     */
    fun updateEditBudgetAmount(amount: Double) {
        _editState.value = _editState.value.copy(budgetAmount = amount)
    }

    /**
     * 更新编辑状态 - 实际金额
     */
    fun updateEditActualAmount(amount: Double) {
        _editState.value = _editState.value.copy(actualAmount = amount)
    }

    /**
     * 更新编辑状态 - 备注
     */
    fun updateEditNote(note: String) {
        _editState.value = _editState.value.copy(note = note)
    }

    /**
     * 保存记录
     */
    fun saveRecord() {
        val state = _editState.value

        // 验证输入
        if (state.fieldId == 0L) {
            _editState.value = state.copy(error = "请选择定投类型")
            return
        }
        if (state.budgetAmount <= 0 && state.actualAmount <= 0) {
            _editState.value = state.copy(error = "请输入预算或实际金额")
            return
        }

        _editState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                if (state.isEditing) {
                    useCase.updateRecord(
                        id = state.id,
                        yearMonth = state.yearMonth,
                        fieldId = state.fieldId,
                        budgetAmount = state.budgetAmount,
                        actualAmount = state.actualAmount,
                        note = state.note
                    )
                } else {
                    useCase.addRecord(
                        yearMonth = state.yearMonth,
                        fieldId = state.fieldId,
                        budgetAmount = state.budgetAmount,
                        actualAmount = state.actualAmount,
                        note = state.note
                    )
                }

                hideEditDialog()
                refresh()
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirm(recordId: Long) {
        _deleteRecordId.value = recordId
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
        _deleteRecordId.value = null
    }

    /**
     * 确认删除记录
     */
    fun confirmDelete() {
        val recordId = _deleteRecordId.value ?: return

        viewModelScope.launch {
            try {
                useCase.deleteRecord(recordId)
                hideDeleteConfirm()
                refresh()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 格式化年月显示
     */
    fun formatYearMonth(yearMonth: Int): String {
        val year = yearMonth / 100
        val month = yearMonth % 100
        return "${year}年${month}月"
    }
}
