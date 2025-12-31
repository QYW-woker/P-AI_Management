package com.lifemanager.app.core.health

import com.lifemanager.app.core.database.dao.*
import com.lifemanager.app.core.database.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 健康追踪服务
 *
 * 管理饮水记录、睡眠追踪和健康目标
 */
@Singleton
class HealthTrackingService @Inject constructor(
    private val waterIntakeDao: WaterIntakeDao,
    private val sleepRecordDao: SleepRecordDao,
    private val healthGoalDao: HealthGoalDao
) {

    companion object {
        // 默认目标
        const val DEFAULT_WATER_GOAL = 2000 // 毫升
        const val DEFAULT_SLEEP_GOAL = 480 // 分钟 (8小时)
        const val DEFAULT_STEPS_GOAL = 10000

        // 饮水杯量预设
        val WATER_PRESETS = listOf(
            WaterPreset("小杯", 150, "🥛"),
            WaterPreset("中杯", 250, "🥤"),
            WaterPreset("大杯", 350, "🍶"),
            WaterPreset("水瓶", 500, "💧"),
            WaterPreset("大瓶", 750, "🍼"),
            WaterPreset("自定义", 0, "✏️")
        )

        // 睡眠质量等级
        val SLEEP_QUALITY_LEVELS = listOf(
            SleepQualityLevel(1, "很差", "😫"),
            SleepQualityLevel(2, "较差", "😞"),
            SleepQualityLevel(3, "一般", "😐"),
            SleepQualityLevel(4, "良好", "😊"),
            SleepQualityLevel(5, "优秀", "😴")
        )
    }

    // ==================== 饮水记录 ====================

    /**
     * 获取今日饮水记录
     */
    fun getTodayWaterRecords(): Flow<List<WaterIntakeEntity>> {
        val today = LocalDate.now().toEpochDay().toInt()
        return waterIntakeDao.getByDate(today)
    }

    /**
     * 获取今日饮水总量
     */
    suspend fun getTodayWaterTotal(): Int {
        val today = LocalDate.now().toEpochDay().toInt()
        return waterIntakeDao.getDailyTotal(today) ?: 0
    }

    /**
     * 记录饮水
     */
    suspend fun recordWaterIntake(
        amount: Int,
        type: String = "水",
        note: String = ""
    ): Long {
        val now = LocalTime.now()
        val today = LocalDate.now().toEpochDay().toInt()

        return waterIntakeDao.insert(
            WaterIntakeEntity(
                date = today,
                time = "%02d:%02d".format(now.hour, now.minute),
                amount = amount,
                type = type,
                note = note
            )
        )
    }

    /**
     * 删除饮水记录
     */
    suspend fun deleteWaterRecord(id: Long) {
        waterIntakeDao.deleteById(id)
    }

    /**
     * 获取饮水进度
     */
    suspend fun getWaterProgress(): WaterProgress {
        val goals = healthGoalDao.getGoalsSync()
        val target = goals?.dailyWaterGoal ?: DEFAULT_WATER_GOAL
        val current = getTodayWaterTotal()

        return WaterProgress(
            current = current,
            target = target,
            percentage = minOf(100, current * 100 / target),
            remaining = maxOf(0, target - current)
        )
    }

    /**
     * 获取周饮水统计
     */
    suspend fun getWeeklyWaterStats(): WeeklyWaterStats {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val startDate = weekStart.toEpochDay().toInt()
        val endDate = today.toEpochDay().toInt()

        val dailyTotals = waterIntakeDao.getDailyTotals(startDate, endDate)
        val average = waterIntakeDao.getWeeklyAverage(startDate, endDate) ?: 0.0
        val goals = healthGoalDao.getGoalsSync()
        val target = goals?.dailyWaterGoal ?: DEFAULT_WATER_GOAL

        val daysReachedGoal = dailyTotals.count { it.total >= target }

        return WeeklyWaterStats(
            dailyTotals = dailyTotals.map { DailyWaterData(it.date, it.total) },
            averageDaily = average.toInt(),
            daysReachedGoal = daysReachedGoal,
            goalTarget = target
        )
    }

    // ==================== 睡眠记录 ====================

    /**
     * 获取今日睡眠记录
     */
    suspend fun getTodaySleepRecord(): SleepRecordEntity? {
        val today = LocalDate.now().toEpochDay().toInt()
        return sleepRecordDao.getByDate(today)
    }

    /**
     * 获取日期范围内的睡眠记录
     */
    fun getSleepRecords(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepRecordEntity>> {
        return sleepRecordDao.getByDateRange(
            startDate.toEpochDay().toInt(),
            endDate.toEpochDay().toInt()
        )
    }

    /**
     * 记录睡眠
     */
    suspend fun recordSleep(
        sleepTime: String,
        wakeTime: String,
        quality: Int,
        isNap: Boolean = false,
        note: String = "",
        tags: String = ""
    ): Long {
        val today = LocalDate.now().toEpochDay().toInt()
        val duration = calculateSleepDuration(sleepTime, wakeTime)

        return sleepRecordDao.insert(
            SleepRecordEntity(
                date = today,
                sleepTime = sleepTime,
                wakeTime = wakeTime,
                duration = duration,
                quality = quality,
                isNap = isNap,
                note = note,
                tags = tags
            )
        )
    }

    /**
     * 更新睡眠记录
     */
    suspend fun updateSleepRecord(record: SleepRecordEntity) {
        val duration = calculateSleepDuration(record.sleepTime, record.wakeTime)
        sleepRecordDao.update(record.copy(duration = duration))
    }

    /**
     * 删除睡眠记录
     */
    suspend fun deleteSleepRecord(id: Long) {
        sleepRecordDao.deleteById(id)
    }

    /**
     * 计算睡眠时长（分钟）
     */
    private fun calculateSleepDuration(sleepTime: String, wakeTime: String): Int {
        val sleepParts = sleepTime.split(":")
        val wakeParts = wakeTime.split(":")

        if (sleepParts.size != 2 || wakeParts.size != 2) return 0

        val sleepMinutes = sleepParts[0].toIntOrNull()?.times(60)?.plus(sleepParts[1].toIntOrNull() ?: 0) ?: 0
        val wakeMinutes = wakeParts[0].toIntOrNull()?.times(60)?.plus(wakeParts[1].toIntOrNull() ?: 0) ?: 0

        return if (wakeMinutes >= sleepMinutes) {
            wakeMinutes - sleepMinutes
        } else {
            // 跨夜
            (24 * 60 - sleepMinutes) + wakeMinutes
        }
    }

    /**
     * 获取睡眠进度
     */
    suspend fun getSleepProgress(): SleepProgress {
        val todaySleep = getTodaySleepRecord()
        val goals = healthGoalDao.getGoalsSync()
        val target = goals?.dailySleepGoal ?: DEFAULT_SLEEP_GOAL

        val current = todaySleep?.duration ?: 0

        return SleepProgress(
            current = current,
            target = target,
            percentage = minOf(100, current * 100 / target),
            quality = todaySleep?.quality ?: 0,
            sleepTime = todaySleep?.sleepTime,
            wakeTime = todaySleep?.wakeTime
        )
    }

    /**
     * 获取周睡眠统计
     */
    suspend fun getWeeklySleepStats(): WeeklySleepStats {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val startDate = weekStart.toEpochDay().toInt()
        val endDate = today.toEpochDay().toInt()

        val avgDuration = sleepRecordDao.getAverageDuration(startDate, endDate) ?: 0.0
        val avgQuality = sleepRecordDao.getAverageQuality(startDate, endDate) ?: 0.0
        val trend = sleepRecordDao.getSleepTrend(startDate, endDate)

        val goals = healthGoalDao.getGoalsSync()
        val target = goals?.dailySleepGoal ?: DEFAULT_SLEEP_GOAL

        val daysReachedGoal = trend.count { it.duration >= target }

        return WeeklySleepStats(
            averageDuration = avgDuration.toInt(),
            averageQuality = avgQuality,
            daysReachedGoal = daysReachedGoal,
            trend = trend.map { DailySleepData(it.date, it.duration, it.quality) },
            goalTarget = target
        )
    }

    // ==================== 健康目标 ====================

    /**
     * 获取健康目标
     */
    fun getHealthGoals(): Flow<HealthGoalEntity?> = healthGoalDao.getGoals()

    /**
     * 初始化健康目标
     */
    suspend fun initHealthGoals() {
        if (healthGoalDao.getGoalsSync() == null) {
            healthGoalDao.insert(
                HealthGoalEntity(
                    id = 1,
                    dailyWaterGoal = DEFAULT_WATER_GOAL,
                    dailySleepGoal = DEFAULT_SLEEP_GOAL,
                    dailyStepsGoal = DEFAULT_STEPS_GOAL,
                    targetWeight = null,
                    targetBMI = null
                )
            )
        }
    }

    /**
     * 更新健康目标
     */
    suspend fun updateHealthGoals(goals: HealthGoalEntity) {
        healthGoalDao.update(goals.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 设置饮水目标
     */
    suspend fun setWaterGoal(amount: Int) {
        val goals = healthGoalDao.getGoalsSync() ?: return
        healthGoalDao.update(
            goals.copy(
                dailyWaterGoal = amount,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * 设置睡眠目标
     */
    suspend fun setSleepGoal(minutes: Int) {
        val goals = healthGoalDao.getGoalsSync() ?: return
        healthGoalDao.update(
            goals.copy(
                dailySleepGoal = minutes,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // ==================== 健康报告 ====================

    /**
     * 生成日健康报告
     */
    suspend fun getDailyHealthReport(): DailyHealthReport {
        val waterProgress = getWaterProgress()
        val sleepProgress = getSleepProgress()

        val suggestions = mutableListOf<String>()

        // 饮水建议
        if (waterProgress.percentage < 50) {
            suggestions.add("今日饮水量不足，建议多喝水保持身体水分")
        } else if (waterProgress.percentage >= 100) {
            suggestions.add("今日饮水目标已达成，继续保持！")
        }

        // 睡眠建议
        if (sleepProgress.current > 0) {
            if (sleepProgress.current < 360) { // 少于6小时
                suggestions.add("睡眠时间偏短，建议保证7-8小时睡眠")
            }
            if (sleepProgress.quality < 3) {
                suggestions.add("睡眠质量欠佳，可以尝试调整睡眠环境")
            }
        }

        return DailyHealthReport(
            date = LocalDate.now(),
            waterProgress = waterProgress,
            sleepProgress = sleepProgress,
            suggestions = suggestions,
            overallScore = calculateHealthScore(waterProgress, sleepProgress)
        )
    }

    private fun calculateHealthScore(water: WaterProgress, sleep: SleepProgress): Int {
        var score = 0

        // 饮水评分 (40分)
        score += minOf(40, water.percentage * 40 / 100)

        // 睡眠评分 (60分)
        if (sleep.current > 0) {
            val durationScore = minOf(30, sleep.percentage * 30 / 100)
            val qualityScore = sleep.quality * 6 // 5分质量 = 30分
            score += durationScore + qualityScore
        }

        return score
    }
}

// ==================== 数据模型 ====================

data class WaterPreset(
    val name: String,
    val amount: Int,
    val icon: String
)

data class SleepQualityLevel(
    val level: Int,
    val name: String,
    val icon: String
)

data class WaterProgress(
    val current: Int,
    val target: Int,
    val percentage: Int,
    val remaining: Int
)

data class DailyWaterData(
    val date: Int,
    val total: Int
)

data class WeeklyWaterStats(
    val dailyTotals: List<DailyWaterData>,
    val averageDaily: Int,
    val daysReachedGoal: Int,
    val goalTarget: Int
)

data class SleepProgress(
    val current: Int,
    val target: Int,
    val percentage: Int,
    val quality: Int,
    val sleepTime: String?,
    val wakeTime: String?
)

data class DailySleepData(
    val date: Int,
    val duration: Int,
    val quality: Int
)

data class WeeklySleepStats(
    val averageDuration: Int,
    val averageQuality: Double,
    val daysReachedGoal: Int,
    val trend: List<DailySleepData>,
    val goalTarget: Int
)

data class DailyHealthReport(
    val date: LocalDate,
    val waterProgress: WaterProgress,
    val sleepProgress: SleepProgress,
    val suggestions: List<String>,
    val overallScore: Int
)
