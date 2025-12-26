package com.lifemanager.app.domain.repository

import com.lifemanager.app.core.database.entity.CustomFieldEntity
import kotlinx.coroutines.flow.Flow

/**
 * 自定义字段仓库接口
 */
interface CustomFieldRepository {

    /**
     * 根据模块类型获取所有启用的字段
     */
    fun getFieldsByModule(moduleType: String): Flow<List<CustomFieldEntity>>

    /**
     * 根据模块类型获取所有字段（包括禁用的）
     */
    fun getAllFieldsByModule(moduleType: String): Flow<List<CustomFieldEntity>>

    /**
     * 根据ID获取字段
     */
    suspend fun getFieldById(id: Long): CustomFieldEntity?

    /**
     * 根据ID列表获取字段
     */
    suspend fun getFieldsByIds(ids: List<Long>): List<CustomFieldEntity>

    /**
     * 插入字段
     */
    suspend fun insert(field: CustomFieldEntity): Long

    /**
     * 更新字段
     */
    suspend fun update(field: CustomFieldEntity)

    /**
     * 删除非预设字段
     */
    suspend fun deleteNonPreset(id: Long): Boolean

    /**
     * 更新字段启用状态
     */
    suspend fun updateEnabled(id: Long, isEnabled: Boolean)

    /**
     * 更新字段排序顺序
     */
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /**
     * 检查是否需要初始化预设数据
     */
    suspend fun needsPresetInit(): Boolean

    /**
     * 初始化预设数据
     */
    suspend fun initPresets()
}
