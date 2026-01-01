package com.lifemanager.app.core.ai.model

/**
 * 命令意图 - 语音/文本解析后的结构化意图
 */
sealed class CommandIntent {

    /**
     * 记账意图
     */
    data class Transaction(
        val type: TransactionType,
        val amount: Double?,
        val categoryName: String? = null,
        val categoryId: Long? = null,
        val date: Int? = null,           // epochDay
        val time: String? = null,
        val note: String? = null,
        val payee: String? = null        // 商家/收款方
    ) : CommandIntent()

    /**
     * 待办意图
     */
    data class Todo(
        val title: String,
        val description: String? = null,
        val dueDate: Int? = null,        // epochDay
        val startTime: String? = null,   // HH:mm格式，事件开始时间
        val endTime: String? = null,     // HH:mm格式，事件结束时间
        val dueTime: String? = null,     // HH:mm格式，截止时间（向后兼容）
        val isAllDay: Boolean = true,    // 是否全天事件
        val location: String? = null,    // 地点
        val priority: String? = null,
        val quadrant: String? = null,    // 四象限：IMPORTANT_URGENT, IMPORTANT_NOT_URGENT, NOT_IMPORTANT_URGENT, NOT_IMPORTANT_NOT_URGENT
        val reminderAt: Long? = null,
        val reminderMinutesBefore: Int = 0  // 提前提醒分钟数
    ) : CommandIntent()

    /**
     * 日记意图
     */
    data class Diary(
        val content: String,
        val mood: Int? = null,           // 1-5
        val weather: String? = null
    ) : CommandIntent()

    /**
     * 习惯打卡意图
     */
    data class HabitCheckin(
        val habitName: String,
        val value: Double? = null        // 数值型习惯的值
    ) : CommandIntent()

    /**
     * 时间追踪意图
     */
    data class TimeTrack(
        val action: TimeTrackAction,
        val categoryName: String? = null,
        val note: String? = null
    ) : CommandIntent()

    /**
     * 导航意图
     */
    data class Navigate(
        val screen: String
    ) : CommandIntent()

    /**
     * 查询意图
     */
    data class Query(
        val type: QueryType,
        val params: Map<String, Any> = emptyMap()
    ) : CommandIntent()

    /**
     * 目标相关意图
     */
    data class Goal(
        val action: GoalAction,
        val goalName: String? = null,
        val progress: Double? = null
    ) : CommandIntent()

    /**
     * 存钱计划意图
     */
    data class Savings(
        val action: SavingsAction,
        val planName: String? = null,
        val amount: Double? = null
    ) : CommandIntent()

    /**
     * 多条记录意图（支持一次输入多条记录）
     */
    data class Multiple(
        val intents: List<CommandIntent>
    ) : CommandIntent()

    /**
     * 无法识别
     */
    data class Unknown(
        val originalText: String,
        val suggestion: String? = null
    ) : CommandIntent()
}

/**
 * 交易类型
 */
enum class TransactionType {
    INCOME,     // 收入
    EXPENSE     // 支出
}

/**
 * 时间追踪动作
 */
enum class TimeTrackAction {
    START,      // 开始计时
    STOP,       // 停止计时
    PAUSE,      // 暂停
    RESUME      // 继续
}

/**
 * 查询类型
 */
enum class QueryType {
    TODAY_EXPENSE,          // 今日支出
    MONTH_EXPENSE,          // 本月支出
    MONTH_INCOME,           // 本月收入
    CATEGORY_EXPENSE,       // 分类支出
    HABIT_STREAK,           // 习惯连续天数
    GOAL_PROGRESS,          // 目标进度
    SAVINGS_PROGRESS        // 存钱进度
}

/**
 * 目标动作
 */
enum class GoalAction {
    CREATE,                 // 创建目标
    UPDATE,                 // 更新进度
    CHECK,                  // 查看状态
    DEPOSIT                 // 目标存款
}

/**
 * 存钱动作
 */
enum class SavingsAction {
    DEPOSIT,                // 存入
    WITHDRAW,               // 取出
    CHECK                   // 查看进度
}
