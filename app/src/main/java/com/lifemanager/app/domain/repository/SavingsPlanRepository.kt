package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 存钱计划仓库接口
 */
interface SavingsPlanRepository {

    /**
     * 获取所有活跃的存钱计划
     */
    fun getActivePlans(): Flow<List<SavingsPlanEntity>>

    /**
     * 获取所有存钱计划（不包括已取消的）
     */
    fun getAllPlans(): Flow<List<SavingsPlanEntity>>

    /**
     * 根据ID获取计划
     */
    suspend fun getPlanById(id: Long): SavingsPlanEntity?

    /**
     * 根据ID获取计划（Flow版本）
     */
    fun getPlanByIdFlow(id: Long): Flow<SavingsPlanEntity?>

    /**
     * 保存计划
     */
    suspend fun savePlan(plan: SavingsPlanEntity): Long

    /**
     * 更新计划
     */
    suspend fun updatePlan(plan: SavingsPlanEntity)

    /**
     * 更新计划状态
     */
    suspend fun updatePlanStatus(id: Long, status: String)

    /**
     * 更新当前金额
     */
    suspend fun updatePlanAmount(id: Long, amount: Double)

    /**
     * 增加存款金额
     */
    suspend fun addAmount(id: Long, amount: Double)

    /**
     * 删除计划
     */
    suspend fun deletePlan(id: Long)

    /**
     * 统计活跃计划数
     */
    suspend fun countActivePlans(): Int

    /**
     * 获取所有计划的总目标金额
     */
    suspend fun getTotalTarget(): Double

    /**
     * 获取所有计划的当前总金额
     */
    suspend fun getTotalCurrent(): Double

    // ============ 存款记录相关 ============

    /**
     * 获取指定计划的所有记录
     */
    fun getRecordsByPlan(planId: Long): Flow<List<SavingsRecordEntity>>

    /**
     * 获取指定计划指定日期范围的记录
     */
    fun getRecordsByPlanAndDateRange(planId: Long, startDate: Int, endDate: Int): Flow<List<SavingsRecordEntity>>

    /**
     * 获取指定计划的总存款金额
     */
    suspend fun getTotalByPlan(planId: Long): Double

    /**
     * 根据ID获取记录
     */
    suspend fun getRecordById(id: Long): SavingsRecordEntity?

    /**
     * 保存存款记录
     */
    suspend fun saveRecord(record: SavingsRecordEntity): Long

    /**
     * 更新存款记录
     */
    suspend fun updateRecord(record: SavingsRecordEntity)

    /**
     * 删除存款记录
     */
    suspend fun deleteRecord(id: Long)

    /**
     * 获取最近的存款记录
     */
    fun getRecentRecords(limit: Int = 20): Flow<List<SavingsRecordEntity>>

    /**
     * 统计指定计划的记录数
     */
    suspend fun countRecordsByPlan(planId: Long): Int

    /**
     * 获取所有存款日期（用于计算连续存款天数）
     */
    suspend fun getAllDepositDates(): List<Int>

    /**
     * 获取总存款天数
     */
    suspend fun getTotalDepositDays(): Int

    /**
     * 获取本周存款总额
     */
    suspend fun getThisWeekDeposits(startOfWeek: Int): Double
}
