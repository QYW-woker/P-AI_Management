package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.dao.MonthTotal
import com.lifemanager.app.core.database.entity.MonthlyIncomeExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * 月度收支仓库接口
 *
 * 定义月度收支数据的操作方法
 */
interface MonthlyIncomeExpenseRepository {

    /**
     * 获取指定月份的所有记录
     */
    fun getByMonth(yearMonth: Int): Flow<List<MonthlyIncomeExpenseEntity>>

    /**
     * 获取指定月份范围的记录
     */
    fun getByRange(startMonth: Int, endMonth: Int): Flow<List<MonthlyIncomeExpenseEntity>>

    /**
     * 获取指定月份的总收入
     */
    suspend fun getTotalIncome(yearMonth: Int): Double

    /**
     * 获取指定月份的总支出
     */
    suspend fun getTotalExpense(yearMonth: Int): Double

    /**
     * 获取指定月份各字段的收入汇总
     */
    fun getIncomeFieldTotals(yearMonth: Int): Flow<List<FieldTotal>>

    /**
     * 获取指定月份各字段的支出汇总
     */
    fun getExpenseFieldTotals(yearMonth: Int): Flow<List<FieldTotal>>

    /**
     * 获取最近的记录
     */
    fun getRecentRecords(limit: Int = 20): Flow<List<MonthlyIncomeExpenseEntity>>

    /**
     * 根据ID获取记录
     */
    suspend fun getById(id: Long): MonthlyIncomeExpenseEntity?

    /**
     * 插入记录
     */
    suspend fun insert(record: MonthlyIncomeExpenseEntity): Long

    /**
     * 更新记录
     */
    suspend fun update(record: MonthlyIncomeExpenseEntity)

    /**
     * 删除记录
     */
    suspend fun delete(id: Long)

    /**
     * 获取有数据的月份列表
     */
    fun getAvailableMonths(): Flow<List<Int>>

    /**
     * 获取指定年份各月份的收入趋势
     */
    fun getMonthlyIncomeTrend(year: Int): Flow<List<MonthTotal>>

    /**
     * 获取指定年份各月份的支出趋势
     */
    fun getMonthlyExpenseTrend(year: Int): Flow<List<MonthTotal>>
}
