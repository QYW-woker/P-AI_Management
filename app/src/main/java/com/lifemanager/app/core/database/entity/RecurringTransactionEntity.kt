package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 周期记账实体
 *
 * 用于定期自动生成交易记录
 */
@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FundAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["ledgerId"]),
        Index(value = ["nextDueDate"]),
        Index(value = ["accountId"]),
        Index(value = ["isEnabled"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * 所属账本ID
     */
    val ledgerId: Long? = null,

    /**
     * 名称/标题
     */
    val name: String,

    /**
     * 交易类型：INCOME/EXPENSE
     */
    val type: String,

    /**
     * 金额
     */
    val amount: Double,

    /**
     * 分类ID
     */
    val categoryId: Long? = null,

    /**
     * 关联的资金账户ID
     */
    val accountId: Long? = null,

    /**
     * 标签，JSON数组格式
     */
    val tags: String = "[]",

    /**
     * 备注
     */
    val note: String = "",

    /**
     * 周期类型：DAILY, WEEKLY, MONTHLY, YEARLY
     */
    val frequency: String,

    /**
     * 周期间隔（例如：每2周、每3个月）
     */
    val interval: Int = 1,

    /**
     * 周几执行（用于WEEKLY类型，1-7表示周一到周日）
     */
    val dayOfWeek: Int? = null,

    /**
     * 每月几号执行（用于MONTHLY类型，1-31）
     */
    val dayOfMonth: Int? = null,

    /**
     * 每年的月份（用于YEARLY类型，1-12）
     */
    val monthOfYear: Int? = null,

    /**
     * 开始日期（epochDay）
     */
    val startDate: Int,

    /**
     * 结束日期（epochDay），null表示无限期
     */
    val endDate: Int? = null,

    /**
     * 下次执行日期（epochDay）
     */
    val nextDueDate: Int,

    /**
     * 最后执行日期（epochDay）
     */
    val lastExecutedDate: Int? = null,

    /**
     * 已执行次数
     */
    val executedCount: Int = 0,

    /**
     * 最大执行次数（null表示无限制）
     */
    val maxOccurrences: Int? = null,

    /**
     * 是否启用
     */
    val isEnabled: Boolean = true,

    /**
     * 是否自动执行（否则仅提醒）
     */
    val autoExecute: Boolean = true,

    /**
     * 提前提醒天数
     */
    val reminderDaysBefore: Int = 0,

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
 * 周期类型枚举
 */
object RecurringFrequency {
    const val DAILY = "DAILY"       // 每日
    const val WEEKLY = "WEEKLY"     // 每周
    const val BIWEEKLY = "BIWEEKLY" // 每两周
    const val MONTHLY = "MONTHLY"   // 每月
    const val QUARTERLY = "QUARTERLY" // 每季度
    const val YEARLY = "YEARLY"     // 每年
}
