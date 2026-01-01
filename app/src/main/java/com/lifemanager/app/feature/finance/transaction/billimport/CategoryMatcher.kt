package com.lifemanager.app.feature.finance.transaction.billimport

import com.lifemanager.app.core.database.entity.CustomFieldEntity

/**
 * 智能分类匹配器
 *
 * 根据交易对方和商品名称自动匹配分类
 */
class CategoryMatcher {

    companion object {
        /**
         * 支出分类匹配规则（按优先级排序）
         * 关键词 -> 分类名称
         */
        private val EXPENSE_RULES = listOf(
            // 餐饮美食
            CategoryMatchRule(
                keywords = listOf(
                    "美团", "饿了么", "肯德基", "麦当劳", "星巴克", "瑞幸",
                    "必胜客", "海底捞", "外卖", "餐厅", "餐饮", "食堂", "早餐",
                    "午餐", "晚餐", "夜宵", "奶茶", "咖啡", "蛋糕", "烘焙",
                    "火锅", "烧烤", "小吃", "面馆", "饭店", "酒楼",
                    "喜茶", "奈雪", "茶百道", "蜜雪冰城", "古茗"
                ),
                categoryName = "餐饮美食"
            ),
            // 交通出行
            CategoryMatchRule(
                keywords = listOf(
                    "滴滴", "高德", "出租车", "公交", "地铁", "火车票", "机票",
                    "12306", "携程", "去哪儿", "飞猪", "航空", "铁路",
                    "加油", "停车", "过路费", "高速", "ETC", "共享单车",
                    "哈啰", "青桔", "美团单车", "货拉拉", "搬家"
                ),
                categoryName = "交通出行"
            ),
            // 日常购物
            CategoryMatchRule(
                keywords = listOf(
                    "超市", "便利店", "711", "全家", "罗森", "盒马", "叮咚",
                    "多多买菜", "美团优选", "淘鲜达", "永辉", "大润发",
                    "沃尔玛", "家乐福", "物美", "华润万家", "菜市场",
                    "水果", "蔬菜", "生鲜", "日用品"
                ),
                categoryName = "日常购物"
            ),
            // 服饰美容
            CategoryMatchRule(
                keywords = listOf(
                    "优衣库", "ZARA", "HM", "UR", "服装", "鞋", "包",
                    "化妆品", "护肤", "美妆", "丝芙兰", "屈臣氏", "名创优品",
                    "理发", "美发", "美甲", "美容院", "SPA"
                ),
                categoryName = "服饰美容"
            ),
            // 生活服务
            CategoryMatchRule(
                keywords = listOf(
                    "物业", "水费", "电费", "燃气", "煤气", "暖气",
                    "维修", "家政", "保洁", "洗衣", "干洗", "快递费"
                ),
                categoryName = "生活服务"
            ),
            // 医疗健康
            CategoryMatchRule(
                keywords = listOf(
                    "医院", "诊所", "药房", "药店", "体检", "挂号",
                    "门诊", "住院", "医疗", "健康", "保健", "药品"
                ),
                categoryName = "医疗健康"
            ),
            // 文化娱乐
            CategoryMatchRule(
                keywords = listOf(
                    "电影", "影院", "万达", "游戏", "Steam", "网易",
                    "腾讯游戏", "KTV", "酒吧", "演唱会", "音乐会",
                    "爱奇艺", "优酷", "腾讯视频", "B站", "哔哩哔哩",
                    "网易云", "QQ音乐", "Spotify", "Netflix"
                ),
                categoryName = "文化娱乐"
            ),
            // 教育培训
            CategoryMatchRule(
                keywords = listOf(
                    "学费", "培训", "课程", "教育", "考试", "书店",
                    "图书", "当当", "京东图书", "网课", "慕课",
                    "得到", "知乎", "樊登", "学习"
                ),
                categoryName = "教育培训"
            ),
            // 人情往来
            CategoryMatchRule(
                keywords = listOf(
                    "红包", "转账", "礼金", "份子钱", "礼物", "鲜花",
                    "生日", "节日", "婚礼", "满月", "祝福"
                ),
                categoryName = "人情往来"
            ),
            // 通讯服务
            CategoryMatchRule(
                keywords = listOf(
                    "话费", "流量", "中国移动", "中国联通", "中国电信",
                    "宽带", "网费", "通讯"
                ),
                categoryName = "通讯服务"
            ),
            // 住房支出
            CategoryMatchRule(
                keywords = listOf(
                    "房租", "租金", "房贷", "按揭", "物业费", "中介费"
                ),
                categoryName = "住房支出"
            ),
            // 金融保险
            CategoryMatchRule(
                keywords = listOf(
                    "保险", "理财", "基金", "股票", "证券", "银行",
                    "手续费", "利息", "还款"
                ),
                categoryName = "金融保险"
            ),
            // 宠物
            CategoryMatchRule(
                keywords = listOf(
                    "宠物", "猫粮", "狗粮", "宠物医院", "宠物店"
                ),
                categoryName = "宠物"
            ),
            // 数码电子
            CategoryMatchRule(
                keywords = listOf(
                    "苹果", "Apple", "华为", "小米", "OPPO", "vivo",
                    "手机", "电脑", "平板", "耳机", "充电", "数码",
                    "京东", "天猫", "淘宝", "拼多多", "苏宁", "国美"
                ),
                categoryName = "数码电子"
            ),
            // 转账支出
            CategoryMatchRule(
                keywords = listOf(
                    "转账-", "转出", "提现"
                ),
                categoryName = "转账支出"
            )
        )

        /**
         * 收入分类匹配规则
         */
        private val INCOME_RULES = listOf(
            // 工资薪酬
            CategoryMatchRule(
                keywords = listOf("工资", "薪资", "薪酬", "代发工资", "发薪"),
                categoryName = "工资薪酬"
            ),
            // 奖金补贴
            CategoryMatchRule(
                keywords = listOf("奖金", "补贴", "津贴", "绩效", "年终"),
                categoryName = "奖金补贴"
            ),
            // 红包收入
            CategoryMatchRule(
                keywords = listOf("红包", "微信红包"),
                categoryName = "红包收入"
            ),
            // 转账收入
            CategoryMatchRule(
                keywords = listOf("转账", "转入", "收款"),
                categoryName = "转账收入"
            ),
            // 退款退货
            CategoryMatchRule(
                keywords = listOf("退款", "退货", "退还"),
                categoryName = "退款退货"
            ),
            // 报销款项
            CategoryMatchRule(
                keywords = listOf("报销", "费用报销"),
                categoryName = "报销款项"
            ),
            // 投资收益
            CategoryMatchRule(
                keywords = listOf("收益", "利息", "分红", "理财收益", "基金收益"),
                categoryName = "投资收益"
            )
        )
    }

    /**
     * 根据交易信息匹配分类
     *
     * @param record 解析后的账单记录
     * @param categories 可用分类列表
     * @return 匹配到的分类ID，未匹配返回null
     */
    fun matchCategory(record: ParsedBillRecord, categories: List<CustomFieldEntity>): Long? {
        val searchText = "${record.counterparty} ${record.goods} ${record.note}".lowercase()

        val rules = if (record.type == "收入") INCOME_RULES else EXPENSE_RULES

        for (rule in rules) {
            if (rule.keywords.any { keyword -> searchText.contains(keyword.lowercase()) }) {
                // 找到匹配的分类
                val category = categories.find {
                    it.name == rule.categoryName && it.isEnabled
                }
                if (category != null) {
                    return category.id
                }
            }
        }

        // 未匹配到，按优先级查找默认分类
        val defaultCategory = categories.find {
            // 优先查找 "其他支出" 或 "其他收入"
            (it.name == "其他支出" && record.type == "支出" && it.isEnabled) ||
            (it.name == "其他收入" && record.type == "收入" && it.isEnabled)
        } ?: categories.find {
            // 其次查找 "其他" 分类
            it.name == "其他" && it.isEnabled
        } ?: categories.find {
            // 最后查找 "未分类" 分类
            it.name == "未分类" && it.isEnabled
        }

        return defaultCategory?.id
    }

    /**
     * 获取或创建"未分类"分类ID的建议
     * 用于没有匹配到任何分类的记录
     */
    fun getUncategorizedName(): String = "未分类"

    /**
     * 批量匹配分类
     */
    fun matchCategories(
        records: List<ParsedBillRecord>,
        categories: List<CustomFieldEntity>
    ): List<ParsedBillRecord> {
        return records.map { record ->
            record.copy(suggestedCategoryId = matchCategory(record, categories))
        }
    }
}
