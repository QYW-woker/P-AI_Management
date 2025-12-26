package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.domain.repository.CustomFieldRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自定义字段用例类
 *
 * 封装自定义字段相关的业务逻辑
 */
@Singleton
class CustomFieldUseCase @Inject constructor(
    private val repository: CustomFieldRepository
) {

    /**
     * 获取收入类别字段
     */
    fun getIncomeFields(): Flow<List<CustomFieldEntity>> {
        return repository.getAllFieldsByModule(ModuleType.INCOME)
    }

    /**
     * 获取支出类别字段
     */
    fun getExpenseFields(): Flow<List<CustomFieldEntity>> {
        return repository.getAllFieldsByModule(ModuleType.EXPENSE)
    }

    /**
     * 获取资产类别字段
     */
    fun getAssetFields(): Flow<List<CustomFieldEntity>> {
        return repository.getAllFieldsByModule(ModuleType.ASSET)
    }

    /**
     * 获取负债类别字段
     */
    fun getLiabilityFields(): Flow<List<CustomFieldEntity>> {
        return repository.getAllFieldsByModule(ModuleType.LIABILITY)
    }

    /**
     * 获取月度开销类别字段
     */
    fun getMonthlyExpenseFields(): Flow<List<CustomFieldEntity>> {
        return repository.getAllFieldsByModule(ModuleType.MONTHLY_EXPENSE)
    }

    /**
     * 根据模块类型获取字段
     */
    fun getFieldsByModule(moduleType: String): Flow<List<CustomFieldEntity>> {
        return repository.getAllFieldsByModule(moduleType)
    }

    /**
     * 添加自定义字段
     */
    suspend fun addField(
        moduleType: String,
        name: String,
        iconName: String,
        color: String
    ): Long {
        val field = CustomFieldEntity(
            moduleType = moduleType,
            name = name,
            iconName = iconName,
            color = color,
            isPreset = false,
            isEnabled = true,
            sortOrder = 100 // 新添加的字段排在后面
        )
        return repository.insert(field)
    }

    /**
     * 更新字段
     */
    suspend fun updateField(field: CustomFieldEntity) {
        repository.update(field)
    }

    /**
     * 删除字段（仅非预设字段）
     */
    suspend fun deleteField(id: Long): Boolean {
        return repository.deleteNonPreset(id)
    }

    /**
     * 切换字段启用状态
     */
    suspend fun toggleFieldEnabled(id: Long, isEnabled: Boolean) {
        repository.updateEnabled(id, isEnabled)
    }

    /**
     * 更新字段排序
     */
    suspend fun updateFieldOrder(id: Long, sortOrder: Int) {
        repository.updateSortOrder(id, sortOrder)
    }

    /**
     * 批量更新字段排序
     */
    suspend fun updateFieldOrders(fields: List<CustomFieldEntity>) {
        fields.forEachIndexed { index, field ->
            repository.updateSortOrder(field.id, index + 1)
        }
    }
}
