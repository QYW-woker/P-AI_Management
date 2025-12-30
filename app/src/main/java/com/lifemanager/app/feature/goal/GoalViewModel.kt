package com.lifemanager.app.feature.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.ai.service.AIDataAnalysisService
import com.lifemanager.app.core.database.entity.AIAnalysisEntity
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.domain.model.GoalEditState
import com.lifemanager.app.domain.model.GoalStatistics
import com.lifemanager.app.domain.model.GoalUiState
import com.lifemanager.app.domain.usecase.GoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 目标管理ViewModel
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val useCase: GoalUseCase,
    private val aiAnalysisService: AIDataAnalysisService
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<GoalUiState>(GoalUiState.Loading)
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    // 目标列表
    private val _goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    val goals: StateFlow<List<GoalEntity>> = _goals.asStateFlow()

    // 统计数据
    private val _statistics = MutableStateFlow(GoalStatistics())
    val statistics: StateFlow<GoalStatistics> = _statistics.asStateFlow()

    // 当前筛选（ALL, ACTIVE, COMPLETED）
    private val _currentFilter = MutableStateFlow("ACTIVE")
    val currentFilter: StateFlow<String> = _currentFilter.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(GoalEditState())
    val editState: StateFlow<GoalEditState> = _editState.asStateFlow()

    // 对话框状态
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _showProgressDialog = MutableStateFlow(false)
    val showProgressDialog: StateFlow<Boolean> = _showProgressDialog.asStateFlow()

    private var goalToDelete: Long? = null
    private var goalToUpdateProgress: GoalEntity? = null

    // AI分析状态
    private val _goalAnalysis = MutableStateFlow<AIAnalysisEntity?>(null)
    val goalAnalysis: StateFlow<AIAnalysisEntity?> = _goalAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    init {
        loadGoals()
        loadAIAnalysis()
    }

    /**
     * 加载目标列表
     */
    private fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = GoalUiState.Loading
            try {
                useCase.getAllGoals()
                    .catch { e ->
                        _uiState.value = GoalUiState.Error(e.message ?: "加载失败")
                    }
                    .collect { allGoals ->
                        val filtered = when (_currentFilter.value) {
                            "ACTIVE" -> allGoals.filter { it.status == GoalStatus.ACTIVE }
                            "COMPLETED" -> allGoals.filter { it.status == GoalStatus.COMPLETED }
                            "ABANDONED" -> allGoals.filter { it.status == GoalStatus.ABANDONED }
                            else -> allGoals
                        }
                        _goals.value = filtered
                        _uiState.value = GoalUiState.Success(filtered)
                        loadStatistics()
                    }
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 加载统计数据
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                _statistics.value = useCase.getStatistics()
            } catch (e: Exception) {
                // 统计加载失败不影响主界面
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadGoals()
    }

    /**
     * 设置筛选器
     */
    fun setFilter(filter: String) {
        _currentFilter.value = filter
        loadGoals()
    }

    /**
     * 显示添加目标对话框
     */
    fun showAddDialog() {
        val today = useCase.getToday()
        _editState.value = GoalEditState(
            startDate = today,
            endDate = today + 365 // 默认一年
        )
        _showEditDialog.value = true
    }

    /**
     * 显示编辑目标对话框
     */
    fun showEditDialog(goalId: Long) {
        viewModelScope.launch {
            val goal = useCase.getGoalById(goalId)
            if (goal != null) {
                _editState.value = GoalEditState(
                    id = goal.id,
                    title = goal.title,
                    description = goal.description,
                    goalType = goal.goalType,
                    category = goal.category,
                    startDate = goal.startDate,
                    endDate = goal.endDate,
                    progressType = goal.progressType,
                    targetValue = goal.targetValue,
                    currentValue = goal.currentValue,
                    unit = goal.unit,
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
        _editState.value = GoalEditState()
    }

    /**
     * 更新编辑状态
     */
    fun updateEditTitle(title: String) {
        _editState.value = _editState.value.copy(title = title, error = null)
    }

    fun updateEditDescription(description: String) {
        _editState.value = _editState.value.copy(description = description)
    }

    fun updateEditGoalType(type: String) {
        _editState.value = _editState.value.copy(goalType = type)
    }

    fun updateEditCategory(category: String) {
        _editState.value = _editState.value.copy(category = category)
    }

    fun updateEditStartDate(date: Int) {
        _editState.value = _editState.value.copy(startDate = date)
    }

    fun updateEditEndDate(date: Int?) {
        _editState.value = _editState.value.copy(endDate = date)
    }

    fun updateEditProgressType(type: String) {
        _editState.value = _editState.value.copy(progressType = type)
    }

    fun updateEditTargetValue(value: Double?) {
        _editState.value = _editState.value.copy(targetValue = value, error = null)
    }

    fun updateEditUnit(unit: String) {
        _editState.value = _editState.value.copy(unit = unit)
    }

    /**
     * 保存目标
     */
    fun saveGoal() {
        val state = _editState.value

        // 验证
        if (state.title.isBlank()) {
            _editState.value = state.copy(error = "请输入目标标题")
            return
        }
        if (state.progressType == "NUMERIC" && (state.targetValue == null || state.targetValue <= 0)) {
            _editState.value = state.copy(error = "请输入有效的目标数值")
            return
        }

        viewModelScope.launch {
            _editState.value = state.copy(isSaving = true, error = null)
            try {
                if (state.isEditing) {
                    val existing = useCase.getGoalById(state.id)
                    if (existing != null) {
                        useCase.updateGoal(
                            existing.copy(
                                title = state.title.trim(),
                                description = state.description.trim(),
                                goalType = state.goalType,
                                category = state.category,
                                startDate = state.startDate,
                                endDate = state.endDate,
                                progressType = state.progressType,
                                targetValue = state.targetValue,
                                unit = state.unit
                            )
                        )
                    }
                } else {
                    useCase.createGoal(
                        title = state.title.trim(),
                        description = state.description.trim(),
                        goalType = state.goalType,
                        category = state.category,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        progressType = state.progressType,
                        targetValue = state.targetValue,
                        unit = state.unit
                    )
                }
                hideEditDialog()
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 显示更新进度对话框
     */
    fun showProgressDialog(goal: GoalEntity) {
        goalToUpdateProgress = goal
        _showProgressDialog.value = true
    }

    /**
     * 隐藏进度对话框
     */
    fun hideProgressDialog() {
        _showProgressDialog.value = false
        goalToUpdateProgress = null
    }

    /**
     * 更新目标进度
     */
    fun updateProgress(value: Double) {
        val goal = goalToUpdateProgress ?: return
        viewModelScope.launch {
            try {
                useCase.updateProgress(goal.id, value)
                hideProgressDialog()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 完成目标
     */
    fun completeGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                useCase.completeGoal(goalId)
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 显示删除确认
     */
    fun showDeleteConfirm(goalId: Long) {
        goalToDelete = goalId
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
        goalToDelete = null
    }

    /**
     * 确认删除
     */
    fun confirmDelete() {
        val id = goalToDelete ?: return
        viewModelScope.launch {
            try {
                useCase.deleteGoal(id)
                hideDeleteConfirm()
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 计算目标进度
     */
    fun calculateProgress(goal: GoalEntity): Float {
        return useCase.calculateProgress(goal)
    }

    /**
     * 格式化日期
     */
    fun formatDate(epochDay: Int): String {
        return useCase.formatDate(epochDay)
    }

    /**
     * 获取剩余天数
     */
    fun getRemainingDays(goal: GoalEntity): Int? {
        return useCase.getRemainingDays(goal.endDate)
    }

    /**
     * 获取进度更新目标
     */
    fun getProgressGoal(): GoalEntity? = goalToUpdateProgress

    /**
     * 加载AI分析结果
     */
    private fun loadAIAnalysis() {
        viewModelScope.launch {
            aiAnalysisService.getGoalAnalysis().collectLatest { analyses ->
                _goalAnalysis.value = analyses.firstOrNull()
            }
        }
    }

    /**
     * 刷新AI分析
     */
    fun refreshAIAnalysis() {
        if (_isAnalyzing.value) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = aiAnalysisService.analyzeGoalData(forceRefresh = true)
                result.onSuccess { analysis ->
                    _goalAnalysis.value = analysis
                }
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}
