package com.lifemanager.app.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.domain.model.PlanEditState
import com.lifemanager.app.domain.model.RecordEditState
import com.lifemanager.app.domain.model.SavingsPlanWithDetails
import com.lifemanager.app.domain.model.SavingsStats
import com.lifemanager.app.domain.model.SavingsUiState
import com.lifemanager.app.domain.model.SavingsPlanTemplate
import java.time.LocalDate
import com.lifemanager.app.domain.usecase.SavingsPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 存钱计划ViewModel
 */
@HiltViewModel
class SavingsPlanViewModel @Inject constructor(
    private val useCase: SavingsPlanUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<SavingsUiState>(SavingsUiState.Loading)
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    // 计划列表
    private val _plans = MutableStateFlow<List<SavingsPlanWithDetails>>(emptyList())
    val plans: StateFlow<List<SavingsPlanWithDetails>> = _plans.asStateFlow()

    // 统计数据
    private val _stats = MutableStateFlow(SavingsStats())
    val stats: StateFlow<SavingsStats> = _stats.asStateFlow()

    // 计划编辑状态
    private val _planEditState = MutableStateFlow(PlanEditState())
    val planEditState: StateFlow<PlanEditState> = _planEditState.asStateFlow()

    // 存款记录编辑状态
    private val _recordEditState = MutableStateFlow(RecordEditState())
    val recordEditState: StateFlow<RecordEditState> = _recordEditState.asStateFlow()

    // 对话框状态
    private val _showPlanDialog = MutableStateFlow(false)
    val showPlanDialog: StateFlow<Boolean> = _showPlanDialog.asStateFlow()

    private val _showDepositDialog = MutableStateFlow(false)
    val showDepositDialog: StateFlow<Boolean> = _showDepositDialog.asStateFlow()

    private val _showWithdrawDialog = MutableStateFlow(false)
    val showWithdrawDialog: StateFlow<Boolean> = _showWithdrawDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _showHistoryDialog = MutableStateFlow(false)
    val showHistoryDialog: StateFlow<Boolean> = _showHistoryDialog.asStateFlow()

    // 当前查看历史的计划
    private val _currentHistoryPlan = MutableStateFlow<SavingsPlanWithDetails?>(null)
    val currentHistoryPlan: StateFlow<SavingsPlanWithDetails?> = _currentHistoryPlan.asStateFlow()

    private var planToDelete: Long? = null
    private var currentDepositPlanId: Long? = null
    private var currentWithdrawPlanId: Long? = null

    init {
        loadPlans()
    }

    /**
     * 加载计划列表
     */
    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.value = SavingsUiState.Loading
            useCase.getPlansWithDetails()
                .catch { e ->
                    _uiState.value = SavingsUiState.Error(e.message ?: "加载失败")
                }
                .collect { plans ->
                    _plans.value = plans
                    _uiState.value = SavingsUiState.Success()
                    loadStats()
                }
        }
    }

    /**
     * 加载统计数据
     */
    private fun loadStats() {
        viewModelScope.launch {
            try {
                _stats.value = useCase.getSavingsStats()
            } catch (e: Exception) {
                // 统计加载失败不影响主界面
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadPlans()
    }

    /**
     * 显示添加计划对话框
     */
    fun showAddPlanDialog() {
        val today = useCase.getToday()
        _planEditState.value = PlanEditState(
            startDate = today,
            targetDate = today + 365 // 默认一年后
        )
        _showPlanDialog.value = true
    }

    /**
     * 显示编辑计划对话框
     */
    fun showEditPlanDialog(planId: Long) {
        viewModelScope.launch {
            val plan = useCase.getPlanById(planId)
            if (plan != null) {
                _planEditState.value = PlanEditState(
                    id = plan.id,
                    name = plan.name,
                    description = plan.description,
                    targetAmount = plan.targetAmount,
                    startDate = plan.startDate,
                    targetDate = plan.targetDate,
                    strategy = plan.strategy,
                    periodAmount = plan.periodAmount,
                    color = plan.color,
                    isEditing = true
                )
                _showPlanDialog.value = true
            }
        }
    }

    /**
     * 隐藏计划对话框
     */
    fun hidePlanDialog() {
        _showPlanDialog.value = false
        _planEditState.value = PlanEditState()
    }

    /**
     * 更新计划编辑状态
     */
    fun updatePlanName(name: String) {
        _planEditState.value = _planEditState.value.copy(name = name, error = null)
    }

    fun updatePlanDescription(description: String) {
        _planEditState.value = _planEditState.value.copy(description = description)
    }

    fun updatePlanTargetAmount(amount: Double) {
        _planEditState.value = _planEditState.value.copy(targetAmount = amount, error = null)
    }

    fun updatePlanStartDate(date: Int) {
        _planEditState.value = _planEditState.value.copy(startDate = date)
    }

    fun updatePlanTargetDate(date: Int) {
        _planEditState.value = _planEditState.value.copy(targetDate = date)
    }

    fun updatePlanStrategy(strategy: String) {
        _planEditState.value = _planEditState.value.copy(strategy = strategy)
    }

    fun updatePlanPeriodAmount(amount: Double?) {
        _planEditState.value = _planEditState.value.copy(periodAmount = amount)
    }

    fun updatePlanColor(color: String) {
        _planEditState.value = _planEditState.value.copy(color = color)
    }

    /**
     * 应用模板
     */
    fun applyTemplate(template: SavingsPlanTemplate) {
        val today = LocalDate.now()
        val targetDate = today.plusMonths(template.suggestedMonths.toLong())

        _planEditState.value = _planEditState.value.copy(
            name = template.name,
            description = template.description,
            targetAmount = template.suggestedAmount,
            startDate = today.toEpochDay().toInt(),
            targetDate = targetDate.toEpochDay().toInt(),
            strategy = template.strategy,
            color = template.color,
            error = null
        )
    }

    /**
     * 保存计划
     */
    fun savePlan() {
        val state = _planEditState.value

        // 验证
        if (state.name.isBlank()) {
            _planEditState.value = state.copy(error = "请输入计划名称")
            return
        }
        if (state.targetAmount <= 0) {
            _planEditState.value = state.copy(error = "请输入目标金额")
            return
        }
        if (state.targetDate <= state.startDate) {
            _planEditState.value = state.copy(error = "目标日期必须晚于开始日期")
            return
        }

        viewModelScope.launch {
            _planEditState.value = state.copy(isSaving = true, error = null)
            try {
                val plan = SavingsPlanEntity(
                    id = if (state.isEditing) state.id else 0,
                    name = state.name.trim(),
                    description = state.description.trim(),
                    targetAmount = state.targetAmount,
                    startDate = state.startDate,
                    targetDate = state.targetDate,
                    strategy = state.strategy,
                    periodAmount = state.periodAmount,
                    color = state.color
                )

                if (state.isEditing) {
                    useCase.updatePlan(plan)
                } else {
                    useCase.createPlan(plan)
                }

                hidePlanDialog()
            } catch (e: Exception) {
                _planEditState.value = _planEditState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 显示存款对话框
     */
    fun showDepositDialog(planId: Long) {
        currentDepositPlanId = planId
        _recordEditState.value = RecordEditState(
            planId = planId,
            date = useCase.getToday()
        )
        _showDepositDialog.value = true
    }

    /**
     * 隐藏存款对话框
     */
    fun hideDepositDialog() {
        _showDepositDialog.value = false
        _recordEditState.value = RecordEditState()
        currentDepositPlanId = null
    }

    /**
     * 更新存款金额
     */
    fun updateDepositAmount(amount: Double) {
        _recordEditState.value = _recordEditState.value.copy(amount = amount, error = null)
    }

    /**
     * 更新存款备注
     */
    fun updateDepositNote(note: String) {
        _recordEditState.value = _recordEditState.value.copy(note = note)
    }

    /**
     * 确认存款
     */
    fun confirmDeposit() {
        val state = _recordEditState.value
        val planId = currentDepositPlanId ?: return

        if (state.amount <= 0) {
            _recordEditState.value = state.copy(error = "请输入存款金额")
            return
        }

        viewModelScope.launch {
            _recordEditState.value = state.copy(isSaving = true, error = null)
            try {
                useCase.deposit(planId, state.amount, state.note)
                hideDepositDialog()
                loadStats()
            } catch (e: Exception) {
                _recordEditState.value = _recordEditState.value.copy(
                    isSaving = false,
                    error = e.message ?: "存款失败"
                )
            }
        }
    }

    /**
     * 显示取款对话框
     */
    fun showWithdrawDialog(planId: Long) {
        currentWithdrawPlanId = planId
        _recordEditState.value = RecordEditState(
            planId = planId,
            date = useCase.getToday(),
            isWithdrawal = true
        )
        _showWithdrawDialog.value = true
    }

    /**
     * 隐藏取款对话框
     */
    fun hideWithdrawDialog() {
        _showWithdrawDialog.value = false
        _recordEditState.value = RecordEditState()
        currentWithdrawPlanId = null
    }

    /**
     * 确认取款
     */
    fun confirmWithdraw() {
        val state = _recordEditState.value
        val planId = currentWithdrawPlanId ?: return

        if (state.amount <= 0) {
            _recordEditState.value = state.copy(error = "请输入取款金额")
            return
        }

        viewModelScope.launch {
            _recordEditState.value = state.copy(isSaving = true, error = null)
            try {
                val success = useCase.withdraw(planId, state.amount, state.note)
                if (success) {
                    hideWithdrawDialog()
                    loadStats()
                } else {
                    _recordEditState.value = _recordEditState.value.copy(
                        isSaving = false,
                        error = "余额不足"
                    )
                }
            } catch (e: Exception) {
                _recordEditState.value = _recordEditState.value.copy(
                    isSaving = false,
                    error = e.message ?: "取款失败"
                )
            }
        }
    }

    /**
     * 显示历史记录对话框
     */
    fun showHistoryDialog(planId: Long) {
        viewModelScope.launch {
            val planDetails = useCase.getPlanDetails(planId)
            if (planDetails != null) {
                _currentHistoryPlan.value = planDetails
                _showHistoryDialog.value = true
            }
        }
    }

    /**
     * 隐藏历史记录对话框
     */
    fun hideHistoryDialog() {
        _showHistoryDialog.value = false
        _currentHistoryPlan.value = null
    }

    /**
     * 快速存款（预设金额）
     */
    fun quickDeposit(planId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                useCase.deposit(planId, amount, "快速存款")
                loadStats()
            } catch (e: Exception) {
                _uiState.value = SavingsUiState.Error(e.message ?: "存款失败")
            }
        }
    }

    /**
     * 显示删除确认
     */
    fun showDeleteConfirm(planId: Long) {
        planToDelete = planId
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
        planToDelete = null
    }

    /**
     * 确认删除
     */
    fun confirmDelete() {
        val id = planToDelete ?: return
        viewModelScope.launch {
            try {
                useCase.deletePlan(id)
                hideDeleteConfirm()
            } catch (e: Exception) {
                _uiState.value = SavingsUiState.Error(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 暂停计划
     */
    fun pausePlan(planId: Long) {
        viewModelScope.launch {
            try {
                useCase.pausePlan(planId)
            } catch (e: Exception) {
                _uiState.value = SavingsUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 恢复计划
     */
    fun resumePlan(planId: Long) {
        viewModelScope.launch {
            try {
                useCase.resumePlan(planId)
            } catch (e: Exception) {
                _uiState.value = SavingsUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 格式化日期
     */
    fun formatDate(epochDay: Int): String {
        return useCase.formatDate(epochDay)
    }
}
