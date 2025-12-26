package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.dao.MonthTotal
import com.lifemanager.app.core.database.dao.MonthlyIncomeExpenseDao
import com.lifemanager.app.core.database.entity.IncomeExpenseType
import com.lifemanager.app.core.database.entity.MonthlyIncomeExpenseEntity
import com.lifemanager.app.domain.repository.MonthlyIncomeExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 月度收支仓库实现类
 */
@Singleton
class MonthlyIncomeExpenseRepositoryImpl @Inject constructor(
    private val dao: MonthlyIncomeExpenseDao
) : MonthlyIncomeExpenseRepository {

    override fun getByMonth(yearMonth: Int): Flow<List<MonthlyIncomeExpenseEntity>> {
        return dao.getByMonth(yearMonth)
    }

    override fun getByRange(startMonth: Int, endMonth: Int): Flow<List<MonthlyIncomeExpenseEntity>> {
        return dao.getByRange(startMonth, endMonth)
    }

    override suspend fun getTotalIncome(yearMonth: Int): Double {
        return dao.getTotalByType(yearMonth, IncomeExpenseType.INCOME)
    }

    override suspend fun getTotalExpense(yearMonth: Int): Double {
        return dao.getTotalByType(yearMonth, IncomeExpenseType.EXPENSE)
    }

    override fun getIncomeFieldTotals(yearMonth: Int): Flow<List<FieldTotal>> {
        return dao.getFieldTotals(yearMonth, IncomeExpenseType.INCOME)
    }

    override fun getExpenseFieldTotals(yearMonth: Int): Flow<List<FieldTotal>> {
        return dao.getFieldTotals(yearMonth, IncomeExpenseType.EXPENSE)
    }

    override fun getRecentRecords(limit: Int): Flow<List<MonthlyIncomeExpenseEntity>> {
        return dao.getRecentRecords(limit)
    }

    override suspend fun getById(id: Long): MonthlyIncomeExpenseEntity? {
        return dao.getById(id)
    }

    override suspend fun insert(record: MonthlyIncomeExpenseEntity): Long {
        return dao.insert(record)
    }

    override suspend fun update(record: MonthlyIncomeExpenseEntity) {
        dao.update(record)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override fun getAvailableMonths(): Flow<List<Int>> {
        return dao.getAvailableMonths()
    }

    override fun getMonthlyIncomeTrend(year: Int): Flow<List<MonthTotal>> {
        return dao.getMonthlyIncomeTotalsByYear(year)
    }

    override fun getMonthlyExpenseTrend(year: Int): Flow<List<MonthTotal>> {
        return dao.getMonthlyExpenseTotalsByYear(year)
    }
}
