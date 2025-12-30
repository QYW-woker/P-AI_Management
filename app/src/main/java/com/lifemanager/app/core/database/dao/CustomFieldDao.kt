package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import kotlinx.coroutines.flow.Flow

/**
 * 自定义字段DAO接口
 *
 * 提供对custom_fields表的所有数据库操作
 * 使用Flow实现响应式数据查询
 */
@Dao
interface CustomFieldDao {

    /**
     * 根据模块类型获取所有启用的字段，按排序顺序排列
     */
    @Query("""
        SELECT * FROM custom_fields
        WHERE moduleType = :moduleType AND isEnabled = 1
        ORDER BY sortOrder ASC
    """)
    fun getFieldsByModule(moduleType: String): Flow<List<CustomFieldEntity>>

    /**
     * 根据模块类型获取所有字段（包括禁用的）
     */
    @Query("""
        SELECT * FROM custom_fields
        WHERE moduleType = :moduleType
        ORDER BY sortOrder ASC
    """)
    fun getAllFieldsByModule(moduleType: String): Flow<List<CustomFieldEntity>>

    /**
     * 获取指定父字段下的子字段
     */
    @Query("""
        SELECT * FROM custom_fields
        WHERE parentId = :parentId
        ORDER BY sortOrder ASC
    """)
    fun getChildFields(parentId: Long): Flow<List<CustomFieldEntity>>

    /**
     * 根据ID获取单个字段
     */
    @Query("SELECT * FROM custom_fields WHERE id = :id")
    suspend fun getFieldById(id: Long): CustomFieldEntity?

    /**
     * 根据ID列表获取多个字段
     */
    @Query("SELECT * FROM custom_fields WHERE id IN (:ids)")
    suspend fun getFieldsByIds(ids: List<Long>): List<CustomFieldEntity>

    /**
     * 根据ID列表获取字段（Flow版本）
     */
    @Query("SELECT * FROM custom_fields WHERE id IN (:ids)")
    fun getFieldsByIdsFlow(ids: List<Long>): Flow<List<CustomFieldEntity>>

    /**
     * 检查字段名称是否在同模块中已存在
     */
    @Query("""
        SELECT COUNT(*) FROM custom_fields
        WHERE moduleType = :moduleType AND name = :name AND id != :excludeId
    """)
    suspend fun countByName(moduleType: String, name: String, excludeId: Long = 0): Int

    /**
     * 获取模块中最大的排序顺序
     */
    @Query("SELECT MAX(sortOrder) FROM custom_fields WHERE moduleType = :moduleType")
    suspend fun getMaxSortOrder(moduleType: String): Int?

    /**
     * 插入或替换字段
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: CustomFieldEntity): Long

    /**
     * 批量插入字段
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<CustomFieldEntity>)

    /**
     * 更新字段
     */
    @Update
    suspend fun update(field: CustomFieldEntity)

    /**
     * 删除非预设字段
     */
    @Query("DELETE FROM custom_fields WHERE id = :id AND isPreset = 0")
    suspend fun deleteNonPreset(id: Long): Int

    /**
     * 更新字段排序顺序
     */
    @Query("UPDATE custom_fields SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /**
     * 更新字段启用状态
     */
    @Query("UPDATE custom_fields SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, isEnabled: Boolean)

    /**
     * 获取所有启用的字段数量
     */
    @Query("SELECT COUNT(*) FROM custom_fields WHERE moduleType = :moduleType AND isEnabled = 1")
    suspend fun countEnabledByModule(moduleType: String): Int

    /**
     * 检查是否已有预设数据
     */
    @Query("SELECT COUNT(*) FROM custom_fields WHERE isPreset = 1")
    suspend fun countPresets(): Int

    /**
     * 根据多个模块类型获取所有启用的字段
     */
    @Query("""
        SELECT * FROM custom_fields
        WHERE moduleType IN (:moduleTypes) AND isEnabled = 1
        ORDER BY moduleType, sortOrder ASC
    """)
    fun getFieldsByModuleTypes(moduleTypes: List<String>): Flow<List<CustomFieldEntity>>

    /**
     * 获取所有字段（同步版本，用于AI分析）
     */
    @Query("SELECT * FROM custom_fields ORDER BY moduleType, sortOrder ASC")
    suspend fun getAllFieldsSync(): List<CustomFieldEntity>
}
