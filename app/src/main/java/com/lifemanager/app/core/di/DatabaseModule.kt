package com.lifemanager.app.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifemanager.app.core.database.AppDatabase
import com.lifemanager.app.core.database.PresetData
import com.lifemanager.app.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 *
 * 提供Room数据库和所有DAO的单例实例
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供Room数据库实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        customFieldDaoProvider: Provider<CustomFieldDao>,
        timeCategoryDaoProvider: Provider<TimeCategoryDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // 数据库创建时初始化预设数据
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // 在协程中插入预设数据
                    CoroutineScope(Dispatchers.IO).launch {
                        // 插入自定义字段预设
                        customFieldDaoProvider.get().insertAll(PresetData.getAllCustomFields())
                        // 插入时间分类预设
                        timeCategoryDaoProvider.get().insertAll(PresetData.timeCategories)
                    }
                }
            })
            // 允许主线程查询（仅用于简单查询，复杂操作应使用协程）
            // .allowMainThreadQueries()
            // 版本迁移策略：破坏性迁移（开发阶段使用，正式发布需要编写迁移脚本）
            .fallbackToDestructiveMigration()
            .build()
    }

    // ==================== DAO 提供者 ====================

    @Provides
    @Singleton
    fun provideCustomFieldDao(database: AppDatabase): CustomFieldDao {
        return database.customFieldDao()
    }

    @Provides
    @Singleton
    fun provideGoalDao(database: AppDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    @Singleton
    fun provideGoalMilestoneDao(database: AppDatabase): GoalMilestoneDao {
        return database.goalMilestoneDao()
    }

    @Provides
    @Singleton
    fun provideMonthlyIncomeExpenseDao(database: AppDatabase): MonthlyIncomeExpenseDao {
        return database.monthlyIncomeExpenseDao()
    }

    @Provides
    @Singleton
    fun provideMonthlyAssetDao(database: AppDatabase): MonthlyAssetDao {
        return database.monthlyAssetDao()
    }

    @Provides
    @Singleton
    fun provideMonthlyExpenseDao(database: AppDatabase): MonthlyExpenseDao {
        return database.monthlyExpenseDao()
    }

    @Provides
    @Singleton
    fun provideDailyTransactionDao(database: AppDatabase): DailyTransactionDao {
        return database.dailyTransactionDao()
    }

    @Provides
    @Singleton
    fun provideTodoDao(database: AppDatabase): TodoDao {
        return database.todoDao()
    }

    @Provides
    @Singleton
    fun provideDiaryDao(database: AppDatabase): DiaryDao {
        return database.diaryDao()
    }

    @Provides
    @Singleton
    fun provideTimeCategoryDao(database: AppDatabase): TimeCategoryDao {
        return database.timeCategoryDao()
    }

    @Provides
    @Singleton
    fun provideTimeRecordDao(database: AppDatabase): TimeRecordDao {
        return database.timeRecordDao()
    }

    @Provides
    @Singleton
    fun provideHabitDao(database: AppDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    @Singleton
    fun provideHabitRecordDao(database: AppDatabase): HabitRecordDao {
        return database.habitRecordDao()
    }

    @Provides
    @Singleton
    fun provideSavingsPlanDao(database: AppDatabase): SavingsPlanDao {
        return database.savingsPlanDao()
    }

    @Provides
    @Singleton
    fun provideSavingsRecordDao(database: AppDatabase): SavingsRecordDao {
        return database.savingsRecordDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideLedgerDao(database: AppDatabase): LedgerDao {
        return database.ledgerDao()
    }

    @Provides
    @Singleton
    fun provideRecurringTransactionDao(database: AppDatabase): RecurringTransactionDao {
        return database.recurringTransactionDao()
    }

    @Provides
    @Singleton
    fun provideGoalRecordDao(database: AppDatabase): GoalRecordDao {
        return database.goalRecordDao()
    }

    @Provides
    @Singleton
    fun provideFundAccountDao(database: AppDatabase): FundAccountDao {
        return database.fundAccountDao()
    }

    @Provides
    @Singleton
    fun provideTransferDao(database: AppDatabase): TransferDao {
        return database.transferDao()
    }

    @Provides
    @Singleton
    fun provideAIAnalysisDao(database: AppDatabase): AIAnalysisDao {
        return database.aiAnalysisDao()
    }

    @Provides
    @Singleton
    fun provideHealthRecordDao(database: AppDatabase): HealthRecordDao {
        return database.healthRecordDao()
    }

    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao {
        return database.bookDao()
    }
}
