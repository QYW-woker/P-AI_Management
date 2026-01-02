package com.lifemanager.app.feature.habit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.HabitEntity
import com.lifemanager.app.core.database.entity.HabitRecordEntity
import com.lifemanager.app.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 习惯详情页 ViewModel
 *
 * 负责管理习惯详情页的所有状态和业务逻辑
 */
@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow<HabitDetailUiState>(HabitDetailUiState.Loading)
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    private val _habit = MutableStateFlow<HabitEntity?>(null)
    val habit: StateFlow<HabitEntity?> = _habit.asStateFlow()

    private val _records = MutableStateFlow<List<HabitRecordEntity>>(emptyList())
    val records: StateFlow<List<HabitRecordEntity>> = _records.asStateFlow()

    private val _statistics = MutableStateFlow(HabitDetailStatistics())
    val statistics: StateFlow<HabitDetailStatistics> = _statistics.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    init {
        if (habitId > 0) {
            loadHabit(habitId)
        }
    }

    fun loadHabit(id: Long) {
        viewModelScope.launch {
            _uiState.value = HabitDetailUiState.Loading
            try {
                // 加载习惯信息
                val habitEntity = habitRepository.getHabitById(id)
                if (habitEntity == null) {
                    _uiState.value = HabitDetailUiState.Error("习惯不存在")
                    return@launch
                }
                _habit.value = habitEntity

                // 加载打卡记录
                loadRecords()

                // 计算统计数据
                calculateStatistics()

                _uiState.value = HabitDetailUiState.Success
            } catch (e: Exception) {
                _uiState.value = HabitDetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    private suspend fun loadRecords() {
        val month = _currentMonth.value
        val startDate = month.atDay(1).toEpochDay().toInt()
        val endDate = month.atEndOfMonth().toEpochDay().toInt()

        habitRepository.getRecordsByHabitAndDateRange(habitId, startDate, endDate)
            .collect { recordList ->
                _records.value = recordList
            }
    }

    private suspend fun calculateStatistics() {
        val today = LocalDate.now().toEpochDay().toInt()

        // 获取所有打卡记录（使用很长的日期范围来获取所有记录）
        val allRecords = habitRepository.getRecordsByHabitAndDateRange(habitId, 0, today + 365).first()
        val checkedDays = allRecords.map { it.date }.toSet()

        // 今日是否已打卡
        val isCheckedToday = checkedDays.contains(today)

        // 当前连续天数
        var currentStreak = 0
        var checkDate = if (isCheckedToday) today else today - 1
        while (checkedDays.contains(checkDate)) {
            currentStreak++
            checkDate--
        }

        // 最长连续天数
        val longestStreak = calculateLongestStreak(checkedDays)

        // 总打卡次数
        val totalCheckins = allRecords.size

        // 完成率（过去30天）
        val last30Days = (today - 29..today).toSet()
        val checkedInLast30 = checkedDays.intersect(last30Days).size
        val completionRate = checkedInLast30 / 30f

        _statistics.value = HabitDetailStatistics(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCheckins = totalCheckins,
            completionRate = completionRate,
            isCheckedToday = isCheckedToday
        )
    }

    private fun calculateLongestStreak(checkedDays: Set<Int>): Int {
        if (checkedDays.isEmpty()) return 0

        val sortedDays = checkedDays.sorted()
        var longest = 1
        var current = 1

        for (i in 1 until sortedDays.size) {
            if (sortedDays[i] == sortedDays[i - 1] + 1) {
                current++
                longest = maxOf(longest, current)
            } else {
                current = 1
            }
        }

        return longest
    }

    fun checkIn() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toEpochDay().toInt()
                val existingRecord = habitRepository.getRecordByHabitAndDate(habitId, today)

                if (existingRecord != null) {
                    // 取消打卡
                    habitRepository.deleteRecord(habitId, today)
                } else {
                    // 打卡
                    val record = HabitRecordEntity(
                        habitId = habitId,
                        date = today
                    )
                    habitRepository.saveRecord(record)
                }

                // 刷新数据
                loadRecords()
                calculateStatistics()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        viewModelScope.launch {
            loadRecords()
        }
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        viewModelScope.launch {
            loadRecords()
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            val currentHabit = _habit.value ?: return@launch
            val newStatus = if (currentHabit.status == "PAUSED") "ACTIVE" else "PAUSED"
            val updatedHabit = currentHabit.copy(status = newStatus)
            habitRepository.updateHabit(updatedHabit)
            _habit.value = updatedHabit
        }
    }

    fun showDeleteConfirm() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
    }

    fun confirmDelete(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val habitToDelete = _habit.value ?: return@launch
                habitRepository.deleteHabit(habitToDelete.id)
                hideDeleteConfirm()
                onComplete()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}
