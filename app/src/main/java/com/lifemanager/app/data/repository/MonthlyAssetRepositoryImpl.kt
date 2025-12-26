package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.dao.MonthTotal
import com.lifemanager.app.core.database.dao.MonthlyAssetDao
import com.lifemanager.app.core.database.entity.MonthlyAssetEntity
import com.lifemanager.app.domain.repository.MonthlyAssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 月度资产仓库实现类
 */
@Singleton
class MonthlyAssetRepositoryImpl @Inject constructor(
    private val dao: MonthlyAssetDao
) : MonthlyAssetRepository {

    override fun getByMonth(yearMonth: Int): Flow<List<MonthlyAssetEntity>> {
        return dao.getByMonth(yearMonth)
    }

    override suspend fun getById(id: Long): MonthlyAssetEntity? {
        return dao.getById(id)
    }

    override suspend fun getTotalAssets(yearMonth: Int): Double {
        return dao.getTotalAssets(yearMonth)
    }

    override suspend fun getTotalLiabilities(yearMonth: Int): Double {
        return dao.getTotalLiabilities(yearMonth)
    }

    override fun getFieldTotals(yearMonth: Int, type: String): Flow<List<FieldTotal>> {
        return dao.getFieldTotals(yearMonth, type)
    }

    override fun getNetWorthTrend(startMonth: Int, endMonth: Int): Flow<List<MonthTotal>> {
        return dao.getNetWorthByRange(startMonth, endMonth)
    }

    override fun getAvailableMonths(): Flow<List<Int>> {
        return dao.getAvailableMonths()
    }

    override suspend fun insert(record: MonthlyAssetEntity): Long {
        return dao.insert(record)
    }

    override suspend fun update(record: MonthlyAssetEntity) {
        dao.update(record)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun copyFromPreviousMonth(sourceMonth: Int, targetMonth: Int) {
        dao.copyFromMonth(sourceMonth, targetMonth)
    }
}
