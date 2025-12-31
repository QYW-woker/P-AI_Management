package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.dao.MoodStat
import com.lifemanager.app.core.database.entity.DiaryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 日记仓库接口
 */
interface DiaryRepository {

    /**
     * 获取指定日期范围的日记
     */
    fun getByDateRange(startDate: Int, endDate: Int): Flow<List<DiaryEntity>>

    /**
     * 获取指定日期的日记
     */
    suspend fun getByDate(date: Int): DiaryEntity?

    /**
     * 获取指定日期的日记（Flow版本）
     */
    fun getByDateFlow(date: Int): Flow<DiaryEntity?>

    /**
     * 根据ID获取日记
     */
    suspend fun getById(id: Long): DiaryEntity?

    /**
     * 获取最近的日记
     */
    fun getRecentDiaries(limit: Int = 30): Flow<List<DiaryEntity>>

    /**
     * 搜索日记内容
     */
    fun searchByContent(keyword: String): Flow<List<DiaryEntity>>

    /**
     * 根据情绪评分筛选
     */
    fun getByMoodScore(moodScore: Int): Flow<List<DiaryEntity>>

    /**
     * 获取有日记的日期列表
     */
    fun getDiaryDates(startDate: Int, endDate: Int): Flow<List<Int>>

    /**
     * 获取情绪统计
     */
    fun getMoodStats(startDate: Int, endDate: Int): Flow<List<MoodStat>>

    /**
     * 插入或更新日记
     */
    suspend fun insert(diary: DiaryEntity): Long

    /**
     * 更新日记
     */
    suspend fun update(diary: DiaryEntity)

    /**
     * 删除日记
     */
    suspend fun deleteById(id: Long)

    /**
     * 统计日记总数
     */
    suspend fun countAll(): Int

    /**
     * 获取连续写日记天数
     */
    suspend fun getStreak(today: Int): Int

    /**
     * 获取收藏的日记
     */
    fun getFavoriteDiaries(): Flow<List<DiaryEntity>>

    /**
     * 设置收藏状态
     */
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
}
