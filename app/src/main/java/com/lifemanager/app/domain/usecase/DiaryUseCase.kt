package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.DiaryEntity
import com.lifemanager.app.domain.model.DiaryStatistics
import com.lifemanager.app.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 日记用例
 */
class DiaryUseCase @Inject constructor(
    private val repository: DiaryRepository
) {

    /**
     * 获取最近日记
     */
    fun getRecentDiaries(limit: Int = 30): Flow<List<DiaryEntity>> {
        return repository.getRecentDiaries(limit)
    }

    /**
     * 获取指定月份的日记
     */
    fun getDiariesByMonth(yearMonth: Int): Flow<List<DiaryEntity>> {
        val year = yearMonth / 100
        val month = yearMonth % 100
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

        return repository.getByDateRange(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    /**
     * 获取指定日期的日记
     */
    suspend fun getDiaryByDate(date: Int): DiaryEntity? {
        return repository.getByDate(date)
    }

    /**
     * 获取指定日期的日记（Flow版本）
     */
    fun getDiaryByDateFlow(date: Int): Flow<DiaryEntity?> {
        return repository.getByDateFlow(date)
    }

    /**
     * 获取今日日记
     */
    suspend fun getTodayDiary(): DiaryEntity? {
        val today = LocalDate.now().toEpochDay().toInt()
        return repository.getByDate(today)
    }

    /**
     * 获取有日记的日期（用于日历）
     */
    fun getDiaryDates(yearMonth: Int): Flow<Set<Int>> {
        val year = yearMonth / 100
        val month = yearMonth % 100
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

        return repository.getDiaryDates(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        ).map { it.toSet() }
    }

    /**
     * 获取统计信息
     */
    suspend fun getStatistics(): DiaryStatistics {
        val today = LocalDate.now().toEpochDay().toInt()
        val totalCount = repository.countAll()
        val streak = repository.getStreak(today)

        // 获取最近30天的情绪统计
        val thirtyDaysAgo = LocalDate.now().minusDays(30).toEpochDay().toInt()
        val moodStats = repository.getMoodStats(thirtyDaysAgo, today).first()

        val moodDistribution = moodStats.associate { (it.moodScore ?: 0) to it.count }
        val totalMoodCount = moodStats.sumOf { it.count }
        val averageMood = if (totalMoodCount > 0) {
            moodStats.sumOf { (it.moodScore ?: 0) * it.count }.toDouble() / totalMoodCount
        } else 0.0

        return DiaryStatistics(
            totalCount = totalCount,
            currentStreak = streak,
            moodDistribution = moodDistribution,
            averageMood = averageMood
        )
    }

    /**
     * 保存日记（新建或更新）
     */
    suspend fun saveDiary(
        date: Int,
        content: String,
        moodScore: Int? = null,
        weather: String? = null,
        location: String? = null,
        sleepMinutes: Int? = null,
        attachments: List<String> = emptyList()
    ): Long {
        val existing = repository.getByDate(date)
        val attachmentsJson = convertAttachmentsToJson(attachments)

        val diary = if (existing != null) {
            existing.copy(
                content = content,
                moodScore = moodScore,
                weather = weather,
                location = location,
                sleepMinutes = sleepMinutes,
                attachments = attachmentsJson,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            DiaryEntity(
                date = date,
                content = content,
                moodScore = moodScore,
                weather = weather,
                location = location,
                sleepMinutes = sleepMinutes,
                attachments = attachmentsJson
            )
        }

        return repository.insert(diary)
    }

    /**
     * 将附件列表转换为JSON字符串
     */
    private fun convertAttachmentsToJson(attachments: List<String>): String {
        if (attachments.isEmpty()) return "[]"
        return "[${attachments.joinToString(",") { "\"$it\"" }}]"
    }

    /**
     * 解析附件JSON为列表
     */
    fun parseAttachments(attachmentsJson: String): List<String> {
        if (attachmentsJson.isBlank() || attachmentsJson == "[]") return emptyList()
        return try {
            attachmentsJson
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 删除日记
     */
    suspend fun deleteDiary(id: Long) {
        repository.deleteById(id)
    }

    /**
     * 搜索日记
     */
    fun searchDiaries(keyword: String): Flow<List<DiaryEntity>> {
        return repository.searchByContent(keyword)
    }

    /**
     * 格式化日期
     */
    fun formatDate(epochDay: Int): String {
        val date = LocalDate.ofEpochDay(epochDay.toLong())
        val today = LocalDate.now()

        return when (epochDay) {
            today.toEpochDay().toInt() -> "今天"
            today.minusDays(1).toEpochDay().toInt() -> "昨天"
            else -> if (date.year == today.year) {
                "${date.monthValue}月${date.dayOfMonth}日"
            } else {
                "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
            }
        }
    }

    /**
     * 获取日期的星期
     */
    fun getDayOfWeek(epochDay: Int): String {
        val date = LocalDate.ofEpochDay(epochDay.toLong())
        return when (date.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    }

    // ==================== 筛选和收藏功能 ====================

    /**
     * 根据情绪和收藏状态筛选日记
     */
    fun filterDiaries(
        moodScore: Int? = null,
        favoritesOnly: Boolean = false
    ): Flow<List<DiaryEntity>> {
        return when {
            favoritesOnly -> repository.getFavoriteDiaries()
            moodScore != null -> repository.getByMoodScore(moodScore)
            else -> getRecentDiaries()
        }
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(diaryId: Long) {
        val diary = repository.getById(diaryId)
        if (diary != null) {
            repository.setFavorite(diaryId, !diary.isFavorite)
        }
    }

    /**
     * 根据ID获取日记
     */
    suspend fun getDiaryById(id: Long): DiaryEntity? {
        return repository.getById(id)
    }

    /**
     * 同步获取指定月份的日记列表
     */
    suspend fun getDiariesByMonthSync(yearMonth: Int): List<DiaryEntity> {
        val year = yearMonth / 100
        val month = yearMonth % 100
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

        return repository.getByDateRange(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        ).first()
    }
}
