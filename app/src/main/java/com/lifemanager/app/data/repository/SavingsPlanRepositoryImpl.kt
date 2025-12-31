package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.SavingsPlanDao
import com.lifemanager.app.core.database.dao.SavingsRecordDao
import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import com.lifemanager.app.domain.repository.SavingsPlanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 存钱计划仓库实现类
 */
@Singleton
class SavingsPlanRepositoryImpl @Inject constructor(
    private val planDao: SavingsPlanDao,
    private val recordDao: SavingsRecordDao
) : SavingsPlanRepository {

    override fun getActivePlans(): Flow<List<SavingsPlanEntity>> {
        return planDao.getActivePlans()
    }

    override fun getAllPlans(): Flow<List<SavingsPlanEntity>> {
        return planDao.getAllPlans()
    }

    override suspend fun getPlanById(id: Long): SavingsPlanEntity? {
        return planDao.getById(id)
    }

    override fun getPlanByIdFlow(id: Long): Flow<SavingsPlanEntity?> {
        return planDao.getByIdFlow(id)
    }

    override suspend fun savePlan(plan: SavingsPlanEntity): Long {
        return planDao.insert(plan)
    }

    override suspend fun updatePlan(plan: SavingsPlanEntity) {
        planDao.update(plan)
    }

    override suspend fun updatePlanStatus(id: Long, status: String) {
        planDao.updateStatus(id, status)
    }

    override suspend fun updatePlanAmount(id: Long, amount: Double) {
        planDao.updateAmount(id, amount)
    }

    override suspend fun addAmount(id: Long, amount: Double) {
        planDao.addAmount(id, amount)
    }

    override suspend fun deletePlan(id: Long) {
        planDao.deleteById(id)
    }

    override suspend fun countActivePlans(): Int {
        return planDao.countActive()
    }

    override suspend fun getTotalTarget(): Double {
        return planDao.getTotalTarget()
    }

    override suspend fun getTotalCurrent(): Double {
        return planDao.getTotalCurrent()
    }

    // ============ 存款记录相关 ============

    override fun getRecordsByPlan(planId: Long): Flow<List<SavingsRecordEntity>> {
        return recordDao.getByPlan(planId)
    }

    override fun getRecordsByPlanAndDateRange(
        planId: Long,
        startDate: Int,
        endDate: Int
    ): Flow<List<SavingsRecordEntity>> {
        return recordDao.getByPlanAndDateRange(planId, startDate, endDate)
    }

    override suspend fun getTotalByPlan(planId: Long): Double {
        return recordDao.getTotalByPlan(planId)
    }

    override suspend fun getRecordById(id: Long): SavingsRecordEntity? {
        return recordDao.getById(id)
    }

    override suspend fun saveRecord(record: SavingsRecordEntity): Long {
        return recordDao.insert(record)
    }

    override suspend fun updateRecord(record: SavingsRecordEntity) {
        recordDao.update(record)
    }

    override suspend fun deleteRecord(id: Long) {
        recordDao.deleteById(id)
    }

    override fun getRecentRecords(limit: Int): Flow<List<SavingsRecordEntity>> {
        return recordDao.getRecentRecords(limit)
    }

    override suspend fun countRecordsByPlan(planId: Long): Int {
        return recordDao.countByPlan(planId)
    }

    override suspend fun getAllDepositDates(): List<Int> {
        return recordDao.getAllDepositDates()
    }

    override suspend fun getTotalDepositDays(): Int {
        return recordDao.getTotalDepositDays()
    }

    override suspend fun getThisWeekDeposits(startOfWeek: Int): Double {
        return recordDao.getThisWeekDeposits(startOfWeek)
    }
}
