package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.InvestmentFieldTotal
import com.lifemanager.app.core.database.dao.MonthTotal
import com.lifemanager.app.core.database.dao.MonthlyInvestmentDao
import com.lifemanager.app.core.database.entity.MonthlyInvestmentEntity
import com.lifemanager.app.domain.repository.MonthlyInvestmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 月度定投仓库实现类
 */
@Singleton
class MonthlyInvestmentRepositoryImpl @Inject constructor(
    private val dao: MonthlyInvestmentDao
) : MonthlyInvestmentRepository {

    override fun getByMonth(yearMonth: Int): Flow<List<MonthlyInvestmentEntity>> {
        return dao.getByMonth(yearMonth)
    }

    override fun getByRange(startMonth: Int, endMonth: Int): Flow<List<MonthlyInvestmentEntity>> {
        return dao.getByRange(startMonth, endMonth)
    }

    override suspend fun getTotalBudget(yearMonth: Int): Double {
        return dao.getTotalBudget(yearMonth)
    }

    override suspend fun getTotalActual(yearMonth: Int): Double {
        return dao.getTotalActual(yearMonth)
    }

    override fun getFieldTotals(yearMonth: Int): Flow<List<InvestmentFieldTotal>> {
        return dao.getFieldTotals(yearMonth)
    }

    override fun getRecentRecords(limit: Int): Flow<List<MonthlyInvestmentEntity>> {
        return dao.getRecentRecords(limit)
    }

    override suspend fun getById(id: Long): MonthlyInvestmentEntity? {
        return dao.getById(id)
    }

    override suspend fun insert(record: MonthlyInvestmentEntity): Long {
        return dao.insert(record)
    }

    override suspend fun update(record: MonthlyInvestmentEntity) {
        dao.update(record)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override fun getAvailableMonths(): Flow<List<Int>> {
        return dao.getAvailableMonths()
    }

    override fun getMonthlyBudgetTrend(year: Int): Flow<List<MonthTotal>> {
        return dao.getMonthlyBudgetTotalsByYear(year)
    }

    override fun getMonthlyActualTrend(year: Int): Flow<List<MonthTotal>> {
        return dao.getMonthlyActualTotalsByYear(year)
    }
}
