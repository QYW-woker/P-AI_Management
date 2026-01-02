package com.lifemanager.app.feature.habit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.HabitEntity
import com.lifemanager.app.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 习惯编辑页 ViewModel
 *
 * 负责管理习惯编辑页的所有状态和业务逻辑
 */
@HiltViewModel
class EditHabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: Long = savedStateHandle.get<Long>("id") ?: 0L
    val isEditMode: Boolean = habitId > 0L

    private val _uiState = MutableStateFlow<EditHabitUiState>(
        if (isEditMode) EditHabitUiState.Loading else EditHabitUiState.Ready
    )
    val uiState: StateFlow<EditHabitUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(HabitFormState())
    val editState: StateFlow<HabitFormState> = _editState.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    init {
        if (isEditMode) {
            loadHabit(habitId)
        }
    }

    private fun loadHabit(id: Long) {
        viewModelScope.launch {
            _uiState.value = EditHabitUiState.Loading
            try {
                val habit = habitRepository.getHabitById(id)
                if (habit == null) {
                    _uiState.value = EditHabitUiState.Error("习惯不存在")
                    return@launch
                }
                _editState.value = HabitFormState(
                    name = habit.name,
                    description = habit.description,
                    iconName = habit.iconName,
                    color = habit.color,
                    frequency = habit.frequency,
                    targetTimes = habit.targetTimes,
                    reminderTime = habit.reminderTime,
                    isNumeric = habit.isNumeric,
                    targetValue = habit.targetValue,
                    unit = habit.unit
                )
                _uiState.value = EditHabitUiState.Ready
            } catch (e: Exception) {
                _uiState.value = EditHabitUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    // 更新表单字段
    fun updateName(name: String) {
        _editState.value = _editState.value.copy(name = name, error = null)
    }

    fun updateDescription(description: String) {
        _editState.value = _editState.value.copy(description = description)
    }

    fun updateIconName(iconName: String) {
        _editState.value = _editState.value.copy(iconName = iconName)
    }

    fun updateColor(color: String) {
        _editState.value = _editState.value.copy(color = color)
    }

    fun updateFrequency(frequency: String) {
        _editState.value = _editState.value.copy(frequency = frequency)
    }

    fun updateTargetTimes(times: Int) {
        _editState.value = _editState.value.copy(targetTimes = times)
    }

    fun updateIsNumeric(isNumeric: Boolean) {
        _editState.value = _editState.value.copy(isNumeric = isNumeric)
    }

    fun updateTargetValue(value: Double?) {
        _editState.value = _editState.value.copy(targetValue = value)
    }

    fun updateUnit(unit: String) {
        _editState.value = _editState.value.copy(unit = unit)
    }

    fun updateReminderTime(time: String?) {
        _editState.value = _editState.value.copy(reminderTime = time)
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
                    id = if (isEditMode) habitId else 0,
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

                if (isEditMode) {
                    habitRepository.updateHabit(habit)
                } else {
                    habitRepository.saveHabit(habit)
                }

                _saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
                _saveResult.value = SaveResult.Error(e.message ?: "保存失败")
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}

/**
 * 编辑页UI状态
 */
sealed class EditHabitUiState {
    object Loading : EditHabitUiState()
    object Ready : EditHabitUiState()
    data class Error(val message: String) : EditHabitUiState()
}

/**
 * 习惯表单状态
 */
data class HabitFormState(
    val name: String = "",
    val description: String = "",
    val iconName: String = "check_circle",
    val color: String = "#4CAF50",
    val frequency: String = "DAILY",
    val targetTimes: Int = 1,
    val reminderTime: String? = null,
    val isNumeric: Boolean = false,
    val targetValue: Double? = null,
    val unit: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 保存结果
 */
sealed class SaveResult {
    object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}
