package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * 订阅服务DAO
 */
@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE status = :status ORDER BY nextBillingDate ASC")
    fun getSubscriptionsByStatus(status: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE status = 'ACTIVE' ORDER BY nextBillingDate ASC")
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE type = :type ORDER BY name")
    fun getSubscriptionsByType(type: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionEntity?

    /**
     * 获取即将到期的订阅（需要提醒）
     */
    @Query("""
        SELECT * FROM subscriptions
        WHERE status = 'ACTIVE'
        AND reminderEnabled = 1
        AND nextBillingDate <= :targetDate
        ORDER BY nextBillingDate ASC
    """)
    suspend fun getUpcomingBillings(targetDate: Int): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity): Long

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 更新下次扣款日期
     */
    @Query("""
        UPDATE subscriptions
        SET nextBillingDate = :nextDate,
            totalPaid = totalPaid + amount,
            paymentCount = paymentCount + 1,
            updatedAt = :now
        WHERE id = :subscriptionId
    """)
    suspend fun updateNextBillingDate(subscriptionId: Long, nextDate: Int, now: Long = System.currentTimeMillis())

    /**
     * 统计活跃订阅
     */
    @Query("""
        SELECT
            COUNT(*) as count,
            SUM(CASE WHEN billingCycle = 'MONTHLY' THEN amount
                     WHEN billingCycle = 'YEARLY' THEN amount / 12
                     WHEN billingCycle = 'QUARTERLY' THEN amount / 3
                     WHEN billingCycle = 'WEEKLY' THEN amount * 4.33
                     ELSE amount END) as monthlyTotal,
            SUM(CASE WHEN billingCycle = 'MONTHLY' THEN amount * 12
                     WHEN billingCycle = 'YEARLY' THEN amount
                     WHEN billingCycle = 'QUARTERLY' THEN amount * 4
                     WHEN billingCycle = 'WEEKLY' THEN amount * 52
                     ELSE amount * 12 END) as yearlyTotal
        FROM subscriptions
        WHERE status = 'ACTIVE'
    """)
    suspend fun getActiveStats(): SubscriptionStats

    /**
     * 按类型统计
     */
    @Query("""
        SELECT type,
               COUNT(*) as count,
               SUM(CASE WHEN billingCycle = 'MONTHLY' THEN amount * 12
                        WHEN billingCycle = 'YEARLY' THEN amount
                        WHEN billingCycle = 'QUARTERLY' THEN amount * 4
                        WHEN billingCycle = 'WEEKLY' THEN amount * 52
                        ELSE amount * 12 END) as yearlyTotal
        FROM subscriptions
        WHERE status = 'ACTIVE'
        GROUP BY type
        ORDER BY yearlyTotal DESC
    """)
    suspend fun getStatsByType(): List<SubscriptionTypeStats>
}

data class SubscriptionStats(
    val count: Int,
    val monthlyTotal: Double?,
    val yearlyTotal: Double?
)

data class SubscriptionTypeStats(
    val type: String,
    val count: Int,
    val yearlyTotal: Double
)

/**
 * 订阅付款记录DAO
 */
@Dao
interface SubscriptionPaymentDao {

    @Query("SELECT * FROM subscription_payments WHERE subscriptionId = :subscriptionId ORDER BY paymentDate DESC")
    fun getBySubscriptionId(subscriptionId: Long): Flow<List<SubscriptionPaymentEntity>>

    @Query("SELECT * FROM subscription_payments WHERE paymentDate BETWEEN :startDate AND :endDate ORDER BY paymentDate DESC")
    suspend fun getByDateRange(startDate: Int, endDate: Int): List<SubscriptionPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: SubscriptionPaymentEntity): Long

    @Query("DELETE FROM subscription_payments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/**
 * 成就DAO
 */
@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, sortOrder ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE type = :type ORDER BY sortOrder")
    fun getAchievementsByType(type: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE code = :code")
    suspend fun getByCode(code: String): AchievementEntity?

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getById(id: Long): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Update
    suspend fun update(achievement: AchievementEntity)

    /**
     * 解锁成就
     */
    @Query("""
        UPDATE achievements
        SET isUnlocked = 1,
            unlockedAt = :unlockedAt,
            progress = 100,
            currentValue = targetValue
        WHERE code = :code AND isUnlocked = 0
    """)
    suspend fun unlockAchievement(code: String, unlockedAt: Long = System.currentTimeMillis()): Int

    /**
     * 更新进度
     */
    @Query("""
        UPDATE achievements
        SET currentValue = :currentValue,
            progress = MIN(100, (:currentValue * 100) / targetValue)
        WHERE code = :code
    """)
    suspend fun updateProgress(code: String, currentValue: Int)

    /**
     * 统计解锁数量
     */
    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    suspend fun countUnlocked(): Int

    /**
     * 获取总成就数
     */
    @Query("SELECT COUNT(*) FROM achievements WHERE isHidden = 0 OR isUnlocked = 1")
    suspend fun countVisible(): Int
}

/**
 * 用户等级DAO
 */
@Dao
interface UserLevelDao {

    @Query("SELECT * FROM user_levels WHERE id = 1")
    fun getUserLevel(): Flow<UserLevelEntity?>

    @Query("SELECT * FROM user_levels WHERE id = 1")
    suspend fun getUserLevelSync(): UserLevelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userLevel: UserLevelEntity)

    @Update
    suspend fun update(userLevel: UserLevelEntity)

    /**
     * 增加经验值
     */
    @Query("""
        UPDATE user_levels
        SET currentExp = currentExp + :exp,
            totalExp = totalExp + :exp,
            updatedAt = :now
        WHERE id = 1
    """)
    suspend fun addExp(exp: Int, now: Long = System.currentTimeMillis())

    /**
     * 升级
     */
    @Query("""
        UPDATE user_levels
        SET level = level + 1,
            currentExp = currentExp - expToNextLevel,
            expToNextLevel = :newExpRequired,
            title = :newTitle,
            updatedAt = :now
        WHERE id = 1 AND currentExp >= expToNextLevel
    """)
    suspend fun levelUp(newExpRequired: Int, newTitle: String, now: Long = System.currentTimeMillis()): Int
}

/**
 * 汇率DAO
 */
@Dao
interface CurrencyRateDao {

    @Query("SELECT * FROM currency_rates ORDER BY currencyCode")
    fun getAllRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code")
    suspend fun getByCode(code: String): CurrencyRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: CurrencyRateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<CurrencyRateEntity>)

    @Query("DELETE FROM currency_rates")
    suspend fun deleteAll()

    /**
     * 获取最后更新时间
     */
    @Query("SELECT MAX(updatedAt) FROM currency_rates")
    suspend fun getLastUpdateTime(): Long?
}

/**
 * 用户货币设置DAO
 */
@Dao
interface UserCurrencySettingDao {

    @Query("SELECT * FROM user_currency_settings WHERE id = 1")
    fun getSettings(): Flow<UserCurrencySettingEntity?>

    @Query("SELECT * FROM user_currency_settings WHERE id = 1")
    suspend fun getSettingsSync(): UserCurrencySettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: UserCurrencySettingEntity)

    @Update
    suspend fun update(settings: UserCurrencySettingEntity)
}

/**
 * 饮水记录DAO
 */
@Dao
interface WaterIntakeDao {

    @Query("SELECT * FROM water_intake_records WHERE date = :date ORDER BY time DESC")
    fun getByDate(date: Int): Flow<List<WaterIntakeEntity>>

    @Query("SELECT * FROM water_intake_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, time DESC")
    fun getByDateRange(startDate: Int, endDate: Int): Flow<List<WaterIntakeEntity>>

    @Query("SELECT SUM(amount) FROM water_intake_records WHERE date = :date")
    suspend fun getDailyTotal(date: Int): Int?

    @Query("""
        SELECT date, SUM(amount) as total
        FROM water_intake_records
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date
    """)
    suspend fun getDailyTotals(startDate: Int, endDate: Int): List<DailyWaterTotal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WaterIntakeEntity): Long

    @Query("DELETE FROM water_intake_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取本周平均饮水量
     */
    @Query("""
        SELECT AVG(daily_total) FROM (
            SELECT date, SUM(amount) as daily_total
            FROM water_intake_records
            WHERE date BETWEEN :startDate AND :endDate
            GROUP BY date
        )
    """)
    suspend fun getWeeklyAverage(startDate: Int, endDate: Int): Double?
}

data class DailyWaterTotal(
    val date: Int,
    val total: Int
)

/**
 * 睡眠记录DAO
 */
@Dao
interface SleepRecordDao {

    @Query("SELECT * FROM sleep_records WHERE date = :date AND isNap = 0")
    suspend fun getByDate(date: Int): SleepRecordEntity?

    @Query("SELECT * FROM sleep_records WHERE date = :date ORDER BY sleepTime")
    fun getAllByDate(date: Int): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: Int, endDate: Int): Flow<List<SleepRecordEntity>>

    @Query("SELECT AVG(duration) FROM sleep_records WHERE date BETWEEN :startDate AND :endDate AND isNap = 0")
    suspend fun getAverageDuration(startDate: Int, endDate: Int): Double?

    @Query("SELECT AVG(quality) FROM sleep_records WHERE date BETWEEN :startDate AND :endDate AND isNap = 0")
    suspend fun getAverageQuality(startDate: Int, endDate: Int): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SleepRecordEntity): Long

    @Update
    suspend fun update(record: SleepRecordEntity)

    @Query("DELETE FROM sleep_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取睡眠趋势
     */
    @Query("""
        SELECT date, duration, quality
        FROM sleep_records
        WHERE date BETWEEN :startDate AND :endDate AND isNap = 0
        ORDER BY date
    """)
    suspend fun getSleepTrend(startDate: Int, endDate: Int): List<SleepTrendData>
}

data class SleepTrendData(
    val date: Int,
    val duration: Int,
    val quality: Int
)

/**
 * 健康目标DAO
 */
@Dao
interface HealthGoalDao {

    @Query("SELECT * FROM health_goals WHERE id = 1")
    fun getGoals(): Flow<HealthGoalEntity?>

    @Query("SELECT * FROM health_goals WHERE id = 1")
    suspend fun getGoalsSync(): HealthGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goals: HealthGoalEntity)

    @Update
    suspend fun update(goals: HealthGoalEntity)
}
