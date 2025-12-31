package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资金账户实体类
 *
 * 用于管理用户的各类资金账户，包括：
 * - 现金账户
 * - 银行卡（储蓄卡）
 * - 信用卡/花呗等信贷账户
 * - 支付宝/微信等电子钱包
 * - 投资账户
 */
@Entity(
    tableName = "fund_accounts",
    indices = [
        Index(value = ["accountType"]),
        Index(value = ["parentId"]),
        Index(value = ["isEnabled"])
    ]
)
data class FundAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 父账户ID（用于账户分组，如"银行卡"下有多个银行）
    val parentId: Long? = null,

    // 账户名称（如"中国银行储蓄卡"、"支付宝余额"）
    val name: String,

    // 账户类型
    val accountType: String,

    // 银行代码（用于银行卡/信用卡，如 ICBC, CCB 等）
    val bankCode: String? = null,

    // 卡号（后4位或完整卡号，用于区分同银行多张卡）
    val cardNumber: String? = null,

    // 账户图标
    val iconName: String = "account_balance_wallet",

    // 账户颜色
    val color: String = "#4CAF50",

    // 当前余额（对于信贷账户为负债金额）
    val balance: Double = 0.0,

    // 信用额度（仅信贷账户使用）
    val creditLimit: Double? = null,

    // 账单日（仅信贷账户使用，1-31）
    val billDay: Int? = null,

    // 还款日（仅信贷账户使用，1-31）
    val repaymentDay: Int? = null,

    // 备注
    val note: String = "",

    // 是否计入总资产/总负债统计
    val includeInTotal: Boolean = true,

    // 是否启用
    val isEnabled: Boolean = true,

    // 是否为系统预设账户
    val isPreset: Boolean = false,

    // 排序顺序
    val sortOrder: Int = 0,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 最后更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 账户类型枚举
 */
object AccountType {
    const val CASH = "CASH"                     // 现金
    const val BANK_CARD = "BANK_CARD"           // 银行卡（储蓄卡）
    const val CREDIT_CARD = "CREDIT_CARD"       // 信用卡
    const val ALIPAY = "ALIPAY"                 // 支付宝
    const val WECHAT = "WECHAT"                 // 微信支付
    const val CREDIT_LOAN = "CREDIT_LOAN"       // 信贷账户（花呗、借呗等）
    const val INVESTMENT = "INVESTMENT"         // 投资账户
    const val OTHER = "OTHER"                   // 其他

    fun getDisplayName(type: String): String = when (type) {
        CASH -> "现金"
        BANK_CARD -> "银行卡"
        CREDIT_CARD -> "信用卡"
        ALIPAY -> "支付宝"
        WECHAT -> "微信支付"
        CREDIT_LOAN -> "信贷账户"
        INVESTMENT -> "投资账户"
        OTHER -> "其他"
        else -> "未知"
    }

    fun getIcon(type: String): String = when (type) {
        CASH -> "💵"
        BANK_CARD -> "💳"
        CREDIT_CARD -> "💳"
        ALIPAY -> "🅰️"
        WECHAT -> "💚"
        CREDIT_LOAN -> "🏦"
        INVESTMENT -> "📈"
        OTHER -> "💰"
        else -> "💰"
    }

    fun isDebtAccount(type: String): Boolean = when (type) {
        CREDIT_CARD, CREDIT_LOAN -> true
        else -> false
    }

    /**
     * 是否需要卡号输入
     */
    fun needsCardNumber(type: String): Boolean = when (type) {
        BANK_CARD, CREDIT_CARD, CREDIT_LOAN, INVESTMENT -> true
        else -> false
    }
}

/**
 * 中国银行预设
 */
object ChineseBank {
    // 国有大型商业银行
    const val ICBC = "ICBC"         // 中国工商银行
    const val CCB = "CCB"           // 中国建设银行
    const val ABC = "ABC"           // 中国农业银行
    const val BOC = "BOC"           // 中国银行
    const val BOCOM = "BOCOM"       // 交通银行
    const val PSBC = "PSBC"         // 中国邮政储蓄银行

    // 股份制商业银行
    const val CMB = "CMB"           // 招商银行
    const val SPDB = "SPDB"         // 浦发银行
    const val CIB = "CIB"           // 兴业银行
    const val CMBC = "CMBC"         // 民生银行
    const val CITIC = "CITIC"       // 中信银行
    const val CEB = "CEB"           // 光大银行
    const val HXB = "HXB"           // 华夏银行
    const val PAB = "PAB"           // 平安银行
    const val GDB = "GDB"           // 广发银行
    const val BOB = "BOB"           // 北京银行
    const val BOS = "BOS"           // 上海银行
    const val NBCB = "NBCB"         // 宁波银行

    // 其他
    const val OTHER = "OTHER"       // 其他银行

    /**
     * 获取银行显示名称
     */
    fun getDisplayName(code: String): String = when (code) {
        ICBC -> "中国工商银行"
        CCB -> "中国建设银行"
        ABC -> "中国农业银行"
        BOC -> "中国银行"
        BOCOM -> "交通银行"
        PSBC -> "中国邮政储蓄银行"
        CMB -> "招商银行"
        SPDB -> "浦发银行"
        CIB -> "兴业银行"
        CMBC -> "民生银行"
        CITIC -> "中信银行"
        CEB -> "光大银行"
        HXB -> "华夏银行"
        PAB -> "平安银行"
        GDB -> "广发银行"
        BOB -> "北京银行"
        BOS -> "上海银行"
        NBCB -> "宁波银行"
        OTHER -> "其他银行"
        else -> code
    }

    /**
     * 获取银行简称
     */
    fun getShortName(code: String): String = when (code) {
        ICBC -> "工商"
        CCB -> "建设"
        ABC -> "农业"
        BOC -> "中行"
        BOCOM -> "交通"
        PSBC -> "邮储"
        CMB -> "招商"
        SPDB -> "浦发"
        CIB -> "兴业"
        CMBC -> "民生"
        CITIC -> "中信"
        CEB -> "光大"
        HXB -> "华夏"
        PAB -> "平安"
        GDB -> "广发"
        BOB -> "北京"
        BOS -> "上海"
        NBCB -> "宁波"
        OTHER -> "其他"
        else -> code
    }

    /**
     * 获取银行颜色
     */
    fun getColor(code: String): String = when (code) {
        ICBC -> "#C41230"       // 工商银行红色
        CCB -> "#003C8B"        // 建设银行蓝色
        ABC -> "#009A61"        // 农业银行绿色
        BOC -> "#C41230"        // 中国银行红色
        BOCOM -> "#003C8B"      // 交通银行蓝色
        PSBC -> "#007F3E"       // 邮储银行绿色
        CMB -> "#C41230"        // 招商银行红色
        SPDB -> "#003C8B"       // 浦发银行蓝色
        CIB -> "#003C8B"        // 兴业银行蓝色
        CMBC -> "#00A0E9"       // 民生银行蓝色
        CITIC -> "#C41230"      // 中信银行红色
        CEB -> "#9E1F63"        // 光大银行紫色
        HXB -> "#C41230"        // 华夏银行红色
        PAB -> "#FA6400"        // 平安银行橙色
        GDB -> "#C41230"        // 广发银行红色
        BOB -> "#C41230"        // 北京银行红色
        BOS -> "#003C8B"        // 上海银行蓝色
        NBCB -> "#F57C00"       // 宁波银行橙色
        else -> "#607D8B"
    }

    /**
     * 获取所有银行列表（用于下拉选择）
     */
    fun getAllBanks(): List<Pair<String, String>> = listOf(
        ICBC to "中国工商银行",
        CCB to "中国建设银行",
        ABC to "中国农业银行",
        BOC to "中国银行",
        BOCOM to "交通银行",
        PSBC to "中国邮政储蓄银行",
        CMB to "招商银行",
        SPDB to "浦发银行",
        CIB to "兴业银行",
        CMBC to "民生银行",
        CITIC to "中信银行",
        CEB to "光大银行",
        HXB to "华夏银行",
        PAB to "平安银行",
        GDB to "广发银行",
        BOB to "北京银行",
        BOS to "上海银行",
        NBCB to "宁波银行",
        OTHER to "其他银行"
    )
}
