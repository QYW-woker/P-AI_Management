package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.HealthRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 健康记录数据访问对象
 */
@Dao
interface HealthRecordDao {

    // ==================== 插入操作 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HealthRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HealthRecordEntity>)

    // ==================== 更新操作 ====================

    @Update
    suspend fun update(record: HealthRecordEntity)

    // ==================== 删除操作 ====================

    @Delete
    suspend fun delete(record: HealthRecordEntity)

    @Query("DELETE FROM health_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM health_records WHERE recordType = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM health_records WHERE date < :beforeDate")
    suspend fun deleteOldRecords(beforeDate: Int)

    // ==================== 查询操作 - Flow ====================

    /**
     * 获取所有记录
     */
    @Query("SELECT * FROM health_records ORDER BY date DESC, time DESC")
    fun getAll(): Flow<List<HealthRecordEntity>>

    /**
     * 按类型获取记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = :type ORDER BY date DESC, time DESC")
    fun getByType(type: String): Flow<List<HealthRecordEntity>>

    /**
     * 获取指定日期的记录
     */
    @Query("SELECT * FROM health_records WHERE date = :date ORDER BY time DESC")
    fun getByDate(date: Int): Flow<List<HealthRecordEntity>>

    /**
     * 获取指定日期范围的记录
     */
    @Query("SELECT * FROM health_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, time DESC")
    fun getByDateRange(startDate: Int, endDate: Int): Flow<List<HealthRecordEntity>>

    /**
     * 获取指定类型和日期范围的记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = :type AND date BETWEEN :startDate AND :endDate ORDER BY date DESC, time DESC")
    fun getByTypeAndDateRange(type: String, startDate: Int, endDate: Int): Flow<List<HealthRecordEntity>>

    /**
     * 获取最近N条记录
     */
    @Query("SELECT * FROM health_records ORDER BY date DESC, time DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<HealthRecordEntity>>

    /**
     * 获取指定类型的最近N条记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = :type ORDER BY date DESC, time DESC LIMIT :limit")
    fun getRecentByType(type: String, limit: Int): Flow<List<HealthRecordEntity>>

    /**
     * 获取今日记录
     */
    @Query("SELECT * FROM health_records WHERE date = :today ORDER BY time DESC")
    fun getTodayRecords(today: Int): Flow<List<HealthRecordEntity>>

    /**
     * 获取今日指定类型的记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = :type AND date = :today ORDER BY time DESC")
    fun getTodayRecordsByType(type: String, today: Int): Flow<List<HealthRecordEntity>>

    // ==================== 查询操作 - 同步 ====================

    @Query("SELECT * FROM health_records WHERE id = :id")
    suspend fun getById(id: Long): HealthRecordEntity?

    @Query("SELECT * FROM health_records WHERE recordType = :type ORDER BY date DESC, time DESC")
    suspend fun getByTypeSync(type: String): List<HealthRecordEntity>

    @Query("SELECT * FROM health_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByDateRangeSync(startDate: Int, endDate: Int): List<HealthRecordEntity>

    @Query("SELECT * FROM health_records WHERE recordType = :type AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByTypeAndDateRangeSync(type: String, startDate: Int, endDate: Int): List<HealthRecordEntity>

    @Query("SELECT * FROM health_records WHERE date = :today")
    suspend fun getTodayRecordsSync(today: Int): List<HealthRecordEntity>

    @Query("SELECT * FROM health_records WHERE recordType = :type AND date = :today LIMIT 1")
    suspend fun getTodayRecordByType(type: String, today: Int): HealthRecordEntity?

    // ==================== 统计查询 ====================

    /**
     * 获取指定类型的记录数量
     */
    @Query("SELECT COUNT(*) FROM health_records WHERE recordType = :type")
    suspend fun getCountByType(type: String): Int

    /**
     * 获取指定日期范围的记录数量
     */
    @Query("SELECT COUNT(*) FROM health_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getCountByDateRange(startDate: Int, endDate: Int): Int

    /**
     * 获取指定类型的平均值
     */
    @Query("SELECT AVG(value) FROM health_records WHERE recordType = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageValue(type: String, startDate: Int, endDate: Int): Double?

    /**
     * 获取指定类型的最大值
     */
    @Query("SELECT MAX(value) FROM health_records WHERE recordType = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getMaxValue(type: String, startDate: Int, endDate: Int): Double?

    /**
     * 获取指定类型的最小值
     */
    @Query("SELECT MIN(value) FROM health_records WHERE recordType = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getMinValue(type: String, startDate: Int, endDate: Int): Double?

    /**
     * 获取指定类型的总和
     */
    @Query("SELECT SUM(value) FROM health_records WHERE recordType = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getSumValue(type: String, startDate: Int, endDate: Int): Double?

    /**
     * 获取指定类型的最新记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = :type ORDER BY date DESC, time DESC LIMIT 1")
    suspend fun getLatestByType(type: String): HealthRecordEntity?

    /**
     * 按日期分组统计某类型的记录（用于趋势图）
     */
    @Query("""
        SELECT date, AVG(value) as value, AVG(secondaryValue) as secondaryValue, AVG(rating) as rating
        FROM health_records
        WHERE recordType = :type AND date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyAverageByType(type: String, startDate: Int, endDate: Int): List<DailyHealthSummary>

    /**
     * 获取今日饮水总量
     */
    @Query("SELECT COALESCE(SUM(value), 0) FROM health_records WHERE recordType = 'WATER' AND date = :today")
    suspend fun getTodayWaterIntake(today: Int): Double

    /**
     * 获取今日运动总时长
     */
    @Query("SELECT COALESCE(SUM(value), 0) FROM health_records WHERE recordType = 'EXERCISE' AND date = :today")
    suspend fun getTodayExerciseMinutes(today: Int): Double

    /**
     * 获取今日步数
     */
    @Query("SELECT COALESCE(SUM(value), 0) FROM health_records WHERE recordType = 'STEPS' AND date = :today")
    suspend fun getTodaySteps(today: Int): Double

    /**
     * 获取最近的体重记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = 'WEIGHT' ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeight(): HealthRecordEntity?

    /**
     * 获取最近的睡眠记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = 'SLEEP' ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSleep(): HealthRecordEntity?

    /**
     * 获取最近的心情记录
     */
    @Query("SELECT * FROM health_records WHERE recordType = 'MOOD' ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMood(): HealthRecordEntity?

    /**
     * 获取指定时间段内各类型的记录统计
     */
    @Query("""
        SELECT recordType, COUNT(*) as count, AVG(value) as avgValue, SUM(value) as totalValue
        FROM health_records
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY recordType
    """)
    suspend fun getTypeSummary(startDate: Int, endDate: Int): List<HealthTypeSummary>
}

/**
 * 每日健康摘要（用于趋势图）
 */
data class DailyHealthSummary(
    val date: Int,
    val value: Double?,
    val secondaryValue: Double?,
    val rating: Double?
)

/**
 * 健康类型统计摘要
 */
data class HealthTypeSummary(
    val recordType: String,
    val count: Int,
    val avgValue: Double?,
    val totalValue: Double?
)
