package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.TimeCategoryEntity
import com.lifemanager.app.core.database.entity.TimeRecordEntity
import com.lifemanager.app.domain.model.*
import com.lifemanager.app.domain.repository.TimeTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 时间统计用例
 */
class TimeTrackUseCase @Inject constructor(
    private val repository: TimeTrackRepository
) {

    /**
     * 获取今日记录（带分类）
     */
    fun getTodayRecords(): Flow<List<TimeRecordWithCategory>> {
        val today = LocalDate.now().toEpochDay().toInt()
        return combine(
            repository.getRecordsByDate(today),
            repository.getEnabledCategories()
        ) { records, categories ->
            val categoryMap = categories.associateBy { it.id }
            records.map { record ->
                TimeRecordWithCategory(
                    record = record,
                    category = record.categoryId?.let { categoryMap[it] }
                )
            }
        }
    }

    /**
     * 获取分类列表
     */
    fun getCategories(): Flow<List<TimeCategoryEntity>> {
        return repository.getEnabledCategories()
    }

    /**
     * 获取活跃记录
     */
    fun getActiveRecord(): Flow<TimeRecordEntity?> {
        return repository.getActiveRecord()
    }

    /**
     * 获取计时器状态
     */
    fun getTimerState(): Flow<TimerState> {
        return combine(
            repository.getActiveRecord(),
            repository.getEnabledCategories()
        ) { activeRecord, categories ->
            if (activeRecord != null) {
                val category = activeRecord.categoryId?.let { catId ->
                    categories.find { it.id == catId }
                }
                val elapsed = (System.currentTimeMillis() - activeRecord.startTime) / 1000
                TimerState(
                    isRunning = true,
                    currentRecordId = activeRecord.id,
                    categoryId = activeRecord.categoryId,
                    categoryName = category?.name ?: "未分类",
                    startTime = activeRecord.startTime,
                    elapsedSeconds = elapsed
                )
            } else {
                TimerState()
            }
        }
    }

    /**
     * 获取今日统计
     */
    suspend fun getTodayStats(): TodayTimeStats {
        val today = LocalDate.now().toEpochDay().toInt()
        val totalMinutes = repository.getTotalDuration(today)
        val records = repository.getRecordsByDate(today).first()
        val hasActive = repository.hasActiveRecord()

        return TodayTimeStats(
            totalMinutes = totalMinutes,
            recordCount = records.size,
            isTracking = hasActive
        )
    }

    /**
     * 获取分类时长统计
     */
    fun getCategoryDurations(startDate: Int, endDate: Int): Flow<List<CategoryDuration>> {
        return combine(
            repository.getCategoryDurations(startDate, endDate),
            repository.getEnabledCategories()
        ) { durations, categories ->
            val categoryMap = categories.associateBy { it.id }
            val totalMinutes = durations.sumOf { it.total.toInt() }

            durations.map { duration ->
                val category = duration.fieldId?.let { categoryMap[it] }
                CategoryDuration(
                    categoryId = duration.fieldId,
                    categoryName = category?.name ?: "未分类",
                    categoryColor = category?.color ?: "#9E9E9E",
                    durationMinutes = duration.total.toInt(),
                    percentage = if (totalMinutes > 0) duration.total / totalMinutes * 100 else 0.0
                )
            }.sortedByDescending { it.durationMinutes }
        }
    }

    /**
     * 开始计时
     */
    suspend fun startTimer(categoryId: Long?, note: String = ""): Long {
        // 如果有活跃记录，先结束
        val activeRecord = repository.getActiveRecord().first()
        if (activeRecord != null) {
            stopTimer(activeRecord.id)
        }

        val today = LocalDate.now().toEpochDay().toInt()
        val now = System.currentTimeMillis()

        val record = TimeRecordEntity(
            categoryId = categoryId,
            date = today,
            startTime = now,
            note = note
        )
        return repository.insertRecord(record)
    }

    /**
     * 停止计时
     */
    suspend fun stopTimer(recordId: Long) {
        val record = repository.getRecordById(recordId) ?: return
        val endTime = System.currentTimeMillis()
        val durationMinutes = ((endTime - record.startTime) / 60000).toInt()
        repository.endRecord(recordId, endTime, durationMinutes)
    }

    /**
     * 手动添加记录
     */
    suspend fun addManualRecord(
        categoryId: Long?,
        date: Int,
        durationMinutes: Int,
        note: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        val record = TimeRecordEntity(
            categoryId = categoryId,
            date = date,
            startTime = now - durationMinutes * 60000L,
            endTime = now,
            durationMinutes = durationMinutes,
            note = note
        )
        return repository.insertRecord(record)
    }

    /**
     * 删除记录
     */
    suspend fun deleteRecord(id: Long) {
        repository.deleteRecord(id)
    }

    /**
     * 添加分类
     */
    suspend fun addCategory(
        name: String,
        color: String,
        iconName: String = "schedule"
    ): Long {
        val category = TimeCategoryEntity(
            name = name,
            color = color,
            iconName = iconName
        )
        return repository.insertCategory(category)
    }

    /**
     * 格式化时长
     */
    fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}分钟"
            minutes % 60 == 0 -> "${minutes / 60}小时"
            else -> "${minutes / 60}小时${minutes % 60}分钟"
        }
    }
}
