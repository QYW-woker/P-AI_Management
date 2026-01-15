package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 月度定投记录实体类
 *
 * 用于记录每月的定投计划和实际投入
 * 每条记录关联一个自定义字段（定投类型）
 * 支持预算和实际金额对比
 */
@Entity(
    tableName = "monthly_investments",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["yearMonth", "fieldId"]),
        Index(value = ["yearMonth"]),
        Index(value = ["fieldId"])
    ]
)
data class MonthlyInvestmentEntity(
    // 主键ID
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 年月，格式为YYYYMM，如202412表示2024年12月
    val yearMonth: Int,

    // 关联的自定义字段ID（定投类型）
    val fieldId: Long?,

    // 预算金额，单位为元
    val budgetAmount: Double = 0.0,

    // 实际投入金额，单位为元
    val actualAmount: Double = 0.0,

    // 记录日期，epochDay格式
    val recordDate: Int,

    // 备注说明
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)
