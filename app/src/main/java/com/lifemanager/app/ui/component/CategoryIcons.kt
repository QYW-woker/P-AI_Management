package com.lifemanager.app.ui.component

/**
 * 分类卡通图标映射
 *
 * 将分类名称或iconName映射到对应的emoji图标
 * 提供统一的卡通风格图标显示
 */
object CategoryIcons {

    /**
     * 收入类分类图标
     */
    private val incomeIcons = mapOf(
        "工资薪酬" to "💰",
        "奖金补贴" to "🏆",
        "兼职外快" to "💼",
        "投资收益" to "📈",
        "生意收入" to "🏪",
        "红包收入" to "🧧",
        "转账收入" to "💸",
        "退款退货" to "↩️",
        "报销款项" to "🧾",
        "租金收入" to "🏘️",
        "理财利息" to "💹",
        "中奖收入" to "🎰",
        "公积金提取" to "🏛️",
        "其他收入" to "💵",
        "work" to "💰",
        "emoji_events" to "🏆",
        "business_center" to "💼",
        "trending_up" to "📈",
        "store" to "🏪",
        "card_giftcard" to "🧧",
        "swap_horiz" to "💸",
        "replay" to "↩️",
        "receipt" to "🧾",
        "real_estate_agent" to "🏘️",
        "savings" to "💹",
        "casino" to "🎰",
        "account_balance_wallet" to "🏛️"
    )

    /**
     * 支出类分类图标
     */
    private val expenseIcons = mapOf(
        "餐饮美食" to "🍜",
        "交通出行" to "🚗",
        "日常购物" to "🛒",
        "服饰美容" to "👗",
        "生活服务" to "🔧",
        "医疗健康" to "🏥",
        "文化娱乐" to "🎮",
        "教育培训" to "📚",
        "人情往来" to "🎁",
        "通讯服务" to "📱",
        "住房支出" to "🏠",
        "金融保险" to "🏦",
        "宠物" to "🐾",
        "数码电子" to "💻",
        "旅行度假" to "✈️",
        "汽车养护" to "🔧",
        "运动健身" to "🏋️",
        "订阅服务" to "📺",
        "育儿母婴" to "👶",
        "书籍阅读" to "📖",
        "烟酒茶" to "🍷",
        "礼物送礼" to "🎀",
        "转账支出" to "💳",
        "其他支出" to "📝",
        "restaurant" to "🍜",
        "directions_car" to "🚗",
        "shopping_bag" to "🛒",
        "checkroom" to "👗",
        "home_repair_service" to "🔧",
        "local_hospital" to "🏥",
        "sports_esports" to "🎮",
        "school" to "📚",
        "wifi" to "📱",
        "home" to "🏠",
        "account_balance" to "🏦",
        "pets" to "🐾",
        "devices" to "💻",
        "credit_card" to "💳",
        "flight" to "✈️",
        "car_repair" to "🔧",
        "fitness_center" to "🏋️",
        "subscriptions" to "📺",
        "child_care" to "👶",
        "menu_book" to "📖",
        "local_bar" to "🍷",
        "redeem" to "🎀"
    )

    /**
     * 资产类分类图标
     */
    private val assetIcons = mapOf(
        "活期存款" to "💳",
        "定期存款" to "🔐",
        "货币基金" to "💵",
        "股票" to "📊",
        "基金" to "📈",
        "养老金账户" to "👴",
        "房产" to "🏡",
        "车辆" to "🚙",
        "数字货币" to "₿",
        "保险产品" to "🛡️",
        "债券" to "📃",
        "黄金贵金属" to "🥇",
        "收藏品" to "🖼️",
        "公积金余额" to "🏛️",
        "lock" to "🔐",
        "monetization_on" to "💵",
        "show_chart" to "📊",
        "pie_chart" to "📈",
        "elderly" to "👴",
        "currency_bitcoin" to "₿",
        "shield" to "🛡️",
        "request_quote" to "📃",
        "diamond" to "🥇",
        "collections" to "🖼️"
    )

    /**
     * 负债类分类图标
     */
    private val liabilityIcons = mapOf(
        "房贷" to "🏠",
        "车贷" to "🚗",
        "信用卡" to "💳",
        "借款" to "🤝",
        "消费贷" to "💸",
        "花呗白条" to "📲",
        "学生贷款" to "🎓",
        "handshake" to "🤝",
        "credit_score" to "💸",
        "payment" to "📲"
    )

    /**
     * 月度开销类分类图标
     */
    private val monthlyExpenseIcons = mapOf(
        "房租/房贷" to "🏠",
        "水电燃气" to "💡",
        "物业费" to "🏢",
        "交通出行" to "🚌",
        "餐饮伙食" to "🍚",
        "日用品" to "🧴",
        "通讯网络" to "📶",
        "医疗保健" to "💊",
        "娱乐休闲" to "🎬",
        "教育学习" to "📖",
        "bolt" to "💡",
        "apartment" to "🏢",
        "shopping_basket" to "🧴",
        "hotel" to "🛏️"
    )

    /**
     * 时间分类图标
     */
    private val timeIcons = mapOf(
        "工作" to "💼",
        "学习" to "📖",
        "运动" to "🏃",
        "娱乐" to "🎮",
        "休息" to "😴",
        "社交" to "👥",
        "其他" to "📋",
        "fitness_center" to "🏃",
        "people" to "👥"
    )

    /**
     * 习惯分类图标
     */
    private val habitIcons = mapOf(
        "健身" to "💪",
        "阅读" to "📚",
        "冥想" to "🧘",
        "早起" to "⏰",
        "喝水" to "💧",
        "写日记" to "📝",
        "散步" to "🚶",
        "睡眠" to "😴"
    )

    /**
     * 默认图标 - 按模块类型
     */
    private val defaultIcons = mapOf(
        "INCOME" to "💰",
        "EXPENSE" to "💸",
        "ASSET" to "🏦",
        "LIABILITY" to "📋",
        "MONTHLY_EXPENSE" to "📅",
        "TIME" to "⏰",
        "HABIT" to "✨"
    )

    /**
     * 获取分类对应的emoji图标
     *
     * @param name 分类名称
     * @param iconName Material Icon名称（可选）
     * @param moduleType 模块类型（可选，用于默认图标）
     * @return emoji图标字符串
     */
    fun getIcon(
        name: String,
        iconName: String? = null,
        moduleType: String? = null
    ): String {
        // 优先按名称匹配
        incomeIcons[name]?.let { return it }
        expenseIcons[name]?.let { return it }
        assetIcons[name]?.let { return it }
        liabilityIcons[name]?.let { return it }
        monthlyExpenseIcons[name]?.let { return it }
        timeIcons[name]?.let { return it }
        habitIcons[name]?.let { return it }

        // 其次按iconName匹配
        if (iconName != null) {
            incomeIcons[iconName]?.let { return it }
            expenseIcons[iconName]?.let { return it }
            assetIcons[iconName]?.let { return it }
            liabilityIcons[iconName]?.let { return it }
            monthlyExpenseIcons[iconName]?.let { return it }
            timeIcons[iconName]?.let { return it }
        }

        // 返回模块默认图标或通用默认
        return moduleType?.let { defaultIcons[it] } ?: "📋"
    }

    /**
     * 获取收入图标
     */
    fun getIncomeIcon(name: String, iconName: String? = null): String {
        return incomeIcons[name]
            ?: iconName?.let { incomeIcons[it] }
            ?: "💰"
    }

    /**
     * 获取支出图标
     */
    fun getExpenseIcon(name: String, iconName: String? = null): String {
        return expenseIcons[name]
            ?: iconName?.let { expenseIcons[it] }
            ?: "💸"
    }

    /**
     * 获取资产图标
     */
    fun getAssetIcon(name: String, iconName: String? = null): String {
        return assetIcons[name]
            ?: iconName?.let { assetIcons[it] }
            ?: "🏦"
    }

    /**
     * 获取负债图标
     */
    fun getLiabilityIcon(name: String, iconName: String? = null): String {
        return liabilityIcons[name]
            ?: iconName?.let { liabilityIcons[it] }
            ?: "📋"
    }

    /**
     * 获取时间分类图标
     */
    fun getTimeIcon(name: String, iconName: String? = null): String {
        return timeIcons[name]
            ?: iconName?.let { timeIcons[it] }
            ?: "⏰"
    }

    /**
     * 获取习惯图标
     */
    fun getHabitIcon(name: String, iconName: String? = null): String {
        return habitIcons[name]
            ?: iconName?.let { habitIcons[it] }
            ?: "✨"
    }
}
