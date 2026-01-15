package com.lifemanager.app.core.database

import com.lifemanager.app.core.database.entity.*

/**
 * 预设数据定义
 *
 * 包含应用初始化时需要插入的所有预设类别
 * 这些预设项不可删除，但可以禁用
 *
 * 分类参考微信/支付宝常见分类
 */
object PresetData {

    /**
     * 收入类别预设
     *
     * 主要分类：
     * - 工资：主要工作收入
     * - 非工资收入：其他所有收入来源
     */
    val incomeFields = listOf(
        // 核心收入分类
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "工资",
            iconName = "work",
            color = "#4CAF50",
            tagType = TagType.OTHER,
            sortOrder = 1,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "非工资收入",
            iconName = "attach_money",
            color = "#2196F3",
            tagType = TagType.OTHER,
            sortOrder = 2,
            isPreset = true
        ),
        // 细分收入分类
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "奖金补贴",
            iconName = "emoji_events",
            color = "#8BC34A",
            tagType = TagType.OTHER,
            sortOrder = 3,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "兼职外快",
            iconName = "business_center",
            color = "#CDDC39",
            tagType = TagType.OTHER,
            sortOrder = 4,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "投资收益",
            iconName = "trending_up",
            color = "#FF9800",
            tagType = TagType.INVESTMENT,
            sortOrder = 5,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "生意收入",
            iconName = "store",
            color = "#3F51B5",
            tagType = TagType.OTHER,
            sortOrder = 6,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "红包收入",
            iconName = "card_giftcard",
            color = "#E91E63",
            tagType = TagType.OTHER,
            sortOrder = 7,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "转账收入",
            iconName = "swap_horiz",
            color = "#9C27B0",
            tagType = TagType.OTHER,
            sortOrder = 8,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "退款退货",
            iconName = "replay",
            color = "#00BCD4",
            tagType = TagType.OTHER,
            sortOrder = 9,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "报销款项",
            iconName = "receipt",
            color = "#5C6BC0",
            tagType = TagType.OTHER,
            sortOrder = 10,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "租金收入",
            iconName = "real_estate_agent",
            color = "#FF7043",
            tagType = TagType.OTHER,
            sortOrder = 11,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "理财利息",
            iconName = "savings",
            color = "#26A69A",
            tagType = TagType.INVESTMENT,
            sortOrder = 12,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.INCOME,
            name = "其他收入",
            iconName = "more_horiz",
            color = "#9E9E9E",
            tagType = TagType.OTHER,
            sortOrder = 99,
            isPreset = true
        )
    )

    /**
     * 支出类别预设
     *
     * 主要分类：
     * - 养老金：养老储蓄支出
     * - 现金：现金保留
     * - 定投：定期投资支出
     * - 日常开销：日常消费支出
     * - 额外开销：大额或非日常支出
     * - 余额：月末剩余金额
     */
    val expenseFields = listOf(
        // 核心支出分类（月度预算分配）
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "养老金",
            iconName = "elderly",
            color = "#3F51B5",
            tagType = TagType.SAVINGS,
            sortOrder = 1,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "现金",
            iconName = "account_balance_wallet",
            color = "#4CAF50",
            tagType = TagType.SAVINGS,
            sortOrder = 2,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "定投",
            iconName = "trending_up",
            color = "#FF9800",
            tagType = TagType.INVESTMENT,
            sortOrder = 3,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "日常开销",
            iconName = "shopping_cart",
            color = "#2196F3",
            tagType = TagType.CONSUMPTION,
            sortOrder = 4,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "额外开销",
            iconName = "add_shopping_cart",
            color = "#F44336",
            tagType = TagType.CONSUMPTION,
            sortOrder = 5,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "余额",
            iconName = "savings",
            color = "#8BC34A",
            tagType = TagType.SAVINGS,
            sortOrder = 6,
            isPreset = true
        ),
        // 细分支出分类
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "餐饮美食",
            iconName = "restaurant",
            color = "#FF5722",
            tagType = TagType.CONSUMPTION,
            sortOrder = 10,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "交通出行",
            iconName = "directions_car",
            color = "#00BCD4",
            tagType = TagType.CONSUMPTION,
            sortOrder = 11,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "日常购物",
            iconName = "shopping_bag",
            color = "#9C27B0",
            tagType = TagType.CONSUMPTION,
            sortOrder = 12,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "服饰美容",
            iconName = "checkroom",
            color = "#E91E63",
            tagType = TagType.CONSUMPTION,
            sortOrder = 13,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "生活服务",
            iconName = "home_repair_service",
            color = "#00ACC1",
            tagType = TagType.CONSUMPTION,
            sortOrder = 14,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "医疗健康",
            iconName = "local_hospital",
            color = "#66BB6A",
            tagType = TagType.CONSUMPTION,
            sortOrder = 15,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "文化娱乐",
            iconName = "sports_esports",
            color = "#673AB7",
            tagType = TagType.CONSUMPTION,
            sortOrder = 16,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "教育培训",
            iconName = "school",
            color = "#5C6BC0",
            tagType = TagType.CONSUMPTION,
            sortOrder = 17,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "人情往来",
            iconName = "card_giftcard",
            color = "#E53935",
            tagType = TagType.CONSUMPTION,
            sortOrder = 18,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "住房支出",
            iconName = "home",
            color = "#795548",
            tagType = TagType.FIXED,
            sortOrder = 19,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "金融保险",
            iconName = "account_balance",
            color = "#607D8B",
            tagType = TagType.OTHER,
            sortOrder = 20,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "通讯服务",
            iconName = "wifi",
            color = "#009688",
            tagType = TagType.FIXED,
            sortOrder = 21,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.EXPENSE,
            name = "其他支出",
            iconName = "more_horiz",
            color = "#9E9E9E",
            tagType = TagType.CONSUMPTION,
            sortOrder = 99,
            isPreset = true
        )
    )

    /**
     * 资产类别预设
     */
    val assetFields = listOf(
        // 现金类
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "现金(支付宝)",
            iconName = "account_balance_wallet",
            color = "#1677FF",
            tagType = TagType.SAVINGS,
            sortOrder = 1,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "现金(微信)",
            iconName = "account_balance_wallet",
            color = "#07C160",
            tagType = TagType.SAVINGS,
            sortOrder = 2,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "活期存款",
            iconName = "account_balance",
            color = "#2196F3",
            tagType = TagType.SAVINGS,
            sortOrder = 3,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "定期存款",
            iconName = "lock",
            color = "#1976D2",
            tagType = TagType.SAVINGS,
            sortOrder = 4,
            isPreset = true
        ),
        // 养老金类
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "养老金(支付宝)",
            iconName = "elderly",
            color = "#3F51B5",
            tagType = TagType.SAVINGS,
            sortOrder = 5,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "公积金余额",
            iconName = "account_balance",
            color = "#1E88E5",
            tagType = TagType.SAVINGS,
            sortOrder = 6,
            isPreset = true
        ),
        // 副业收入
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "副业收入",
            iconName = "business_center",
            color = "#FF9800",
            tagType = TagType.OTHER,
            sortOrder = 7,
            isPreset = true
        ),
        // 理财投资类
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "货币基金",
            iconName = "monetization_on",
            color = "#00BCD4",
            tagType = TagType.SAVINGS,
            sortOrder = 8,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "股票",
            iconName = "show_chart",
            color = "#E91E63",
            tagType = TagType.INVESTMENT,
            sortOrder = 9,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "基金",
            iconName = "pie_chart",
            color = "#9C27B0",
            tagType = TagType.INVESTMENT,
            sortOrder = 10,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "理财产品",
            iconName = "savings",
            color = "#4CAF50",
            tagType = TagType.INVESTMENT,
            sortOrder = 11,
            isPreset = true
        ),
        // 可用资金
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "可用资金",
            iconName = "attach_money",
            color = "#8BC34A",
            tagType = TagType.SAVINGS,
            sortOrder = 12,
            isPreset = true
        ),
        // 固定资产
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "房产",
            iconName = "home",
            color = "#FF5722",
            tagType = TagType.OTHER,
            sortOrder = 13,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "车辆",
            iconName = "directions_car",
            color = "#607D8B",
            tagType = TagType.OTHER,
            sortOrder = 14,
            isPreset = true
        ),
        // 其他投资
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "数字货币",
            iconName = "currency_bitcoin",
            color = "#F7931A",
            tagType = TagType.INVESTMENT,
            sortOrder = 15,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "保险产品",
            iconName = "shield",
            color = "#5C6BC0",
            tagType = TagType.OTHER,
            sortOrder = 16,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "债券",
            iconName = "request_quote",
            color = "#00897B",
            tagType = TagType.INVESTMENT,
            sortOrder = 17,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "黄金贵金属",
            iconName = "diamond",
            color = "#FFD700",
            tagType = TagType.INVESTMENT,
            sortOrder = 18,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.ASSET,
            name = "收藏品",
            iconName = "collections",
            color = "#8D6E63",
            tagType = TagType.OTHER,
            sortOrder = 19,
            isPreset = true
        )
    )

    /**
     * 负债类别预设
     * 支持在备注中填写欠款说明
     */
    val liabilityFields = listOf(
        // 贷款类
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "房贷",
            iconName = "home",
            color = "#F44336",
            tagType = TagType.FIXED,
            sortOrder = 1,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "车贷",
            iconName = "directions_car",
            color = "#E91E63",
            tagType = TagType.FIXED,
            sortOrder = 2,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "消费贷",
            iconName = "credit_score",
            color = "#FF7043",
            tagType = TagType.OTHER,
            sortOrder = 3,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "学生贷款",
            iconName = "school",
            color = "#66BB6A",
            tagType = TagType.FIXED,
            sortOrder = 4,
            isPreset = true
        ),
        // 信用类
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "信用卡",
            iconName = "credit_card",
            color = "#9C27B0",
            tagType = TagType.OTHER,
            sortOrder = 5,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "花呗",
            iconName = "payment",
            color = "#1677FF",
            tagType = TagType.OTHER,
            sortOrder = 6,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "白条",
            iconName = "payment",
            color = "#E62117",
            tagType = TagType.OTHER,
            sortOrder = 7,
            isPreset = true
        ),
        // 欠款类（支持备注说明）
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "欠款",
            iconName = "handshake",
            color = "#795548",
            tagType = TagType.OTHER,
            sortOrder = 8,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "亲友借款",
            iconName = "people",
            color = "#FF9800",
            tagType = TagType.OTHER,
            sortOrder = 9,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.LIABILITY,
            name = "其他负债",
            iconName = "more_horiz",
            color = "#9E9E9E",
            tagType = TagType.OTHER,
            sortOrder = 99,
            isPreset = true
        )
    )

    /**
     * 月度开销类别预设
     */
    val monthlyExpenseFields = listOf(
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "房租/房贷",
            iconName = "home",
            color = "#F44336",
            tagType = TagType.FIXED,
            sortOrder = 1,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "水电燃气",
            iconName = "bolt",
            color = "#FF9800",
            tagType = TagType.FIXED,
            sortOrder = 2,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "物业费",
            iconName = "apartment",
            color = "#FF5722",
            tagType = TagType.FIXED,
            sortOrder = 3,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "交通出行",
            iconName = "directions_car",
            color = "#2196F3",
            tagType = TagType.OTHER,
            sortOrder = 4,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "餐饮伙食",
            iconName = "restaurant",
            color = "#4CAF50",
            tagType = TagType.OTHER,
            sortOrder = 5,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "日用品",
            iconName = "shopping_basket",
            color = "#9C27B0",
            tagType = TagType.OTHER,
            sortOrder = 6,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "通讯网络",
            iconName = "wifi",
            color = "#00BCD4",
            tagType = TagType.FIXED,
            sortOrder = 7,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "医疗保健",
            iconName = "local_hospital",
            color = "#E91E63",
            tagType = TagType.OTHER,
            sortOrder = 8,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "娱乐休闲",
            iconName = "sports_esports",
            color = "#673AB7",
            tagType = TagType.OTHER,
            sortOrder = 9,
            isPreset = true
        ),
        CustomFieldEntity(
            moduleType = ModuleType.MONTHLY_EXPENSE,
            name = "教育学习",
            iconName = "school",
            color = "#3F51B5",
            tagType = TagType.OTHER,
            sortOrder = 10,
            isPreset = true
        )
    )

    /**
     * 时间统计分类预设
     */
    val timeCategories = listOf(
        TimeCategoryEntity(
            name = "工作",
            iconName = "work",
            color = "#2196F3",
            sortOrder = 1
        ),
        TimeCategoryEntity(
            name = "学习",
            iconName = "school",
            color = "#4CAF50",
            sortOrder = 2
        ),
        TimeCategoryEntity(
            name = "运动",
            iconName = "fitness_center",
            color = "#FF9800",
            sortOrder = 3
        ),
        TimeCategoryEntity(
            name = "娱乐",
            iconName = "sports_esports",
            color = "#9C27B0",
            sortOrder = 4
        ),
        TimeCategoryEntity(
            name = "休息",
            iconName = "hotel",
            color = "#607D8B",
            sortOrder = 5
        ),
        TimeCategoryEntity(
            name = "社交",
            iconName = "people",
            color = "#E91E63",
            sortOrder = 6
        ),
        TimeCategoryEntity(
            name = "其他",
            iconName = "more_horiz",
            color = "#9E9E9E",
            sortOrder = 99
        )
    )

    /**
     * 获取所有自定义字段预设
     */
    fun getAllCustomFields(): List<CustomFieldEntity> {
        return incomeFields + expenseFields + assetFields + liabilityFields + monthlyExpenseFields
    }
}
