package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

/**
 * 习惯DAO接口
 */
@Dao
interface HabitDao {

    /**
     * 获取所有活跃习惯
     */
    @Query("""
        SELECT * FROM habits
        WHERE status = 'ACTIVE'
        ORDER BY createdAt ASC
    """)
    fun getActiveHabits(): Flow<List<HabitEntity>>

    /**
     * 获取所有习惯（包括暂停的）
     */
    @Query("""
        SELECT * FROM habits
        WHERE status != 'ARCHIVED'
        ORDER BY status ASC, createdAt ASC
    """)
    fun getAllHabits(): Flow<List<HabitEntity>>

    /**
     * 获取归档的习惯
     */
    @Query("SELECT * FROM habits WHERE status = 'ARCHIVED' ORDER BY createdAt DESC")
    fun getArchivedHabits(): Flow<List<HabitEntity>>

    /**
     * 根据ID获取习惯
     */
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    /**
     * 根据ID获取习惯（Flow版本）
     */
    @Query("SELECT * FROM habits WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<HabitEntity?>

    /**
     * 获取关联指定目标的习惯
     */
    @Query("SELECT * FROM habits WHERE linkedGoalId = :goalId")
    fun getByGoal(goalId: Long): Flow<List<HabitEntity>>

    /**
     * 获取有提醒的习惯
     */
    @Query("""
        SELECT * FROM habits
        WHERE status = 'ACTIVE' AND reminderTime IS NOT NULL
        ORDER BY reminderTime ASC
    """)
    fun getHabitsWithReminder(): Flow<List<HabitEntity>>

    /**
     * 插入习惯
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    /**
     * 更新习惯
     */
    @Update
    suspend fun update(habit: HabitEntity)

    /**
     * 更新习惯状态
     */
    @Query("UPDATE habits SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    /**
     * 删除习惯
     */
    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 统计活跃习惯数量
     */
    @Query("SELECT COUNT(*) FROM habits WHERE status = 'ACTIVE'")
    suspend fun countActive(): Int

    /**
     * 获取所有活跃习惯（同步版本，用于AI分析）
     */
    @Query("""
        SELECT * FROM habits
        WHERE status = 'ACTIVE'
        ORDER BY createdAt ASC
    """)
    suspend fun getEnabledSync(): List<HabitEntity>

    /**
     * 获取所有活跃习惯（同步版本，用于Widget）
     */
    @Query("""
        SELECT * FROM habits
        WHERE status = 'ACTIVE'
        ORDER BY createdAt ASC
    """)
    suspend fun getActiveHabitsSync(): List<HabitEntity>
}
