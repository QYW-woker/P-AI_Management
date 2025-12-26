package com.lifemanager.app.core.di

import com.lifemanager.app.data.repository.CustomFieldRepositoryImpl
import com.lifemanager.app.data.repository.MonthlyIncomeExpenseRepositoryImpl
import com.lifemanager.app.domain.repository.CustomFieldRepository
import com.lifemanager.app.domain.repository.MonthlyIncomeExpenseRepository
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
}
