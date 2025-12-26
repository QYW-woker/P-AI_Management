package com.lifemanager.app.feature.finance.asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.domain.model.AssetFieldStats
import com.lifemanager.app.domain.model.AssetStats
import com.lifemanager.app.domain.model.AssetUiState
import com.lifemanager.app.domain.model.EditAssetState
import com.lifemanager.app.domain.model.MonthlyAssetWithField
import com.lifemanager.app.domain.model.NetWorthTrendPoint
import com.lifemanager.app.domain.usecase.MonthlyAssetUseCase
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
 * 月度资产ViewModel
 *
 * 管理资产/负债页面的UI状态和业务逻辑
 */
@HiltViewModel
class MonthlyAssetViewModel @Inject constructor(
    private val useCase: MonthlyAssetUseCase
) : ViewModel() {

    // 当前选中的年月
    private val _currentYearMonth = MutableStateFlow(getCurrentYearMonth())
    val currentYearMonth: StateFlow<Int> = _currentYearMonth.asStateFlow()

    // UI状态
    private val _uiState = MutableStateFlow<AssetUiState>(AssetUiState.Loading)
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    // 记录列表
    private val _records = MutableStateFlow<List<MonthlyAssetWithField>>(emptyList())
    val records: StateFlow<List<MonthlyAssetWithField>> = _records.asStateFlow()

    // 月度统计
    private val _assetStats = MutableStateFlow(AssetStats(0, 0.0, 0.0))
    val assetStats: StateFlow<AssetStats> = _assetStats.asStateFlow()

    // 资产字段统计
    private val _assetFieldStats = MutableStateFlow<List<AssetFieldStats>>(emptyList())
    val assetFieldStats: StateFlow<List<AssetFieldStats>> = _assetFieldStats.asStateFlow()

    // 负债字段统计
    private val _liabilityFieldStats = MutableStateFlow<List<AssetFieldStats>>(emptyList())
    val liabilityFieldStats: StateFlow<List<AssetFieldStats>> = _liabilityFieldStats.asStateFlow()

    // 净资产趋势
    private val _netWorthTrend = MutableStateFlow<List<NetWorthTrendPoint>>(emptyList())
    val netWorthTrend: StateFlow<List<NetWorthTrendPoint>> = _netWorthTrend.asStateFlow()

    // 资产类别字段列表
    val assetFields: StateFlow<List<CustomFieldEntity>> = useCase.getAssetFields()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 负债类别字段列表
    val liabilityFields: StateFlow<List<CustomFieldEntity>> = useCase.getLiabilityFields()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 编辑状态
    private val _editState = MutableStateFlow(EditAssetState())
    val editState: StateFlow<EditAssetState> = _editState.asStateFlow()

    // 是否显示编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 是否显示删除确认对话框
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 待删除的记录ID
    private val _deleteRecordId = MutableStateFlow<Long?>(null)

    // 是否显示复制上月数据对话框
    private val _showCopyDialog = MutableStateFlow(false)
    val showCopyDialog: StateFlow<Boolean> = _showCopyDialog.asStateFlow()

    init {
        loadMonthData(_currentYearMonth.value)
    }

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
     * 上一个月
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
     * 下一个月
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
     * 加载月份数据
     */
    private fun loadMonthData(yearMonth: Int) {
        _uiState.value = AssetUiState.Loading

        viewModelScope.launch {
            try {
                // 收集记录
                launch {
                    useCase.getRecordsWithFields(yearMonth).collect { records ->
                        _records.value = records
                    }
                }

                // 收集资产字段统计
                launch {
                    useCase.getAssetFieldStats(yearMonth).collect { stats ->
                        _assetFieldStats.value = stats
                    }
                }

                // 收集负债字段统计
                launch {
                    useCase.getLiabilityFieldStats(yearMonth).collect { stats ->
                        _liabilityFieldStats.value = stats
                    }
                }

                // 获取月度统计
                val stats = useCase.getAssetStats(yearMonth)
                _assetStats.value = stats

                // 加载趋势数据
                launch {
                    useCase.getNetWorthTrend(yearMonth).collect { trend ->
                        _netWorthTrend.value = trend
                    }
                }

                _uiState.value = AssetUiState.Success(
                    records = _records.value,
                    stats = stats,
                    assetsByField = _assetFieldStats.value,
                    liabilitiesByField = _liabilityFieldStats.value
                )

            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadMonthData(_currentYearMonth.value)
    }

    /**
     * 显示添加对话框
     */
    fun showAddDialog(isAsset: Boolean = true) {
        _editState.value = EditAssetState(
            yearMonth = _currentYearMonth.value,
            isAsset = isAsset,
            isEditing = false
        )
        _showEditDialog.value = true
    }

    /**
     * 显示编辑对话框
     */
    fun showEditDialog(recordId: Long) {
        viewModelScope.launch {
            val record = useCase.getRecordById(recordId)
            if (record != null) {
                _editState.value = EditAssetState(
                    id = record.id,
                    yearMonth = record.yearMonth,
                    isAsset = record.type == "ASSET",
                    fieldId = record.fieldId ?: 0,
                    amount = record.amount,
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
        _editState.value = EditAssetState()
    }

    /**
     * 更新编辑状态 - 类型
     */
    fun updateEditType(isAsset: Boolean) {
        _editState.value = _editState.value.copy(isAsset = isAsset, fieldId = 0)
    }

    /**
     * 更新编辑状态 - 字段
     */
    fun updateEditField(fieldId: Long) {
        _editState.value = _editState.value.copy(fieldId = fieldId)
    }

    /**
     * 更新编辑状态 - 金额
     */
    fun updateEditAmount(amount: Double) {
        _editState.value = _editState.value.copy(amount = amount)
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

        if (state.fieldId == 0L) {
            _editState.value = state.copy(error = "请选择类别")
            return
        }
        if (state.amount <= 0) {
            _editState.value = state.copy(error = "请输入有效金额")
            return
        }

        _editState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                if (state.isEditing) {
                    useCase.updateRecord(
                        id = state.id,
                        yearMonth = state.yearMonth,
                        isAsset = state.isAsset,
                        fieldId = state.fieldId,
                        amount = state.amount,
                        note = state.note
                    )
                } else {
                    useCase.addRecord(
                        yearMonth = state.yearMonth,
                        isAsset = state.isAsset,
                        fieldId = state.fieldId,
                        amount = state.amount,
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
     * 确认删除
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
     * 显示复制上月数据对话框
     */
    fun showCopyFromPreviousMonth() {
        _showCopyDialog.value = true
    }

    /**
     * 隐藏复制对话框
     */
    fun hideCopyDialog() {
        _showCopyDialog.value = false
    }

    /**
     * 确认复制上月数据
     */
    fun confirmCopyFromPreviousMonth() {
        viewModelScope.launch {
            try {
                useCase.copyFromPreviousMonth(_currentYearMonth.value)
                hideCopyDialog()
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
