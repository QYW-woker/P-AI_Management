package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.dao.MonthTotal
import com.lifemanager.app.core.database.entity.MonthlyAssetEntity
import kotlinx.coroutines.flow.Flow

/**
 * 月度资产仓库接口
 *
 * 定义资产/负债数据的存取操作
 */
interface MonthlyAssetRepository {

    /**
     * 获取指定月份的所有记录
     */
    fun getByMonth(yearMonth: Int): Flow<List<MonthlyAssetEntity>>

    /**
     * 根据ID获取记录
     */
    suspend fun getById(id: Long): MonthlyAssetEntity?

    /**
     * 获取指定月份的资产总额
     */
    suspend fun getTotalAssets(yearMonth: Int): Double

    /**
     * 获取指定月份的负债总额
     */
    suspend fun getTotalLiabilities(yearMonth: Int): Double

    /**
     * 获取指定月份各字段的统计
     */
    fun getFieldTotals(yearMonth: Int, type: String): Flow<List<FieldTotal>>

    /**
     * 获取月份范围内的净资产趋势
     */
    fun getNetWorthTrend(startMonth: Int, endMonth: Int): Flow<List<MonthTotal>>

    /**
     * 获取有数据的月份列表
     */
    fun getAvailableMonths(): Flow<List<Int>>

    /**
     * 插入记录
     */
    suspend fun insert(record: MonthlyAssetEntity): Long

    /**
     * 更新记录
     */
    suspend fun update(record: MonthlyAssetEntity)

    /**
     * 删除记录
     */
    suspend fun delete(id: Long)

    /**
     * 从上月复制数据
     */
    suspend fun copyFromPreviousMonth(sourceMonth: Int, targetMonth: Int)
}
