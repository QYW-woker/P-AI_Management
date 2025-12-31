package com.lifemanager.app.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.*
import com.lifemanager.app.data.repository.HealthAnalysisData
import com.lifemanager.app.data.repository.HealthRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 健康记录ViewModel
 */
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRecordRepository
) : ViewModel() {

    // ==================== UI状态 ====================

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Idle)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ==================== 数据流 ====================

    // 今日记录
    val todayRecords = repository.getTodayRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 最近记录
    val recentRecords = repository.getRecentRecords(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ==================== 今日概览数据 ====================

    private val _todaySummary = MutableStateFlow(TodayHealthSummary())
    val todaySummary: StateFlow<TodayHealthSummary> = _todaySummary.asStateFlow()

    private val _weeklyAnalysis = MutableStateFlow<HealthAnalysisData?>(null)
    val weeklyAnalysis: StateFlow<HealthAnalysisData?> = _weeklyAnalysis.asStateFlow()

    // ==================== 选中的记录类型 ====================

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    val filteredRecords = combine(recentRecords, _selectedType) { records, type ->
        if (type == null) records else records.filter { it.recordType == type }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ==================== 对话框状态 ====================

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _addDialogType = MutableStateFlow(HealthRecordType.WEIGHT)
    val addDialogType: StateFlow<String> = _addDialogType.asStateFlow()

    private val _editingRecord = MutableStateFlow<HealthRecordEntity?>(null)
    val editingRecord: StateFlow<HealthRecordEntity?> = _editingRecord.asStateFlow()

    private val _showDeleteConfirm = MutableStateFlow<HealthRecordEntity?>(null)
    val showDeleteConfirm: StateFlow<HealthRecordEntity?> = _showDeleteConfirm.asStateFlow()

    init {
        loadTodaySummary()
        loadWeeklyAnalysis()
    }

    // ==================== 加载数据 ====================

    fun loadTodaySummary() {
        viewModelScope.launch {
            try {
                val latestWeight = repository.getLatestWeight()
                val latestSleep = repository.getLatestSleep()
                val latestMood = repository.getLatestMood()
                val todayWater = repository.getTodayWaterIntake()
                val todayExercise = repository.getTodayExerciseMinutes()
                val todaySteps = repository.getTodaySteps()

                _todaySummary.value = TodayHealthSummary(
                    weight = latestWeight?.value,
                    sleepHours = latestSleep?.value,
                    sleepQuality = latestSleep?.rating,
                    moodRating = latestMood?.rating,
                    waterIntake = todayWater,
                    exerciseMinutes = todayExercise,
                    steps = todaySteps.toInt()
                )
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("加载数据失败: ${e.message}")
            }
        }
    }

    fun loadWeeklyAnalysis() {
        viewModelScope.launch {
            try {
                val analysis = repository.getHealthAnalysisData(7)
                _weeklyAnalysis.value = analysis
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    // ==================== 筛选 ====================

    fun selectType(type: String?) {
        _selectedType.value = type
    }

    // ==================== 对话框控制 ====================

    fun showAddDialog(type: String = HealthRecordType.WEIGHT) {
        _addDialogType.value = type
        _editingRecord.value = null
        _showAddDialog.value = true
    }

    fun showEditDialog(record: HealthRecordEntity) {
        _addDialogType.value = record.recordType
        _editingRecord.value = record
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
        _editingRecord.value = null
    }

    fun showDeleteConfirm(record: HealthRecordEntity) {
        _showDeleteConfirm.value = record
    }

    fun hideDeleteConfirm() {
        _showDeleteConfirm.value = null
    }

    // ==================== 记录操作 ====================

    fun saveRecord(
        type: String,
        value: Double,
        secondaryValue: Double? = null,
        rating: Int? = null,
        category: String? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val today = LocalDate.now()
                val time = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                )

                val existingRecord = _editingRecord.value

                if (existingRecord != null) {
                    // 更新现有记录
                    repository.update(
                        existingRecord.copy(
                            value = value,
                            secondaryValue = secondaryValue,
                            rating = rating,
                            category = category,
                            note = note,
                            time = time
                        )
                    )
                    _uiState.value = HealthUiState.Success("记录已更新")
                } else {
                    // 创建新记录
                    val record = HealthRecordEntity(
                        recordType = type,
                        date = today.toEpochDay().toInt(),
                        time = time,
                        value = value,
                        secondaryValue = secondaryValue,
                        rating = rating,
                        category = category,
                        note = note,
                        unit = HealthRecordType.getUnit(type)
                    )
                    repository.insert(record)
                    _uiState.value = HealthUiState.Success("记录已保存")
                }

                hideAddDialog()
                loadTodaySummary()
                loadWeeklyAnalysis()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("保存失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRecord(record: HealthRecordEntity) {
        viewModelScope.launch {
            try {
                repository.delete(record)
                _uiState.value = HealthUiState.Success("记录已删除")
                hideDeleteConfirm()
                loadTodaySummary()
                loadWeeklyAnalysis()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("删除失败: ${e.message}")
            }
        }
    }

    // ==================== 快捷记录方法 ====================

    fun quickRecordWeight(weight: Double) {
        viewModelScope.launch {
            try {
                repository.recordWeight(weight)
                _uiState.value = HealthUiState.Success("体重已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败")
            }
        }
    }

    fun quickRecordWater(ml: Double = 250.0) {
        viewModelScope.launch {
            try {
                repository.recordWater(ml)
                _uiState.value = HealthUiState.Success("饮水已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败")
            }
        }
    }

    fun quickRecordMood(rating: Int) {
        viewModelScope.launch {
            try {
                repository.recordMood(rating)
                _uiState.value = HealthUiState.Success("心情已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败")
            }
        }
    }

    fun quickRecordSteps(steps: Double) {
        viewModelScope.launch {
            try {
                repository.recordSteps(steps)
                _uiState.value = HealthUiState.Success("步数已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败")
            }
        }
    }

    // ==================== 状态清理 ====================

    fun clearUiState() {
        _uiState.value = HealthUiState.Idle
    }
}

/**
 * 健康页面UI状态
 */
sealed class HealthUiState {
    data object Idle : HealthUiState()
    data class Success(val message: String) : HealthUiState()
    data class Error(val message: String) : HealthUiState()
}

/**
 * 今日健康概览
 */
data class TodayHealthSummary(
    val weight: Double? = null,
    val sleepHours: Double? = null,
    val sleepQuality: Int? = null,
    val moodRating: Int? = null,
    val waterIntake: Double = 0.0,
    val exerciseMinutes: Double = 0.0,
    val steps: Int = 0
)
