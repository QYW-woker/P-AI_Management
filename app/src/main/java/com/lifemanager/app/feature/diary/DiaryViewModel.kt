package com.lifemanager.app.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.DiaryEntity
import com.lifemanager.app.core.database.entity.DiaryLocation
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.domain.usecase.DiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 日记ViewModel
 */
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryUseCase: DiaryUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<DiaryUiState>(DiaryUiState.Loading)
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    // 当前年月
    private val _currentYearMonth = MutableStateFlow(
        YearMonth.now().let { it.year * 100 + it.monthValue }
    )
    val currentYearMonth: StateFlow<Int> = _currentYearMonth.asStateFlow()

    // 选中日期
    private val _selectedDate = MutableStateFlow(LocalDate.now().toEpochDay().toInt())
    val selectedDate: StateFlow<Int> = _selectedDate.asStateFlow()

    // 日记列表
    private val _diaries = MutableStateFlow<List<DiaryEntity>>(emptyList())
    val diaries: StateFlow<List<DiaryEntity>> = _diaries.asStateFlow()

    // 有日记的日期集合
    private val _diaryDates = MutableStateFlow<Set<Int>>(emptySet())
    val diaryDates: StateFlow<Set<Int>> = _diaryDates.asStateFlow()

    // 统计信息
    private val _statistics = MutableStateFlow(DiaryStatistics())
    val statistics: StateFlow<DiaryStatistics> = _statistics.asStateFlow()

    // 当前查看/编辑的日记
    private val _currentDiary = MutableStateFlow<DiaryEntity?>(null)
    val currentDiary: StateFlow<DiaryEntity?> = _currentDiary.asStateFlow()

    // 显示编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 显示删除确认
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(DiaryEditState())
    val editState: StateFlow<DiaryEditState> = _editState.asStateFlow()

    // 视图模式: LIST, CALENDAR
    private val _viewMode = MutableStateFlow("LIST")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    // 位置选择器显示状态
    private val _showLocationPicker = MutableStateFlow(false)
    val showLocationPicker: StateFlow<Boolean> = _showLocationPicker.asStateFlow()

    // 位置加载状态
    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<DiaryEntity>>(emptyList())
    val searchResults: StateFlow<List<DiaryEntity>> = _searchResults.asStateFlow()

    // 筛选条件
    private val _filterMood = MutableStateFlow<Int?>(null)
    val filterMood: StateFlow<Int?> = _filterMood.asStateFlow()

    // 收藏筛选
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        loadData()
        observeDiaries()
        observeDiaryDates()
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = DiaryUiState.Loading
                _statistics.value = diaryUseCase.getStatistics()
                _uiState.value = DiaryUiState.Success
            } catch (e: Exception) {
                _uiState.value = DiaryUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 观察日记列表
     */
    private fun observeDiaries() {
        viewModelScope.launch {
            _currentYearMonth.flatMapLatest { yearMonth ->
                diaryUseCase.getDiariesByMonth(yearMonth)
            }.catch { e ->
                _uiState.value = DiaryUiState.Error(e.message ?: "加载失败")
            }.collect { diaries ->
                _diaries.value = diaries
                if (_uiState.value is DiaryUiState.Loading) {
                    _uiState.value = DiaryUiState.Success
                }
            }
        }
    }

    /**
     * 观察有日记的日期
     */
    private fun observeDiaryDates() {
        viewModelScope.launch {
            _currentYearMonth.flatMapLatest { yearMonth ->
                diaryUseCase.getDiaryDates(yearMonth)
            }.collect { dates ->
                _diaryDates.value = dates
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }

    /**
     * 切换视图模式
     */
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == "LIST") "CALENDAR" else "LIST"
    }

    /**
     * 上个月
     */
    fun previousMonth() {
        val current = _currentYearMonth.value
        val year = current / 100
        val month = current % 100
        _currentYearMonth.value = if (month == 1) {
            (year - 1) * 100 + 12
        } else {
            year * 100 + (month - 1)
        }
    }

    /**
     * 下个月
     */
    fun nextMonth() {
        val current = _currentYearMonth.value
        val year = current / 100
        val month = current % 100
        _currentYearMonth.value = if (month == 12) {
            (year + 1) * 100 + 1
        } else {
            year * 100 + (month + 1)
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(date: Int) {
        _selectedDate.value = date
        viewModelScope.launch {
            _currentDiary.value = diaryUseCase.getDiaryByDate(date)
        }
    }

    /**
     * 格式化年月
     */
    fun formatYearMonth(yearMonth: Int): String {
        val year = yearMonth / 100
        val month = yearMonth % 100
        return "${year}年${month}月"
    }

    /**
     * 格式化日期
     */
    fun formatDate(epochDay: Int): String {
        return diaryUseCase.formatDate(epochDay)
    }

    /**
     * 获取星期
     */
    fun getDayOfWeek(epochDay: Int): String {
        return diaryUseCase.getDayOfWeek(epochDay)
    }

    /**
     * 显示添加/编辑对话框
     */
    fun showEditDialog(date: Int? = null) {
        val targetDate = date ?: _selectedDate.value

        viewModelScope.launch {
            val existing = diaryUseCase.getDiaryByDate(targetDate)

            _editState.value = if (existing != null) {
                DiaryEditState(
                    id = existing.id,
                    isEditing = true,
                    date = existing.date,
                    content = existing.content,
                    moodScore = existing.moodScore,
                    weather = existing.weather,
                    location = existing.location,
                    sleepMinutes = existing.sleepMinutes,
                    attachments = diaryUseCase.parseAttachments(existing.attachments)
                )
            } else {
                DiaryEditState(
                    date = targetDate
                )
            }
            _showEditDialog.value = true
        }
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editState.value = DiaryEditState()
    }

    /**
     * 更新编辑内容
     */
    fun updateEditContent(content: String) {
        _editState.value = _editState.value.copy(content = content)
    }

    /**
     * 更新编辑心情
     */
    fun updateEditMood(moodScore: Int?) {
        _editState.value = _editState.value.copy(moodScore = moodScore)
    }

    /**
     * 更新编辑天气
     */
    fun updateEditWeather(weather: String?) {
        _editState.value = _editState.value.copy(weather = weather)
    }

    /**
     * 更新编辑睡眠时长
     */
    fun updateEditSleep(sleepMinutes: Int?) {
        _editState.value = _editState.value.copy(sleepMinutes = sleepMinutes)
    }

    /**
     * 添加附件
     */
    fun addAttachment(uri: String) {
        val currentAttachments = _editState.value.attachments.toMutableList()
        if (!currentAttachments.contains(uri)) {
            currentAttachments.add(uri)
            _editState.value = _editState.value.copy(attachments = currentAttachments)
        }
    }

    /**
     * 移除附件
     */
    fun removeAttachment(uri: String) {
        val currentAttachments = _editState.value.attachments.toMutableList()
        currentAttachments.remove(uri)
        _editState.value = _editState.value.copy(attachments = currentAttachments)
    }

    /**
     * 保存日记
     */
    fun saveDiary() {
        val state = _editState.value
        if (state.content.isBlank()) {
            _editState.value = state.copy(error = "请输入日记内容")
            return
        }

        viewModelScope.launch {
            try {
                _editState.value = state.copy(isSaving = true, error = null)

                diaryUseCase.saveDiary(
                    date = state.date,
                    content = state.content,
                    moodScore = state.moodScore,
                    weather = state.weather,
                    location = state.location,
                    sleepMinutes = state.sleepMinutes,
                    attachments = state.attachments
                )

                hideEditDialog()
                refresh()

                // 更新当前查看的日记
                if (_selectedDate.value == state.date) {
                    _currentDiary.value = diaryUseCase.getDiaryByDate(state.date)
                }
            } catch (e: Exception) {
                _editState.value = state.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 显示删除确认
     */
    fun showDeleteConfirm() {
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
    }

    /**
     * 确认删除
     */
    fun confirmDelete() {
        val diary = _currentDiary.value ?: return

        viewModelScope.launch {
            try {
                diaryUseCase.deleteDiary(diary.id)
                hideDeleteConfirm()
                _currentDiary.value = null
                refresh()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    // ==================== 位置功能 ====================

    /**
     * 显示位置选择器
     */
    fun showLocationPicker() {
        _showLocationPicker.value = true
    }

    /**
     * 隐藏位置选择器
     */
    fun hideLocationPicker() {
        _showLocationPicker.value = false
    }

    /**
     * 设置位置
     */
    fun setLocation(location: DiaryLocation?) {
        _editState.value = _editState.value.copy(
            locationName = location?.name,
            locationAddress = location?.address,
            latitude = location?.latitude,
            longitude = location?.longitude,
            poiName = location?.poiName
        )
        hideLocationPicker()
    }

    /**
     * 清除位置
     */
    fun clearLocation() {
        _editState.value = _editState.value.copy(
            locationName = null,
            locationAddress = null,
            latitude = null,
            longitude = null,
            poiName = null
        )
    }

    /**
     * 请求当前位置
     */
    fun requestCurrentLocation() {
        _isLoadingLocation.value = true
        // 位置请求由UI层处理，获取到位置后调用setLocation
    }

    /**
     * 位置获取完成
     */
    fun onLocationResult(location: DiaryLocation?) {
        _isLoadingLocation.value = false
        if (location != null) {
            setLocation(location)
        }
    }

    // ==================== 搜索功能 ====================

    /**
     * 搜索日记
     */
    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                diaryUseCase.searchDiaries(query).collect { results ->
                    _searchResults.value = results
                }
            } catch (e: Exception) {
                // 搜索错误处理
            }
        }
    }

    /**
     * 清除搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // ==================== 筛选功能 ====================

    /**
     * 按心情筛选
     */
    fun filterByMood(moodScore: Int?) {
        _filterMood.value = moodScore
        applyFilters()
    }

    /**
     * 切换收藏筛选
     */
    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
        applyFilters()
    }

    /**
     * 应用筛选条件
     */
    private fun applyFilters() {
        viewModelScope.launch {
            val mood = _filterMood.value
            val favoritesOnly = _showFavoritesOnly.value

            if (mood == null && !favoritesOnly) {
                observeDiaries()
                return@launch
            }

            try {
                diaryUseCase.filterDiaries(
                    moodScore = mood,
                    favoritesOnly = favoritesOnly
                ).collect { filtered ->
                    _diaries.value = filtered
                }
            } catch (e: Exception) {
                // 筛选错误处理
            }
        }
    }

    /**
     * 清除所有筛选
     */
    fun clearFilters() {
        _filterMood.value = null
        _showFavoritesOnly.value = false
        observeDiaries()
    }

    // ==================== 收藏功能 ====================

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(diaryId: Long) {
        viewModelScope.launch {
            try {
                diaryUseCase.toggleFavorite(diaryId)
                // 刷新当前日记
                if (_currentDiary.value?.id == diaryId) {
                    _currentDiary.value = diaryUseCase.getDiaryByDate(_selectedDate.value)
                }
                refresh()
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    // ==================== 导出功能 ====================

    /**
     * 获取日记用于导出
     */
    suspend fun getDiaryForExport(id: Long): DiaryEntity? {
        return diaryUseCase.getDiaryById(id)
    }

    /**
     * 获取月份日记用于导出
     */
    suspend fun getMonthDiariesForExport(yearMonth: Int): List<DiaryEntity> {
        return diaryUseCase.getDiariesByMonthSync(yearMonth)
    }

    // ==================== 日期跳转 ====================

    /**
     * 跳转到今天
     */
    fun goToToday() {
        val today = LocalDate.now()
        _currentYearMonth.value = today.year * 100 + today.monthValue
        _selectedDate.value = today.toEpochDay().toInt()
        viewModelScope.launch {
            _currentDiary.value = diaryUseCase.getDiaryByDate(_selectedDate.value)
        }
    }

    /**
     * 跳转到指定年月
     */
    fun goToYearMonth(yearMonth: Int) {
        _currentYearMonth.value = yearMonth
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前位置信息
     */
    fun getCurrentLocation(): DiaryLocation? {
        val state = _editState.value
        if (state.latitude == null || state.longitude == null) return null
        return DiaryLocation(
            name = state.locationName ?: "",
            address = state.locationAddress,
            latitude = state.latitude,
            longitude = state.longitude,
            poiName = state.poiName
        )
    }

    /**
     * 检查是否有位置
     */
    fun hasLocation(): Boolean {
        return _editState.value.latitude != null && _editState.value.longitude != null
    }

    /**
     * 获取位置显示文本
     */
    fun getLocationDisplayText(): String? {
        val state = _editState.value
        return state.poiName ?: state.locationName
    }
}
