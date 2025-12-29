package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 日常交易记录实体类
 *
 * 用于记录日常的收入和支出流水
 * 支持语音输入、截图识别等多种录入方式
 * 可自动汇总到月度开销统计
 */
@Entity(
    tableName = "daily_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["categoryId"]),
        Index(value = ["type"]),
        Index(value = ["ledgerId"])
    ]
)
data class DailyTransactionEntity(
    // 主键ID
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 账本ID（可选，null表示默认账本）
    val ledgerId: Long? = null,

    // 类型: INCOME(收入) 或 EXPENSE(支出)
    val type: String,

    // 金额
    val amount: Double,

    // 分类ID
    val categoryId: Long?,

    // 日期，epochDay格式
    val date: Int,

    // 时间，HH:mm格式
    val time: String = "",

    // 备注说明
    val note: String = "",

    // 标签，JSON数组格式
    val tags: String = "[]",

    // 附件路径，JSON数组格式
    val attachments: String = "[]",

    // 录入来源
    // MANUAL: 手动输入
    // VOICE: 语音识别
    // SCREENSHOT: 截图识别
    // IMPORT: 导入
    val source: String = "MANUAL",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 录入来源枚举
 */
object TransactionSource {
    const val MANUAL = "MANUAL"         // 手动输入
    const val VOICE = "VOICE"           // 语音识别
    const val SCREENSHOT = "SCREENSHOT" // 截图识别
    const val IMPORT = "IMPORT"         // 导入
}
