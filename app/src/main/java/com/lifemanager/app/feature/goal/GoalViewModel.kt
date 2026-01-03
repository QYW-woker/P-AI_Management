package com.lifemanager.app.feature.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.domain.model.AIAnalysisState
import com.lifemanager.app.domain.model.GoalDetailState
import com.lifemanager.app.domain.model.GoalEditState
import com.lifemanager.app.domain.model.GoalStatistics
import com.lifemanager.app.domain.model.GoalTreeNode
import com.lifemanager.app.domain.model.GoalUiState
import com.lifemanager.app.domain.model.OperationResult
import com.lifemanager.app.domain.model.SubGoalEditState
import com.lifemanager.app.domain.model.categoryToTypeMapping
import com.lifemanager.app.core.ai.service.AIService
import com.lifemanager.app.domain.usecase.GoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 目标管理ViewModel
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val useCase: GoalUseCase,
    private val aiService: AIService
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<GoalUiState>(GoalUiState.Loading)
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    // 目标列表
    private val _goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    val goals: StateFlow<List<GoalEntity>> = _goals.asStateFlow()

    // 目标树列表（用于展示多级目标）
    private val _goalTree = MutableStateFlow<List<GoalTreeNode>>(emptyList())
    val goalTree: StateFlow<List<GoalTreeNode>> = _goalTree.asStateFlow()

    // 展开的目标ID集合
    private val _expandedGoalIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedGoalIds: StateFlow<Set<Long>> = _expandedGoalIds.asStateFlow()

    // 扁平化的目标列表（根据展开状态）
    private val _flattenedGoals = MutableStateFlow<List<GoalTreeNode>>(emptyList())
    val flattenedGoals: StateFlow<List<GoalTreeNode>> = _flattenedGoals.asStateFlow()

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

    // 操作结果状态（用于UI反馈）
    private val _operationResult = MutableStateFlow<OperationResult>(OperationResult.Idle)
    val operationResult: StateFlow<OperationResult> = _operationResult.asStateFlow()

    // 目标详情状态（用于详情页响应式更新）
    private val _goalDetailState = MutableStateFlow(GoalDetailState())
    val goalDetailState: StateFlow<GoalDetailState> = _goalDetailState.asStateFlow()

    // AI分析状态
    private val _aiAnalysisState = MutableStateFlow<AIAnalysisState>(AIAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AIAnalysisState> = _aiAnalysisState.asStateFlow()

    // 当前查看的目标ID
    private var currentDetailGoalId: Long? = null

    init {
        loadGoals()
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
        // 根据分类自动推荐目标类型
        val recommendedType = categoryToTypeMapping[category] ?: _editState.value.goalType
        _editState.value = _editState.value.copy(
            category = category,
            goalType = recommendedType
        )
    }

    /**
     * 获取分类对应的推荐目标类型
     */
    fun getRecommendedGoalType(category: String): String {
        return categoryToTypeMapping[category] ?: "YEARLY"
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
            _operationResult.value = OperationResult.Loading
            try {
                useCase.updateProgress(goal.id, value)
                _operationResult.value = OperationResult.Success("进度已更新")
                hideProgressDialog()
                // 刷新详情页数据
                refreshGoalDetail(goal.id)
                // 延迟后重置状态
                delay(2000)
                _operationResult.value = OperationResult.Idle
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error(e.message ?: "更新进度失败")
                delay(3000)
                _operationResult.value = OperationResult.Idle
            }
        }
    }

    /**
     * 完成目标
     */
    fun completeGoal(goalId: Long) {
        viewModelScope.launch {
            _operationResult.value = OperationResult.Loading
            try {
                useCase.completeGoal(goalId)
                _operationResult.value = OperationResult.Success("目标已完成！恭喜！")
                // 刷新详情页和列表
                refreshGoalDetail(goalId)
                loadGoals()
                // 延迟后重置状态
                delay(2500)
                _operationResult.value = OperationResult.Idle
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error(e.message ?: "操作失败")
                delay(3000)
                _operationResult.value = OperationResult.Idle
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
     * 根据ID获取目标（用于详情页）
     */
    fun getGoalById(goalId: Long) = kotlinx.coroutines.flow.flow {
        emit(useCase.getGoalById(goalId))
    }

    /**
     * 加载目标详情（响应式）
     */
    fun loadGoalDetail(goalId: Long) {
        currentDetailGoalId = goalId
        refreshGoalDetail(goalId)
    }

    /**
     * 刷新目标详情状态
     */
    private fun refreshGoalDetail(goalId: Long) {
        viewModelScope.launch {
            _goalDetailState.value = _goalDetailState.value.copy(isLoading = true)
            try {
                val goal = useCase.getGoalById(goalId)
                if (goal != null) {
                    val progress = useCase.calculateProgress(goal)
                    val remainingDays = useCase.getRemainingDays(goal.endDate)
                    val progressRecords = useCase.getProgressRecords(goalId)
                    _goalDetailState.value = GoalDetailState(
                        goal = goal,
                        isLoading = false,
                        progress = progress,
                        remainingDays = remainingDays,
                        progressRecords = progressRecords,
                        operationResult = _operationResult.value
                    )
                } else {
                    _goalDetailState.value = GoalDetailState(
                        isLoading = false,
                        operationResult = OperationResult.Error("目标不存在")
                    )
                }
            } catch (e: Exception) {
                _goalDetailState.value = GoalDetailState(
                    isLoading = false,
                    operationResult = OperationResult.Error(e.message ?: "加载失败")
                )
            }
        }
    }

    /**
     * 清除操作结果状态
     */
    fun clearOperationResult() {
        _operationResult.value = OperationResult.Idle
    }

    /**
     * 删除目标
     */
    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                useCase.deleteGoal(goalId)
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 创建新目标（用于独立页面）
     */
    fun createGoal(
        title: String,
        description: String,
        category: String,
        goalType: String,
        targetValue: Double?,
        unit: String,
        progressType: String,
        deadline: Int?
    ) {
        viewModelScope.launch {
            try {
                val today = useCase.getToday()
                useCase.createGoal(
                    title = title,
                    description = description,
                    goalType = goalType,
                    category = category,
                    startDate = today,
                    endDate = deadline,
                    progressType = progressType,
                    targetValue = targetValue,
                    unit = unit
                )
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "创建失败")
            }
        }
    }

    /**
     * 更新目标（用于独立页面）
     */
    fun updateGoal(
        id: Long,
        title: String,
        description: String,
        category: String,
        goalType: String,
        targetValue: Double?,
        unit: String,
        progressType: String,
        deadline: Int?
    ) {
        viewModelScope.launch {
            try {
                val existing = useCase.getGoalById(id)
                if (existing != null) {
                    useCase.updateGoal(
                        existing.copy(
                            title = title,
                            description = description,
                            goalType = goalType,
                            category = category,
                            endDate = deadline,
                            progressType = progressType,
                            targetValue = targetValue,
                            unit = unit
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "更新失败")
            }
        }
    }

    // ==================== 多级目标相关方法 ====================

    /**
     * 加载目标树
     */
    fun loadGoalTree() {
        viewModelScope.launch {
            try {
                useCase.getAllTopLevelGoals()
                    .catch { e ->
                        _uiState.value = GoalUiState.Error(e.message ?: "加载失败")
                    }
                    .collect { topLevelGoals ->
                        // 根据筛选条件过滤
                        val filtered = when (_currentFilter.value) {
                            "ACTIVE" -> topLevelGoals.filter { it.status == GoalStatus.ACTIVE }
                            "COMPLETED" -> topLevelGoals.filter { it.status == GoalStatus.COMPLETED }
                            "ABANDONED" -> topLevelGoals.filter { it.status == GoalStatus.ABANDONED }
                            else -> topLevelGoals
                        }

                        // 构建目标树
                        val tree = useCase.buildGoalTree(filtered)
                        _goalTree.value = tree

                        // 更新扁平化列表
                        updateFlattenedGoals()

                        _uiState.value = GoalUiState.Success(filtered)
                    }
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 更新扁平化的目标列表
     */
    private fun updateFlattenedGoals() {
        val tree = _goalTree.value
        val expandedIds = _expandedGoalIds.value
        _flattenedGoals.value = useCase.flattenTree(tree, expandedIds)
    }

    /**
     * 切换目标展开/收起状态
     */
    fun toggleExpand(goalId: Long) {
        val currentExpanded = _expandedGoalIds.value.toMutableSet()
        if (currentExpanded.contains(goalId)) {
            currentExpanded.remove(goalId)
        } else {
            currentExpanded.add(goalId)
        }
        _expandedGoalIds.value = currentExpanded
        updateFlattenedGoals()
    }

    /**
     * 展开所有目标
     */
    fun expandAll() {
        val allIds = _goalTree.value.flatMap { collectAllIds(it) }.toSet()
        _expandedGoalIds.value = allIds
        updateFlattenedGoals()
    }

    /**
     * 收起所有目标
     */
    fun collapseAll() {
        _expandedGoalIds.value = emptySet()
        updateFlattenedGoals()
    }

    /**
     * 收集所有目标ID（包括子目标）
     */
    private fun collectAllIds(node: GoalTreeNode): List<Long> {
        val result = mutableListOf(node.goal.id)
        node.children.forEach { child ->
            result.addAll(collectAllIds(child))
        }
        return result
    }

    /**
     * 获取子目标列表
     */
    fun getChildGoals(parentId: Long) = kotlinx.coroutines.flow.flow {
        emit(useCase.getChildGoalsSync(parentId))
    }

    /**
     * 添加子目标
     */
    fun addSubGoal(
        parentId: Long,
        title: String,
        description: String = "",
        progressType: String = "PERCENTAGE",
        targetValue: Double? = null,
        unit: String = ""
    ) {
        viewModelScope.launch {
            try {
                useCase.addSubGoal(parentId, title, description, progressType, targetValue, unit)
                loadGoalTree() // 刷新树
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "添加子目标失败")
            }
        }
    }

    /**
     * 创建带子目标的多级目标
     */
    fun createGoalWithSubGoals(
        title: String,
        description: String,
        category: String,
        goalType: String,
        targetValue: Double?,
        unit: String,
        progressType: String,
        deadline: Int?,
        subGoals: List<SubGoalEditState>
    ) {
        viewModelScope.launch {
            try {
                val today = useCase.getToday()
                useCase.createGoalWithSubGoals(
                    title = title,
                    description = description,
                    goalType = goalType,
                    category = category,
                    startDate = today,
                    endDate = deadline,
                    progressType = progressType,
                    targetValue = targetValue,
                    unit = unit,
                    subGoals = subGoals
                )
                loadGoalTree() // 刷新树
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "创建失败")
            }
        }
    }

    /**
     * 删除目标（包括子目标）
     */
    fun deleteGoalWithChildren(goalId: Long) {
        viewModelScope.launch {
            try {
                useCase.deleteGoalWithChildren(goalId)
                loadGoalTree() // 刷新树
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 更新目标进度（带父目标自动更新）
     */
    fun updateGoalProgress(goalId: Long, value: Double) {
        viewModelScope.launch {
            _operationResult.value = OperationResult.Loading
            try {
                useCase.updateProgress(goalId, value)
                _operationResult.value = OperationResult.Success("进度已更新")
                loadGoalTree() // 刷新树以更新父目标进度
                refreshGoalDetail(goalId)
                // 延迟后重置状态
                delay(2000)
                _operationResult.value = OperationResult.Idle
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error(e.message ?: "更新进度失败")
                delay(3000)
                _operationResult.value = OperationResult.Idle
            }
        }
    }

    /**
     * 放弃目标
     */
    fun abandonGoal(goalId: Long) {
        viewModelScope.launch {
            _operationResult.value = OperationResult.Loading
            try {
                useCase.abandonGoal(goalId)
                _operationResult.value = OperationResult.Success("目标已放弃")
                refreshGoalDetail(goalId)
                loadGoals()
                delay(2000)
                _operationResult.value = OperationResult.Idle
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error(e.message ?: "操作失败")
                delay(3000)
                _operationResult.value = OperationResult.Idle
            }
        }
    }

    /**
     * 恢复已放弃的目标
     */
    fun reactivateGoal(goalId: Long) {
        viewModelScope.launch {
            _operationResult.value = OperationResult.Loading
            try {
                useCase.reactivateGoal(goalId)
                _operationResult.value = OperationResult.Success("目标已恢复")
                refreshGoalDetail(goalId)
                loadGoals()
                delay(2000)
                _operationResult.value = OperationResult.Idle
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error(e.message ?: "操作失败")
                delay(3000)
                _operationResult.value = OperationResult.Idle
            }
        }
    }

    // ==================== AI分析功能 ====================

    /**
     * 检查AI服务是否已配置
     */
    fun isAIConfigured(): Boolean {
        return aiService.isConfigured()
    }

    /**
     * 分析目标并给出建议
     */
    fun analyzeGoal(goalId: Long) {
        viewModelScope.launch {
            _aiAnalysisState.value = AIAnalysisState.Loading
            try {
                val goal = useCase.getGoalById(goalId)
                if (goal == null) {
                    _aiAnalysisState.value = AIAnalysisState.Error("目标不存在")
                    return@launch
                }

                val progress = useCase.calculateProgress(goal)
                val remainingDays = useCase.getRemainingDays(goal.endDate)

                val result = aiService.analyzeGoal(
                    title = goal.title,
                    description = goal.description,
                    category = goal.category,
                    goalType = goal.goalType,
                    progress = progress,
                    remainingDays = remainingDays
                )

                result.fold(
                    onSuccess = { analysis ->
                        _aiAnalysisState.value = AIAnalysisState.Success(analysis)
                    },
                    onFailure = { error ->
                        _aiAnalysisState.value = AIAnalysisState.Error(error.message ?: "分析失败")
                    }
                )
            } catch (e: Exception) {
                _aiAnalysisState.value = AIAnalysisState.Error(e.message ?: "分析失败")
            }
        }
    }

    /**
     * 清除AI分析状态
     */
    fun clearAIAnalysis() {
        _aiAnalysisState.value = AIAnalysisState.Idle
    }
}
