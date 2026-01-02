package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.entity.SavingsPlanEntity
import com.lifemanager.app.core.database.entity.SavingsPlanStatus
import com.lifemanager.app.core.database.entity.SavingsRecordEntity
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

        return SavingsPlanWithDetails(
            plan = plan,
            records = records,
            progress = progress,
            daysRemaining = daysRemaining,
            daysElapsed = daysElapsed,
            dailyTarget = dailyTarget,
            expectedAmount = expectedAmount,
            isOnTrack = isOnTrack
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

        // 计算本月存款
        val today = getToday()
        val monthStart = LocalDate.now().withDayOfMonth(1).toEpochDay().toInt()
        // 简化：使用总金额作为近似值
        val thisMonthDeposit = repository.getRecentRecords(100).first()
            .filter { it.date >= monthStart && it.date <= today }
            .sumOf { it.amount }

        return SavingsStats(
            activePlans = activePlans,
            totalTarget = totalTarget,
            totalCurrent = totalCurrent,
            overallProgress = overallProgress,
            thisMonthDeposit = thisMonthDeposit
        )
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
     * 根据ID获取存款记录
     */
    suspend fun getRecordById(recordId: Long): SavingsRecordEntity? {
        return repository.getRecordById(recordId)
    }

    /**
     * 删除存款记录
     */
    suspend fun deleteRecord(recordId: Long) {
        val record = repository.getRecordById(recordId) ?: return
        repository.deleteRecord(recordId)
        // 更新计划金额
        val plan = repository.getPlanById(record.planId) ?: return
        val newAmount = (plan.currentAmount - record.amount).coerceAtLeast(0.0)
        repository.updatePlanAmount(record.planId, newAmount)
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
