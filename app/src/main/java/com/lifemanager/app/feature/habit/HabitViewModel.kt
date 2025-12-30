package com.lifemanager.app.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.ai.service.AIDataAnalysisService
import com.lifemanager.app.core.database.entity.AIAnalysisEntity
import com.lifemanager.app.core.database.entity.HabitEntity
import com.lifemanager.app.domain.model.HabitEditState
import com.lifemanager.app.domain.model.HabitStats
import com.lifemanager.app.domain.model.HabitUiState
import com.lifemanager.app.domain.model.HabitWithStatus
import com.lifemanager.app.domain.usecase.HabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 习惯打卡ViewModel
 */
@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitUseCase: HabitUseCase,
    private val aiAnalysisService: AIDataAnalysisService
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<HabitUiState>(HabitUiState.Loading)
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    // 习惯列表（带状态）
    private val _habits = MutableStateFlow<List<HabitWithStatus>>(emptyList())
    val habits: StateFlow<List<HabitWithStatus>> = _habits.asStateFlow()

    // 习惯统计
    private val _stats = MutableStateFlow(HabitStats())
    val stats: StateFlow<HabitStats> = _stats.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(HabitEditState())
    val editState: StateFlow<HabitEditState> = _editState.asStateFlow()

    // 对话框状态
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private var habitToDelete: Long? = null

    // AI分析状态
    private val _habitAnalysis = MutableStateFlow<AIAnalysisEntity?>(null)
    val habitAnalysis: StateFlow<AIAnalysisEntity?> = _habitAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    init {
        loadHabits()
        loadAIAnalysis()
    }

    /**
     * 加载习惯列表
     */
    private fun loadHabits() {
        viewModelScope.launch {
            _uiState.value = HabitUiState.Loading
            habitUseCase.getHabitsWithStatus()
                .catch { e ->
                    _uiState.value = HabitUiState.Error(e.message ?: "加载失败")
                }
                .collect { habits ->
                    _habits.value = habits
                    _uiState.value = HabitUiState.Success()
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
                _stats.value = habitUseCase.getHabitStats()
            } catch (e: Exception) {
                // 统计加载失败不影响主界面
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadHabits()
    }

    /**
     * 打卡/取消打卡
     */
    fun toggleCheckIn(habitId: Long) {
        viewModelScope.launch {
            try {
                habitUseCase.toggleCheckIn(habitId)
                loadStats()
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 更新数值型习惯的值
     */
    fun updateNumericValue(habitId: Long, value: Double) {
        viewModelScope.launch {
            try {
                habitUseCase.updateNumericValue(habitId, value)
                loadStats()
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 显示添加对话框
     */
    fun showAddDialog() {
        _editState.value = HabitEditState()
        _showEditDialog.value = true
    }

    /**
     * 显示编辑对话框
     */
    fun showEditDialog(habitId: Long) {
        viewModelScope.launch {
            val habit = habitUseCase.getHabitById(habitId)
            if (habit != null) {
                _editState.value = HabitEditState(
                    id = habit.id,
                    name = habit.name,
                    description = habit.description,
                    iconName = habit.iconName,
                    color = habit.color,
                    frequency = habit.frequency,
                    targetTimes = habit.targetTimes,
                    reminderTime = habit.reminderTime,
                    isNumeric = habit.isNumeric,
                    targetValue = habit.targetValue,
                    unit = habit.unit,
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
        _editState.value = HabitEditState()
    }

    /**
     * 更新编辑状态
     */
    fun updateEditName(name: String) {
        _editState.value = _editState.value.copy(name = name, error = null)
    }

    fun updateEditDescription(description: String) {
        _editState.value = _editState.value.copy(description = description)
    }

    fun updateEditIcon(iconName: String) {
        _editState.value = _editState.value.copy(iconName = iconName)
    }

    fun updateEditColor(color: String) {
        _editState.value = _editState.value.copy(color = color)
    }

    fun updateEditFrequency(frequency: String) {
        _editState.value = _editState.value.copy(frequency = frequency)
    }

    fun updateEditTargetTimes(times: Int) {
        _editState.value = _editState.value.copy(targetTimes = times)
    }

    fun updateEditIsNumeric(isNumeric: Boolean) {
        _editState.value = _editState.value.copy(isNumeric = isNumeric)
    }

    fun updateEditTargetValue(value: Double?) {
        _editState.value = _editState.value.copy(targetValue = value)
    }

    fun updateEditUnit(unit: String) {
        _editState.value = _editState.value.copy(unit = unit)
    }

    /**
     * 保存习惯
     */
    fun saveHabit() {
        val state = _editState.value

        // 验证
        if (state.name.isBlank()) {
            _editState.value = state.copy(error = "请输入习惯名称")
            return
        }

        viewModelScope.launch {
            _editState.value = state.copy(isSaving = true, error = null)
            try {
                val habit = HabitEntity(
                    id = if (state.isEditing) state.id else 0,
                    name = state.name.trim(),
                    description = state.description.trim(),
                    iconName = state.iconName,
                    color = state.color,
                    frequency = state.frequency,
                    targetTimes = state.targetTimes,
                    reminderTime = state.reminderTime,
                    isNumeric = state.isNumeric,
                    targetValue = state.targetValue,
                    unit = state.unit
                )

                if (state.isEditing) {
                    habitUseCase.updateHabit(habit)
                } else {
                    habitUseCase.saveHabit(habit)
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
     * 显示删除确认
     */
    fun showDeleteConfirm(habitId: Long) {
        habitToDelete = habitId
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
        habitToDelete = null
    }

    /**
     * 确认删除
     */
    fun confirmDelete() {
        val id = habitToDelete ?: return
        viewModelScope.launch {
            try {
                habitUseCase.deleteHabit(id)
                hideDeleteConfirm()
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 暂停习惯
     */
    fun pauseHabit(habitId: Long) {
        viewModelScope.launch {
            try {
                habitUseCase.pauseHabit(habitId)
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 恢复习惯
     */
    fun resumeHabit(habitId: Long) {
        viewModelScope.launch {
            try {
                habitUseCase.resumeHabit(habitId)
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 归档习惯
     */
    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            try {
                habitUseCase.archiveHabit(habitId)
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 加载AI分析结果
     */
    private fun loadAIAnalysis() {
        viewModelScope.launch {
            aiAnalysisService.getHabitAnalysis().collectLatest { analyses ->
                _habitAnalysis.value = analyses.firstOrNull()
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
                val result = aiAnalysisService.analyzeHabitData(forceRefresh = true)
                result.onSuccess { analysis ->
                    _habitAnalysis.value = analysis
                }
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}
