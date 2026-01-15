package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自定义字段定义表
 *
 * 用于存储用户自定义的收入/支出/资产/开销类别
 * 支持层级结构，可以有父子关系
 * 用户可以自由添加、编辑、删除自定义类别（预设类别不可删除）
 */
@Entity(tableName = "custom_fields")
data class CustomFieldEntity(
    // 主键ID，自动生成
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 字段所属模块类型
    // 可选值: INCOME(收入), EXPENSE(支出), ASSET(资产), LIABILITY(负债), MONTHLY_EXPENSE(月度开销)
    val moduleType: String,

    // 字段名称，如"工资收入"、"房租"等
    val name: String,

    // 父字段ID，用于层级结构
    // 0表示顶级字段，非0表示子字段
    val parentId: Long = 0,

    // 图标名称，对应Material Icon名称
    // 如: "work", "home", "shopping_cart"等
    val iconName: String = "category",

    // 颜色值，十六进制格式
    // 如: "#4CAF50", "#2196F3"等
    val color: String = "#4CAF50",

    // 标签类型，用于统计分类
    // 可选值: SAVINGS(储蓄), INVESTMENT(投资), CONSUMPTION(消费), FIXED(固定支出), OTHER(其他)
    val tagType: String = "OTHER",

    // 排序顺序，数字越小越靠前
    val sortOrder: Int = 0,

    // 是否启用，禁用后不会在列表中显示
    val isEnabled: Boolean = true,

    // 是否计入统计，禁用后不会计入总额统计
    val includeInStats: Boolean = true,

    // 是否为系统预设项
    // 预设项不可删除，但可以禁用
    val isPreset: Boolean = false,

    // 创建时间戳
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 模块类型枚举
 */
object ModuleType {
    const val INCOME = "INCOME"                 // 收入
    const val EXPENSE = "EXPENSE"               // 支出
    const val ASSET = "ASSET"                   // 资产
    const val LIABILITY = "LIABILITY"           // 负债
    const val MONTHLY_EXPENSE = "MONTHLY_EXPENSE" // 月度开销
    const val INVESTMENT = "INVESTMENT"         // 定投
}

/**
 * 标签类型枚举
 */
object TagType {
    const val SAVINGS = "SAVINGS"           // 储蓄类
    const val INVESTMENT = "INVESTMENT"     // 投资类
    const val CONSUMPTION = "CONSUMPTION"   // 消费类
    const val FIXED = "FIXED"               // 固定支出
    const val OTHER = "OTHER"               // 其他
}
