package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 待办事项实体类
 *
 * 支持重要紧急四象限分类
 * 支持设置提醒、重复规则
 * 可关联目标进行任务管理
 * 支持子任务层级
 */
@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedGoalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["status"]),
        Index(value = ["linkedGoalId"]),
        Index(value = ["parentId"]),
        // 复合索引优化常用查询
        Index(value = ["status", "dueDate"]),
        Index(value = ["status", "quadrant"]),
        Index(value = ["status", "priority"])
    ]
)
data class TodoEntity(
    // 主键ID
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 标题
    val title: String,

    // 详细描述
    val description: String = "",

    // 优先级: HIGH, MEDIUM, LOW, NONE
    val priority: String = "NONE",

    // 重要紧急象限
    // IMPORTANT_URGENT: 重要且紧急
    // IMPORTANT_NOT_URGENT: 重要不紧急
    // NOT_IMPORTANT_URGENT: 不重要但紧急
    // NOT_IMPORTANT_NOT_URGENT: 不重要不紧急
    val quadrant: String? = null,

    // 截止日期，epochDay格式
    val dueDate: Int? = null,

    // 开始时间，HH:mm格式（用于有时间段的事件）
    val startTime: String? = null,

    // 结束时间，HH:mm格式（用于有时间段的事件）
    val endTime: String? = null,

    // 截止时间，HH:mm格式（向后兼容，用于简单的截止时间）
    val dueTime: String? = null,

    // 是否全天事件
    val isAllDay: Boolean = true,

    // 地点
    val location: String? = null,

    // 提醒时间戳
    val reminderAt: Long? = null,

    // 提前提醒分钟数（0=准时，5=提前5分钟，等）
    val reminderMinutesBefore: Int = 0,

    // 重复规则
    // NONE: 不重复
    // DAILY: 每天
    // WEEKLY: 每周
    // MONTHLY: 每月
    // CUSTOM: 自定义
    val repeatRule: String = "NONE",

    // 自定义重复规则，JSON格式
    // 如: {"weekdays": [1,3,5]} 表示每周一三五
    val customRepeatRule: String? = null,

    // 关联目标ID
    val linkedGoalId: Long? = null,

    // 父任务ID（用于子任务）
    val parentId: Long? = null,

    // 日历事件ID（用于同步系统日历）
    val calendarEventId: Long? = null,

    // 是否已同步到日历
    val isSyncedToCalendar: Boolean = false,

    // 状态: PENDING(待完成), COMPLETED(已完成), CANCELLED(已取消)
    val status: String = "PENDING",

    // 完成时间
    val completedAt: Long? = null,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 优先级枚举
 */
object Priority {
    const val HIGH = "HIGH"
    const val MEDIUM = "MEDIUM"
    const val LOW = "LOW"
    const val NONE = "NONE"
}

/**
 * 四象限枚举
 */
object Quadrant {
    const val IMPORTANT_URGENT = "IMPORTANT_URGENT"                     // 重要且紧急
    const val IMPORTANT_NOT_URGENT = "IMPORTANT_NOT_URGENT"             // 重要不紧急
    const val NOT_IMPORTANT_URGENT = "NOT_IMPORTANT_URGENT"             // 不重要但紧急
    const val NOT_IMPORTANT_NOT_URGENT = "NOT_IMPORTANT_NOT_URGENT"     // 不重要不紧急
}

/**
 * 重复规则枚举
 */
object RepeatRule {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val CUSTOM = "CUSTOM"
}

/**
 * 待办状态枚举
 */
object TodoStatus {
    const val PENDING = "PENDING"       // 待完成
    const val COMPLETED = "COMPLETED"   // 已完成
    const val CANCELLED = "CANCELLED"   // 已取消
}
