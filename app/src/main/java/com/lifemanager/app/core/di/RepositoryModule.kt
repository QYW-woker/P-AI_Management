package com.lifemanager.app.core.di

import com.lifemanager.app.data.repository.CustomFieldRepositoryImpl
import com.lifemanager.app.data.repository.DailyTransactionRepositoryImpl
import com.lifemanager.app.data.repository.DiaryRepositoryImpl
import com.lifemanager.app.data.repository.HabitRepositoryImpl
import com.lifemanager.app.data.repository.SavingsPlanRepositoryImpl
import com.lifemanager.app.data.repository.MonthlyAssetRepositoryImpl
import com.lifemanager.app.data.repository.TimeTrackRepositoryImpl
import com.lifemanager.app.data.repository.MonthlyExpenseRepositoryImpl
import com.lifemanager.app.data.repository.MonthlyIncomeExpenseRepositoryImpl
import com.lifemanager.app.data.repository.TodoRepositoryImpl
import com.lifemanager.app.domain.repository.CustomFieldRepository
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import com.lifemanager.app.domain.repository.DiaryRepository
import com.lifemanager.app.domain.repository.HabitRepository
import com.lifemanager.app.domain.repository.SavingsPlanRepository
import com.lifemanager.app.domain.repository.MonthlyAssetRepository
import com.lifemanager.app.domain.repository.TimeTrackRepository
import com.lifemanager.app.domain.repository.MonthlyExpenseRepository
import com.lifemanager.app.domain.repository.MonthlyIncomeExpenseRepository
import com.lifemanager.app.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository依赖注入模块
 *
 * 将Repository接口绑定到具体实现类
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 提供月度收支仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindMonthlyIncomeExpenseRepository(
        impl: MonthlyIncomeExpenseRepositoryImpl
    ): MonthlyIncomeExpenseRepository

    /**
     * 提供自定义字段仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindCustomFieldRepository(
        impl: CustomFieldRepositoryImpl
    ): CustomFieldRepository

    /**
     * 提供月度资产仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindMonthlyAssetRepository(
        impl: MonthlyAssetRepositoryImpl
    ): MonthlyAssetRepository

    /**
     * 提供月度开销仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindMonthlyExpenseRepository(
        impl: MonthlyExpenseRepositoryImpl
    ): MonthlyExpenseRepository

    /**
     * 提供日常记账仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindDailyTransactionRepository(
        impl: DailyTransactionRepositoryImpl
    ): DailyTransactionRepository

    /**
     * 提供待办记事仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        impl: TodoRepositoryImpl
    ): TodoRepository

    /**
     * 提供日记仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindDiaryRepository(
        impl: DiaryRepositoryImpl
    ): DiaryRepository

    /**
     * 提供时间统计仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindTimeTrackRepository(
        impl: TimeTrackRepositoryImpl
    ): TimeTrackRepository

    /**
     * 提供习惯打卡仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindHabitRepository(
        impl: HabitRepositoryImpl
    ): HabitRepository

    /**
     * 提供存钱计划仓库实例
     */
    @Binds
    @Singleton
    abstract fun bindSavingsPlanRepository(
        impl: SavingsPlanRepositoryImpl
    ): SavingsPlanRepository
}
