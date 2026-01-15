package com.lifemanager.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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
        MonthlyInvestmentEntity::class,
        DailyTransactionEntity::class,
        BudgetEntity::class,
        LedgerEntity::class,
        RecurringTransactionEntity::class,
        FundAccountEntity::class,
        TransferEntity::class,
        // 高级记账功能
        SplitTransactionEntity::class,
        MerchantEntity::class,
        BillEntity::class,
        RefundEntity::class,
        InstallmentPlanEntity::class,
        InstallmentPaymentEntity::class,
        TransactionTemplateEntity::class,
        SearchPresetEntity::class,
        // 订阅管理
        SubscriptionEntity::class,
        SubscriptionPaymentEntity::class,
        // 成就系统
        AchievementEntity::class,
        UserLevelEntity::class,
        // 多币种
        CurrencyRateEntity::class,
        UserCurrencySettingEntity::class,
        // 健康追踪增强
        WaterIntakeEntity::class,
        SleepRecordEntity::class,
        HealthGoalEntity::class,
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
        AIAnalysisEntity::class,
        // 健康记录
        HealthRecordEntity::class,
        // 阅读模块
        BookEntity::class,
        ReadingSessionEntity::class,
        ReadingNoteEntity::class,
        BookShelfEntity::class,
        BookShelfMappingEntity::class,
        ReadingGoalEntity::class
    ],
    version = 19,
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
     * 月度定投DAO
     */
    abstract fun monthlyInvestmentDao(): MonthlyInvestmentDao

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

    /**
     * 健康记录DAO
     */
    abstract fun healthRecordDao(): HealthRecordDao

    // ==================== 高级记账DAO ====================

    /**
     * 拆分交易DAO
     */
    abstract fun splitTransactionDao(): SplitTransactionDao

    /**
     * 商家DAO
     */
    abstract fun merchantDao(): MerchantDao

    /**
     * 账单DAO
     */
    abstract fun billDao(): BillDao

    /**
     * 退款DAO
     */
    abstract fun refundDao(): RefundDao

    /**
     * 分期计划DAO
     */
    abstract fun installmentPlanDao(): InstallmentPlanDao

    /**
     * 分期还款DAO
     */
    abstract fun installmentPaymentDao(): InstallmentPaymentDao

    /**
     * 交易模板DAO
     */
    abstract fun transactionTemplateDao(): TransactionTemplateDao

    /**
     * 搜索预设DAO
     */
    abstract fun searchPresetDao(): SearchPresetDao

    // ==================== 扩展功能DAO ====================

    /**
     * 订阅服务DAO
     */
    abstract fun subscriptionDao(): SubscriptionDao

    /**
     * 订阅付款DAO
     */
    abstract fun subscriptionPaymentDao(): SubscriptionPaymentDao

    /**
     * 成就DAO
     */
    abstract fun achievementDao(): AchievementDao

    /**
     * 用户等级DAO
     */
    abstract fun userLevelDao(): UserLevelDao

    /**
     * 汇率DAO
     */
    abstract fun currencyRateDao(): CurrencyRateDao

    /**
     * 用户货币设置DAO
     */
    abstract fun userCurrencySettingDao(): UserCurrencySettingDao

    /**
     * 饮水记录DAO
     */
    abstract fun waterIntakeDao(): WaterIntakeDao

    /**
     * 睡眠记录DAO
     */
    abstract fun sleepRecordDao(): SleepRecordDao

    /**
     * 健康目标DAO
     */
    abstract fun healthGoalDao(): HealthGoalDao

    // ==================== 阅读模块DAO ====================

    /**
     * 书籍DAO
     */
    abstract fun bookDao(): BookDao

    companion object {
        /**
         * 数据库名称
         */
        const val DATABASE_NAME = "life_manager_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库单例（用于Widget等非Hilt注入的场景）
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
