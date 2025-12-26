package com.lifemanager.app.feature.timetrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.TimeCategoryEntity
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.domain.usecase.TimeTrackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 时间统计ViewModel
 */
@HiltViewModel
class TimeTrackViewModel @Inject constructor(
    private val useCase: TimeTrackUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<TimeTrackUiState>(TimeTrackUiState.Loading)
    val uiState: StateFlow<TimeTrackUiState> = _uiState.asStateFlow()

    // 今日记录
    private val _todayRecords = MutableStateFlow<List<TimeRecordWithCategory>>(emptyList())
    val todayRecords: StateFlow<List<TimeRecordWithCategory>> = _todayRecords.asStateFlow()

    // 分类列表
    private val _categories = MutableStateFlow<List<TimeCategoryEntity>>(emptyList())
    val categories: StateFlow<List<TimeCategoryEntity>> = _categories.asStateFlow()

    // 计时器状态
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // 今日统计
    private val _todayStats = MutableStateFlow(TodayTimeStats())
    val todayStats: StateFlow<TodayTimeStats> = _todayStats.asStateFlow()

    // 分类时长统计
    private val _categoryDurations = MutableStateFlow<List<CategoryDuration>>(emptyList())
    val categoryDurations: StateFlow<List<CategoryDuration>> = _categoryDurations.asStateFlow()

    // 显示选择分类对话框
    private val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog: StateFlow<Boolean> = _showCategoryDialog.asStateFlow()

    // 显示添加记录对话框
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(TimeRecordEditState())
    val editState: StateFlow<TimeRecordEditState> = _editState.asStateFlow()

    init {
        loadData()
        observeRecords()
        observeCategories()
        observeTimer()
        startTimerTicker()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = TimeTrackUiState.Loading
                _todayStats.value = useCase.getTodayStats()
                _uiState.value = TimeTrackUiState.Success
            } catch (e: Exception) {
                _uiState.value = TimeTrackUiState.Error(e.message ?: "加载失败")
            }
        }

        // 加载分类时长
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay().toInt()
            useCase.getCategoryDurations(today, today).collect {
                _categoryDurations.value = it
            }
        }
    }

    private fun observeRecords() {
        viewModelScope.launch {
            useCase.getTodayRecords().catch { e ->
                _uiState.value = TimeTrackUiState.Error(e.message ?: "加载失败")
            }.collect { records ->
                _todayRecords.value = records
                if (_uiState.value is TimeTrackUiState.Loading) {
                    _uiState.value = TimeTrackUiState.Success
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            useCase.getCategories().collect {
                _categories.value = it
            }
        }
    }

    private fun observeTimer() {
        viewModelScope.launch {
            useCase.getTimerState().collect {
                _timerState.value = it
            }
        }
    }

    private fun startTimerTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _timerState.value
                if (current.isRunning) {
                    val elapsed = (System.currentTimeMillis() - current.startTime) / 1000
                    _timerState.value = current.copy(elapsedSeconds = elapsed)
                }
            }
        }
    }

    fun refresh() {
        loadData()
    }

    /**
     * 显示分类选择对话框（开始计时）
     */
    fun showCategoryPicker() {
        _showCategoryDialog.value = true
    }

    fun hideCategoryPicker() {
        _showCategoryDialog.value = false
    }

    /**
     * 开始计时
     */
    fun startTimer(categoryId: Long?) {
        viewModelScope.launch {
            try {
                useCase.startTimer(categoryId)
                hideCategoryPicker()
                refresh()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 停止计时
     */
    fun stopTimer() {
        viewModelScope.launch {
            try {
                val recordId = _timerState.value.currentRecordId ?: return@launch
                useCase.stopTimer(recordId)
                refresh()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 显示手动添加对话框
     */
    fun showAddDialog() {
        _editState.value = TimeRecordEditState(
            date = LocalDate.now().toEpochDay().toInt()
        )
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
        _editState.value = TimeRecordEditState()
    }

    fun updateEditCategory(categoryId: Long?) {
        _editState.value = _editState.value.copy(categoryId = categoryId)
    }

    fun updateEditDuration(minutes: Int) {
        _editState.value = _editState.value.copy(durationMinutes = minutes)
    }

    fun updateEditNote(note: String) {
        _editState.value = _editState.value.copy(note = note)
    }

    /**
     * 保存手动记录
     */
    fun saveManualRecord() {
        val state = _editState.value
        if (state.durationMinutes <= 0) {
            _editState.value = state.copy(error = "请输入时长")
            return
        }

        viewModelScope.launch {
            try {
                _editState.value = state.copy(isSaving = true, error = null)
                useCase.addManualRecord(
                    categoryId = state.categoryId,
                    date = state.date,
                    durationMinutes = state.durationMinutes,
                    note = state.note
                )
                hideAddDialog()
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
     * 删除记录
     */
    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            useCase.deleteRecord(id)
            refresh()
        }
    }

    /**
     * 格式化时长
     */
    fun formatDuration(minutes: Int): String {
        return useCase.formatDuration(minutes)
    }

    /**
     * 格式化秒数为 HH:MM:SS
     */
    fun formatElapsedTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
