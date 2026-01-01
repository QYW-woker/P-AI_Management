package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.dao.FieldTotal
import com.lifemanager.app.core.database.entity.IncomeExpenseType
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.core.database.entity.MonthlyIncomeExpenseEntity
import com.lifemanager.app.domain.model.FieldStats
import com.lifemanager.app.domain.model.MonthlyIncomeExpenseWithField
import com.lifemanager.app.domain.model.IncomeExpenseMonthlyStats
import com.lifemanager.app.domain.model.MonthlyTrendPoint
import com.lifemanager.app.domain.repository.CustomFieldRepository
import com.lifemanager.app.domain.repository.MonthlyIncomeExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 月度收支用例类
 *
 * 封装月度收支相关的业务逻辑，提供给ViewModel使用
 */
@Singleton
class MonthlyIncomeExpenseUseCase @Inject constructor(
    private val repository: MonthlyIncomeExpenseRepository,
    private val fieldRepository: CustomFieldRepository
) {

    /**
     * 获取指定月份的收支记录（带字段详情）
     */
    fun getRecordsWithFields(yearMonth: Int): Flow<List<MonthlyIncomeExpenseWithField>> {
        return repository.getByMonth(yearMonth).map { records ->
            val fieldIds = records.mapNotNull { it.fieldId }.distinct()
            val fields = if (fieldIds.isNotEmpty()) {
                fieldRepository.getFieldsByIds(fieldIds).associateBy { it.id }
            } else {
                emptyMap()
            }

            records.map { record ->
                MonthlyIncomeExpenseWithField(
                    record = record,
                    field = fields[record.fieldId]
                )
            }
        }
    }

    /**
     * 获取月度统计数据
     */
    suspend fun getMonthlyStats(yearMonth: Int): IncomeExpenseMonthlyStats {
        val totalIncome = repository.getTotalIncome(yearMonth)
        val totalExpense = repository.getTotalExpense(yearMonth)

        return IncomeExpenseMonthlyStats(
            yearMonth = yearMonth,
            totalIncome = totalIncome,
            totalExpense = totalExpense
        )
    }

    /**
     * 获取收入字段统计（带字段详情）
     */
    fun getIncomeFieldStats(yearMonth: Int): Flow<List<FieldStats>> {
        return combine(
            repository.getIncomeFieldTotals(yearMonth),
            fieldRepository.getFieldsByModule(ModuleType.INCOME)
        ) { totals, fields ->
            convertToFieldStats(totals, fields.associateBy { it.id })
        }
    }

    /**
     * 获取支出字段统计（带字段详情）
     */
    fun getExpenseFieldStats(yearMonth: Int): Flow<List<FieldStats>> {
        return combine(
            repository.getExpenseFieldTotals(yearMonth),
            fieldRepository.getFieldsByModule(ModuleType.EXPENSE)
        ) { totals, fields ->
            convertToFieldStats(totals, fields.associateBy { it.id })
        }
    }

    /**
     * 将DAO返回的统计数据转换为FieldStats
     */
    private fun convertToFieldStats(
        totals: List<FieldTotal>,
        fieldsMap: Map<Long, com.lifemanager.app.core.database.entity.CustomFieldEntity>
    ): List<FieldStats> {
        val total = totals.sumOf { it.total }

        return totals.mapNotNull { fieldTotal ->
            fieldsMap[fieldTotal.fieldId]?.let { field ->
                FieldStats(
                    fieldId = field.id,
                    fieldName = field.name,
                    fieldColor = field.color,
                    fieldIcon = field.iconName,
                    amount = fieldTotal.total,
                    percentage = if (total > 0) (fieldTotal.total / total) * 100 else 0.0
                )
            }
        }.sortedByDescending { it.amount }
    }

    /**
     * 获取年度收入趋势
     */
    fun getYearlyIncomeTrend(year: Int): Flow<List<MonthlyTrendPoint>> {
        return repository.getMonthlyIncomeTrend(year).map { monthTotals ->
            monthTotals.map { monthTotal ->
                MonthlyTrendPoint(
                    yearMonth = monthTotal.yearMonth,
                    amount = monthTotal.total
                )
            }
        }
    }

    /**
     * 获取年度支出趋势
     */
    fun getYearlyExpenseTrend(year: Int): Flow<List<MonthlyTrendPoint>> {
        return repository.getMonthlyExpenseTrend(year).map { monthTotals ->
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
    fun getRecentRecords(limit: Int = 10): Flow<List<MonthlyIncomeExpenseWithField>> {
        return repository.getRecentRecords(limit).map { records ->
            val fieldIds = records.mapNotNull { it.fieldId }.distinct()
            val fields = if (fieldIds.isNotEmpty()) {
                fieldRepository.getFieldsByIds(fieldIds).associateBy { it.id }
            } else {
                emptyMap()
            }

            records.map { record ->
                MonthlyIncomeExpenseWithField(
                    record = record,
                    field = fields[record.fieldId]
                )
            }
        }
    }

    /**
     * 根据ID获取记录
     */
    suspend fun getRecordById(id: Long): MonthlyIncomeExpenseEntity? {
        return repository.getById(id)
    }

    /**
     * 添加收支记录
     */
    suspend fun addRecord(
        yearMonth: Int,
        type: String,
        fieldId: Long,
        amount: Double,
        note: String,
        recordDate: Int = java.time.LocalDate.now().toEpochDay().toInt()
    ): Long {
        val record = MonthlyIncomeExpenseEntity(
            yearMonth = yearMonth,
            type = type,
            fieldId = fieldId,
            amount = amount,
            note = note,
            recordDate = recordDate
        )
        return repository.insert(record)
    }

    /**
     * 更新收支记录
     */
    suspend fun updateRecord(
        id: Long,
        yearMonth: Int,
        type: String,
        fieldId: Long,
        amount: Double,
        note: String
    ) {
        val existingRecord = repository.getById(id) ?: return

        val updatedRecord = existingRecord.copy(
            yearMonth = yearMonth,
            type = type,
            fieldId = fieldId,
            amount = amount,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        repository.update(updatedRecord)
    }

    /**
     * 删除收支记录
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
     * 获取收入类别字段列表
     */
    fun getIncomeFields(): Flow<List<com.lifemanager.app.core.database.entity.CustomFieldEntity>> {
        return fieldRepository.getFieldsByModule(ModuleType.INCOME)
    }

    /**
     * 获取支出类别字段列表
     */
    fun getExpenseFields(): Flow<List<com.lifemanager.app.core.database.entity.CustomFieldEntity>> {
        return fieldRepository.getFieldsByModule(ModuleType.EXPENSE)
    }
}
