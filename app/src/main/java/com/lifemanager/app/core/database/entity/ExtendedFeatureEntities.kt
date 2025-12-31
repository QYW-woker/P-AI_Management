package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 订阅服务实体
 *
 * 追踪用户的各类订阅服务（视频会员、软件订阅、健身房等）
 */
@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FundAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["status"]),
        Index(value = ["nextBillingDate"])
    ]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 订阅名称
    val name: String,

    // 服务提供商
    val provider: String? = null,

    // 订阅类型
    val type: String = SubscriptionType.OTHER,

    // 订阅金额
    val amount: Double,

    // 计费周期: MONTHLY, QUARTERLY, YEARLY, WEEKLY
    val billingCycle: String = BillingCycle.MONTHLY,

    // 分类ID
    val categoryId: Long? = null,

    // 扣款账户ID
    val accountId: Long? = null,

    // 订阅开始日期（epochDay）
    val startDate: Int,

    // 下次扣款日期（epochDay）
    val nextBillingDate: Int,

    // 订阅结束日期（epochDay，可选）
    val endDate: Int? = null,

    // 状态: ACTIVE, PAUSED, CANCELLED, EXPIRED
    val status: String = SubscriptionStatus.ACTIVE,

    // 是否自动续费
    val autoRenew: Boolean = true,

    // 提前提醒天数
    val reminderDays: Int = 3,

    // 是否启用提醒
    val reminderEnabled: Boolean = true,

    // 订阅图标（预设图标名或自定义URL）
    val icon: String? = null,

    // 订阅颜色
    val color: String? = null,

    // 服务URL/官网
    val serviceUrl: String? = null,

    // 账号信息（加密存储）
    val accountInfo: String? = null,

    // 备注
    val note: String = "",

    // 标签（JSON数组）
    val tags: String = "[]",

    // 累计已付金额
    val totalPaid: Double = 0.0,

    // 扣款次数
    val paymentCount: Int = 0,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 计算年度费用
     */
    fun getYearlyCost(): Double = when (billingCycle) {
        BillingCycle.WEEKLY -> amount * 52
        BillingCycle.MONTHLY -> amount * 12
        BillingCycle.QUARTERLY -> amount * 4
        BillingCycle.YEARLY -> amount
        else -> amount * 12
    }

    /**
     * 计算月均费用
     */
    fun getMonthlyCost(): Double = getYearlyCost() / 12
}

object SubscriptionType {
    const val VIDEO = "VIDEO"                   // 视频会员（爱奇艺、优酷、Netflix等）
    const val MUSIC = "MUSIC"                   // 音乐会员（QQ音乐、网易云、Spotify等）
    const val STORAGE = "STORAGE"               // 云存储（iCloud、百度网盘等）
    const val SOFTWARE = "SOFTWARE"             // 软件订阅（Office365、Adobe等）
    const val GAMING = "GAMING"                 // 游戏会员（PlayStation、Xbox、Steam等）
    const val FITNESS = "FITNESS"               // 健身会员（健身房、Keep等）
    const val LEARNING = "LEARNING"             // 学习平台（得到、知乎盐选等）
    const val NEWS = "NEWS"                     // 新闻资讯（财新、华尔街日报等）
    const val SHOPPING = "SHOPPING"             // 购物会员（京东Plus、淘宝88等）
    const val TELECOM = "TELECOM"               // 电信服务（手机话费、宽带等）
    const val UTILITY = "UTILITY"               // 公共服务（水电燃气等）
    const val INSURANCE = "INSURANCE"           // 保险
    const val OTHER = "OTHER"                   // 其他

    fun getDisplayName(type: String): String = when (type) {
        VIDEO -> "视频会员"
        MUSIC -> "音乐会员"
        STORAGE -> "云存储"
        SOFTWARE -> "软件订阅"
        GAMING -> "游戏会员"
        FITNESS -> "健身会员"
        LEARNING -> "学习平台"
        NEWS -> "新闻资讯"
        SHOPPING -> "购物会员"
        TELECOM -> "电信服务"
        UTILITY -> "公共服务"
        INSURANCE -> "保险"
        else -> "其他"
    }

    fun getIcon(type: String): String = when (type) {
        VIDEO -> "Videocam"
        MUSIC -> "MusicNote"
        STORAGE -> "Cloud"
        SOFTWARE -> "Computer"
        GAMING -> "SportsEsports"
        FITNESS -> "FitnessCenter"
        LEARNING -> "School"
        NEWS -> "Article"
        SHOPPING -> "ShoppingBag"
        TELECOM -> "PhoneAndroid"
        UTILITY -> "Home"
        INSURANCE -> "Security"
        else -> "Subscriptions"
    }

    fun getAllTypes(): List<Pair<String, String>> = listOf(
        VIDEO to "视频会员",
        MUSIC to "音乐会员",
        STORAGE to "云存储",
        SOFTWARE to "软件订阅",
        GAMING to "游戏会员",
        FITNESS to "健身会员",
        LEARNING to "学习平台",
        NEWS to "新闻资讯",
        SHOPPING to "购物会员",
        TELECOM to "电信服务",
        UTILITY to "公共服务",
        INSURANCE to "保险",
        OTHER to "其他"
    )
}

object BillingCycle {
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val QUARTERLY = "QUARTERLY"
    const val YEARLY = "YEARLY"

    fun getDisplayName(cycle: String): String = when (cycle) {
        WEEKLY -> "每周"
        MONTHLY -> "每月"
        QUARTERLY -> "每季度"
        YEARLY -> "每年"
        else -> "每月"
    }

    fun getAllCycles(): List<Pair<String, String>> = listOf(
        WEEKLY to "每周",
        MONTHLY to "每月",
        QUARTERLY to "每季度",
        YEARLY to "每年"
    )
}

object SubscriptionStatus {
    const val ACTIVE = "ACTIVE"         // 活跃
    const val PAUSED = "PAUSED"         // 暂停
    const val CANCELLED = "CANCELLED"   // 已取消
    const val EXPIRED = "EXPIRED"       // 已过期

    fun getDisplayName(status: String): String = when (status) {
        ACTIVE -> "活跃"
        PAUSED -> "暂停"
        CANCELLED -> "已取消"
        EXPIRED -> "已过期"
        else -> "未知"
    }
}

/**
 * 订阅付款记录实体
 */
@Entity(
    tableName = "subscription_payments",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DailyTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["subscriptionId"]),
        Index(value = ["transactionId"]),
        Index(value = ["paymentDate"])
    ]
)
data class SubscriptionPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 订阅ID
    val subscriptionId: Long,

    // 支付金额
    val amount: Double,

    // 支付日期（epochDay）
    val paymentDate: Int,

    // 关联交易记录ID
    val transactionId: Long? = null,

    // 状态: PAID, FAILED, PENDING
    val status: String = "PAID",

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 成就/徽章实体
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 成就代码（唯一标识）
    val code: String,

    // 成就名称
    val name: String,

    // 成就描述
    val description: String,

    // 成就类型
    val type: String = AchievementType.GENERAL,

    // 成就图标
    val icon: String,

    // 成就颜色
    val color: String = "#FFD700",

    // 是否已解锁
    val isUnlocked: Boolean = false,

    // 解锁时间
    val unlockedAt: Long? = null,

    // 当前进度（0-100）
    val progress: Int = 0,

    // 目标值
    val targetValue: Int = 1,

    // 当前值
    val currentValue: Int = 0,

    // 经验值奖励
    val expReward: Int = 0,

    // 是否隐藏（解锁前不显示）
    val isHidden: Boolean = false,

    // 排序顺序
    val sortOrder: Int = 0
)

object AchievementType {
    const val FINANCE = "FINANCE"       // 财务相关
    const val HABIT = "HABIT"           // 习惯相关
    const val GOAL = "GOAL"             // 目标相关
    const val SAVINGS = "SAVINGS"       // 存钱相关
    const val STREAK = "STREAK"         // 连续打卡
    const val MILESTONE = "MILESTONE"   // 里程碑
    const val GENERAL = "GENERAL"       // 通用

    fun getDisplayName(type: String): String = when (type) {
        FINANCE -> "理财达人"
        HABIT -> "习惯养成"
        GOAL -> "目标达成"
        SAVINGS -> "存钱高手"
        STREAK -> "坚持不懈"
        MILESTONE -> "里程碑"
        else -> "通用成就"
    }
}

/**
 * 用户等级实体
 */
@Entity(tableName = "user_levels")
data class UserLevelEntity(
    @PrimaryKey
    val id: Long = 1,  // 单用户，固定ID

    // 当前等级
    val level: Int = 1,

    // 当前经验值
    val currentExp: Int = 0,

    // 升级所需经验值
    val expToNextLevel: Int = 100,

    // 总经验值
    val totalExp: Int = 0,

    // 称号
    val title: String = "新手管家",

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取等级称号
     */
    companion object {
        fun getTitleForLevel(level: Int): String = when {
            level < 5 -> "新手管家"
            level < 10 -> "生活达人"
            level < 20 -> "理财高手"
            level < 30 -> "自律大师"
            level < 50 -> "人生赢家"
            level < 100 -> "传奇管家"
            else -> "至尊管家"
        }

        fun getExpForLevel(level: Int): Int = when {
            level < 5 -> 100
            level < 10 -> 200
            level < 20 -> 500
            level < 50 -> 1000
            else -> 2000
        }
    }
}

/**
 * 多币种汇率实体
 */
@Entity(
    tableName = "currency_rates",
    indices = [Index(value = ["currencyCode"], unique = true)]
)
data class CurrencyRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 货币代码 (USD, EUR, JPY, etc.)
    val currencyCode: String,

    // 货币名称
    val currencyName: String,

    // 货币符号
    val symbol: String,

    // 对人民币汇率
    val rateToBaseCurrency: Double,

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 常用货币预设
 */
object CurrencyPresets {
    val currencies = listOf(
        Triple("CNY", "人民币", "¥"),
        Triple("USD", "美元", "$"),
        Triple("EUR", "欧元", "€"),
        Triple("JPY", "日元", "¥"),
        Triple("GBP", "英镑", "£"),
        Triple("HKD", "港币", "HK$"),
        Triple("TWD", "新台币", "NT$"),
        Triple("KRW", "韩元", "₩"),
        Triple("SGD", "新加坡元", "S$"),
        Triple("AUD", "澳元", "A$"),
        Triple("CAD", "加元", "C$"),
        Triple("CHF", "瑞士法郎", "Fr"),
        Triple("THB", "泰铢", "฿"),
        Triple("MYR", "马来西亚林吉特", "RM"),
        Triple("RUB", "俄罗斯卢布", "₽")
    )

    fun getSymbol(code: String): String =
        currencies.find { it.first == code }?.third ?: code
}

/**
 * 用户货币设置
 */
@Entity(tableName = "user_currency_settings")
data class UserCurrencySettingEntity(
    @PrimaryKey
    val id: Long = 1,

    // 主货币
    val primaryCurrency: String = "CNY",

    // 启用的辅助货币（JSON数组）
    val enabledCurrencies: String = "[\"USD\", \"EUR\"]",

    // 是否显示汇率转换
    val showConversion: Boolean = false,

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 饮水记录实体
 */
@Entity(
    tableName = "water_intake_records",
    indices = [Index(value = ["date"])]
)
data class WaterIntakeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 日期（epochDay）
    val date: Int,

    // 饮水量（毫升）
    val amount: Int,

    // 饮水时间
    val time: String = "",

    // 饮品类型: WATER, TEA, COFFEE, JUICE, MILK, OTHER
    val drinkType: String = DrinkType.WATER,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)

object DrinkType {
    const val WATER = "WATER"       // 白开水
    const val TEA = "TEA"           // 茶
    const val COFFEE = "COFFEE"     // 咖啡
    const val JUICE = "JUICE"       // 果汁
    const val MILK = "MILK"         // 牛奶
    const val SODA = "SODA"         // 碳酸饮料
    const val OTHER = "OTHER"       // 其他

    fun getDisplayName(type: String): String = when (type) {
        WATER -> "白开水"
        TEA -> "茶"
        COFFEE -> "咖啡"
        JUICE -> "果汁"
        MILK -> "牛奶"
        SODA -> "碳酸饮料"
        else -> "其他"
    }

    fun getIcon(type: String): String = when (type) {
        WATER -> "WaterDrop"
        TEA -> "EmojiFoodBeverage"
        COFFEE -> "Coffee"
        JUICE -> "LocalBar"
        MILK -> "LocalDrink"
        SODA -> "BubbleChart"
        else -> "LocalDrink"
    }
}

/**
 * 睡眠记录实体
 */
@Entity(
    tableName = "sleep_records",
    indices = [Index(value = ["date"])]
)
data class SleepRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 日期（epochDay，记录的是睡眠结束的日期）
    val date: Int,

    // 入睡时间 (HH:mm)
    val sleepTime: String,

    // 起床时间 (HH:mm)
    val wakeTime: String,

    // 睡眠时长（分钟）
    val duration: Int,

    // 睡眠质量 (1-5)
    val quality: Int = 3,

    // 是否午睡
    val isNap: Boolean = false,

    // 备注
    val note: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取睡眠时长描述
     */
    fun getDurationText(): String {
        val hours = duration / 60
        val minutes = duration % 60
        return if (minutes > 0) "${hours}小时${minutes}分钟" else "${hours}小时"
    }

    /**
     * 获取睡眠质量描述
     */
    fun getQualityText(): String = when (quality) {
        1 -> "很差"
        2 -> "较差"
        3 -> "一般"
        4 -> "良好"
        5 -> "优秀"
        else -> "一般"
    }
}

/**
 * 每日健康目标设置
 */
@Entity(tableName = "health_goals")
data class HealthGoalEntity(
    @PrimaryKey
    val id: Long = 1,

    // 每日饮水目标（毫升）
    val dailyWaterGoal: Int = 2000,

    // 每日睡眠目标（小时）
    val dailySleepGoal: Double = 8.0,

    // 每日步数目标
    val dailyStepsGoal: Int = 8000,

    // 每日运动目标（分钟）
    val dailyExerciseGoal: Int = 30,

    // 目标体重
    val targetWeight: Double? = null,

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)
