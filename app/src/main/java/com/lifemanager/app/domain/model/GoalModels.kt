package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.GoalEntity

/**
 * 目标UI状态
 */
sealed class GoalUiState {
    object Loading : GoalUiState()
    data class Success(val goals: List<GoalEntity> = emptyList()) : GoalUiState()
    data class Error(val message: String) : GoalUiState()
}

/**
 * 目标编辑状态
 */
data class GoalEditState(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val goalType: String = "YEARLY",
    val category: String = "CAREER",
    val startDate: Int = 0,
    val endDate: Int? = null,
    val progressType: String = "PERCENTAGE",
    val targetValue: Double? = null,
    val currentValue: Double = 0.0,
    val unit: String = "",
    val priority: Int = 2, // 1-高, 2-中, 3-低
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 目标统计数据
 */
data class GoalStatistics(
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val totalProgress: Float = 0f
)

/**
 * 目标及其子目标的完整结构
 */
data class GoalWithChildren(
    val goal: GoalEntity,
    val children: List<GoalEntity> = emptyList(),
    val childCount: Int = 0,
    val completedChildCount: Int = 0,
    val childProgress: Float = 0f  // 子目标完成进度 0-1
) {
    /**
     * 是否有子目标
     */
    fun hasChildren(): Boolean = childCount > 0

    /**
     * 是否所有子目标都已完成
     */
    fun allChildrenCompleted(): Boolean = childCount > 0 && completedChildCount >= childCount

    /**
     * 获取子目标进度百分比文本
     */
    fun getChildProgressText(): String {
        return if (childCount > 0) {
            "$completedChildCount/$childCount"
        } else {
            ""
        }
    }
}

/**
 * 子目标编辑状态
 */
data class SubGoalEditState(
    val parentId: Long = 0,
    val title: String = "",
    val description: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * 目标模板
 */
data class GoalTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val goalType: String,
    val icon: String,
    val color: String,
    val suggestedDuration: Int, // 建议天数
    val progressType: String = "PERCENTAGE",
    val targetValue: Double? = null,
    val unit: String = "",
    val suggestedMilestones: List<String> = emptyList()
)

/**
 * 目标进度历史记录
 */
data class GoalProgressRecord(
    val id: Long = 0,
    val goalId: Long,
    val previousValue: Double,
    val newValue: Double,
    val recordedAt: Long,
    val note: String = ""
) {
    val change: Double get() = newValue - previousValue
    val isIncrease: Boolean get() = change > 0
}

/**
 * 目标时间线数据
 */
data class GoalTimelineData(
    val goalId: Long,
    val goalTitle: String,
    val startDate: Int,
    val endDate: Int?,
    val progressRecords: List<GoalProgressRecord>,
    val currentProgress: Float,
    val daysElapsed: Int,
    val daysRemaining: Int?,
    val expectedProgress: Float, // 基于时间的预期进度
    val isOnTrack: Boolean // 是否按计划进行
)

/**
 * 目标洞察分析
 */
data class GoalInsights(
    val totalGoals: Int = 0,
    val activeGoals: Int = 0,
    val completedGoals: Int = 0,
    val abandonedGoals: Int = 0,
    val completionRate: Float = 0f, // 完成率
    val averageCompletionDays: Int = 0, // 平均完成天数
    val mostActiveCategory: String? = null, // 最活跃分类
    val categoryStats: List<CategoryGoalStats> = emptyList(),
    val monthlyStats: List<MonthlyGoalStats> = emptyList(),
    val upcomingDeadlines: List<GoalEntity> = emptyList(), // 即将到期
    val overdueGoals: List<GoalEntity> = emptyList(), // 已逾期
    val streakData: GoalStreakData? = null
)

/**
 * 分类目标统计
 */
data class CategoryGoalStats(
    val category: String,
    val categoryName: String,
    val totalCount: Int,
    val completedCount: Int,
    val activeCount: Int,
    val completionRate: Float,
    val color: String
)

/**
 * 月度目标统计
 */
data class MonthlyGoalStats(
    val yearMonth: Int, // 格式：202401
    val monthLabel: String,
    val createdCount: Int, // 新建数量
    val completedCount: Int, // 完成数量
    val abandonedCount: Int // 放弃数量
)

/**
 * 目标连续完成数据
 */
data class GoalStreakData(
    val currentStreak: Int = 0, // 当前连续完成天数（至少完成一个目标的天数）
    val longestStreak: Int = 0, // 最长连续
    val totalCompletionDays: Int = 0, // 总共有目标完成的天数
    val lastCompletionDate: Int? = null
)

/**
 * 目标优先级
 */
object GoalPriority {
    const val HIGH = 1
    const val MEDIUM = 2
    const val LOW = 3

    fun getDisplayName(priority: Int): String = when (priority) {
        HIGH -> "高优先级"
        MEDIUM -> "中优先级"
        LOW -> "低优先级"
        else -> "中优先级"
    }

    fun getColor(priority: Int): String = when (priority) {
        HIGH -> "#EF4444"
        MEDIUM -> "#F59E0B"
        LOW -> "#10B981"
        else -> "#F59E0B"
    }
}

/**
 * 里程碑数据
 */
data class GoalMilestone(
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val description: String = "",
    val targetDate: Int? = null,
    val targetValue: Double? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val order: Int = 0
)

/**
 * 目标回顾数据
 */
data class GoalReviewData(
    val period: String, // "WEEKLY", "MONTHLY", "YEARLY"
    val startDate: Int,
    val endDate: Int,
    val goalsCreated: Int,
    val goalsCompleted: Int,
    val goalsAbandoned: Int,
    val totalProgressMade: Float, // 总体进度提升
    val topAchievements: List<GoalEntity>, // 完成的重要目标
    val insights: String // AI生成的洞察
)

/**
 * 目标类型选项
 */
val goalTypeOptions = listOf(
    "YEARLY" to "年度目标",
    "QUARTERLY" to "季度目标",
    "MONTHLY" to "月度目标",
    "LONG_TERM" to "长期目标",
    "CUSTOM" to "自定义"
)

/**
 * 目标分类选项
 */
val goalCategoryOptions = listOf(
    "CAREER" to "事业",
    "FINANCE" to "财务",
    "HEALTH" to "健康",
    "LEARNING" to "学习",
    "RELATIONSHIP" to "人际关系",
    "LIFESTYLE" to "生活方式",
    "HOBBY" to "兴趣爱好"
)

/**
 * 获取目标分类显示名称
 */
fun getCategoryDisplayName(category: String): String {
    return goalCategoryOptions.find { it.first == category }?.second ?: category
}

/**
 * 获取目标类型显示名称
 */
fun getGoalTypeDisplayName(type: String): String {
    return goalTypeOptions.find { it.first == type }?.second ?: type
}

/**
 * 预定义目标模板
 */
val goalTemplates = listOf(
    // 健康类
    GoalTemplate(
        id = "health_weight",
        name = "减重目标",
        description = "设定减重目标，记录体重变化",
        category = "HEALTH",
        goalType = "MONTHLY",
        icon = "fitness_center",
        color = "#EC4899",
        suggestedDuration = 90,
        progressType = "NUMERIC",
        targetValue = 5.0,
        unit = "公斤",
        suggestedMilestones = listOf("第一周适应期", "中期检查点", "最终目标达成")
    ),
    GoalTemplate(
        id = "health_exercise",
        name = "运动打卡",
        description = "每周运动次数目标",
        category = "HEALTH",
        goalType = "MONTHLY",
        icon = "directions_run",
        color = "#EC4899",
        suggestedDuration = 30,
        progressType = "NUMERIC",
        targetValue = 12.0,
        unit = "次",
        suggestedMilestones = listOf("第一周3次", "第二周3次", "第三周3次", "第四周3次")
    ),
    GoalTemplate(
        id = "health_sleep",
        name = "早睡早起",
        description = "养成规律作息习惯",
        category = "HEALTH",
        goalType = "MONTHLY",
        icon = "bedtime",
        color = "#EC4899",
        suggestedDuration = 21,
        progressType = "PERCENTAGE"
    ),

    // 学习类
    GoalTemplate(
        id = "learning_reading",
        name = "年度阅读",
        description = "设定年度阅读目标",
        category = "LEARNING",
        goalType = "YEARLY",
        icon = "menu_book",
        color = "#F59E0B",
        suggestedDuration = 365,
        progressType = "NUMERIC",
        targetValue = 24.0,
        unit = "本",
        suggestedMilestones = listOf("Q1 6本", "Q2 6本", "Q3 6本", "Q4 6本")
    ),
    GoalTemplate(
        id = "learning_skill",
        name = "学习新技能",
        description = "掌握一门新技能",
        category = "LEARNING",
        goalType = "QUARTERLY",
        icon = "school",
        color = "#F59E0B",
        suggestedDuration = 90,
        progressType = "PERCENTAGE",
        suggestedMilestones = listOf("基础入门", "进阶练习", "实战项目", "技能精通")
    ),
    GoalTemplate(
        id = "learning_course",
        name = "完成课程",
        description = "完成在线课程学习",
        category = "LEARNING",
        goalType = "MONTHLY",
        icon = "play_circle",
        color = "#F59E0B",
        suggestedDuration = 30,
        progressType = "NUMERIC",
        targetValue = 20.0,
        unit = "课时"
    ),

    // 财务类
    GoalTemplate(
        id = "finance_saving",
        name = "储蓄目标",
        description = "设定储蓄金额目标",
        category = "FINANCE",
        goalType = "YEARLY",
        icon = "savings",
        color = "#10B981",
        suggestedDuration = 365,
        progressType = "NUMERIC",
        targetValue = 50000.0,
        unit = "元",
        suggestedMilestones = listOf("每月存款4000+", "半年检查点25000", "年终目标达成")
    ),
    GoalTemplate(
        id = "finance_income",
        name = "收入增长",
        description = "提升收入水平",
        category = "FINANCE",
        goalType = "YEARLY",
        icon = "trending_up",
        color = "#10B981",
        suggestedDuration = 365,
        progressType = "PERCENTAGE"
    ),
    GoalTemplate(
        id = "finance_investment",
        name = "投资理财",
        description = "建立投资组合",
        category = "FINANCE",
        goalType = "LONG_TERM",
        icon = "account_balance",
        color = "#10B981",
        suggestedDuration = 365,
        progressType = "NUMERIC",
        targetValue = 100000.0,
        unit = "元"
    ),

    // 事业类
    GoalTemplate(
        id = "career_promotion",
        name = "职位晋升",
        description = "争取职位晋升机会",
        category = "CAREER",
        goalType = "YEARLY",
        icon = "work",
        color = "#3B82F6",
        suggestedDuration = 365,
        progressType = "PERCENTAGE",
        suggestedMilestones = listOf("能力提升", "项目成果", "领导认可", "晋升成功")
    ),
    GoalTemplate(
        id = "career_project",
        name = "项目完成",
        description = "完成重要项目",
        category = "CAREER",
        goalType = "QUARTERLY",
        icon = "assignment",
        color = "#3B82F6",
        suggestedDuration = 90,
        progressType = "PERCENTAGE",
        suggestedMilestones = listOf("需求分析", "开发实现", "测试验收", "上线交付")
    ),
    GoalTemplate(
        id = "career_certification",
        name = "考取证书",
        description = "获得专业认证证书",
        category = "CAREER",
        goalType = "QUARTERLY",
        icon = "verified",
        color = "#3B82F6",
        suggestedDuration = 90,
        progressType = "PERCENTAGE",
        suggestedMilestones = listOf("学习备考", "模拟练习", "正式考试")
    ),

    // 人际关系类
    GoalTemplate(
        id = "relationship_family",
        name = "家庭时光",
        description = "增加与家人相处时间",
        category = "RELATIONSHIP",
        goalType = "MONTHLY",
        icon = "family_restroom",
        color = "#8B5CF6",
        suggestedDuration = 30,
        progressType = "NUMERIC",
        targetValue = 8.0,
        unit = "次"
    ),
    GoalTemplate(
        id = "relationship_friends",
        name = "朋友聚会",
        description = "维护朋友关系",
        category = "RELATIONSHIP",
        goalType = "MONTHLY",
        icon = "people",
        color = "#8B5CF6",
        suggestedDuration = 30,
        progressType = "NUMERIC",
        targetValue = 4.0,
        unit = "次"
    ),

    // 生活方式类
    GoalTemplate(
        id = "lifestyle_travel",
        name = "旅行计划",
        description = "完成旅行目标",
        category = "LIFESTYLE",
        goalType = "YEARLY",
        icon = "flight",
        color = "#06B6D4",
        suggestedDuration = 365,
        progressType = "NUMERIC",
        targetValue = 3.0,
        unit = "次"
    ),
    GoalTemplate(
        id = "lifestyle_declutter",
        name = "整理收纳",
        description = "整理生活空间",
        category = "LIFESTYLE",
        goalType = "MONTHLY",
        icon = "home",
        color = "#06B6D4",
        suggestedDuration = 30,
        progressType = "PERCENTAGE"
    ),

    // 兴趣爱好类
    GoalTemplate(
        id = "hobby_creative",
        name = "创作目标",
        description = "完成创意作品",
        category = "HOBBY",
        goalType = "QUARTERLY",
        icon = "palette",
        color = "#EF4444",
        suggestedDuration = 90,
        progressType = "NUMERIC",
        targetValue = 10.0,
        unit = "件"
    ),
    GoalTemplate(
        id = "hobby_music",
        name = "音乐练习",
        description = "学习乐器或唱歌",
        category = "HOBBY",
        goalType = "MONTHLY",
        icon = "music_note",
        color = "#EF4444",
        suggestedDuration = 30,
        progressType = "NUMERIC",
        targetValue = 20.0,
        unit = "小时"
    )
)

/**
 * 根据分类获取模板
 */
fun getTemplatesByCategory(category: String): List<GoalTemplate> {
    return goalTemplates.filter { it.category == category }
}

/**
 * 根据ID获取模板
 */
fun getTemplateById(id: String): GoalTemplate? {
    return goalTemplates.find { it.id == id }
}
