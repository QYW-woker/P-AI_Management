package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.dao.DateTotal
import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.entity.TimeCategoryEntity
import com.lifemanager.app.core.database.entity.TimeRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 时间统计仓库接口
 */
interface TimeTrackRepository {

    // ==================== 时间记录 ====================

    fun getRecordsByDateRange(startDate: Int, endDate: Int): Flow<List<TimeRecordEntity>>

    fun getRecordsByDate(date: Int): Flow<List<TimeRecordEntity>>

    fun getActiveRecord(): Flow<TimeRecordEntity?>

    suspend fun hasActiveRecord(): Boolean

    fun getCategoryDurations(startDate: Int, endDate: Int): Flow<List<FieldTotal>>

    suspend fun getTotalDuration(date: Int): Int

    fun getDailyDurations(startDate: Int, endDate: Int): Flow<List<DateTotal>>

    suspend fun getRecordById(id: Long): TimeRecordEntity?

    suspend fun insertRecord(record: TimeRecordEntity): Long

    suspend fun updateRecord(record: TimeRecordEntity)

    suspend fun endRecord(id: Long, endTime: Long, durationMinutes: Int)

    suspend fun deleteRecord(id: Long)

    // ==================== 时间分类 ====================

    fun getEnabledCategories(): Flow<List<TimeCategoryEntity>>

    fun getAllCategories(): Flow<List<TimeCategoryEntity>>

    suspend fun getCategoryById(id: Long): TimeCategoryEntity?

    suspend fun insertCategory(category: TimeCategoryEntity): Long

    suspend fun updateCategory(category: TimeCategoryEntity)

    suspend fun deleteCategory(id: Long)
}
