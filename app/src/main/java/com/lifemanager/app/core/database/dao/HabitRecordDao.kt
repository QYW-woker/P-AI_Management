package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 习惯打卡记录DAO接口
 */
@Dao
interface HabitRecordDao {

    /**
     * 获取指定习惯指定日期范围的打卡记录
     */
    @Query("""
        SELECT * FROM habit_records
        WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    fun getByHabitAndDateRange(habitId: Long, startDate: Int, endDate: Int): Flow<List<HabitRecordEntity>>

    /**
     * 获取指定日期的所有习惯打卡记录
     */
    @Query("SELECT * FROM habit_records WHERE date = :date")
    fun getByDate(date: Int): Flow<List<HabitRecordEntity>>

    /**
     * 获取指定习惯指定日期的打卡记录
     */
    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun getByHabitAndDate(habitId: Long, date: Int): HabitRecordEntity?

    /**
     * 检查指定习惯指定日期是否已打卡
     */
    @Query("SELECT COUNT(*) FROM habit_records WHERE habitId = :habitId AND date = :date AND isCompleted = 1")
    suspend fun isCheckedIn(habitId: Long, date: Int): Int

    /**
     * 获取指定习惯的连续打卡天数
     */
    @Query("""
        SELECT COUNT(*) FROM habit_records
        WHERE habitId = :habitId
        AND date <= :today
        AND isCompleted = 1
    """)
    suspend fun getTotalCheckins(habitId: Long, today: Int): Int

    /**
     * 获取指定习惯在日期范围内的完成次数
     */
    @Query("""
        SELECT COUNT(*) FROM habit_records
        WHERE habitId = :habitId
        AND date BETWEEN :startDate AND :endDate
        AND isCompleted = 1
    """)
    suspend fun getCompletedCount(habitId: Long, startDate: Int, endDate: Int): Int

    /**
     * 获取今日各习惯的打卡状态
     */
    @Query("""
        SELECT habitId, isCompleted, value
        FROM habit_records
        WHERE date = :today
    """)
    fun getTodayStatus(today: Int): Flow<List<HabitCheckStatus>>

    /**
     * 插入打卡记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HabitRecordEntity): Long

    /**
     * 更新打卡记录
     */
    @Update
    suspend fun update(record: HabitRecordEntity)

    /**
     * 删除打卡记录
     */
    @Query("DELETE FROM habit_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 删除指定习惯指定日期的打卡记录
     */
    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun deleteByHabitAndDate(habitId: Long, date: Int)

    /**
     * 获取打卡日历数据
     */
    @Query("""
        SELECT date FROM habit_records
        WHERE habitId = :habitId AND isCompleted = 1
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getCheckedDates(habitId: Long, startDate: Int, endDate: Int): Flow<List<Int>>

    /**
     * 统计今日已打卡习惯数
     */
    @Query("SELECT COUNT(DISTINCT habitId) FROM habit_records WHERE date = :today AND isCompleted = 1")
    suspend fun countTodayCheckins(today: Int): Int

    /**
     * 获取指定日期范围内的所有打卡记录（同步版本，用于AI分析）
     */
    @Query("""
        SELECT * FROM habit_records
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    suspend fun getRecordsInRangeSync(startDate: Int, endDate: Int): List<HabitRecordEntity>
}

/**
 * 习惯打卡状态数据类
 */
data class HabitCheckStatus(
    val habitId: Long,
    val isCompleted: Boolean,
    val value: Double?
)
