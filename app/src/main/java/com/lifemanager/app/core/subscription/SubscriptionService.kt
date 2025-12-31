package com.lifemanager.app.core.subscription

import com.lifemanager.app.core.database.dao.SubscriptionDao
import com.lifemanager.app.core.database.dao.SubscriptionPaymentDao
import com.lifemanager.app.core.database.dao.SubscriptionStats
import com.lifemanager.app.core.database.dao.SubscriptionTypeStats
import com.lifemanager.app.core.database.entity.SubscriptionEntity
import com.lifemanager.app.core.database.entity.SubscriptionPaymentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 订阅管理服务
 *
 * 管理用户的订阅服务，追踪费用和到期提醒
 */
@Singleton
class SubscriptionService @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val subscriptionPaymentDao: SubscriptionPaymentDao
) {

    companion object {
        // 订阅类型
        val SUBSCRIPTION_TYPES = listOf(
            SubscriptionType("VIDEO", "视频会员", "🎬", listOf("爱奇艺", "腾讯视频", "优酷", "哔哩哔哩", "Netflix", "Disney+")),
            SubscriptionType("MUSIC", "音乐会员", "🎵", listOf("网易云音乐", "QQ音乐", "Apple Music", "Spotify")),
            SubscriptionType("CLOUD", "云服务", "☁️", listOf("iCloud", "百度网盘", "阿里云盘", "Dropbox", "OneDrive")),
            SubscriptionType("TOOL", "工具软件", "🛠️", listOf("Microsoft 365", "Adobe CC", "1Password", "Notion")),
            SubscriptionType("GAME", "游戏会员", "🎮", listOf("PS Plus", "Xbox Game Pass", "Nintendo Online", "Steam")),
            SubscriptionType("NEWS", "新闻阅读", "📰", listOf("微信读书", "知乎盐选", "得到", "喜马拉雅")),
            SubscriptionType("FITNESS", "健身运动", "💪", listOf("Keep", "超级猩猩", "健身房会员")),
            SubscriptionType("FOOD", "外卖餐饮", "🍔", listOf("美团会员", "饿了么会员", "星巴克")),
            SubscriptionType("SHOPPING", "购物会员", "🛒", listOf("京东Plus", "淘宝88会员", "亚马逊Prime", "Costco")),
            SubscriptionType("OTHER", "其他", "📦", emptyList())
        )

        // 计费周期
        val BILLING_CYCLES = listOf(
            BillingCycle("MONTHLY", "月付", 1),
            BillingCycle("QUARTERLY", "季付", 3),
            BillingCycle("YEARLY", "年付", 12),
            BillingCycle("WEEKLY", "周付", 0)
        )
    }

    // ==================== 订阅管理 ====================

    /**
     * 获取所有订阅
     */
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    /**
     * 获取活跃订阅
     */
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.getActiveSubscriptions()

    /**
     * 按类型获取订阅
     */
    fun getSubscriptionsByType(type: String): Flow<List<SubscriptionEntity>> =
        subscriptionDao.getSubscriptionsByType(type)

    /**
     * 获取订阅详情
     */
    suspend fun getSubscription(id: Long): SubscriptionEntity? = subscriptionDao.getById(id)

    /**
     * 创建订阅
     */
    suspend fun createSubscription(
        name: String,
        type: String,
        amount: Double,
        billingCycle: String,
        startDate: Int,
        autoRenew: Boolean = true,
        reminderDays: Int = 3,
        note: String = "",
        iconName: String = "",
        color: String = ""
    ): Long {
        val nextBillingDate = calculateNextBillingDate(startDate, billingCycle)

        return subscriptionDao.insert(
            SubscriptionEntity(
                name = name,
                type = type,
                amount = amount,
                billingCycle = billingCycle,
                startDate = startDate,
                nextBillingDate = nextBillingDate,
                autoRenew = autoRenew,
                reminderEnabled = reminderDays > 0,
                reminderDays = reminderDays,
                note = note,
                iconName = iconName.ifEmpty { getDefaultIcon(type) },
                color = color.ifEmpty { getDefaultColor(type) }
            )
        )
    }

    /**
     * 更新订阅
     */
    suspend fun updateSubscription(subscription: SubscriptionEntity) {
        subscriptionDao.update(subscription.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 删除订阅
     */
    suspend fun deleteSubscription(id: Long) {
        subscriptionDao.deleteById(id)
    }

    /**
     * 暂停订阅
     */
    suspend fun pauseSubscription(id: Long) {
        val subscription = subscriptionDao.getById(id) ?: return
        subscriptionDao.update(
            subscription.copy(
                status = "PAUSED",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * 恢复订阅
     */
    suspend fun resumeSubscription(id: Long) {
        val subscription = subscriptionDao.getById(id) ?: return
        subscriptionDao.update(
            subscription.copy(
                status = "ACTIVE",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * 取消订阅
     */
    suspend fun cancelSubscription(id: Long) {
        val subscription = subscriptionDao.getById(id) ?: return
        subscriptionDao.update(
            subscription.copy(
                status = "CANCELLED",
                autoRenew = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // ==================== 扣款处理 ====================

    /**
     * 处理订阅扣款
     */
    suspend fun processPayment(subscriptionId: Long, actualAmount: Double? = null): Long {
        val subscription = subscriptionDao.getById(subscriptionId) ?: return -1
        val today = LocalDate.now().toEpochDay().toInt()
        val amount = actualAmount ?: subscription.amount

        // 记录付款
        val paymentId = subscriptionPaymentDao.insert(
            SubscriptionPaymentEntity(
                subscriptionId = subscriptionId,
                amount = amount,
                paymentDate = today,
                billingPeriodStart = subscription.nextBillingDate,
                billingPeriodEnd = calculateNextBillingDate(subscription.nextBillingDate, subscription.billingCycle)
            )
        )

        // 更新下次扣款日期
        val nextDate = calculateNextBillingDate(subscription.nextBillingDate, subscription.billingCycle)
        subscriptionDao.updateNextBillingDate(subscriptionId, nextDate)

        return paymentId
    }

    /**
     * 获取订阅付款记录
     */
    fun getPaymentHistory(subscriptionId: Long): Flow<List<SubscriptionPaymentEntity>> =
        subscriptionPaymentDao.getBySubscriptionId(subscriptionId)

    // ==================== 提醒和统计 ====================

    /**
     * 获取即将到期的订阅
     */
    suspend fun getUpcomingBillings(days: Int = 7): List<SubscriptionEntity> {
        val targetDate = LocalDate.now().plusDays(days.toLong()).toEpochDay().toInt()
        return subscriptionDao.getUpcomingBillings(targetDate)
    }

    /**
     * 获取活跃订阅统计
     */
    suspend fun getActiveStats(): SubscriptionStats = subscriptionDao.getActiveStats()

    /**
     * 按类型统计
     */
    suspend fun getStatsByType(): List<SubscriptionTypeStats> = subscriptionDao.getStatsByType()

    /**
     * 获取订阅概览
     */
    suspend fun getSubscriptionOverview(): SubscriptionOverview {
        val stats = subscriptionDao.getActiveStats()
        val upcoming = getUpcomingBillings(7)
        val typeStats = subscriptionDao.getStatsByType()

        return SubscriptionOverview(
            activeCount = stats.count,
            monthlyTotal = stats.monthlyTotal ?: 0.0,
            yearlyTotal = stats.yearlyTotal ?: 0.0,
            upcomingCount = upcoming.size,
            upcomingAmount = upcoming.sumOf { it.amount },
            typeDistribution = typeStats.map {
                TypeDistribution(it.type, it.count, it.yearlyTotal)
            }
        )
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算下次扣款日期
     */
    private fun calculateNextBillingDate(currentDate: Int, billingCycle: String): Int {
        val date = LocalDate.ofEpochDay(currentDate.toLong())
        val nextDate = when (billingCycle) {
            "WEEKLY" -> date.plusWeeks(1)
            "MONTHLY" -> date.plusMonths(1)
            "QUARTERLY" -> date.plusMonths(3)
            "YEARLY" -> date.plusYears(1)
            else -> date.plusMonths(1)
        }
        return nextDate.toEpochDay().toInt()
    }

    /**
     * 获取类型默认图标
     */
    private fun getDefaultIcon(type: String): String {
        return SUBSCRIPTION_TYPES.find { it.id == type }?.icon ?: "📦"
    }

    /**
     * 获取类型默认颜色
     */
    private fun getDefaultColor(type: String): String {
        return when (type) {
            "VIDEO" -> "#E91E63"
            "MUSIC" -> "#9C27B0"
            "CLOUD" -> "#2196F3"
            "TOOL" -> "#FF9800"
            "GAME" -> "#4CAF50"
            "NEWS" -> "#795548"
            "FITNESS" -> "#00BCD4"
            "FOOD" -> "#F44336"
            "SHOPPING" -> "#FFC107"
            else -> "#607D8B"
        }
    }

    /**
     * 估算年度订阅费用
     */
    suspend fun estimateYearlyCost(): Double {
        val stats = subscriptionDao.getActiveStats()
        return stats.yearlyTotal ?: 0.0
    }

    /**
     * 查找可能重复的订阅
     */
    suspend fun findDuplicates(): List<List<SubscriptionEntity>> {
        val subscriptions = subscriptionDao.getActiveSubscriptions().first()
        val grouped = subscriptions.groupBy { it.type }

        return grouped.values
            .filter { it.size > 1 }
            .map { it.sortedBy { sub -> sub.amount } }
    }

    /**
     * 获取订阅节省建议
     */
    suspend fun getSavingSuggestions(): List<SavingSuggestion> {
        val suggestions = mutableListOf<SavingSuggestion>()
        val subscriptions = subscriptionDao.getActiveSubscriptions().first()

        // 检查月付转年付
        subscriptions.filter { it.billingCycle == "MONTHLY" }.forEach { sub ->
            val monthlyTotal = sub.amount * 12
            val estimatedYearly = monthlyTotal * 0.85 // 假设年付优惠15%
            val savings = monthlyTotal - estimatedYearly

            if (savings > 10) {
                suggestions.add(
                    SavingSuggestion(
                        subscriptionId = sub.id,
                        subscriptionName = sub.name,
                        type = "SWITCH_TO_YEARLY",
                        description = "将${sub.name}从月付切换为年付",
                        potentialSavings = savings
                    )
                )
            }
        }

        // 检查同类型重复订阅
        val duplicates = findDuplicates()
        duplicates.forEach { group ->
            val typeName = SUBSCRIPTION_TYPES.find { it.id == group.first().type }?.name ?: "服务"
            val names = group.joinToString("、") { it.name }
            suggestions.add(
                SavingSuggestion(
                    subscriptionId = group.first().id,
                    subscriptionName = names,
                    type = "DUPLICATE",
                    description = "您有多个$typeName：$names，考虑保留一个",
                    potentialSavings = group.drop(1).sumOf {
                        when (it.billingCycle) {
                            "MONTHLY" -> it.amount * 12
                            "QUARTERLY" -> it.amount * 4
                            "YEARLY" -> it.amount
                            else -> it.amount * 12
                        }
                    }
                )
            )
        }

        return suggestions.sortedByDescending { it.potentialSavings }
    }
}

// ==================== 数据模型 ====================

data class SubscriptionType(
    val id: String,
    val name: String,
    val icon: String,
    val commonServices: List<String>
)

data class BillingCycle(
    val id: String,
    val name: String,
    val months: Int
)

data class SubscriptionOverview(
    val activeCount: Int,
    val monthlyTotal: Double,
    val yearlyTotal: Double,
    val upcomingCount: Int,
    val upcomingAmount: Double,
    val typeDistribution: List<TypeDistribution>
)

data class TypeDistribution(
    val type: String,
    val count: Int,
    val yearlyTotal: Double
)

data class SavingSuggestion(
    val subscriptionId: Long,
    val subscriptionName: String,
    val type: String,
    val description: String,
    val potentialSavings: Double
)
