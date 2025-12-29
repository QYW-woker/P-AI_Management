package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.TodoEntity

/**
 * 待办UI状态
 */
sealed class TodoUiState {
    object Loading : TodoUiState()
    object Success : TodoUiState()
    data class Error(val message: String) : TodoUiState()
}

/**
 * 待办分组（按日期）
 */
data class TodoGroup(
    val title: String,
    val todos: List<TodoEntity>,
    val isExpanded: Boolean = true
)

/**
 * 四象限视图数据
 */
data class QuadrantData(
    val importantUrgent: List<TodoEntity>,
    val importantNotUrgent: List<TodoEntity>,
    val notImportantUrgent: List<TodoEntity>,
    val notImportantNotUrgent: List<TodoEntity>
)

/**
 * 待办编辑状态
 */
data class TodoEditState(
    val id: Long = 0,
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val priority: String = "NONE",
    val quadrant: String? = null,
    val dueDate: Int? = null,
    val dueTime: String? = null,
    val reminderAt: Long? = null,
    val repeatRule: String = "NONE",
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 待办统计数据
 */
data class TodoStatistics(
    val totalPending: Int = 0,
    val todayTotal: Int = 0,
    val todayCompleted: Int = 0,
    val overdueCount: Int = 0,
    val completedToday: Int = 0
)

/**
 * 筛选器类型
 */
enum class TodoFilter {
    ALL,        // 全部
    TODAY,      // 今日
    UPCOMING,   // 未来/计划
    OVERDUE,    // 逾期
    COMPLETED   // 已完成
}

/**
 * 排序方式
 */
enum class TodoSortBy {
    PRIORITY,   // 按优先级
    DUE_DATE,   // 按截止日期
    CREATED     // 按创建时间
}

/**
 * 快捷待办模板
 */
data class QuickTodoTemplate(
    val id: Long = 0,
    val title: String,
    val priority: String = "NONE",
    val quadrant: String? = null
)

/**
 * 优先级显示信息
 */
data class PriorityInfo(
    val code: String,
    val name: String,
    val color: Long
)

/**
 * 预定义优先级列表
 */
val priorityList = listOf(
    PriorityInfo("HIGH", "高优先级", 0xFFF44336),
    PriorityInfo("MEDIUM", "中优先级", 0xFFFF9800),
    PriorityInfo("LOW", "低优先级", 0xFF4CAF50),
    PriorityInfo("NONE", "无优先级", 0xFF9E9E9E)
)

/**
 * 四象限显示信息
 */
data class QuadrantInfo(
    val code: String,
    val name: String,
    val color: Long,
    val description: String
)

/**
 * 预定义四象限列表
 */
val quadrantList = listOf(
    QuadrantInfo("IMPORTANT_URGENT", "重要且紧急", 0xFFF44336, "立即处理"),
    QuadrantInfo("IMPORTANT_NOT_URGENT", "重要不紧急", 0xFF2196F3, "计划安排"),
    QuadrantInfo("NOT_IMPORTANT_URGENT", "不重要但紧急", 0xFFFF9800, "委托他人"),
    QuadrantInfo("NOT_IMPORTANT_NOT_URGENT", "不重要不紧急", 0xFF9E9E9E, "考虑放弃")
)
