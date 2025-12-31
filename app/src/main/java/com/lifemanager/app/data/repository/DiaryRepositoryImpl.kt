package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.DiaryDao
import com.lifemanager.app.core.database.dao.MoodStat
import com.lifemanager.app.core.database.entity.DiaryEntity
import com.lifemanager.app.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日记仓库实现
 */
@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val dao: DiaryDao
) : DiaryRepository {

    override fun getByDateRange(startDate: Int, endDate: Int): Flow<List<DiaryEntity>> {
        return dao.getByDateRange(startDate, endDate)
    }

    override suspend fun getByDate(date: Int): DiaryEntity? {
        return dao.getByDate(date)
    }

    override fun getByDateFlow(date: Int): Flow<DiaryEntity?> {
        return dao.getByDateFlow(date)
    }

    override suspend fun getById(id: Long): DiaryEntity? {
        return dao.getById(id)
    }

    override fun getRecentDiaries(limit: Int): Flow<List<DiaryEntity>> {
        return dao.getRecentDiaries(limit)
    }

    override fun searchByContent(keyword: String): Flow<List<DiaryEntity>> {
        return dao.searchByContent(keyword)
    }

    override fun getByMoodScore(moodScore: Int): Flow<List<DiaryEntity>> {
        return dao.getByMoodScore(moodScore)
    }

    override fun getDiaryDates(startDate: Int, endDate: Int): Flow<List<Int>> {
        return dao.getDiaryDates(startDate, endDate)
    }

    override fun getMoodStats(startDate: Int, endDate: Int): Flow<List<MoodStat>> {
        return dao.getMoodStats(startDate, endDate)
    }

    override suspend fun insert(diary: DiaryEntity): Long {
        return dao.insert(diary)
    }

    override suspend fun update(diary: DiaryEntity) {
        dao.update(diary)
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun countAll(): Int {
        return dao.countAll()
    }

    override suspend fun getStreak(today: Int): Int {
        return dao.getStreak(today)
    }

    override fun getFavoriteDiaries(): Flow<List<DiaryEntity>> {
        return dao.getFavoriteDiaries()
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite)
    }
}
