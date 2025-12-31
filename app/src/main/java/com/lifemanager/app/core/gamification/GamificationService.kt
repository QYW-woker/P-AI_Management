package com.lifemanager.app.core.gamification

import com.lifemanager.app.core.database.dao.*
import com.lifemanager.app.core.database.entity.AchievementEntity
import com.lifemanager.app.core.database.entity.UserLevelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 成就系统服务
 *
 * 管理用户成就解锁、经验值和等级
 */
@Singleton
class GamificationService @Inject constructor(
    private val achievementDao: AchievementDao,
    private val userLevelDao: UserLevelDao,
    private val dailyTransactionDao: DailyTransactionDao,
    private val habitRecordDao: HabitRecordDao,
    private val todoDao: TodoDao,
    private val goalDao: GoalDao,
    private val savingsPlanDao: SavingsPlanDao,
    private val diaryDao: DiaryDao
) {

    companion object {
        // 经验值配置
        const val EXP_RECORD_TRANSACTION = 5
        const val EXP_COMPLETE_TODO = 10
        const val EXP_CHECK_HABIT = 8
        const val EXP_WRITE_DIARY = 15
        const val EXP_ACHIEVE_GOAL = 100
        const val EXP_SAVE_MONEY = 20
        const val EXP_CONSECUTIVE_DAYS = 25

        // 等级配置
        val LEVEL_TITLES = listOf(
            "理财新手",      // 1
            "记账学徒",      // 2
            "财务助手",      // 3
            "生活管家",      // 4
            "理财达人",      // 5
            "规划专家",      // 6
            "财务顾问",      // 7
            "生活大师",      // 8
            "财富管理师",    // 9
            "人生赢家"       // 10+
        )

        fun getExpForLevel(level: Int): Int = when {
            level <= 5 -> level * 100
            level <= 10 -> 500 + (level - 5) * 200
            level <= 20 -> 1500 + (level - 10) * 300
            else -> 4500 + (level - 20) * 500
        }

        fun getTitleForLevel(level: Int): String = when {
            level <= LEVEL_TITLES.size -> LEVEL_TITLES[level - 1]
            else -> "传奇大师 Lv.${level}"
        }
    }

    /**
     * 获取用户等级信息
     */
    fun getUserLevel(): Flow<UserLevelEntity?> = userLevelDao.getUserLevel()

    /**
     * 获取所有成就
     */
    fun getAllAchievements(): Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()

    /**
     * 获取已解锁成就
     */
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>> = achievementDao.getUnlockedAchievements()

    /**
     * 初始化用户等级
     */
    suspend fun initUserLevel() {
        if (userLevelDao.getUserLevelSync() == null) {
            userLevelDao.insert(
                UserLevelEntity(
                    id = 1,
                    level = 1,
                    currentExp = 0,
                    expToNextLevel = getExpForLevel(1),
                    totalExp = 0,
                    title = getTitleForLevel(1)
                )
            )
        }
    }

    /**
     * 初始化成就列表
     */
    suspend fun initAchievements() {
        val existing = achievementDao.getAllAchievements().first()
        if (existing.isEmpty()) {
            achievementDao.insertAll(getDefaultAchievements())
        }
    }

    /**
     * 记录活动并检查成就
     */
    suspend fun onActivityCompleted(activity: UserActivity) {
        // 添加经验值
        val exp = when (activity) {
            is UserActivity.RecordTransaction -> EXP_RECORD_TRANSACTION
            is UserActivity.CompleteTodo -> EXP_COMPLETE_TODO
            is UserActivity.CheckHabit -> EXP_CHECK_HABIT
            is UserActivity.WriteDiary -> EXP_WRITE_DIARY
            is UserActivity.AchieveGoal -> EXP_ACHIEVE_GOAL
            is UserActivity.SaveMoney -> EXP_SAVE_MONEY
            is UserActivity.ConsecutiveDays -> EXP_CONSECUTIVE_DAYS
        }

        addExperience(exp)

        // 检查并解锁成就
        checkAndUnlockAchievements(activity)
    }

    /**
     * 添加经验值
     */
    suspend fun addExperience(exp: Int) {
        userLevelDao.addExp(exp)

        // 检查升级
        val userLevel = userLevelDao.getUserLevelSync() ?: return
        if (userLevel.currentExp >= userLevel.expToNextLevel) {
            val newLevel = userLevel.level + 1
            userLevelDao.levelUp(
                newExpRequired = getExpForLevel(newLevel),
                newTitle = getTitleForLevel(newLevel)
            )
        }
    }

    /**
     * 检查并解锁成就
     */
    private suspend fun checkAndUnlockAchievements(activity: UserActivity) {
        when (activity) {
            is UserActivity.RecordTransaction -> checkTransactionAchievements()
            is UserActivity.CompleteTodo -> checkTodoAchievements()
            is UserActivity.CheckHabit -> checkHabitAchievements(activity.consecutiveDays)
            is UserActivity.WriteDiary -> checkDiaryAchievements()
            is UserActivity.AchieveGoal -> checkGoalAchievements()
            is UserActivity.SaveMoney -> checkSavingsAchievements(activity.totalSaved)
            is UserActivity.ConsecutiveDays -> checkConsecutiveAchievements(activity.days)
        }
    }

    private suspend fun checkTransactionAchievements() {
        val today = java.time.LocalDate.now().toEpochDay().toInt()
        val monthStart = java.time.LocalDate.now().withDayOfMonth(1).toEpochDay().toInt()

        // 这里需要实现获取交易统计的方法
        // 简化版本：检查基础成就
        achievementDao.updateProgress("first_transaction", 1)

        // 检查是否解锁
        val achievement = achievementDao.getByCode("first_transaction")
        if (achievement != null && !achievement.isUnlocked && achievement.currentValue >= achievement.targetValue) {
            achievementDao.unlockAchievement("first_transaction")
        }
    }

    private suspend fun checkTodoAchievements() {
        achievementDao.updateProgress("first_todo", 1)
        val achievement = achievementDao.getByCode("first_todo")
        if (achievement != null && !achievement.isUnlocked) {
            achievementDao.unlockAchievement("first_todo")
        }
    }

    private suspend fun checkHabitAchievements(consecutiveDays: Int) {
        // 7天连续打卡
        if (consecutiveDays >= 7) {
            achievementDao.unlockAchievement("habit_week")
        }
        // 30天连续打卡
        if (consecutiveDays >= 30) {
            achievementDao.unlockAchievement("habit_month")
        }
        // 100天连续打卡
        if (consecutiveDays >= 100) {
            achievementDao.unlockAchievement("habit_master")
        }
    }

    private suspend fun checkDiaryAchievements() {
        achievementDao.updateProgress("first_diary", 1)
        val achievement = achievementDao.getByCode("first_diary")
        if (achievement != null && !achievement.isUnlocked) {
            achievementDao.unlockAchievement("first_diary")
        }
    }

    private suspend fun checkGoalAchievements() {
        achievementDao.unlockAchievement("first_goal_achieved")
    }

    private suspend fun checkSavingsAchievements(totalSaved: Double) {
        when {
            totalSaved >= 100000 -> achievementDao.unlockAchievement("save_100k")
            totalSaved >= 50000 -> achievementDao.unlockAchievement("save_50k")
            totalSaved >= 10000 -> achievementDao.unlockAchievement("save_10k")
            totalSaved >= 1000 -> achievementDao.unlockAchievement("save_1k")
        }
    }

    private suspend fun checkConsecutiveAchievements(days: Int) {
        when {
            days >= 365 -> achievementDao.unlockAchievement("use_year")
            days >= 100 -> achievementDao.unlockAchievement("use_100_days")
            days >= 30 -> achievementDao.unlockAchievement("use_month")
            days >= 7 -> achievementDao.unlockAchievement("use_week")
        }
    }

    /**
     * 获取成就进度统计
     */
    suspend fun getAchievementStats(): AchievementStats {
        val unlocked = achievementDao.countUnlocked()
        val visible = achievementDao.countVisible()
        return AchievementStats(
            unlockedCount = unlocked,
            totalCount = visible,
            percentage = if (visible > 0) (unlocked * 100 / visible) else 0
        )
    }

    /**
     * 默认成就列表
     */
    private fun getDefaultAchievements(): List<AchievementEntity> = listOf(
        // 记账成就
        AchievementEntity(
            code = "first_transaction",
            name = "初次记账",
            description = "完成第一笔记账",
            iconName = "ic_achievement_first",
            type = "FINANCE",
            targetValue = 1,
            expReward = 50,
            sortOrder = 1
        ),
        AchievementEntity(
            code = "transaction_100",
            name = "记账达人",
            description = "累计记账100笔",
            iconName = "ic_achievement_100",
            type = "FINANCE",
            targetValue = 100,
            expReward = 200,
            sortOrder = 2
        ),
        AchievementEntity(
            code = "transaction_1000",
            name = "记账大师",
            description = "累计记账1000笔",
            iconName = "ic_achievement_master",
            type = "FINANCE",
            targetValue = 1000,
            expReward = 500,
            sortOrder = 3
        ),

        // 储蓄成就
        AchievementEntity(
            code = "save_1k",
            name = "小有积蓄",
            description = "累计存款达1000元",
            iconName = "ic_save_bronze",
            type = "SAVINGS",
            targetValue = 1000,
            expReward = 100,
            sortOrder = 10
        ),
        AchievementEntity(
            code = "save_10k",
            name = "初具规模",
            description = "累计存款达10000元",
            iconName = "ic_save_silver",
            type = "SAVINGS",
            targetValue = 10000,
            expReward = 300,
            sortOrder = 11
        ),
        AchievementEntity(
            code = "save_50k",
            name = "财富增长",
            description = "累计存款达50000元",
            iconName = "ic_save_gold",
            type = "SAVINGS",
            targetValue = 50000,
            expReward = 500,
            sortOrder = 12
        ),
        AchievementEntity(
            code = "save_100k",
            name = "财务自由",
            description = "累计存款达100000元",
            iconName = "ic_save_platinum",
            type = "SAVINGS",
            targetValue = 100000,
            expReward = 1000,
            sortOrder = 13
        ),

        // 习惯成就
        AchievementEntity(
            code = "habit_week",
            name = "坚持一周",
            description = "连续打卡7天",
            iconName = "ic_habit_week",
            type = "HABIT",
            targetValue = 7,
            expReward = 100,
            sortOrder = 20
        ),
        AchievementEntity(
            code = "habit_month",
            name = "坚持一月",
            description = "连续打卡30天",
            iconName = "ic_habit_month",
            type = "HABIT",
            targetValue = 30,
            expReward = 300,
            sortOrder = 21
        ),
        AchievementEntity(
            code = "habit_master",
            name = "习惯大师",
            description = "连续打卡100天",
            iconName = "ic_habit_master",
            type = "HABIT",
            targetValue = 100,
            expReward = 800,
            sortOrder = 22
        ),

        // 待办成就
        AchievementEntity(
            code = "first_todo",
            name = "第一步",
            description = "完成第一个待办事项",
            iconName = "ic_todo_first",
            type = "TODO",
            targetValue = 1,
            expReward = 30,
            sortOrder = 30
        ),
        AchievementEntity(
            code = "todo_100",
            name = "高效达人",
            description = "完成100个待办事项",
            iconName = "ic_todo_100",
            type = "TODO",
            targetValue = 100,
            expReward = 200,
            sortOrder = 31
        ),

        // 日记成就
        AchievementEntity(
            code = "first_diary",
            name = "心情记录",
            description = "写下第一篇日记",
            iconName = "ic_diary_first",
            type = "DIARY",
            targetValue = 1,
            expReward = 50,
            sortOrder = 40
        ),
        AchievementEntity(
            code = "diary_30",
            name = "月记作家",
            description = "累计写30篇日记",
            iconName = "ic_diary_30",
            type = "DIARY",
            targetValue = 30,
            expReward = 200,
            sortOrder = 41
        ),

        // 目标成就
        AchievementEntity(
            code = "first_goal_achieved",
            name = "目标达成",
            description = "完成第一个目标",
            iconName = "ic_goal_first",
            type = "GOAL",
            targetValue = 1,
            expReward = 150,
            sortOrder = 50
        ),

        // 使用成就
        AchievementEntity(
            code = "use_week",
            name = "初识相逢",
            description = "连续使用7天",
            iconName = "ic_use_week",
            type = "USAGE",
            targetValue = 7,
            expReward = 100,
            sortOrder = 60
        ),
        AchievementEntity(
            code = "use_month",
            name = "老朋友",
            description = "连续使用30天",
            iconName = "ic_use_month",
            type = "USAGE",
            targetValue = 30,
            expReward = 300,
            sortOrder = 61
        ),
        AchievementEntity(
            code = "use_100_days",
            name = "生活伴侣",
            description = "连续使用100天",
            iconName = "ic_use_100",
            type = "USAGE",
            targetValue = 100,
            expReward = 500,
            sortOrder = 62
        ),
        AchievementEntity(
            code = "use_year",
            name = "忠实用户",
            description = "连续使用365天",
            iconName = "ic_use_year",
            type = "USAGE",
            targetValue = 365,
            expReward = 1000,
            sortOrder = 63,
            isHidden = true
        )
    )
}

/**
 * 用户活动类型
 */
sealed class UserActivity {
    object RecordTransaction : UserActivity()
    object CompleteTodo : UserActivity()
    data class CheckHabit(val consecutiveDays: Int) : UserActivity()
    object WriteDiary : UserActivity()
    object AchieveGoal : UserActivity()
    data class SaveMoney(val totalSaved: Double) : UserActivity()
    data class ConsecutiveDays(val days: Int) : UserActivity()
}

/**
 * 成就统计
 */
data class AchievementStats(
    val unlockedCount: Int,
    val totalCount: Int,
    val percentage: Int
)
