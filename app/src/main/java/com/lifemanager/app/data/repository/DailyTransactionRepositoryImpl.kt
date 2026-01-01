package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.DailyTransactionDao
import com.lifemanager.app.core.database.dao.DateTotal
import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日常记账仓库实现
 */
@Singleton
class DailyTransactionRepositoryImpl @Inject constructor(
    private val dao: DailyTransactionDao
) : DailyTransactionRepository {

    override fun getByDateRange(startDate: Int, endDate: Int): Flow<List<DailyTransactionEntity>> {
        return dao.getByDateRange(startDate, endDate)
    }

    override fun getByDate(date: Int): Flow<List<DailyTransactionEntity>> {
        return dao.getByDate(date)
    }

    override fun getRecentTransactions(limit: Int): Flow<List<DailyTransactionEntity>> {
        return dao.getRecentTransactions(limit)
    }

    override suspend fun getTotalByTypeInRange(startDate: Int, endDate: Int, type: String): Double {
        return dao.getTotalByTypeInRange(startDate, endDate, type)
    }

    override fun getCategoryTotalsInRange(startDate: Int, endDate: Int, type: String): Flow<List<FieldTotal>> {
        return dao.getCategoryTotalsInRange(startDate, endDate, type)
    }

    override fun getDailyExpenseTotals(startDate: Int, endDate: Int): Flow<List<DateTotal>> {
        return dao.getDailyExpenseTotals(startDate, endDate)
    }

    override suspend fun getById(id: Long): DailyTransactionEntity? {
        return dao.getById(id)
    }

    override fun searchByNote(keyword: String, limit: Int): Flow<List<DailyTransactionEntity>> {
        return dao.searchByNote(keyword, limit)
    }

    override suspend fun insert(transaction: DailyTransactionEntity): Long {
        return dao.insert(transaction)
    }

    override suspend fun insertAll(transactions: List<DailyTransactionEntity>) {
        dao.insertAll(transactions)
    }

    override suspend fun update(transaction: DailyTransactionEntity) {
        dao.update(transaction)
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        dao.deleteByIds(ids)
    }

    override suspend fun getTodayExpense(today: Int): Double {
        return dao.getTodayExpense(today)
    }

    override suspend fun countInRange(startDate: Int, endDate: Int): Int {
        return dao.countInRange(startDate, endDate)
    }

    override suspend fun getTotalByCategoryInRange(startDate: Int, endDate: Int, categoryId: Long): Double {
        return dao.getTotalByCategoryInRange(startDate, endDate, categoryId)
    }

    override suspend fun findPotentialDuplicates(
        date: Int,
        type: String,
        amount: Double,
        categoryId: Long?
    ): List<DailyTransactionEntity> {
        return dao.findPotentialDuplicates(date, type, amount, categoryId)
    }

    override suspend fun findDuplicatesInTimeWindow(
        date: Int,
        type: String,
        amount: Double,
        timeWindowMinutes: Int
    ): List<DailyTransactionEntity> {
        val now = System.currentTimeMillis()
        val windowMs = timeWindowMinutes * 60 * 1000L
        return dao.findDuplicatesInTimeWindow(
            date = date,
            type = type,
            amount = amount,
            minCreatedAt = now - windowMs,
            maxCreatedAt = now + windowMs
        )
    }

    override suspend fun getByAccountId(accountId: Long, startDate: Int, endDate: Int): List<DailyTransactionEntity> {
        return dao.getByAccountIds(listOf(accountId), startDate, endDate)
    }
}
