package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.dao.InvestmentFieldTotal
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.core.database.entity.MonthlyInvestmentEntity
import com.lifemanager.app.domain.model.InvestmentFieldStats
import com.lifemanager.app.domain.model.MonthlyInvestmentWithField
import com.lifemanager.app.domain.model.InvestmentMonthlyStats
import com.lifemanager.app.domain.model.MonthlyTrendPoint
import com.lifemanager.app.domain.repository.CustomFieldRepository
import com.lifemanager.app.domain.repository.MonthlyInvestmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 月度定投用例类
 *
 * 封装月度定投相关的业务逻辑，提供给ViewModel使用
 */
@Singleton
class MonthlyInvestmentUseCase @Inject constructor(
    private val repository: MonthlyInvestmentRepository,
    private val fieldRepository: CustomFieldRepository
) {

    /**
     * 获取指定月份的定投记录（带字段详情）
     */
    fun getRecordsWithFields(yearMonth: Int): Flow<List<MonthlyInvestmentWithField>> {
        return repository.getByMonth(yearMonth).map { records ->
            val fieldIds = records.mapNotNull { it.fieldId }.distinct()
            val fields = if (fieldIds.isNotEmpty()) {
                fieldRepository.getFieldsByIds(fieldIds).associateBy { it.id }
            } else {
                emptyMap()
            }

            records.map { record ->
                MonthlyInvestmentWithField(
                    record = record,
                    field = fields[record.fieldId]
                )
            }
        }
    }

    /**
     * 获取月度统计数据
     */
    suspend fun getMonthlyStats(yearMonth: Int): InvestmentMonthlyStats {
        val totalBudget = repository.getTotalBudget(yearMonth)
        val totalActual = repository.getTotalActual(yearMonth)

        return InvestmentMonthlyStats(
            yearMonth = yearMonth,
            totalBudget = totalBudget,
            totalActual = totalActual
        )
    }

    /**
     * 获取定投字段统计（带字段详情）
     */
    fun getFieldStats(yearMonth: Int): Flow<List<InvestmentFieldStats>> {
        return combine(
            repository.getFieldTotals(yearMonth),
            fieldRepository.getFieldsByModule(ModuleType.INVESTMENT)
        ) { totals, fields ->
            convertToFieldStats(totals, fields.associateBy { it.id })
        }
    }

    /**
     * 将DAO返回的统计数据转换为InvestmentFieldStats
     */
    private fun convertToFieldStats(
        totals: List<InvestmentFieldTotal>,
        fieldsMap: Map<Long, com.lifemanager.app.core.database.entity.CustomFieldEntity>
    ): List<InvestmentFieldStats> {
        val total = totals.sumOf { it.totalActual }

        return totals.mapNotNull { fieldTotal ->
            fieldsMap[fieldTotal.fieldId]?.let { field ->
                InvestmentFieldStats(
                    fieldId = field.id,
                    fieldName = field.name,
                    fieldColor = field.color,
                    fieldIcon = field.iconName,
                    budgetAmount = fieldTotal.totalBudget,
                    actualAmount = fieldTotal.totalActual,
                    percentage = if (total > 0) (fieldTotal.totalActual / total) * 100 else 0.0
                )
            }
        }.sortedByDescending { it.actualAmount }
    }

    /**
     * 获取年度预算趋势
     */
    fun getYearlyBudgetTrend(year: Int): Flow<List<MonthlyTrendPoint>> {
        return repository.getMonthlyBudgetTrend(year).map { monthTotals ->
            monthTotals.map { monthTotal ->
                MonthlyTrendPoint(
                    yearMonth = monthTotal.yearMonth,
                    amount = monthTotal.total
                )
            }
        }
    }

    /**
     * 获取年度实际投入趋势
     */
    fun getYearlyActualTrend(year: Int): Flow<List<MonthlyTrendPoint>> {
        return repository.getMonthlyActualTrend(year).map { monthTotals ->
            monthTotals.map { monthTotal ->
                MonthlyTrendPoint(
                    yearMonth = monthTotal.yearMonth,
                    amount = monthTotal.total
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
     * 获取最近的记录
     */
    fun getRecentRecords(limit: Int = 10): Flow<List<MonthlyInvestmentWithField>> {
        return repository.getRecentRecords(limit).map { records ->
            val fieldIds = records.mapNotNull { it.fieldId }.distinct()
            val fields = if (fieldIds.isNotEmpty()) {
                fieldRepository.getFieldsByIds(fieldIds).associateBy { it.id }
            } else {
                emptyMap()
            }

            records.map { record ->
                MonthlyInvestmentWithField(
                    record = record,
                    field = fields[record.fieldId]
                )
            }
        }
    }

    /**
     * 根据ID获取记录
     */
    suspend fun getRecordById(id: Long): MonthlyInvestmentEntity? {
        return repository.getById(id)
    }

    /**
     * 添加定投记录
     */
    suspend fun addRecord(
        yearMonth: Int,
        fieldId: Long,
        budgetAmount: Double,
        actualAmount: Double,
        note: String,
        recordDate: Int = java.time.LocalDate.now().toEpochDay().toInt()
    ): Long {
        val record = MonthlyInvestmentEntity(
            yearMonth = yearMonth,
            fieldId = fieldId,
            budgetAmount = budgetAmount,
            actualAmount = actualAmount,
            note = note,
            recordDate = recordDate
        )
        return repository.insert(record)
    }

    /**
     * 更新定投记录
     */
    suspend fun updateRecord(
        id: Long,
        yearMonth: Int,
        fieldId: Long,
        budgetAmount: Double,
        actualAmount: Double,
        note: String
    ) {
        val existingRecord = repository.getById(id) ?: return

        val updatedRecord = existingRecord.copy(
            yearMonth = yearMonth,
            fieldId = fieldId,
            budgetAmount = budgetAmount,
            actualAmount = actualAmount,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        repository.update(updatedRecord)
    }

    /**
     * 删除定投记录
     */
    suspend fun deleteRecord(id: Long) {
        repository.delete(id)
    }

    /**
     * 初始化预设字段（如果需要）
     */
    suspend fun initPresetFieldsIfNeeded() {
        if (fieldRepository.needsPresetInit()) {
            fieldRepository.initPresets()
        }
    }

    /**
     * 获取定投类别字段列表
     */
    fun getInvestmentFields(): Flow<List<com.lifemanager.app.core.database.entity.CustomFieldEntity>> {
        return fieldRepository.getFieldsByModule(ModuleType.INVESTMENT)
    }
}
