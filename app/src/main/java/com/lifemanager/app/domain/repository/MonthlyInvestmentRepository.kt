package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.dao.InvestmentFieldTotal
import com.lifemanager.app.core.database.dao.MonthTotal
import com.lifemanager.app.core.database.entity.MonthlyInvestmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * 月度定投仓库接口
 *
 * 定义月度定投数据的操作方法
 */
interface MonthlyInvestmentRepository {

    /**
     * 获取指定月份的所有记录
     */
    fun getByMonth(yearMonth: Int): Flow<List<MonthlyInvestmentEntity>>

    /**
     * 获取指定月份范围的记录
     */
    fun getByRange(startMonth: Int, endMonth: Int): Flow<List<MonthlyInvestmentEntity>>

    /**
     * 获取指定月份的总预算
     */
    suspend fun getTotalBudget(yearMonth: Int): Double

    /**
     * 获取指定月份的实际投入总额
     */
    suspend fun getTotalActual(yearMonth: Int): Double

    /**
     * 获取指定月份各字段的汇总
     */
    fun getFieldTotals(yearMonth: Int): Flow<List<InvestmentFieldTotal>>

    /**
     * 获取最近的记录
     */
    fun getRecentRecords(limit: Int = 20): Flow<List<MonthlyInvestmentEntity>>

    /**
     * 根据ID获取记录
     */
    suspend fun getById(id: Long): MonthlyInvestmentEntity?

    /**
     * 插入记录
     */
    suspend fun insert(record: MonthlyInvestmentEntity): Long

    /**
     * 更新记录
     */
    suspend fun update(record: MonthlyInvestmentEntity)

    /**
     * 删除记录
     */
    suspend fun delete(id: Long)

    /**
     * 获取有数据的月份列表
     */
    fun getAvailableMonths(): Flow<List<Int>>

    /**
     * 获取指定年份各月份的预算趋势
     */
    fun getMonthlyBudgetTrend(year: Int): Flow<List<MonthTotal>>

    /**
     * 获取指定年份各月份的实际投入趋势
     */
    fun getMonthlyActualTrend(year: Int): Flow<List<MonthTotal>>
}
