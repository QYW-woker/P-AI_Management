package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.PresetData
import com.lifemanager.app.core.database.dao.CustomFieldDao
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.domain.repository.CustomFieldRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自定义字段仓库实现类
 *
 * 提供字段的增删改查功能，并负责预设数据的初始化
 */
@Singleton
class CustomFieldRepositoryImpl @Inject constructor(
    private val dao: CustomFieldDao
) : CustomFieldRepository {

    override fun getFieldsByModule(moduleType: String): Flow<List<CustomFieldEntity>> {
        return dao.getFieldsByModule(moduleType)
    }

    override fun getAllFieldsByModule(moduleType: String): Flow<List<CustomFieldEntity>> {
        return dao.getAllFieldsByModule(moduleType)
    }

    override suspend fun getFieldById(id: Long): CustomFieldEntity? {
        return dao.getFieldById(id)
    }

    override suspend fun getFieldsByIds(ids: List<Long>): List<CustomFieldEntity> {
        return dao.getFieldsByIds(ids)
    }

    override suspend fun insert(field: CustomFieldEntity): Long {
        return dao.insert(field)
    }

    override suspend fun update(field: CustomFieldEntity) {
        dao.update(field)
    }

    override suspend fun deleteNonPreset(id: Long): Boolean {
        return dao.deleteNonPreset(id) > 0
    }

    override suspend fun updateEnabled(id: Long, isEnabled: Boolean) {
        dao.updateEnabled(id, isEnabled)
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) {
        dao.updateSortOrder(id, sortOrder)
    }

    override suspend fun needsPresetInit(): Boolean {
        return dao.countPresets() == 0
    }

    override suspend fun initPresets() {
        // 插入所有预设数据
        dao.insertAll(PresetData.getAllCustomFields())
    }
}
