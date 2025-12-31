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
 *
 * 支持内置类型和用户自定义类型
 */
object LedgerType {
    const val PERSONAL = "PERSONAL"
    const val FAMILY = "FAMILY"
    const val BUSINESS = "BUSINESS"
    const val TRAVEL = "TRAVEL"
    const val PROJECT = "PROJECT"
    const val INVESTMENT = "INVESTMENT"
    const val CUSTOM = "CUSTOM"

    // 获取所有内置类型
    val builtInTypes = listOf(PERSONAL, FAMILY, BUSINESS, TRAVEL, PROJECT, INVESTMENT)

    // 获取显示名称
    fun getDisplayName(type: String): String = when (type) {
        PERSONAL -> "个人"
        FAMILY -> "家庭"
        BUSINESS -> "生意"
        TRAVEL -> "旅行"
        PROJECT -> "项目"
        INVESTMENT -> "投资"
        else -> type  // 自定义类型返回类型名称本身
    }

    // 获取图标
    fun getIcon(type: String): String = when (type) {
        PERSONAL -> "👤"
        FAMILY -> "👨‍👩‍👧"
        BUSINESS -> "💼"
        TRAVEL -> "✈️"
        PROJECT -> "📋"
        INVESTMENT -> "📈"
        else -> "📒"  // 自定义类型默认图标
    }
}

/**
 * 自定义账本类型实体
 */
@Entity(tableName = "custom_ledger_types")
data class CustomLedgerTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "📒",
    val color: String = "#2196F3",
    val createdAt: Long = System.currentTimeMillis()
)
