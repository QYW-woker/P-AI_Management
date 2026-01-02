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

    // ==================== 输入验证 ====================

    /**
     * 验证健康记录数值的有效范围
     * @return 错误消息，如果验证通过返回null
     */
    private fun validateRecordValue(type: String, value: Double, secondaryValue: Double? = null, rating: Int? = null): String? {
        // 验证记录类型
        val validTypes = listOf(
            HealthRecordType.WEIGHT, HealthRecordType.SLEEP, HealthRecordType.EXERCISE,
            HealthRecordType.MOOD, HealthRecordType.WATER, HealthRecordType.BLOOD_PRESSURE,
            HealthRecordType.HEART_RATE, HealthRecordType.STEPS, HealthRecordType.CUSTOM
        )
        if (type !in validTypes) {
            return "无效的记录类型"
        }

        // 按类型验证数值范围
        return when (type) {
            HealthRecordType.WEIGHT -> {
                when {
                    value <= 0 -> "体重必须大于0"
                    value < 20 -> "体重数值过低，请检查输入"
                    value > 500 -> "体重数值过高，请检查输入"
                    else -> null
                }
            }
            HealthRecordType.SLEEP -> {
                when {
                    value < 0 -> "睡眠时长不能为负数"
                    value > 24 -> "睡眠时长不能超过24小时"
                    else -> null
                }
            }
            HealthRecordType.EXERCISE -> {
                when {
                    value < 0 -> "运动时长不能为负数"
                    value > 1440 -> "运动时长不能超过24小时"
                    secondaryValue != null && secondaryValue < 0 -> "消耗热量不能为负数"
                    else -> null
                }
            }
            HealthRecordType.MOOD -> {
                val moodValue = rating ?: value.toInt()
                when {
                    moodValue < 1 || moodValue > 5 -> "心情评分必须在1-5之间"
                    else -> null
                }
            }
            HealthRecordType.WATER -> {
                when {
                    value <= 0 -> "饮水量必须大于0"
                    value > 10000 -> "单次饮水量不能超过10000ml"
                    else -> null
                }
            }
            HealthRecordType.BLOOD_PRESSURE -> {
                when {
                    value < 60 || value > 300 -> "收缩压应在60-300mmHg之间"
                    secondaryValue == null -> "请输入舒张压"
                    secondaryValue < 30 || secondaryValue > 200 -> "舒张压应在30-200mmHg之间"
                    value <= secondaryValue -> "收缩压应大于舒张压"
                    else -> null
                }
            }
            HealthRecordType.HEART_RATE -> {
                when {
                    value < 30 || value > 300 -> "心率应在30-300bpm之间"
                    else -> null
                }
            }
            HealthRecordType.STEPS -> {
                when {
                    value < 0 -> "步数不能为负数"
                    value > 200000 -> "单日步数过高，请检查输入"
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * 验证备注长度
     */
    private fun validateNote(note: String): String? {
        return if (note.length > 500) "备注不能超过500个字符" else null
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
        // 输入验证
        val validationError = validateRecordValue(type, value, secondaryValue, rating)
        if (validationError != null) {
            _uiState.value = HealthUiState.Error(validationError)
            return
        }

        val noteError = validateNote(note)
        if (noteError != null) {
            _uiState.value = HealthUiState.Error(noteError)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val today = LocalDate.now()
                val time = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                )

                val existingRecord = _editingRecord.value
                val trimmedNote = note.trim()

                if (existingRecord != null) {
                    // 更新现有记录
                    repository.update(
                        existingRecord.copy(
                            value = value,
                            secondaryValue = secondaryValue,
                            rating = rating,
                            category = category,
                            note = trimmedNote,
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
                        note = trimmedNote,
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
        // 验证输入
        val error = validateRecordValue(HealthRecordType.WEIGHT, weight)
        if (error != null) {
            _uiState.value = HealthUiState.Error(error)
            return
        }

        viewModelScope.launch {
            try {
                repository.recordWeight(weight)
                _uiState.value = HealthUiState.Success("体重 ${String.format("%.1f", weight)}kg 已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败: ${e.message}")
            }
        }
    }

    fun quickRecordWater(ml: Double = 250.0) {
        // 验证输入
        val error = validateRecordValue(HealthRecordType.WATER, ml)
        if (error != null) {
            _uiState.value = HealthUiState.Error(error)
            return
        }

        viewModelScope.launch {
            try {
                repository.recordWater(ml)
                _uiState.value = HealthUiState.Success("饮水 ${ml.toInt()}ml 已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败: ${e.message}")
            }
        }
    }

    fun quickRecordMood(rating: Int) {
        // 验证输入
        if (rating < 1 || rating > 5) {
            _uiState.value = HealthUiState.Error("心情评分必须在1-5之间")
            return
        }

        viewModelScope.launch {
            try {
                repository.recordMood(rating)
                val moodText = MoodRating.getDisplayName(rating)
                _uiState.value = HealthUiState.Success("心情「$moodText」已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败: ${e.message}")
            }
        }
    }

    fun quickRecordSteps(steps: Double) {
        // 验证输入
        val error = validateRecordValue(HealthRecordType.STEPS, steps)
        if (error != null) {
            _uiState.value = HealthUiState.Error(error)
            return
        }

        viewModelScope.launch {
            try {
                repository.recordSteps(steps)
                _uiState.value = HealthUiState.Success("步数 ${steps.toInt()} 已记录")
                loadTodaySummary()
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error("记录失败: ${e.message}")
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
