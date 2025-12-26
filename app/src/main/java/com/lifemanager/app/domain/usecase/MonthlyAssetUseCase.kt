package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.AssetType
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.core.database.entity.MonthlyAssetEntity
import com.lifemanager.app.domain.model.AssetFieldStats
import com.lifemanager.app.domain.model.AssetStats
import com.lifemanager.app.domain.model.MonthlyAssetWithField
import com.lifemanager.app.domain.model.NetWorthTrendPoint
import com.lifemanager.app.domain.repository.CustomFieldRepository
import com.lifemanager.app.domain.repository.MonthlyAssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 月度资产用例类
 *
 * 封装资产/负债相关的业务逻辑
 */
@Singleton
class MonthlyAssetUseCase @Inject constructor(
    private val repository: MonthlyAssetRepository,
    private val fieldRepository: CustomFieldRepository
) {

    /**
     * 获取指定月份的记录（带字段详情）
     */
    fun getRecordsWithFields(yearMonth: Int): Flow<List<MonthlyAssetWithField>> {
        return repository.getByMonth(yearMonth).map { records ->
            val fieldIds = records.mapNotNull { it.fieldId }.distinct()
            val fields = if (fieldIds.isNotEmpty()) {
                fieldRepository.getFieldsByIds(fieldIds).associateBy { it.id }
            } else {
                emptyMap()
            }

            records.map { record ->
                MonthlyAssetWithField(
                    record = record,
                    field = record.fieldId?.let { fields[it] }
                )
            }
        }
    }

    /**
     * 获取月度统计数据
     */
    suspend fun getAssetStats(yearMonth: Int): AssetStats {
        val totalAssets = repository.getTotalAssets(yearMonth)
        val totalLiabilities = repository.getTotalLiabilities(yearMonth)

        return AssetStats(
            yearMonth = yearMonth,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities
        )
    }

    /**
     * 获取资产字段统计
     */
    fun getAssetFieldStats(yearMonth: Int): Flow<List<AssetFieldStats>> {
        return combine(
            repository.getFieldTotals(yearMonth, AssetType.ASSET),
            fieldRepository.getFieldsByModule(ModuleType.ASSET)
        ) { totals, fields ->
            convertToFieldStats(totals, fields.associateBy { it.id }, true)
        }
    }

    /**
     * 获取负债字段统计
     */
    fun getLiabilityFieldStats(yearMonth: Int): Flow<List<AssetFieldStats>> {
        return combine(
            repository.getFieldTotals(yearMonth, AssetType.LIABILITY),
            fieldRepository.getFieldsByModule(ModuleType.LIABILITY)
        ) { totals, fields ->
            convertToFieldStats(totals, fields.associateBy { it.id }, false)
        }
    }

    /**
     * 将统计数据转换为AssetFieldStats
     */
    private fun convertToFieldStats(
        totals: List<com.lifemanager.app.core.database.dao.FieldTotal>,
        fieldsMap: Map<Long, CustomFieldEntity>,
        isAsset: Boolean
    ): List<AssetFieldStats> {
        val total = totals.sumOf { it.total }

        return totals.mapNotNull { fieldTotal ->
            fieldTotal.fieldId?.let { fieldId ->
                fieldsMap[fieldId]?.let { field ->
                    AssetFieldStats(
                        fieldId = field.id,
                        fieldName = field.name,
                        fieldColor = field.color,
                        fieldIcon = field.iconName,
                        amount = fieldTotal.total,
                        percentage = if (total > 0) (fieldTotal.total / total) * 100 else 0.0,
                        isAsset = isAsset
                    )
                }
            }
        }.sortedByDescending { it.amount }
    }

    /**
     * 获取净资产趋势（过去12个月）
     */
    fun getNetWorthTrend(currentYearMonth: Int): Flow<List<NetWorthTrendPoint>> {
        val year = currentYearMonth / 100
        val month = currentYearMonth % 100

        // 计算12个月前的年月
        val startYear = if (month <= 12) year - 1 else year
        val startMonth = if (month <= 12) month else month - 12
        val startYearMonth = startYear * 100 + startMonth

        return repository.getNetWorthTrend(startYearMonth, currentYearMonth).map { monthTotals ->
            monthTotals.map { monthTotal ->
                NetWorthTrendPoint(
                    yearMonth = monthTotal.yearMonth,
                    netWorth = monthTotal.total
                )
            }
        }
    }

    /**
     * 获取可用的月份列表
     */
    fun getAvailableMonths(): Flow<List<Int>> {
        return repository.getAvailableMonths()
    }

    /**
     * 根据ID获取记录
     */
    suspend fun getRecordById(id: Long): MonthlyAssetEntity? {
        return repository.getById(id)
    }

    /**
     * 添加记录
     */
    suspend fun addRecord(
        yearMonth: Int,
        isAsset: Boolean,
        fieldId: Long,
        amount: Double,
        note: String
    ): Long {
        val record = MonthlyAssetEntity(
            yearMonth = yearMonth,
            type = if (isAsset) AssetType.ASSET else AssetType.LIABILITY,
            fieldId = fieldId,
            amount = amount,
            note = note
        )
        return repository.insert(record)
    }

    /**
     * 更新记录
     */
    suspend fun updateRecord(
        id: Long,
        yearMonth: Int,
        isAsset: Boolean,
        fieldId: Long,
        amount: Double,
        note: String
    ) {
        val existingRecord = repository.getById(id) ?: return

        val updatedRecord = existingRecord.copy(
            yearMonth = yearMonth,
            type = if (isAsset) AssetType.ASSET else AssetType.LIABILITY,
            fieldId = fieldId,
            amount = amount,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        repository.update(updatedRecord)
    }

    /**
     * 删除记录
     */
    suspend fun deleteRecord(id: Long) {
        repository.delete(id)
    }

    /**
     * 从上月复制数据
     */
    suspend fun copyFromPreviousMonth(currentYearMonth: Int) {
        val year = currentYearMonth / 100
        val month = currentYearMonth % 100

        val previousYearMonth = if (month == 1) {
            (year - 1) * 100 + 12
        } else {
            year * 100 + (month - 1)
        }

        repository.copyFromPreviousMonth(previousYearMonth, currentYearMonth)
    }

    /**
     * 获取资产类别字段列表
     */
    fun getAssetFields(): Flow<List<CustomFieldEntity>> {
        return fieldRepository.getFieldsByModule(ModuleType.ASSET)
    }

    /**
     * 获取负债类别字段列表
     */
    fun getLiabilityFields(): Flow<List<CustomFieldEntity>> {
        return fieldRepository.getFieldsByModule(ModuleType.LIABILITY)
    }
}
