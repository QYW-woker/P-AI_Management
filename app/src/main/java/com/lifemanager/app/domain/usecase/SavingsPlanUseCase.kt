package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.RecordType
import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsPlanStatus
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
import com.lifemanager.app.domain.model.Milestone
import com.lifemanager.app.domain.model.SavingsPlanWithDetails
import com.lifemanager.app.domain.model.SavingsStats
import com.lifemanager.app.domain.repository.SavingsPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 存钱计划业务逻辑用例
 */
@Singleton
class SavingsPlanUseCase @Inject constructor(
    private val repository: SavingsPlanRepository
) {
    /**
     * 获取今日日期（epochDay格式）
     */
    fun getToday(): Int {
        return LocalDate.now().toEpochDay().toInt()
    }

    /**
     * 获取所有活跃计划及其详情
     */
    fun getPlansWithDetails(): Flow<List<SavingsPlanWithDetails>> {
        val today = getToday()
        return repository.getActivePlans().map { plans ->
            plans.map { plan ->
                calculatePlanDetails(plan, today)
            }
        }
    }

    /**
     * 获取所有计划（包括已完成和暂停的）
     */
    fun getAllPlansWithDetails(): Flow<List<SavingsPlanWithDetails>> {
        val today = getToday()
        return repository.getAllPlans().map { plans ->
            plans.map { plan ->
                calculatePlanDetails(plan, today)
            }
        }
    }

    /**
     * 计算计划详情
     */
    private suspend fun calculatePlanDetails(plan: SavingsPlanEntity, today: Int): SavingsPlanWithDetails {
        val records = repository.getRecordsByPlan(plan.id).first()

        // 计算进度
        val progress = if (plan.targetAmount > 0) {
            (plan.currentAmount / plan.targetAmount).toFloat().coerceIn(0f, 1f)
        } else 0f

        // 计算天数
        val totalDays = plan.targetDate - plan.startDate
        val daysElapsed = (today - plan.startDate).coerceAtLeast(0)
        val daysRemaining = (plan.targetDate - today).coerceAtLeast(0)

        // 计算每日目标金额
        val dailyTarget = if (totalDays > 0) {
            plan.targetAmount / totalDays
        } else 0.0

        // 计算预期应存金额
        val expectedAmount = dailyTarget * daysElapsed

        // 判断是否符合预期进度
        val isOnTrack = plan.currentAmount >= expectedAmount * 0.9 // 允许10%的偏差

        // 计算存款和取款统计
        val deposits = records.filter { it.type == RecordType.DEPOSIT }
        val withdrawals = records.filter { it.type == RecordType.WITHDRAWAL }
        val totalDeposits = deposits.sumOf { it.amount }
        val totalWithdrawals = withdrawals.sumOf { it.amount }

        // 计算里程碑
        val progressPercent = (progress * 100).toInt()
        val currentMilestone = when {
            progressPercent >= 100 -> Milestone.COMPLETE
            progressPercent >= 75 -> Milestone.THREE_QUARTERS
            progressPercent >= 50 -> Milestone.HALF
            progressPercent >= 25 -> Milestone.QUARTER
            else -> Milestone.START
        }
        val nextMilestone = when {
            progressPercent >= 100 -> null
            progressPercent >= 75 -> Milestone.COMPLETE
            progressPercent >= 50 -> Milestone.THREE_QUARTERS
            progressPercent >= 25 -> Milestone.HALF
            else -> Milestone.QUARTER
        }

        return SavingsPlanWithDetails(
            plan = plan,
            records = records,
            progress = progress,
            daysRemaining = daysRemaining,
            daysElapsed = daysElapsed,
            dailyTarget = dailyTarget,
            expectedAmount = expectedAmount,
            isOnTrack = isOnTrack,
            totalDeposits = totalDeposits,
            totalWithdrawals = totalWithdrawals,
            depositCount = deposits.size,
            withdrawalCount = withdrawals.size,
            currentMilestone = currentMilestone,
            nextMilestone = nextMilestone
        )
    }

    /**
     * 获取计划详情
     */
    suspend fun getPlanDetails(planId: Long): SavingsPlanWithDetails? {
        val plan = repository.getPlanById(planId) ?: return null
        return calculatePlanDetails(plan, getToday())
    }

    /**
     * 获取存钱统计数据
     */
    suspend fun getSavingsStats(): SavingsStats {
        val activePlans = repository.countActivePlans()
        val totalTarget = repository.getTotalTarget()
        val totalCurrent = repository.getTotalCurrent()

        val overallProgress = if (totalTarget > 0) {
            (totalCurrent / totalTarget).toFloat().coerceIn(0f, 1f)
        } else 0f

        // 计算本月和上月存款
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1).toEpochDay().toInt()
        val lastMonthStart = today.minusMonths(1).withDayOfMonth(1).toEpochDay().toInt()

        val recentRecords = repository.getRecentRecords(500).first()

        // 本月存款（仅存款类型）
        val thisMonthDeposit = recentRecords
            .filter { it.date >= monthStart && it.type == RecordType.DEPOSIT }
            .sumOf { it.amount }

        // 上月存款（仅存款类型）
        val lastMonthDeposit = recentRecords
            .filter { it.date >= lastMonthStart && it.date < monthStart && it.type == RecordType.DEPOSIT }
            .sumOf { it.amount }

        // 月度变化百分比
        val monthlyChange = if (lastMonthDeposit > 0) {
            ((thisMonthDeposit - lastMonthDeposit) / lastMonthDeposit * 100)
        } else if (thisMonthDeposit > 0) {
            100.0
        } else {
            0.0
        }

        // 总存款和总取款
        val totalDeposits = recentRecords
            .filter { it.type == RecordType.DEPOSIT }
            .sumOf { it.amount }
        val totalWithdrawals = recentRecords
            .filter { it.type == RecordType.WITHDRAWAL }
            .sumOf { it.amount }

        // 计算连续存款天数
        val savingsStreak = calculateSavingsStreak()

        // 获取总存款天数
        val totalDepositDays = repository.getTotalDepositDays()

        return SavingsStats(
            activePlans = activePlans,
            totalTarget = totalTarget,
            totalCurrent = totalCurrent,
            overallProgress = overallProgress,
            thisMonthDeposit = thisMonthDeposit,
            lastMonthDeposit = lastMonthDeposit,
            monthlyChange = monthlyChange,
            totalDeposits = totalDeposits,
            totalWithdrawals = totalWithdrawals,
            savingsStreak = savingsStreak,
            totalRecords = totalDepositDays
        )
    }

    /**
     * 计算连续存款天数（从今天或昨天往前数）
     */
    private suspend fun calculateSavingsStreak(): Int {
        val depositDates = repository.getAllDepositDates()
        if (depositDates.isEmpty()) return 0

        val today = getToday()
        val sortedDates = depositDates.sorted().reversed()  // 从最近日期开始

        // 检查最近一次存款是否为今天或昨天（允许一天的间隔保持streak）
        val latestDate = sortedDates.first()
        if (today - latestDate > 1) {
            // 如果最近存款不是今天或昨天，streak为0
            return 0
        }

        // 计算连续天数
        var streak = 1
        var previousDate = latestDate

        for (i in 1 until sortedDates.size) {
            val currentDate = sortedDates[i]
            // 如果日期连续或者是同一天（多次存款）
            if (previousDate - currentDate <= 1) {
                if (previousDate - currentDate == 1) {
                    streak++
                }
                previousDate = currentDate
            } else {
                // 连续中断
                break
            }
        }

        return streak
    }

    /**
     * 创建存钱计划
     */
    suspend fun createPlan(plan: SavingsPlanEntity): Long {
        return repository.savePlan(plan)
    }

    /**
     * 更新存钱计划
     */
    suspend fun updatePlan(plan: SavingsPlanEntity) {
        repository.updatePlan(plan.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 存款
     */
    suspend fun deposit(planId: Long, amount: Double, note: String = "") {
        val today = getToday()
        val record = SavingsRecordEntity(
            planId = planId,
            amount = amount,
            type = RecordType.DEPOSIT,
            date = today,
            note = note
        )
        repository.saveRecord(record)
        repository.addAmount(planId, amount)

        // 检查是否完成目标
        val plan = repository.getPlanById(planId)
        if (plan != null && plan.currentAmount + amount >= plan.targetAmount) {
            repository.updatePlanStatus(planId, SavingsPlanStatus.COMPLETED)
        }
    }

    /**
     * 取款
     */
    suspend fun withdraw(planId: Long, amount: Double, note: String = ""): Boolean {
        val plan = repository.getPlanById(planId) ?: return false

        // 检查余额是否足够
        if (plan.currentAmount < amount) {
            return false
        }

        val today = getToday()
        val record = SavingsRecordEntity(
            planId = planId,
            amount = amount,
            type = RecordType.WITHDRAWAL,
            date = today,
            note = note
        )
        repository.saveRecord(record)

        // 减少计划金额
        val newAmount = (plan.currentAmount - amount).coerceAtLeast(0.0)
        repository.updatePlanAmount(planId, newAmount)

        // 如果计划已完成，取款后重新激活
        if (plan.status == SavingsPlanStatus.COMPLETED && newAmount < plan.targetAmount) {
            repository.updatePlanStatus(planId, SavingsPlanStatus.ACTIVE)
        }

        return true
    }

    /**
     * 删除存款/取款记录
     */
    suspend fun deleteRecord(recordId: Long) {
        val record = repository.getRecordById(recordId) ?: return
        repository.deleteRecord(recordId)

        // 更新计划金额
        val plan = repository.getPlanById(record.planId) ?: return
        val adjustment = if (record.type == RecordType.DEPOSIT) {
            // 删除存款记录，减少金额
            -record.amount
        } else {
            // 删除取款记录，恢复金额
            record.amount
        }
        val newAmount = (plan.currentAmount + adjustment).coerceAtLeast(0.0)
        repository.updatePlanAmount(record.planId, newAmount)

        // 检查是否需要更新计划状态
        if (newAmount >= plan.targetAmount && plan.status == SavingsPlanStatus.ACTIVE) {
            repository.updatePlanStatus(record.planId, SavingsPlanStatus.COMPLETED)
        } else if (newAmount < plan.targetAmount && plan.status == SavingsPlanStatus.COMPLETED) {
            repository.updatePlanStatus(record.planId, SavingsPlanStatus.ACTIVE)
        }
    }

    /**
     * 暂停计划
     */
    suspend fun pausePlan(planId: Long) {
        repository.updatePlanStatus(planId, SavingsPlanStatus.PAUSED)
    }

    /**
     * 恢复计划
     */
    suspend fun resumePlan(planId: Long) {
        repository.updatePlanStatus(planId, SavingsPlanStatus.ACTIVE)
    }

    /**
     * 取消计划
     */
    suspend fun cancelPlan(planId: Long) {
        repository.updatePlanStatus(planId, SavingsPlanStatus.CANCELLED)
    }

    /**
     * 删除计划
     */
    suspend fun deletePlan(planId: Long) {
        repository.deletePlan(planId)
    }

    /**
     * 根据ID获取计划
     */
    suspend fun getPlanById(planId: Long): SavingsPlanEntity? {
        return repository.getPlanById(planId)
    }

    /**
     * 获取计划的存款记录
     */
    fun getPlanRecords(planId: Long): Flow<List<SavingsRecordEntity>> {
        return repository.getRecordsByPlan(planId)
    }

    /**
     * 格式化日期显示
     */
    fun formatDate(epochDay: Int): String {
        val date = LocalDate.ofEpochDay(epochDay.toLong())
        return "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }

    /**
     * 日期转epochDay
     */
    fun dateToEpochDay(year: Int, month: Int, day: Int): Int {
        return LocalDate.of(year, month, day).toEpochDay().toInt()
    }
}
