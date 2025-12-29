package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

/**
 * 账本DAO接口
 */
@Dao
interface LedgerDao {

    /**
     * 获取所有账本（按排序顺序）
     */
    @Query("""
        SELECT * FROM ledgers
        WHERE isArchived = 0
        ORDER BY isDefault DESC, sortOrder ASC, createdAt ASC
    """)
    fun getAllLedgers(): Flow<List<LedgerEntity>>

    /**
     * 获取所有账本（包括归档）
     */
    @Query("""
        SELECT * FROM ledgers
        ORDER BY isArchived ASC, isDefault DESC, sortOrder ASC
    """)
    fun getAllLedgersIncludingArchived(): Flow<List<LedgerEntity>>

    /**
     * 获取默认账本
     */
    @Query("SELECT * FROM ledgers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultLedger(): LedgerEntity?

    /**
     * 获取默认账本（Flow版本）
     */
    @Query("SELECT * FROM ledgers WHERE isDefault = 1 LIMIT 1")
    fun getDefaultLedgerFlow(): Flow<LedgerEntity?>

    /**
     * 根据ID获取账本
     */
    @Query("SELECT * FROM ledgers WHERE id = :id")
    suspend fun getLedgerById(id: Long): LedgerEntity?

    /**
     * 根据ID获取账本（Flow版本）
     */
    @Query("SELECT * FROM ledgers WHERE id = :id")
    fun getLedgerByIdFlow(id: Long): Flow<LedgerEntity?>

    /**
     * 插入账本
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: LedgerEntity): Long

    /**
     * 更新账本
     */
    @Update
    suspend fun update(ledger: LedgerEntity)

    /**
     * 删除账本
     */
    @Query("DELETE FROM ledgers WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * 设置默认账本（先清除其他默认，再设置新默认）
     */
    @Query("UPDATE ledgers SET isDefault = 0 WHERE isDefault = 1")
    suspend fun clearDefaultLedger()

    @Query("UPDATE ledgers SET isDefault = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDefaultLedger(id: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * 归档账本
     */
    @Query("UPDATE ledgers SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新排序顺序
     */
    @Query("UPDATE ledgers SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long = System.currentTimeMillis())

    /**
     * 统计账本数量
     */
    @Query("SELECT COUNT(*) FROM ledgers WHERE isArchived = 0")
    suspend fun countLedgers(): Int

    /**
     * 获取最大排序顺序
     */
    @Query("SELECT MAX(sortOrder) FROM ledgers")
    suspend fun getMaxSortOrder(): Int?
}
