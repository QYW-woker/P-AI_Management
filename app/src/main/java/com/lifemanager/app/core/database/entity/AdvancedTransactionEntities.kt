package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 拆分交易实体
 *
 * 用于将一笔交易拆分到多个分类
 * 例如：超市购物100元 = 食品60元 + 日用品40元
 */
@Entity(
    tableName = "split_transactions",
    foreignKeys = [
        ForeignKey(
            entity = DailyTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTransactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["parentTransactionId"]),
        Index(value = ["categoryId"])
    ]
)
data class SplitTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 父交易ID
    val parentTransactionId: Long,

    // 分类ID
    val categoryId: Long?,

    // 金额
    val amount: Double,

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 商家实体
 *
 * 记录常用商家信息，用于智能分类和消费分析
 */
@Entity(
    tableName = "merchants",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["defaultCategoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["name"]),
        Index(value = ["defaultCategoryId"])
    ]
)
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 商家名称
    val name: String,

    // 商家别名/关键词（用于自动匹配，JSON数组）
    val aliases: String = "[]",

    // 默认分类ID
    val defaultCategoryId: Long? = null,

    // 商家类型: RETAIL, RESTAURANT, TRANSPORT, ENTERTAINMENT, UTILITY, OTHER
    val type: String = MerchantType.OTHER,

    // 商家图标（可选，本地路径或URL）
    val icon: String? = null,

    // 商家地址
    val address: String? = null,

    // 商家电话
    val phone: String? = null,

    // 是否常用商家
    val isFavorite: Boolean = false,

    // 交易次数（统计用）
    val transactionCount: Int = 0,

    // 总消费金额（统计用）
    val totalAmount: Double = 0.0,

    // 最后交易日期（epochDay）
    val lastTransactionDate: Int? = null,

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

object MerchantType {
    const val RETAIL = "RETAIL"               // 零售
    const val RESTAURANT = "RESTAURANT"       // 餐饮
    const val TRANSPORT = "TRANSPORT"         // 交通
    const val ENTERTAINMENT = "ENTERTAINMENT" // 娱乐
    const val UTILITY = "UTILITY"             // 水电煤等
    const val SHOPPING = "SHOPPING"           // 网购
    const val SUPERMARKET = "SUPERMARKET"     // 超市
    const val MEDICAL = "MEDICAL"             // 医疗
    const val EDUCATION = "EDUCATION"         // 教育
    const val OTHER = "OTHER"                 // 其他

    fun getDisplayName(type: String): String = when (type) {
        RETAIL -> "零售"
        RESTAURANT -> "餐饮"
        TRANSPORT -> "交通"
        ENTERTAINMENT -> "娱乐"
        UTILITY -> "公共事业"
        SHOPPING -> "网购"
        SUPERMARKET -> "超市"
        MEDICAL -> "医疗"
        EDUCATION -> "教育"
        else -> "其他"
    }

    fun getAllTypes(): List<Pair<String, String>> = listOf(
        RETAIL to "零售",
        RESTAURANT to "餐饮",
        TRANSPORT to "交通",
        ENTERTAINMENT to "娱乐",
        UTILITY to "公共事业",
        SHOPPING to "网购",
        SUPERMARKET to "超市",
        MEDICAL to "医疗",
        EDUCATION to "教育",
        OTHER to "其他"
    )
}

/**
 * 账单实体
 *
 * 用于追踪定期账单（房租、水电费、订阅服务等）
 */
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FundAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["merchantId"]),
        Index(value = ["dueDate"]),
        Index(value = ["status"])
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 账单名称
    val name: String,

    // 账单金额（预期金额）
    val amount: Double,

    // 实际支付金额
    val paidAmount: Double? = null,

    // 分类ID
    val categoryId: Long? = null,

    // 支付账户ID
    val accountId: Long? = null,

    // 商家ID
    val merchantId: Long? = null,

    // 账单日期（epochDay）
    val dueDate: Int,

    // 账单状态: PENDING, PAID, OVERDUE, CANCELLED
    val status: String = BillStatus.PENDING,

    // 支付日期（epochDay）
    val paidDate: Int? = null,

    // 关联的交易记录ID（支付后关联）
    val transactionId: Long? = null,

    // 是否周期性账单
    val isRecurring: Boolean = false,

    // 周期类型: MONTHLY, QUARTERLY, YEARLY
    val recurringType: String? = null,

    // 提醒设置（提前几天提醒）
    val reminderDays: Int = 3,

    // 是否启用提醒
    val reminderEnabled: Boolean = true,

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

object BillStatus {
    const val PENDING = "PENDING"     // 待支付
    const val PAID = "PAID"           // 已支付
    const val OVERDUE = "OVERDUE"     // 已逾期
    const val CANCELLED = "CANCELLED" // 已取消

    fun getDisplayName(status: String): String = when (status) {
        PENDING -> "待支付"
        PAID -> "已支付"
        OVERDUE -> "已逾期"
        CANCELLED -> "已取消"
        else -> "未知"
    }
}

/**
 * 退款记录实体
 *
 * 追踪退款状态，关联原始交易
 */
@Entity(
    tableName = "refunds",
    foreignKeys = [
        ForeignKey(
            entity = DailyTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalTransactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DailyTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["refundTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["originalTransactionId"]),
        Index(value = ["refundTransactionId"]),
        Index(value = ["status"])
    ]
)
data class RefundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 原始交易ID
    val originalTransactionId: Long,

    // 退款交易ID（退款完成后关联到收入记录）
    val refundTransactionId: Long? = null,

    // 退款金额
    val amount: Double,

    // 退款状态: PENDING, PROCESSING, COMPLETED, REJECTED
    val status: String = RefundStatus.PENDING,

    // 退款原因
    val reason: String = "",

    // 申请日期（epochDay）
    val applyDate: Int,

    // 预计退款日期（epochDay）
    val expectedDate: Int? = null,

    // 实际退款日期（epochDay）
    val completedDate: Int? = null,

    // 退款方式: ORIGINAL, BALANCE, BANK_CARD
    val refundMethod: String? = null,

    // 订单号/交易号（用于核对）
    val orderNumber: String? = null,

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

object RefundStatus {
    const val PENDING = "PENDING"       // 待退款
    const val PROCESSING = "PROCESSING" // 退款中
    const val COMPLETED = "COMPLETED"   // 已完成
    const val REJECTED = "REJECTED"     // 已拒绝

    fun getDisplayName(status: String): String = when (status) {
        PENDING -> "待退款"
        PROCESSING -> "退款中"
        COMPLETED -> "已完成"
        REJECTED -> "已拒绝"
        else -> "未知"
    }
}

/**
 * 分期付款计划实体
 *
 * 追踪分期付款（信用卡分期、消费贷等）
 */
@Entity(
    tableName = "installment_plans",
    foreignKeys = [
        ForeignKey(
            entity = DailyTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalTransactionId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FundAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["originalTransactionId"]),
        Index(value = ["accountId"]),
        Index(value = ["status"])
    ]
)
data class InstallmentPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 分期名称
    val name: String,

    // 原始交易ID（可选）
    val originalTransactionId: Long? = null,

    // 支付账户ID（通常是信用卡）
    val accountId: Long? = null,

    // 总金额
    val totalAmount: Double,

    // 总期数
    val totalPeriods: Int,

    // 已还期数
    val paidPeriods: Int = 0,

    // 每期金额
    val periodAmount: Double,

    // 手续费/利息（每期）
    val periodFee: Double = 0.0,

    // 年利率（如有）
    val annualRate: Double? = null,

    // 开始日期（epochDay）
    val startDate: Int,

    // 还款日（每月几号）
    val repaymentDay: Int,

    // 状态: ACTIVE, COMPLETED, CANCELLED
    val status: String = InstallmentStatus.ACTIVE,

    // 提前还款日期（如有）
    val earlyRepaymentDate: Int? = null,

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
) {
    // 计算剩余金额
    fun getRemainingAmount(): Double = (totalPeriods - paidPeriods) * (periodAmount + periodFee)

    // 计算总手续费
    fun getTotalFee(): Double = totalPeriods * periodFee

    // 计算下一期还款日期
    fun getNextPaymentDate(currentEpochDay: Int): Int? {
        if (status != InstallmentStatus.ACTIVE) return null
        val monthsElapsed = paidPeriods
        // 简化计算：从开始日期加上已还期数个月
        return startDate + (monthsElapsed * 30) // 近似值
    }
}

object InstallmentStatus {
    const val ACTIVE = "ACTIVE"         // 进行中
    const val COMPLETED = "COMPLETED"   // 已结清
    const val CANCELLED = "CANCELLED"   // 已取消

    fun getDisplayName(status: String): String = when (status) {
        ACTIVE -> "进行中"
        COMPLETED -> "已结清"
        CANCELLED -> "已取消"
        else -> "未知"
    }
}

/**
 * 分期付款记录实体
 *
 * 每一期的还款记录
 */
@Entity(
    tableName = "installment_payments",
    foreignKeys = [
        ForeignKey(
            entity = InstallmentPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DailyTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["transactionId"]),
        Index(value = ["dueDate"])
    ]
)
data class InstallmentPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 分期计划ID
    val planId: Long,

    // 期数（第几期）
    val periodNumber: Int,

    // 本期应还金额
    val amount: Double,

    // 本期手续费
    val fee: Double = 0.0,

    // 应还日期（epochDay）
    val dueDate: Int,

    // 实际还款日期（epochDay）
    val paidDate: Int? = null,

    // 状态: PENDING, PAID, OVERDUE
    val status: String = InstallmentPaymentStatus.PENDING,

    // 关联的交易记录ID
    val transactionId: Long? = null,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)

object InstallmentPaymentStatus {
    const val PENDING = "PENDING"   // 待还款
    const val PAID = "PAID"         // 已还款
    const val OVERDUE = "OVERDUE"   // 已逾期

    fun getDisplayName(status: String): String = when (status) {
        PENDING -> "待还款"
        PAID -> "已还款"
        OVERDUE -> "已逾期"
        else -> "未知"
    }
}

/**
 * 交易模板实体
 *
 * 快速记账模板，支持拆分
 */
@Entity(
    tableName = "transaction_templates",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FundAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["merchantId"])
    ]
)
data class TransactionTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 模板名称
    val name: String,

    // 交易类型: INCOME, EXPENSE
    val type: String,

    // 默认金额（可选）
    val amount: Double? = null,

    // 分类ID
    val categoryId: Long? = null,

    // 账户ID
    val accountId: Long? = null,

    // 商家ID
    val merchantId: Long? = null,

    // 默认备注
    val note: String = "",

    // 标签（JSON数组）
    val tags: String = "[]",

    // 是否包含拆分（JSON数组格式的拆分项）
    val splitItems: String = "[]",

    // 模板图标
    val icon: String = "Receipt",

    // 模板颜色
    val color: String = "#4CAF50",

    // 使用次数
    val usageCount: Int = 0,

    // 最后使用时间
    val lastUsedAt: Long? = null,

    // 是否启用
    val isEnabled: Boolean = true,

    // 排序顺序
    val sortOrder: Int = 0,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 高级搜索过滤条件
 */
data class TransactionFilter(
    // 日期范围
    val startDate: Int? = null,
    val endDate: Int? = null,

    // 金额范围
    val minAmount: Double? = null,
    val maxAmount: Double? = null,

    // 交易类型
    val types: List<String>? = null,

    // 分类ID列表
    val categoryIds: List<Long>? = null,

    // 账户ID列表
    val accountIds: List<Long>? = null,

    // 商家ID
    val merchantId: Long? = null,

    // 关键词搜索
    val keyword: String? = null,

    // 标签
    val tags: List<String>? = null,

    // 录入来源
    val sources: List<String>? = null,

    // 是否有附件
    val hasAttachments: Boolean? = null,

    // 是否有退款
    val hasRefund: Boolean? = null,

    // 排序方式
    val sortBy: TransactionSortBy = TransactionSortBy.DATE_DESC
)

enum class TransactionSortBy {
    DATE_DESC,      // 日期降序（最新在前）
    DATE_ASC,       // 日期升序
    AMOUNT_DESC,    // 金额降序
    AMOUNT_ASC,     // 金额升序
    CATEGORY,       // 按分类
    CREATED_DESC    // 创建时间降序
}

/**
 * 搜索预设
 */
@Entity(tableName = "search_presets")
data class SearchPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 预设名称
    val name: String,

    // 过滤条件（JSON格式）
    val filterJson: String,

    // 使用次数
    val usageCount: Int = 0,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)
