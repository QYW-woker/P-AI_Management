package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.DateTotal
import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.dao.TimeCategoryDao
import com.lifemanager.app.core.database.dao.TimeRecordDao
import com.lifemanager.app.core.database.entity.TimeCategoryEntity
import com.lifemanager.app.core.database.entity.TimeRecordEntity
import com.lifemanager.app.domain.repository.TimeTrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 时间统计仓库实现
 */
@Singleton
class TimeTrackRepositoryImpl @Inject constructor(
    private val recordDao: TimeRecordDao,
    private val categoryDao: TimeCategoryDao
) : TimeTrackRepository {

    override fun getRecordsByDateRange(startDate: Int, endDate: Int): Flow<List<TimeRecordEntity>> {
        return recordDao.getByDateRange(startDate, endDate)
    }

    override fun getRecordsByDate(date: Int): Flow<List<TimeRecordEntity>> {
        return recordDao.getByDate(date)
    }

    override fun getActiveRecord(): Flow<TimeRecordEntity?> {
        return recordDao.getActiveRecord()
    }

    override suspend fun hasActiveRecord(): Boolean {
        return recordDao.hasActiveRecord() > 0
    }

    override fun getCategoryDurations(startDate: Int, endDate: Int): Flow<List<FieldTotal>> {
        return recordDao.getCategoryDurations(startDate, endDate)
    }

    override suspend fun getTotalDuration(date: Int): Int {
        return recordDao.getTotalDuration(date)
    }

    override fun getDailyDurations(startDate: Int, endDate: Int): Flow<List<DateTotal>> {
        return recordDao.getDailyDurations(startDate, endDate)
    }

    override suspend fun getRecordById(id: Long): TimeRecordEntity? {
        return recordDao.getById(id)
    }

    override suspend fun insertRecord(record: TimeRecordEntity): Long {
        return recordDao.insert(record)
    }

    override suspend fun updateRecord(record: TimeRecordEntity) {
        recordDao.update(record)
    }

    override suspend fun endRecord(id: Long, endTime: Long, durationMinutes: Int) {
        recordDao.endRecord(id, endTime, durationMinutes)
    }

    override suspend fun deleteRecord(id: Long) {
        recordDao.deleteById(id)
    }

    override fun getEnabledCategories(): Flow<List<TimeCategoryEntity>> {
        return categoryDao.getEnabledCategories()
    }

    override fun getAllCategories(): Flow<List<TimeCategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun getCategoryById(id: Long): TimeCategoryEntity? {
        return categoryDao.getById(id)
    }

    override suspend fun insertCategory(category: TimeCategoryEntity): Long {
        return categoryDao.insert(category)
    }

    override suspend fun updateCategory(category: TimeCategoryEntity) {
        categoryDao.update(category)
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }
}
