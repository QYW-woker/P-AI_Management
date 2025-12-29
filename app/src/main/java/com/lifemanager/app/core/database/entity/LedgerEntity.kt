package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账本实体
 *
 * 支持多账本管理，每个账本可以独立记录交易
 */
@Entity(tableName = "ledgers")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * 账本名称
     */
    val name: String,

    /**
     * 账本描述
     */
    val description: String = "",

    /**
     * 账本图标（emoji或图标名称）
     */
    val icon: String = "book",

    /**
     * 账本颜色
     */
    val color: String = "#2196F3",

    /**
     * 是否为默认账本
     */
    val isDefault: Boolean = false,

    /**
     * 排序顺序
     */
    val sortOrder: Int = 0,

    /**
     * 是否归档
     */
    val isArchived: Boolean = false,

    /**
     * 账本类型：PERSONAL（个人）、FAMILY（家庭）、BUSINESS（生意）
     */
    val ledgerType: String = "PERSONAL",

    /**
     * 预算金额（可选）
     */
    val budgetAmount: Double? = null,

    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 更新时间
     */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 账本类型枚举
 */
object LedgerType {
    const val PERSONAL = "PERSONAL"
    const val FAMILY = "FAMILY"
    const val BUSINESS = "BUSINESS"
}
