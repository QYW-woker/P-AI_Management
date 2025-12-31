package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.DailyHealthSummary
import com.lifemanager.app.core.database.dao.HealthRecordDao
import com.lifemanager.app.core.database.dao.HealthTypeSummary
import com.lifemanager.app.core.database.entity.HealthRecordEntity
import com.lifemanager.app.core.database.entity.HealthRecordType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 健康记录仓库
 *
 * 提供健康记录的数据操作接口
 */
@Singleton
class HealthRecordRepository @Inject constructor(
    private val healthRecordDao: HealthRecordDao
) {
    // ==================== 基础CRUD操作 ====================

    suspend fun insert(record: HealthRecordEntity): Long {
        return healthRecordDao.insert(record)
    }

    suspend fun insertAll(records: List<HealthRecordEntity>) {
        healthRecordDao.insertAll(records)
    }

    suspend fun update(record: HealthRecordEntity) {
        healthRecordDao.update(record.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(record: HealthRecordEntity) {
        healthRecordDao.delete(record)
    }

    suspend fun deleteById(id: Long) {
        healthRecordDao.deleteById(id)
    }

    suspend fun getById(id: Long): HealthRecordEntity? {
        return healthRecordDao.getById(id)
    }

    // ==================== Flow查询 ====================

    fun getAllRecords(): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getAll()
    }

    fun getRecordsByType(type: String): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getByType(type)
    }

    fun getRecordsByDate(date: LocalDate): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getByDate(date.toEpochDay().toInt())
    }

    fun getRecordsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getByDateRange(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    fun getRecordsByTypeAndDateRange(
        type: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getByTypeAndDateRange(
            type,
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    fun getRecentRecords(limit: Int = 20): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getRecent(limit)
    }

    fun getRecentRecordsByType(type: String, limit: Int = 10): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getRecentByType(type, limit)
    }

    fun getTodayRecords(): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getTodayRecords(LocalDate.now().toEpochDay().toInt())
    }

    fun getTodayRecordsByType(type: String): Flow<List<HealthRecordEntity>> {
        return healthRecordDao.getTodayRecordsByType(type, LocalDate.now().toEpochDay().toInt())
    }

    // ==================== 同步查询 ====================

    suspend fun getRecordsByTypeSync(type: String): List<HealthRecordEntity> {
        return healthRecordDao.getByTypeSync(type)
    }

    suspend fun getRecordsByDateRangeSync(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HealthRecordEntity> {
        return healthRecordDao.getByDateRangeSync(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    suspend fun getTodayRecordsSync(): List<HealthRecordEntity> {
        return healthRecordDao.getTodayRecordsSync(LocalDate.now().toEpochDay().toInt())
    }

    suspend fun getTodayRecordByType(type: String): HealthRecordEntity? {
        return healthRecordDao.getTodayRecordByType(type, LocalDate.now().toEpochDay().toInt())
    }

    // ==================== 统计查询 ====================

    suspend fun getAverageValue(
        type: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double? {
        return healthRecordDao.getAverageValue(
            type,
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    suspend fun getMaxValue(
        type: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double? {
        return healthRecordDao.getMaxValue(
            type,
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    suspend fun getMinValue(
        type: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double? {
        return healthRecordDao.getMinValue(
            type,
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    suspend fun getSumValue(
        type: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double? {
        return healthRecordDao.getSumValue(
            type,
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    suspend fun getLatestByType(type: String): HealthRecordEntity? {
        return healthRecordDao.getLatestByType(type)
    }

    suspend fun getDailyTrend(
        type: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyHealthSummary> {
        return healthRecordDao.getDailyAverageByType(
            type,
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    suspend fun getTypeSummary(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HealthTypeSummary> {
        return healthRecordDao.getTypeSummary(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取今日饮水总量（毫升）
     */
    suspend fun getTodayWaterIntake(): Double {
        return healthRecordDao.getTodayWaterIntake(LocalDate.now().toEpochDay().toInt())
    }

    /**
     * 获取今日运动总时长（分钟）
     */
    suspend fun getTodayExerciseMinutes(): Double {
        return healthRecordDao.getTodayExerciseMinutes(LocalDate.now().toEpochDay().toInt())
    }

    /**
     * 获取今日步数
     */
    suspend fun getTodaySteps(): Double {
        return healthRecordDao.getTodaySteps(LocalDate.now().toEpochDay().toInt())
    }

    /**
     * 获取最新体重
     */
    suspend fun getLatestWeight(): HealthRecordEntity? {
        return healthRecordDao.getLatestWeight()
    }

    /**
     * 获取最新睡眠记录
     */
    suspend fun getLatestSleep(): HealthRecordEntity? {
        return healthRecordDao.getLatestSleep()
    }

    /**
     * 获取最新心情记录
     */
    suspend fun getLatestMood(): HealthRecordEntity? {
        return healthRecordDao.getLatestMood()
    }

    // ==================== 快速记录方法 ====================

    /**
     * 快速记录体重
     */
    suspend fun recordWeight(weight: Double, note: String = ""): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.WEIGHT,
                date = today.toEpochDay().toInt(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                value = weight,
                unit = "kg",
                note = note
            )
        )
    }

    /**
     * 快速记录睡眠
     */
    suspend fun recordSleep(hours: Double, quality: Int, note: String = ""): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.SLEEP,
                date = today.toEpochDay().toInt(),
                value = hours,
                secondaryValue = quality.toDouble(),
                rating = quality,
                unit = "小时",
                note = note
            )
        )
    }

    /**
     * 快速记录运动
     */
    suspend fun recordExercise(
        minutes: Double,
        category: String,
        calories: Double? = null,
        note: String = ""
    ): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.EXERCISE,
                date = today.toEpochDay().toInt(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                value = minutes,
                secondaryValue = calories,
                category = category,
                unit = "分钟",
                note = note
            )
        )
    }

    /**
     * 快速记录心情
     */
    suspend fun recordMood(rating: Int, source: String? = null, note: String = ""): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.MOOD,
                date = today.toEpochDay().toInt(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                value = rating.toDouble(),
                rating = rating,
                category = source,
                note = note
            )
        )
    }

    /**
     * 快速记录饮水
     */
    suspend fun recordWater(ml: Double, note: String = ""): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.WATER,
                date = today.toEpochDay().toInt(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                value = ml,
                unit = "ml",
                note = note
            )
        )
    }

    /**
     * 快速记录步数
     */
    suspend fun recordSteps(steps: Double, note: String = ""): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.STEPS,
                date = today.toEpochDay().toInt(),
                value = steps,
                unit = "步",
                note = note
            )
        )
    }

    /**
     * 快速记录血压
     */
    suspend fun recordBloodPressure(
        systolic: Double,
        diastolic: Double,
        note: String = ""
    ): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.BLOOD_PRESSURE,
                date = today.toEpochDay().toInt(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                value = systolic,
                secondaryValue = diastolic,
                unit = "mmHg",
                note = note
            )
        )
    }

    /**
     * 快速记录心率
     */
    suspend fun recordHeartRate(bpm: Double, note: String = ""): Long {
        val today = LocalDate.now()
        return insert(
            HealthRecordEntity(
                recordType = HealthRecordType.HEART_RATE,
                date = today.toEpochDay().toInt(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                value = bpm,
                unit = "bpm",
                note = note
            )
        )
    }

    // ==================== 健康分析数据 ====================

    /**
     * 获取健康分析所需的综合数据
     */
    suspend fun getHealthAnalysisData(days: Int = 7): HealthAnalysisData {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)

        val records = getRecordsByDateRangeSync(startDate, today)
        val typeSummary = getTypeSummary(startDate, today)

        val weightRecords = records.filter { it.recordType == HealthRecordType.WEIGHT }
        val sleepRecords = records.filter { it.recordType == HealthRecordType.SLEEP }
        val exerciseRecords = records.filter { it.recordType == HealthRecordType.EXERCISE }
        val moodRecords = records.filter { it.recordType == HealthRecordType.MOOD }
        val waterRecords = records.filter { it.recordType == HealthRecordType.WATER }

        return HealthAnalysisData(
            periodDays = days,
            totalRecords = records.size,
            typeSummary = typeSummary,
            // 体重
            latestWeight = weightRecords.maxByOrNull { it.date }?.value,
            avgWeight = weightRecords.map { it.value }.takeIf { it.isNotEmpty() }?.average(),
            weightChange = if (weightRecords.size >= 2) {
                val sorted = weightRecords.sortedBy { it.date }
                sorted.last().value - sorted.first().value
            } else null,
            // 睡眠
            avgSleepHours = sleepRecords.map { it.value }.takeIf { it.isNotEmpty() }?.average(),
            avgSleepQuality = sleepRecords.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average(),
            // 运动
            totalExerciseMinutes = exerciseRecords.sumOf { it.value },
            exerciseDays = exerciseRecords.map { it.date }.distinct().size,
            totalCaloriesBurned = exerciseRecords.mapNotNull { it.secondaryValue }.sum(),
            // 心情
            avgMoodRating = moodRecords.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average(),
            moodDistribution = moodRecords.groupBy { it.rating ?: 3 }.mapValues { it.value.size },
            // 饮水
            avgWaterIntake = waterRecords.groupBy { it.date }.map { it.value.sumOf { r -> r.value } }
                .takeIf { it.isNotEmpty() }?.average(),
            totalWaterIntake = waterRecords.sumOf { it.value }
        )
    }
}

/**
 * 健康分析数据
 */
data class HealthAnalysisData(
    val periodDays: Int,
    val totalRecords: Int,
    val typeSummary: List<HealthTypeSummary>,
    // 体重相关
    val latestWeight: Double?,
    val avgWeight: Double?,
    val weightChange: Double?,
    // 睡眠相关
    val avgSleepHours: Double?,
    val avgSleepQuality: Double?,
    // 运动相关
    val totalExerciseMinutes: Double,
    val exerciseDays: Int,
    val totalCaloriesBurned: Double,
    // 心情相关
    val avgMoodRating: Double?,
    val moodDistribution: Map<Int, Int>,
    // 饮水相关
    val avgWaterIntake: Double?,
    val totalWaterIntake: Double
)
