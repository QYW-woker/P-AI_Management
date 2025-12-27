package com.lifemanager.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 个人中心UI状态
 */
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object Success : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

/**
 * 用户统计数据
 */
data class UserStatistics(
    val totalDays: Int = 0,           // 使用天数
    val totalTodos: Int = 0,          // 累计待办
    val totalDiaries: Int = 0,        // 累计日记
    val totalFocusHours: Int = 0,     // 累计专注时长
    val totalHabitCheckins: Int = 0,  // 累计打卡
    val totalSavingsAmount: Double = 0.0, // 累计存款
    val completedGoals: Int = 0,      // 完成目标
    val currentStreak: Int = 0        // 当前连续使用天数
)

/**
 * 个人中心ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val diaryRepository: DiaryRepository,
    private val habitRepository: HabitRepository,
    private val timeTrackRepository: TimeTrackRepository,
    private val savingsPlanRepository: SavingsPlanRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // 用户统计
    private val _statistics = MutableStateFlow(UserStatistics())
    val statistics: StateFlow<UserStatistics> = _statistics.asStateFlow()

    init {
        loadStatistics()
    }

    /**
     * 加载统计数据
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val stats = calculateStatistics()
                _statistics.value = stats
                _uiState.value = ProfileUiState.Success
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 计算统计数据
     */
    private suspend fun calculateStatistics(): UserStatistics {
        val today = LocalDate.now().toEpochDay().toInt()

        // 获取日记数据 - 获取从很早开始到今天的所有日记
        val allDiaries = diaryRepository.getByDateRange(0, today).first()
        val earliestDate = allDiaries.minOfOrNull { it.date } ?: today
        val totalDays = today - earliestDate + 1

        // 待办总数 - 获取已完成和未完成的
        val completedTodos = todoRepository.getCompletedTodos(Int.MAX_VALUE).first()
        val pendingTodos = todoRepository.getPendingTodos().first()
        val totalTodos = completedTodos.size + pendingTodos.size

        // 日记总数
        val totalDiaries = allDiaries.size

        // 专注时长（分钟转小时）
        val allTimeRecords = timeTrackRepository.getRecordsByDateRange(0, today).first()
        val totalFocusMinutes = allTimeRecords.sumOf { it.durationMinutes }
        val totalFocusHours = totalFocusMinutes / 60

        // 习惯打卡总数
        val habits = habitRepository.getActiveHabits().first()
        var totalCheckins = 0
        habits.forEach { habit ->
            totalCheckins += habitRepository.getTotalCheckins(habit.id, today)
        }

        // 存款总额
        val savingsPlans = savingsPlanRepository.getActivePlans().first()
        val totalSavings = savingsPlans.sumOf { it.currentAmount }

        // 完成的目标
        val allGoals = goalRepository.getAllGoals().first()
        val completedGoals = allGoals.count { it.status == "COMPLETED" }

        // 连续使用天数 - 使用日记仓库提供的方法
        val streak = diaryRepository.getStreak(today)

        return UserStatistics(
            totalDays = totalDays.coerceAtLeast(1),
            totalTodos = totalTodos,
            totalDiaries = totalDiaries,
            totalFocusHours = totalFocusHours,
            totalHabitCheckins = totalCheckins,
            totalSavingsAmount = totalSavings,
            completedGoals = completedGoals,
            currentStreak = streak
        )
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadStatistics()
    }
}
