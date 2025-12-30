package com.lifemanager.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifemanager.app.core.database.converter.Converters
import com.lifemanager.app.core.database.dao.*
import com.lifemanager.app.core.database.entity.*

/**
 * Room数据库主类
 *
 * 包含应用的所有数据表和DAO接口
 * 使用单例模式通过Hilt注入
 */
@Database(
    entities = [
        // 用户
        UserEntity::class,
        // 基础配置
        CustomFieldEntity::class,
        // 目标管理
        GoalEntity::class,
        GoalMilestoneEntity::class,
        GoalRecordEntity::class,
        // 财务模块
        MonthlyIncomeExpenseEntity::class,
        MonthlyAssetEntity::class,
        MonthlyExpenseEntity::class,
        DailyTransactionEntity::class,
        BudgetEntity::class,
        LedgerEntity::class,
        RecurringTransactionEntity::class,
        FundAccountEntity::class,
        TransferEntity::class,
        // 待办记事
        TodoEntity::class,
        // 日记
        DiaryEntity::class,
        // 时间统计
        TimeCategoryEntity::class,
        TimeRecordEntity::class,
        // 习惯打卡
        HabitEntity::class,
        HabitRecordEntity::class,
        // 存钱计划
        SavingsPlanEntity::class,
        SavingsRecordEntity::class,
        // AI分析
        AIAnalysisEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // ==================== DAO接口 ====================

    /**
     * 自定义字段DAO
     */
    abstract fun customFieldDao(): CustomFieldDao

    /**
     * 目标DAO
     */
    abstract fun goalDao(): GoalDao

    /**
     * 目标里程碑DAO
     */
    abstract fun goalMilestoneDao(): GoalMilestoneDao

    /**
     * 目标记录DAO
     */
    abstract fun goalRecordDao(): GoalRecordDao

    /**
     * 月度收支DAO
     */
    abstract fun monthlyIncomeExpenseDao(): MonthlyIncomeExpenseDao

    /**
     * 月度资产DAO
     */
    abstract fun monthlyAssetDao(): MonthlyAssetDao

    /**
     * 月度开销DAO
     */
    abstract fun monthlyExpenseDao(): MonthlyExpenseDao

    /**
     * 日常交易DAO
     */
    abstract fun dailyTransactionDao(): DailyTransactionDao

    /**
     * 待办事项DAO
     */
    abstract fun todoDao(): TodoDao

    /**
     * 日记DAO
     */
    abstract fun diaryDao(): DiaryDao

    /**
     * 时间分类DAO
     */
    abstract fun timeCategoryDao(): TimeCategoryDao

    /**
     * 时间记录DAO
     */
    abstract fun timeRecordDao(): TimeRecordDao

    /**
     * 习惯DAO
     */
    abstract fun habitDao(): HabitDao

    /**
     * 习惯记录DAO
     */
    abstract fun habitRecordDao(): HabitRecordDao

    /**
     * 存钱计划DAO
     */
    abstract fun savingsPlanDao(): SavingsPlanDao

    /**
     * 存钱记录DAO
     */
    abstract fun savingsRecordDao(): SavingsRecordDao

    /**
     * 用户DAO
     */
    abstract fun userDao(): UserDao

    /**
     * 预算DAO
     */
    abstract fun budgetDao(): BudgetDao

    /**
     * 账本DAO
     */
    abstract fun ledgerDao(): LedgerDao

    /**
     * 周期记账DAO
     */
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    /**
     * 资金账户DAO
     */
    abstract fun fundAccountDao(): FundAccountDao

    /**
     * 转账记录DAO
     */
    abstract fun transferDao(): TransferDao

    /**
     * AI分析DAO
     */
    abstract fun aiAnalysisDao(): AIAnalysisDao

    companion object {
        /**
         * 数据库名称
         */
        const val DATABASE_NAME = "life_manager_db"
    }
}
