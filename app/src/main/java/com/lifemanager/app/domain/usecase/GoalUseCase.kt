package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalRecordEntity
import com.lifemanager.app.core.database.entity.GoalRecordType
import com.lifemanager.app.core.database.entity.GoalStatus
import com.lifemanager.app.domain.model.GoalProgressRecordUI
import com.lifemanager.app.domain.model.GoalStatistics
import com.lifemanager.app.domain.model.GoalTreeNode
import com.lifemanager.app.domain.model.SubGoalEditState
import com.lifemanager.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
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
        parentId: Long? = null,
        level: Int = 0,
        isMultiLevel: Boolean = false
    ): Long {
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
            level = level,
            isMultiLevel = isMultiLevel
        )
        return repository.insert(goal)
    }

    /**
     * 创建带子目标的多级目标
     */
    suspend fun createGoalWithSubGoals(
        title: String,
        description: String,
        goalType: String,
        category: String,
        startDate: Int,
        endDate: Int?,
        progressType: String,
        targetValue: Double?,
        unit: String,
        subGoals: List<SubGoalEditState>
    ): Long {
        // 创建父目标
        val parentId = createGoal(
            title = title,
            description = description,
            goalType = goalType,
            category = category,
            startDate = startDate,
            endDate = endDate,
            progressType = progressType,
            targetValue = targetValue,
            unit = unit,
            isMultiLevel = subGoals.isNotEmpty()
        )

        // 创建子目标
        subGoals.forEach { subGoal ->
            createGoal(
                title = subGoal.title,
                description = subGoal.description,
                goalType = goalType,
                category = category,
                startDate = startDate,
                endDate = endDate,
                progressType = subGoal.progressType,
                targetValue = subGoal.targetValue,
                unit = subGoal.unit,
                parentId = parentId,
                level = 1
            )
        }

        return parentId
    }

    /**
     * 更新目标
     */
    suspend fun updateGoal(goal: GoalEntity) {
        repository.update(goal.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 更新目标进度（累加制）
     * @param id 目标ID
     * @param changeValue 变化值（增加或减少的数值，正数为增加，负数为减少）
     */
    suspend fun updateProgress(id: Long, changeValue: Double) {
        val goal = repository.getGoalById(id) ?: return
        val previousValue = goal.currentValue
        val newValue = previousValue + changeValue
        val today = getToday()

        // 更新目标进度
        repository.updateProgress(id, newValue)

        // 创建进度记录
        val record = GoalRecordEntity(
            goalId = id,
            recordType = GoalRecordType.PROGRESS,
            title = "进度更新",
            content = if (goal.progressType == "NUMERIC") {
                "从 ${previousValue.toInt()}${goal.unit} 增加到 ${newValue.toInt()}${goal.unit}（+${changeValue.toInt()}${goal.unit}）"
            } else {
                "进度从 ${previousValue.toInt()}% 更新至 ${newValue.toInt()}%"
            },
            progressValue = changeValue,
            previousValue = previousValue,
            recordDate = today
        )
        repository.insertProgressRecord(record)

        // 检查是否达成目标
        if (goal.targetValue != null && newValue >= goal.targetValue) {
            repository.updateStatus(id, GoalStatus.COMPLETED)
            // 添加完成记录
            val completeRecord = GoalRecordEntity(
                goalId = id,
                recordType = GoalRecordType.COMPLETE,
                title = "目标完成",
                content = "恭喜！目标已达成！",
                recordDate = today
            )
            repository.insertProgressRecord(completeRecord)
        }

        // 如果有父目标，更新父目标进度
        goal.parentId?.let { parentId ->
            updateParentProgress(parentId)
        }
    }

    /**
     * 获取目标的进度记录列表（转换为UI模型）
     */
    suspend fun getProgressRecords(goalId: Long): List<GoalProgressRecordUI> {
        val records = repository.getProgressRecords(goalId)
        return records
            .filter { it.recordType == GoalRecordType.PROGRESS }
            .map { record ->
                GoalProgressRecordUI(
                    id = record.id,
                    changeValue = record.progressValue ?: 0.0,
                    totalValue = (record.previousValue ?: 0.0) + (record.progressValue ?: 0.0),
                    previousValue = record.previousValue ?: 0.0,
                    title = record.title,
                    content = record.content,
                    recordDate = record.recordDate,
                    createdAt = record.createdAt
                )
            }
    }

    /**
     * 更新父目标进度（根据子目标进度自动计算）
     */
    private suspend fun updateParentProgress(parentId: Long) {
        val children = repository.getChildGoalsSync(parentId)
        if (children.isEmpty()) return

        // 计算子目标平均进度
        val childProgresses = children.map { child ->
            calculateProgress(child)
        }
        val averageProgress = childProgresses.average().toFloat()

        // 获取父目标信息
        val parent = repository.getGoalById(parentId) ?: return

        // 根据父目标的进度类型更新进度值
        val progressValue = when (parent.progressType) {
            "PERCENTAGE" -> (averageProgress * 100).toDouble()
            "NUMERIC" -> {
                if (parent.targetValue != null) {
                    averageProgress * parent.targetValue
                } else {
                    (averageProgress * 100).toDouble()
                }
            }
            else -> (averageProgress * 100).toDouble()
        }

        repository.updateProgress(parentId, progressValue)

        // 检查是否完成
        if (averageProgress >= 1f) {
            repository.updateStatus(parentId, GoalStatus.COMPLETED)
        }

        // 递归更新更上层的父目标
        parent.parentId?.let { grandParentId ->
            updateParentProgress(grandParentId)
        }
    }

    /**
     * 完成目标
     */
    suspend fun completeGoal(id: Long) {
        repository.updateStatus(id, GoalStatus.COMPLETED)
    }

    /**
     * 放弃目标
     */
    suspend fun abandonGoal(id: Long) {
        repository.updateStatus(id, GoalStatus.ABANDONED)
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
    }

    /**
     * 删除目标
     */
    suspend fun deleteGoal(id: Long) {
        repository.delete(id)
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
     * 计算目标进度
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

    // ==================== 多级目标相关方法 ====================

    /**
     * 获取顶级目标列表
     */
    fun getTopLevelGoals(): Flow<List<GoalEntity>> {
        return repository.getTopLevelGoals()
    }

    /**
     * 获取所有顶级目标（包含所有状态）
     */
    fun getAllTopLevelGoals(): Flow<List<GoalEntity>> {
        return repository.getAllTopLevelGoals()
    }

    /**
     * 获取子目标列表
     */
    fun getChildGoals(parentId: Long): Flow<List<GoalEntity>> {
        return repository.getChildGoals(parentId)
    }

    /**
     * 获取子目标列表（同步版本）
     */
    suspend fun getChildGoalsSync(parentId: Long): List<GoalEntity> {
        return repository.getChildGoalsSync(parentId)
    }

    /**
     * 获取子目标数量
     */
    suspend fun countChildGoals(parentId: Long): Int {
        return repository.countChildGoals(parentId)
    }

    /**
     * 构建目标树
     */
    suspend fun buildGoalTree(goals: List<GoalEntity>): List<GoalTreeNode> {
        return goals.map { goal ->
            buildTreeNode(goal)
        }
    }

    /**
     * 构建单个目标的树节点
     */
    private suspend fun buildTreeNode(goal: GoalEntity): GoalTreeNode {
        val children = repository.getChildGoalsSync(goal.id)
        val childNodes = children.map { buildTreeNode(it) }
        val progress = if (children.isNotEmpty()) {
            // 多级目标：根据子目标计算进度
            childNodes.map { it.progress }.average().toFloat()
        } else {
            calculateProgress(goal)
        }

        return GoalTreeNode(
            goal = goal,
            level = goal.level,
            children = childNodes,
            isExpanded = false,
            childCount = children.size,
            progress = progress
        )
    }

    /**
     * 将目标树扁平化为列表（仅包含展开的节点）
     */
    fun flattenTree(
        nodes: List<GoalTreeNode>,
        expandedIds: Set<Long> = emptySet()
    ): List<GoalTreeNode> {
        val result = mutableListOf<GoalTreeNode>()
        nodes.forEach { node ->
            val isExpanded = expandedIds.contains(node.goal.id)
            result.add(node.copy(isExpanded = isExpanded))
            if (isExpanded && node.children.isNotEmpty()) {
                result.addAll(flattenTree(node.children, expandedIds))
            }
        }
        return result
    }

    /**
     * 添加子目标
     */
    suspend fun addSubGoal(
        parentId: Long,
        title: String,
        description: String = "",
        progressType: String = "PERCENTAGE",
        targetValue: Double? = null,
        unit: String = ""
    ): Long {
        val parent = repository.getGoalById(parentId) ?: return -1

        val subGoalId = createGoal(
            title = title,
            description = description,
            goalType = parent.goalType,
            category = parent.category,
            startDate = parent.startDate,
            endDate = parent.endDate,
            progressType = progressType,
            targetValue = targetValue,
            unit = unit,
            parentId = parentId,
            level = parent.level + 1
        )

        // 更新父目标为多级目标
        if (!parent.isMultiLevel) {
            repository.updateMultiLevelFlag(parentId, true)
        }

        return subGoalId
    }

    /**
     * 删除目标（包括子目标）
     */
    suspend fun deleteGoalWithChildren(id: Long) {
        // 先删除所有子目标
        repository.deleteChildGoals(id)
        // 再删除目标本身
        repository.delete(id)

        // 检查父目标是否还有子目标
        val goal = repository.getGoalById(id)
        goal?.parentId?.let { parentId ->
            val childCount = repository.countChildGoals(parentId)
            if (childCount == 0) {
                repository.updateMultiLevelFlag(parentId, false)
            }
            // 更新父目标进度
            updateParentProgress(parentId)
        }
    }
}
