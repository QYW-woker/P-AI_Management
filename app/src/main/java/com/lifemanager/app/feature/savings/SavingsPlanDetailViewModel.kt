package com.lifemanager.app.feature.savings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import com.lifemanager.app.domain.model.Milestone
import com.lifemanager.app.domain.repository.SavingsPlanRepository
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 存钱计划详情 ViewModel
 */
@HiltViewModel
class SavingsPlanDetailViewModel @Inject constructor(
    private val repository: SavingsPlanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow<SavingsPlanDetailUiState>(SavingsPlanDetailUiState.Loading)
    val uiState: StateFlow<SavingsPlanDetailUiState> = _uiState.asStateFlow()

    private val _plan = MutableStateFlow<SavingsPlanEntity?>(null)
    val plan: StateFlow<SavingsPlanEntity?> = _plan.asStateFlow()

    private val _records = MutableStateFlow<List<SavingsRecordEntity>>(emptyList())
    val records: StateFlow<List<SavingsRecordEntity>> = _records.asStateFlow()

    private val _statistics = MutableStateFlow(SavingsPlanStatistics())
    val statistics: StateFlow<SavingsPlanStatistics> = _statistics.asStateFlow()

    private val _milestones = MutableStateFlow<List<MilestoneWithStatus>>(emptyList())
    val milestones: StateFlow<List<MilestoneWithStatus>> = _milestones.asStateFlow()

    // 对话框状态
    private val _showDepositDialog = MutableStateFlow(false)
    val showDepositDialog: StateFlow<Boolean> = _showDepositDialog.asStateFlow()

    private val _showWithdrawDialog = MutableStateFlow(false)
    val showWithdrawDialog: StateFlow<Boolean> = _showWithdrawDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 操作状态
    private val _depositAmount = MutableStateFlow("")
    val depositAmount: StateFlow<String> = _depositAmount.asStateFlow()

    private val _depositNote = MutableStateFlow("")
    val depositNote: StateFlow<String> = _depositNote.asStateFlow()

    private val _withdrawAmount = MutableStateFlow("")
    val withdrawAmount: StateFlow<String> = _withdrawAmount.asStateFlow()

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating.asStateFlow()

    init {
        if (planId > 0) {
            loadPlan(planId)
        }
    }

    fun loadPlan(id: Long) {
        viewModelScope.launch {
            _uiState.value = SavingsPlanDetailUiState.Loading
            try {
                val planEntity = repository.getPlanById(id)
                if (planEntity == null) {
                    _uiState.value = SavingsPlanDetailUiState.Error("计划不存在")
                    return@launch
                }
                _plan.value = planEntity

                // 加载记录
                loadRecords(id)

                // 计算统计和里程碑
                calculateStatistics(planEntity)
                calculateMilestones(planEntity)

                _uiState.value = SavingsPlanDetailUiState.Success
            } catch (e: Exception) {
                _uiState.value = SavingsPlanDetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    private suspend fun loadRecords(planId: Long) {
        try {
            val recordList = repository.getRecordsByPlan(planId).first()
            _records.value = recordList.sortedByDescending { it.date }
        } catch (e: Exception) {
            _records.value = emptyList()
        }
    }

    private fun calculateStatistics(plan: SavingsPlanEntity) {
        val today = LocalDate.now().toEpochDay().toInt()
        val totalDays = plan.targetDate - plan.startDate
        val elapsedDays = maxOf(0, today - plan.startDate)
        val remainingDays = maxOf(0, plan.targetDate - today)

        val progress = if (plan.targetAmount > 0) {
            (plan.currentAmount / plan.targetAmount).coerceIn(0.0, 1.0).toFloat()
        } else 0f

        val expectedProgress = if (totalDays > 0) {
            (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f)
        } else 0f

        val isOnTrack = progress >= expectedProgress * 0.9f

        val dailyTarget = if (remainingDays > 0 && plan.targetAmount > plan.currentAmount) {
            (plan.targetAmount - plan.currentAmount) / remainingDays
        } else 0.0

        _statistics.value = SavingsPlanStatistics(
            currentAmount = plan.currentAmount,
            targetAmount = plan.targetAmount,
            progress = progress,
            expectedProgress = expectedProgress,
            isOnTrack = isOnTrack,
            remainingDays = remainingDays,
            remainingAmount = (plan.targetAmount - plan.currentAmount).coerceAtLeast(0.0),
            dailyTarget = dailyTarget,
            totalDeposits = _records.value.filter { it.amount > 0 }.sumOf { it.amount },
            totalWithdrawals = _records.value.filter { it.amount < 0 }.sumOf { -it.amount },
            recordCount = _records.value.size
        )
    }

    private fun calculateMilestones(plan: SavingsPlanEntity) {
        val milestoneList = listOf(
            Milestone.QUARTER,
            Milestone.HALF,
            Milestone.THREE_QUARTERS,
            Milestone.COMPLETE
        )

        val progressPercent = if (plan.targetAmount > 0) {
            ((plan.currentAmount / plan.targetAmount) * 100).toInt()
        } else 0

        _milestones.value = milestoneList.map { milestone ->
            MilestoneWithStatus(
                milestone = milestone,
                isAchieved = progressPercent >= milestone.percentage,
                amountReached = plan.targetAmount * milestone.percentage / 100.0
            )
        }
    }

    // 存款操作
    fun showDepositDialog() {
        _depositAmount.value = ""
        _depositNote.value = ""
        _operationError.value = null
        _showDepositDialog.value = true
    }

    fun hideDepositDialog() {
        _showDepositDialog.value = false
    }

    fun updateDepositAmount(amount: String) {
        _depositAmount.value = amount
        _operationError.value = null
    }

    fun updateDepositNote(note: String) {
        _depositNote.value = note
    }

    fun confirmDeposit() {
        val amount = _depositAmount.value.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _operationError.value = "请输入有效金额"
            return
        }

        viewModelScope.launch {
            _isOperating.value = true
            try {
                val record = SavingsRecordEntity(
                    planId = planId,
                    amount = amount,
                    note = _depositNote.value.takeIf { it.isNotBlank() } ?: "",
                    date = LocalDate.now().toEpochDay().toInt()
                )
                repository.saveRecord(record)

                // 更新计划金额
                _plan.value?.let { plan ->
                    val updated = plan.copy(currentAmount = plan.currentAmount + amount)
                    repository.updatePlan(updated)
                    _plan.value = updated
                    calculateStatistics(updated)
                    calculateMilestones(updated)
                }

                loadRecords(planId)
                hideDepositDialog()
            } catch (e: Exception) {
                _operationError.value = e.message ?: "存款失败"
            } finally {
                _isOperating.value = false
            }
        }
    }

    // 取款操作
    fun showWithdrawDialog() {
        _withdrawAmount.value = ""
        _operationError.value = null
        _showWithdrawDialog.value = true
    }

    fun hideWithdrawDialog() {
        _showWithdrawDialog.value = false
    }

    fun updateWithdrawAmount(amount: String) {
        _withdrawAmount.value = amount
        _operationError.value = null
    }

    fun confirmWithdraw() {
        val amount = _withdrawAmount.value.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _operationError.value = "请输入有效金额"
            return
        }

        val currentAmount = _plan.value?.currentAmount ?: 0.0
        if (amount > currentAmount) {
            _operationError.value = "余额不足"
            return
        }

        viewModelScope.launch {
            _isOperating.value = true
            try {
                val record = SavingsRecordEntity(
                    planId = planId,
                    amount = -amount,
                    note = "取款",
                    date = LocalDate.now().toEpochDay().toInt()
                )
                repository.saveRecord(record)

                // 更新计划金额
                _plan.value?.let { plan ->
                    val updated = plan.copy(currentAmount = plan.currentAmount - amount)
                    repository.updatePlan(updated)
                    _plan.value = updated
                    calculateStatistics(updated)
                    calculateMilestones(updated)
                }

                loadRecords(planId)
                hideWithdrawDialog()
            } catch (e: Exception) {
                _operationError.value = e.message ?: "取款失败"
            } finally {
                _isOperating.value = false
            }
        }
    }

    // 删除操作
    fun showDeleteConfirm() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
    }

    fun confirmDelete(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _plan.value?.let { plan ->
                    repository.deletePlan(plan.id)
                    hideDeleteConfirm()
                    onComplete()
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    // 暂停/恢复
    fun togglePause() {
        viewModelScope.launch {
            _plan.value?.let { plan ->
                val newStatus = if (plan.status == "PAUSED") "ACTIVE" else "PAUSED"
                val updated = plan.copy(status = newStatus)
                repository.updatePlan(updated)
                _plan.value = updated
            }
        }
    }

    fun formatDate(epochDay: Int): String {
        return try {
            val date = LocalDate.ofEpochDay(epochDay.toLong())
            date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * UI状态
 */
sealed class SavingsPlanDetailUiState {
    object Loading : SavingsPlanDetailUiState()
    object Success : SavingsPlanDetailUiState()
    data class Error(val message: String) : SavingsPlanDetailUiState()
}

/**
 * 存钱计划统计数据
 */
data class SavingsPlanStatistics(
    val currentAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val progress: Float = 0f,
    val expectedProgress: Float = 0f,
    val isOnTrack: Boolean = true,
    val remainingDays: Int = 0,
    val remainingAmount: Double = 0.0,
    val dailyTarget: Double = 0.0,
    val totalDeposits: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val recordCount: Int = 0
)

/**
 * 里程碑状态
 */
data class MilestoneWithStatus(
    val milestone: Milestone,
    val isAchieved: Boolean,
    val amountReached: Double
)
