package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.GoalCategory
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.core.database.entity.ProgressType
import com.lifemanager.app.domain.model.CategoryGoalStats
import com.lifemanager.app.domain.model.GoalInsights
import com.lifemanager.app.domain.model.GoalStatistics
import com.lifemanager.app.domain.model.GoalStreakData
import com.lifemanager.app.domain.model.GoalTemplate
import com.lifemanager.app.domain.model.GoalTimelineData
import com.lifemanager.app.domain.model.GoalWithChildren
import com.lifemanager.app.domain.model.MonthlyGoalStats
import com.lifemanager.app.domain.model.getCategoryDisplayName
import com.lifemanager.app.domain.model.goalCategoryOptions
import com.lifemanager.app.domain.model.goalTemplates
import com.lifemanager.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 目标用例
 */
class GoalUseCase @Inject constructor(
    private val repository: GoalRepository
) {

    /**
     * 获取活跃目标
     */
    fun getActiveGoals(): Flow<List<GoalEntity>> {
        return repository.getActiveGoals()
    }

    /**
     * 获取所有目标
     */
    fun getAllGoals(): Flow<List<GoalEntity>> {
        return repository.getAllGoals()
    }

    /**
     * 获取顶级目标（用于层级显示）
     */
    fun getTopLevelGoals(): Flow<List<GoalEntity>> {
        return repository.getTopLevelGoals()
    }

    /**
     * 获取子目标
     */
    fun getChildGoals(parentId: Long): Flow<List<GoalEntity>> {
        return repository.getChildGoals(parentId)
    }

    /**
     * 获取目标及其子目标的完整结构
     */
    suspend fun getGoalWithChildren(goalId: Long): GoalWithChildren? {
        val goal = repository.getGoalById(goalId) ?: return null
        val children = repository.getChildGoalsSync(goalId)
        val childCount = children.size
        val completedChildCount = children.count { it.status == GoalStatus.COMPLETED }

        return GoalWithChildren(
            goal = goal,
            children = children,
            childCount = childCount,
            completedChildCount = completedChildCount,
            childProgress = if (childCount > 0) completedChildCount.toFloat() / childCount else 0f
        )
    }

    /**
     * 根据类型获取目标
     */
    fun getGoalsByType(type: String): Flow<List<GoalEntity>> {
        return repository.getGoalsByType(type)
    }

    /**
     * 根据分类获取目标
     */
    fun getGoalsByCategory(category: String): Flow<List<GoalEntity>> {
        return repository.getGoalsByCategory(category)
    }

    /**
     * 获取目标详情
     */
    suspend fun getGoalById(id: Long): GoalEntity? {
        return repository.getGoalById(id)
    }

    /**
     * 创建目标
     */
    suspend fun createGoal(
        title: String,
        description: String,
        goalType: String,
        category: String,
        startDate: Int,
        endDate: Int?,
        progressType: String,
        targetValue: Double?,
        unit: String,
        parentId: Long? = null
    ): Long {
        // 如果有父目标，计算层级
        val level = if (parentId != null) {
            val parentGoal = repository.getGoalById(parentId)
            (parentGoal?.level ?: 0) + 1
        } else 0

        val goal = GoalEntity(
            title = title,
            description = description,
            goalType = goalType,
            category = category,
            startDate = startDate,
            endDate = endDate,
            progressType = progressType,
            targetValue = targetValue,
            unit = unit,
            parentId = parentId,
            level = level
        )
        return repository.insert(goal)
    }

    /**
     * 创建子目标
     */
    suspend fun createSubGoal(
        parentId: Long,
        title: String,
        description: String = ""
    ): Long {
        val parentGoal = repository.getGoalById(parentId)
            ?: throw IllegalArgumentException("Parent goal not found")

        val subGoal = GoalEntity(
            title = title,
            description = description,
            goalType = parentGoal.goalType,
            category = parentGoal.category,
            startDate = parentGoal.startDate,
            endDate = parentGoal.endDate,
            progressType = ProgressType.PERCENTAGE,  // 子目标默认使用百分比进度
            parentId = parentId,
            level = parentGoal.level + 1
        )

        val subGoalId = repository.insert(subGoal)

        // 如果父目标还没有子目标，将其进度类型设置为基于子目标
        // 父目标的进度将由子目标完成情况自动计算
        return subGoalId
    }

    /**
     * 更新目标
     */
    suspend fun updateGoal(goal: GoalEntity) {
        repository.update(goal.copy(updatedAt = System.currentTimeMillis()))

        // 如果有父目标，更新父目标进度
        goal.parentId?.let { parentId ->
            updateParentGoalProgress(parentId)
        }
    }

    /**
     * 更新目标进度
     */
    suspend fun updateProgress(id: Long, value: Double) {
        repository.updateProgress(id, value)

        // 检查是否达成目标
        val goal = repository.getGoalById(id)
        if (goal != null && goal.targetValue != null && value >= goal.targetValue) {
            repository.updateStatus(id, GoalStatus.COMPLETED)

            // 如果有父目标，更新父目标进度
            goal.parentId?.let { parentId ->
                updateParentGoalProgress(parentId)
            }
        }
    }

    /**
     * 完成目标
     */
    suspend fun completeGoal(id: Long) {
        repository.updateStatus(id, GoalStatus.COMPLETED)

        // 如果有父目标，更新父目标进度
        val goal = repository.getGoalById(id)
        goal?.parentId?.let { parentId ->
            updateParentGoalProgress(parentId)
        }
    }

    /**
     * 更新父目标进度（基于子目标完成情况）
     * 当子目标状态变化时调用此方法
     */
    suspend fun updateParentGoalProgress(parentId: Long) {
        val childCount = repository.countChildGoals(parentId)
        if (childCount == 0) return

        val completedCount = repository.countCompletedChildGoals(parentId)
        val progressPercentage = (completedCount.toDouble() / childCount) * 100

        // 更新父目标进度
        repository.updateProgress(parentId, progressPercentage)

        // 如果所有子目标都完成了，自动完成父目标
        if (completedCount >= childCount) {
            repository.updateStatus(parentId, GoalStatus.COMPLETED)

            // 递归检查更上层的父目标
            val parentGoal = repository.getGoalById(parentId)
            parentGoal?.parentId?.let { grandParentId ->
                updateParentGoalProgress(grandParentId)
            }
        }
    }

    /**
     * 获取子目标数量
     */
    suspend fun getChildCount(goalId: Long): Int {
        return repository.countChildGoals(goalId)
    }

    /**
     * 获取已完成的子目标数量
     */
    suspend fun getCompletedChildCount(goalId: Long): Int {
        return repository.countCompletedChildGoals(goalId)
    }

    /**
     * 检查目标是否有子目标
     */
    suspend fun hasChildren(goalId: Long): Boolean {
        return repository.countChildGoals(goalId) > 0
    }

    /**
     * 放弃目标
     */
    suspend fun abandonGoal(id: Long) {
        repository.updateStatus(id, GoalStatus.ABANDONED)

        // 如果有父目标，更新父目标进度（放弃不算完成，但可能影响整体进度显示）
        val goal = repository.getGoalById(id)
        goal?.parentId?.let { parentId ->
            updateParentGoalProgress(parentId)
        }
    }

    /**
     * 归档目标
     */
    suspend fun archiveGoal(id: Long) {
        repository.updateStatus(id, GoalStatus.ARCHIVED)
    }

    /**
     * 重新激活目标
     */
    suspend fun reactivateGoal(id: Long) {
        repository.updateStatus(id, GoalStatus.ACTIVE)

        // 如果有父目标，更新父目标进度
        val goal = repository.getGoalById(id)
        goal?.parentId?.let { parentId ->
            updateParentGoalProgress(parentId)
        }
    }

    /**
     * 删除目标
     */
    suspend fun deleteGoal(id: Long) {
        val goal = repository.getGoalById(id)
        val parentId = goal?.parentId

        // 如果有子目标，一并删除
        val childCount = repository.countChildGoals(id)
        if (childCount > 0) {
            repository.deleteWithChildren(id)
        } else {
            repository.delete(id)
        }

        // 如果有父目标，更新父目标进度
        parentId?.let {
            updateParentGoalProgress(it)
        }
    }

    /**
     * 获取目标统计
     */
    suspend fun getStatistics(): GoalStatistics {
        val allGoals = repository.getAllGoals().first()
        val activeGoals = allGoals.filter { it.status == GoalStatus.ACTIVE }
        val completedGoals = allGoals.filter { it.status == GoalStatus.COMPLETED }

        val totalProgress = if (activeGoals.isNotEmpty()) {
            activeGoals.map { calculateProgress(it) }.average().toFloat()
        } else 0f

        return GoalStatistics(
            activeCount = activeGoals.size,
            completedCount = completedGoals.size,
            totalProgress = totalProgress
        )
    }

    /**
     * 计算目标进度（简单版本，用于列表展示）
     */
    fun calculateProgress(goal: GoalEntity): Float {
        return when {
            goal.status == GoalStatus.COMPLETED -> 1f
            goal.progressType == "NUMERIC" && goal.targetValue != null && goal.targetValue > 0 -> {
                (goal.currentValue / goal.targetValue).toFloat().coerceIn(0f, 1f)
            }
            goal.progressType == "PERCENTAGE" -> {
                (goal.currentValue / 100.0).toFloat().coerceIn(0f, 1f)
            }
            else -> 0f
        }
    }

    /**
     * 计算目标进度（完整版本，考虑子目标）
     * 如果目标有子目标，进度基于子目标完成情况计算
     */
    suspend fun calculateProgressWithChildren(goal: GoalEntity): Float {
        if (goal.status == GoalStatus.COMPLETED) return 1f

        // 检查是否有子目标
        val childCount = repository.countChildGoals(goal.id)
        if (childCount > 0) {
            val completedCount = repository.countCompletedChildGoals(goal.id)
            return (completedCount.toFloat() / childCount).coerceIn(0f, 1f)
        }

        // 无子目标时使用原有逻辑
        return calculateProgress(goal)
    }

    /**
     * 获取当前日期
     */
    fun getToday(): Int {
        return LocalDate.now().toEpochDay().toInt()
    }

    /**
     * 格式化日期
     */
    fun formatDate(epochDay: Int): String {
        val date = LocalDate.ofEpochDay(epochDay.toLong())
        return "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }

    /**
     * 计算剩余天数
     */
    fun getRemainingDays(endDate: Int?): Int? {
        if (endDate == null) return null
        val today = getToday()
        return endDate - today
    }

    // ============ 扩展功能 ============

    /**
     * 获取目标洞察分析
     */
    suspend fun getGoalInsights(): GoalInsights {
        val allGoals = repository.getAllGoals().first()
        val today = getToday()

        val activeGoals = allGoals.filter { it.status == GoalStatus.ACTIVE }
        val completedGoals = allGoals.filter { it.status == GoalStatus.COMPLETED }
        val abandonedGoals = allGoals.filter { it.status == GoalStatus.ABANDONED }

        // 计算完成率
        val completionRate = if (allGoals.isNotEmpty()) {
            completedGoals.size.toFloat() / allGoals.size
        } else 0f

        // 计算平均完成天数
        val avgCompletionDays = completedGoals
            .filter { it.completedAt != null }
            .mapNotNull { goal ->
                val startDate = LocalDate.ofEpochDay(goal.startDate.toLong())
                val completedDate = LocalDate.ofEpochDay(goal.completedAt!! / (24 * 60 * 60 * 1000))
                ChronoUnit.DAYS.between(startDate, completedDate).toInt()
            }
            .takeIf { it.isNotEmpty() }
            ?.average()?.toInt() ?: 0

        // 获取分类统计
        val categoryStats = getCategoryStats(allGoals)

        // 找出最活跃分类
        val mostActiveCategory = categoryStats.maxByOrNull { it.activeCount }?.category

        // 获取月度统计
        val monthlyStats = getMonthlyStats(allGoals)

        // 即将到期的目标（7天内）
        val upcomingDeadlines = activeGoals
            .filter { goal ->
                goal.endDate?.let { it - today in 0..7 } ?: false
            }
            .sortedBy { it.endDate }

        // 已逾期目标
        val overdueGoals = activeGoals
            .filter { goal ->
                goal.endDate?.let { it < today } ?: false
            }
            .sortedBy { it.endDate }

        // 获取连续完成数据
        val streakData = calculateGoalStreak(completedGoals)

        return GoalInsights(
            totalGoals = allGoals.size,
            activeGoals = activeGoals.size,
            completedGoals = completedGoals.size,
            abandonedGoals = abandonedGoals.size,
            completionRate = completionRate,
            averageCompletionDays = avgCompletionDays,
            mostActiveCategory = mostActiveCategory,
            categoryStats = categoryStats,
            monthlyStats = monthlyStats,
            upcomingDeadlines = upcomingDeadlines,
            overdueGoals = overdueGoals,
            streakData = streakData
        )
    }

    /**
     * 获取分类目标统计
     */
    private fun getCategoryStats(allGoals: List<GoalEntity>): List<CategoryGoalStats> {
        val categoryColors = mapOf(
            "CAREER" to "#3B82F6",
            "FINANCE" to "#10B981",
            "HEALTH" to "#EC4899",
            "LEARNING" to "#F59E0B",
            "RELATIONSHIP" to "#8B5CF6",
            "LIFESTYLE" to "#06B6D4",
            "HOBBY" to "#EF4444"
        )

        return goalCategoryOptions.map { (category, _) ->
            val categoryGoals = allGoals.filter { it.category == category }
            val completedCount = categoryGoals.count { it.status == GoalStatus.COMPLETED }
            val activeCount = categoryGoals.count { it.status == GoalStatus.ACTIVE }

            CategoryGoalStats(
                category = category,
                categoryName = getCategoryDisplayName(category),
                totalCount = categoryGoals.size,
                completedCount = completedCount,
                activeCount = activeCount,
                completionRate = if (categoryGoals.isNotEmpty()) {
                    completedCount.toFloat() / categoryGoals.size
                } else 0f,
                color = categoryColors[category] ?: "#6B7280"
            )
        }.filter { it.totalCount > 0 }
    }

    /**
     * 获取月度统计（最近6个月）
     */
    private fun getMonthlyStats(allGoals: List<GoalEntity>): List<MonthlyGoalStats> {
        val now = YearMonth.now()
        return (0..5).map { monthsAgo ->
            val yearMonth = now.minusMonths(monthsAgo.toLong())
            val yearMonthInt = yearMonth.year * 100 + yearMonth.monthValue
            val startOfMonth = yearMonth.atDay(1).toEpochDay().toInt()
            val endOfMonth = yearMonth.atEndOfMonth().toEpochDay().toInt()

            val createdInMonth = allGoals.count { goal ->
                val createdDate = LocalDate.ofEpochDay(goal.createdAt / (24 * 60 * 60 * 1000))
                val createdEpochDay = createdDate.toEpochDay().toInt()
                createdEpochDay in startOfMonth..endOfMonth
            }

            val completedInMonth = allGoals.count { goal ->
                goal.completedAt?.let { completedAt ->
                    val completedDate = LocalDate.ofEpochDay(completedAt / (24 * 60 * 60 * 1000))
                    val completedEpochDay = completedDate.toEpochDay().toInt()
                    completedEpochDay in startOfMonth..endOfMonth
                } ?: false
            }

            val abandonedInMonth = allGoals.count { goal ->
                goal.abandonedAt?.let { abandonedAt ->
                    val abandonedDate = LocalDate.ofEpochDay(abandonedAt / (24 * 60 * 60 * 1000))
                    val abandonedEpochDay = abandonedDate.toEpochDay().toInt()
                    abandonedEpochDay in startOfMonth..endOfMonth
                } ?: false
            }

            MonthlyGoalStats(
                yearMonth = yearMonthInt,
                monthLabel = "${yearMonth.monthValue}月",
                createdCount = createdInMonth,
                completedCount = completedInMonth,
                abandonedCount = abandonedInMonth
            )
        }.reversed()
    }

    /**
     * 计算目标完成连续天数
     */
    private fun calculateGoalStreak(completedGoals: List<GoalEntity>): GoalStreakData {
        if (completedGoals.isEmpty()) {
            return GoalStreakData()
        }

        // 获取所有完成日期
        val completionDates = completedGoals
            .mapNotNull { it.completedAt }
            .map { LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)).toEpochDay().toInt() }
            .toSet()
            .sorted()

        if (completionDates.isEmpty()) {
            return GoalStreakData(totalCompletionDays = 0)
        }

        val today = getToday()
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 1

        // 计算最长连续和当前连续
        for (i in 1 until completionDates.size) {
            if (completionDates[i] - completionDates[i - 1] == 1) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        // 计算当前连续（从今天往回数）
        if (completionDates.last() == today || completionDates.last() == today - 1) {
            currentStreak = 1
            var checkDate = completionDates.last() - 1
            while (completionDates.contains(checkDate)) {
                currentStreak++
                checkDate--
            }
        }

        return GoalStreakData(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletionDays = completionDates.size,
            lastCompletionDate = completionDates.lastOrNull()
        )
    }

    /**
     * 获取目标时间线数据
     */
    suspend fun getGoalTimeline(goalId: Long): GoalTimelineData? {
        val goal = repository.getGoalById(goalId) ?: return null
        val today = getToday()

        val startDate = goal.startDate
        val endDate = goal.endDate

        val daysElapsed = today - startDate
        val daysRemaining = endDate?.let { it - today }
        val totalDays = endDate?.let { it - startDate } ?: 365

        // 计算预期进度（基于时间）
        val expectedProgress = if (totalDays > 0) {
            (daysElapsed.toFloat() / totalDays).coerceIn(0f, 1f)
        } else 0f

        val currentProgress = calculateProgress(goal)
        val isOnTrack = currentProgress >= expectedProgress * 0.9f // 允许10%的偏差

        return GoalTimelineData(
            goalId = goalId,
            goalTitle = goal.title,
            startDate = startDate,
            endDate = endDate,
            progressRecords = emptyList(), // 需要从记录表获取
            currentProgress = currentProgress,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            expectedProgress = expectedProgress,
            isOnTrack = isOnTrack
        )
    }

    /**
     * 获取即将到期的目标
     */
    suspend fun getUpcomingDeadlines(days: Int = 7): List<GoalEntity> {
        val allGoals = repository.getAllGoals().first()
        val today = getToday()

        return allGoals
            .filter { it.status == GoalStatus.ACTIVE }
            .filter { goal ->
                goal.endDate?.let { it - today in 0..days } ?: false
            }
            .sortedBy { it.endDate }
    }

    /**
     * 获取已逾期目标
     */
    suspend fun getOverdueGoals(): List<GoalEntity> {
        val allGoals = repository.getAllGoals().first()
        val today = getToday()

        return allGoals
            .filter { it.status == GoalStatus.ACTIVE }
            .filter { goal ->
                goal.endDate?.let { it < today } ?: false
            }
            .sortedBy { it.endDate }
    }

    /**
     * 获取推荐目标（基于用户历史和当前活跃目标）
     */
    suspend fun getRecommendedTemplates(): List<GoalTemplate> {
        val allGoals = repository.getAllGoals().first()

        // 找出用户最常创建的分类
        val categoryCount = allGoals.groupBy { it.category }
            .mapValues { it.value.size }

        // 找出用户尚未尝试的分类
        val unusedCategories = goalCategoryOptions.map { it.first }
            .filter { category -> categoryCount[category] == null || categoryCount[category] == 0 }

        // 获取推荐模板
        val recommendations = mutableListOf<GoalTemplate>()

        // 1. 从最常用分类中推荐
        categoryCount.entries
            .sortedByDescending { it.value }
            .take(2)
            .forEach { (category, _) ->
                goalTemplates
                    .filter { it.category == category }
                    .take(1)
                    .forEach { recommendations.add(it) }
            }

        // 2. 从未尝试分类中推荐
        unusedCategories.take(2).forEach { category ->
            goalTemplates
                .filter { it.category == category }
                .take(1)
                .forEach { recommendations.add(it) }
        }

        // 3. 补充一些热门模板
        val popularTemplates = listOf("learning_reading", "health_exercise", "finance_saving")
        popularTemplates.forEach { id ->
            goalTemplates.find { it.id == id }?.let { template ->
                if (!recommendations.contains(template)) {
                    recommendations.add(template)
                }
            }
        }

        return recommendations.take(6)
    }

    /**
     * 从模板创建目标
     */
    suspend fun createGoalFromTemplate(template: GoalTemplate, customTitle: String? = null): Long {
        val today = getToday()
        val endDate = today + template.suggestedDuration

        return createGoal(
            title = customTitle ?: template.name,
            description = template.description,
            goalType = template.goalType,
            category = template.category,
            startDate = today,
            endDate = endDate,
            progressType = template.progressType,
            targetValue = template.targetValue,
            unit = template.unit
        )
    }

    /**
     * 获取目标完成率趋势（最近6个月）
     */
    suspend fun getCompletionRateTrend(): List<Pair<String, Float>> {
        val insights = getGoalInsights()
        return insights.monthlyStats.map { stats ->
            val total = stats.createdCount + stats.completedCount
            val rate = if (total > 0) stats.completedCount.toFloat() / total else 0f
            stats.monthLabel to rate
        }
    }

    /**
     * 获取按分类分组的活跃目标
     */
    suspend fun getActiveGoalsByCategory(): Map<String, List<GoalEntity>> {
        val activeGoals = repository.getAllGoals().first()
            .filter { it.status == GoalStatus.ACTIVE }

        return activeGoals.groupBy { it.category }
    }

    /**
     * 计算目标健康度（0-100）
     */
    fun calculateGoalHealth(goal: GoalEntity): Int {
        if (goal.status == GoalStatus.COMPLETED) return 100
        if (goal.status == GoalStatus.ABANDONED) return 0

        val today = getToday()
        val progress = calculateProgress(goal)

        // 如果没有截止日期，仅基于进度评估
        if (goal.endDate == null) {
            return (progress * 100).toInt()
        }

        val totalDays = goal.endDate - goal.startDate
        val daysElapsed = today - goal.startDate
        val expectedProgress = if (totalDays > 0) {
            (daysElapsed.toFloat() / totalDays).coerceIn(0f, 1f)
        } else 1f

        // 健康度计算：实际进度与预期进度的比值
        val healthRatio = if (expectedProgress > 0) {
            (progress / expectedProgress).coerceIn(0f, 1.5f)
        } else 1f

        // 如果已逾期，降低健康度
        val overdueRatio = if (today > goal.endDate) {
            val overdueDays = today - goal.endDate
            maxOf(0.5f, 1f - (overdueDays * 0.02f))
        } else 1f

        return (healthRatio * overdueRatio * 100).toInt().coerceIn(0, 100)
    }
}
