package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 月度资产/负债记录实体类
 *
 * 用于记录每月的资产和负债状况
 * 包括存款、股票、基金、房产等资产，以及房贷、车贷等负债
 * 每月记录一次，用于追踪净资产变化
 */
@Entity(
    tableName = "monthly_assets",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
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
        Index(value = ["yearMonth", "fieldId"]),
        Index(value = ["yearMonth"]),
        Index(value = ["fieldId"]),
        Index(value = ["type"]),
        Index(value = ["accountId"])
    ]
)
data class MonthlyAssetEntity(
    // 主键ID
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 年月，格式YYYYMM
    val yearMonth: Int,

    // 类型: ASSET(资产) 或 LIABILITY(负债)
    val type: String,

    // 关联的自定义字段ID（资产/负债分类）
    val fieldId: Long?,

    // 关联的资金账户ID（可选，用于关联银行卡、信用卡等）
    val accountId: Long? = null,

    // 金额
    val amount: Double,

    // 负债名称（仅负债使用，如"房贷"、"车贷"）
    val liabilityName: String? = null,

    // 利率（仅负债使用，年化利率百分比）
    val interestRate: Double? = null,

    // 到期日（仅负债使用，epochDay格式）
    val dueDate: Int? = null,

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 资产类型枚举
 */
object AssetType {
    const val ASSET = "ASSET"           // 资产
    const val LIABILITY = "LIABILITY"   // 负债
}
