package com.lifemanager.app.core.ocr

import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.core.ai.model.TransactionType
import com.lifemanager.app.core.ai.service.AIService
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 截图解析结果
 */
sealed class ScreenshotParseResult {
    /** 解析成功 */
    data class Success(
        val paymentInfo: PaymentInfo,
        val rawText: String,
        val source: PaymentSource
    ) : ScreenshotParseResult()

    /** 需要AI辅助解析 */
    data class NeedAIAssist(
        val rawText: String,
        val partialInfo: PartialPaymentInfo?
    ) : ScreenshotParseResult()

    /** 无法识别 */
    data class NotRecognized(
        val rawText: String,
        val reason: String
    ) : ScreenshotParseResult()
}

/**
 * 支付来源
 */
enum class PaymentSource {
    WECHAT_PAY,     // 微信支付
    ALIPAY,         // 支付宝
    BANK_APP,       // 银行APP
    CLOUD_PAY,      // 云闪付
    INVOICE,        // 发票
    RECEIPT,        // 收据
    UNKNOWN         // 未知
}

/**
 * 部分支付信息（用于AI辅助）
 */
data class PartialPaymentInfo(
    val amount: Double? = null,
    val type: TransactionType? = null,
    val payee: String? = null,
    val timestamp: String? = null
)

/**
 * 截图解析器
 * 解析支付截图、发票等图片中的交易信息
 */
@Singleton
class ScreenshotParser @Inject constructor(
    private val ocrRecognizer: OcrRecognizer,
    private val aiService: AIService
) {
    // 金额匹配模式
    private val amountPatterns = listOf(
        // 支付宝/微信格式：¥12.34 或 -12.34
        Pattern.compile("[¥￥]\\s*-?([0-9]+(?:\\.[0-9]{1,2})?)"),
        // 纯数字金额：12.34元
        Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)\\s*元"),
        // 金额：12.34
        Pattern.compile("金额[：:]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
        // 实付款/实收款
        Pattern.compile("实[付收]款?[：:]?\\s*[¥￥]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
        // 付款金额
        Pattern.compile("付款金额[：:]?\\s*[¥￥]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
        // 订单金额
        Pattern.compile("订单金额[：:]?\\s*[¥￥]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)")
    )

    // 时间匹配模式
    private val timePatterns = listOf(
        // 2024-01-15 14:30:25
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}(?::\\d{2})?)"),
        // 2024/01/15 14:30
        Pattern.compile("(\\d{4}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}(?::\\d{2})?)"),
        // 2024年1月15日 14:30
        Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日\\s*\\d{2}:\\d{2}(?::\\d{2})?)")
    )

    // 微信支付关键词
    private val wechatKeywords = listOf("微信支付", "微信转账", "微信红包", "零钱", "微信零钱")

    // 支付宝关键词
    private val alipayKeywords = listOf("支付宝", "蚂蚁花呗", "余额宝", "淘宝", "天猫")

    // 银行关键词
    private val bankKeywords = listOf("银行卡", "储蓄卡", "信用卡", "工商银行", "建设银行", "农业银行", "中国银行", "招商银行", "交通银行")

    /**
     * 解析截图
     */
    suspend fun parseScreenshot(ocrResult: OcrResult): ScreenshotParseResult {
        val text = ocrResult.rawText

        if (text.isBlank()) {
            return ScreenshotParseResult.NotRecognized(text, "未识别到文字内容")
        }

        // 1. 识别支付来源
        val source = detectPaymentSource(text)

        // 2. 尝试本地规则解析
        val localResult = parseWithLocalRules(text, source)

        return when {
            localResult != null && localResult.amount > 0 -> {
                ScreenshotParseResult.Success(localResult, text, source)
            }
            else -> {
                // 3. 需要AI辅助解析
                val partialInfo = extractPartialInfo(text)
                ScreenshotParseResult.NeedAIAssist(text, partialInfo)
            }
        }
    }

    /**
     * 使用AI辅助解析
     */
    suspend fun parseWithAI(rawText: String): Result<PaymentInfo> {
        return aiService.parsePaymentScreenshot(rawText)
    }

    /**
     * 检测支付来源
     */
    private fun detectPaymentSource(text: String): PaymentSource {
        return when {
            wechatKeywords.any { text.contains(it) } -> PaymentSource.WECHAT_PAY
            alipayKeywords.any { text.contains(it) } -> PaymentSource.ALIPAY
            bankKeywords.any { text.contains(it) } -> PaymentSource.BANK_APP
            text.contains("云闪付") -> PaymentSource.CLOUD_PAY
            text.contains("发票") || text.contains("税额") -> PaymentSource.INVOICE
            text.contains("收据") || text.contains("收款") -> PaymentSource.RECEIPT
            else -> PaymentSource.UNKNOWN
        }
    }

    /**
     * 使用本地规则解析
     */
    private fun parseWithLocalRules(text: String, source: PaymentSource): PaymentInfo? {
        // 提取金额
        val amount = extractAmount(text) ?: return null

        // 判断交易类型
        val type = detectTransactionType(text)

        // 提取商家/收款方
        val payee = extractPayee(text, source)

        // 提取时间
        val timestamp = extractTimestamp(text)

        // 提取支付方式
        val paymentMethod = when (source) {
            PaymentSource.WECHAT_PAY -> "微信支付"
            PaymentSource.ALIPAY -> "支付宝"
            PaymentSource.BANK_APP -> "银行卡"
            PaymentSource.CLOUD_PAY -> "云闪付"
            else -> null
        }

        return PaymentInfo(
            amount = amount,
            type = type,
            payee = payee,
            category = null,  // 分类需要AI判断
            timestamp = null,
            paymentMethod = paymentMethod,
            rawText = text
        )
    }

    /**
     * 提取金额
     */
    private fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountStr = matcher.group(1)
                try {
                    val amount = amountStr?.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        return amount
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }

    /**
     * 检测交易类型
     */
    private fun detectTransactionType(text: String): TransactionType {
        val expenseKeywords = listOf("支出", "付款", "消费", "扣款", "转出", "购买", "支付成功", "付款成功")
        val incomeKeywords = listOf("收入", "收款", "到账", "转入", "红包", "退款", "收款成功")

        val hasExpense = expenseKeywords.any { text.contains(it) }
        val hasIncome = incomeKeywords.any { text.contains(it) }

        return when {
            hasIncome && !hasExpense -> TransactionType.INCOME
            hasExpense && !hasIncome -> TransactionType.EXPENSE
            text.contains("-") -> TransactionType.EXPENSE
            text.contains("+") -> TransactionType.INCOME
            else -> TransactionType.EXPENSE  // 默认为支出
        }
    }

    /**
     * 提取商家/收款方
     */
    private fun extractPayee(text: String, source: PaymentSource): String? {
        val payeePatterns = listOf(
            Pattern.compile("(?:收款方|商户名称|店铺|商家)[：:]?\\s*([^\\n\\r]+)"),
            Pattern.compile("(?:向|付款给|转账给)\\s*([^\\n\\r]+?)\\s*(?:付款|转账)?"),
            Pattern.compile("(?:来自|收到)\\s*([^\\n\\r]+?)\\s*(?:的|转账)?")
        )

        for (pattern in payeePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val payee = matcher.group(1)?.trim()
                if (!payee.isNullOrBlank() && payee.length <= 30) {
                    return payee
                }
            }
        }

        return null
    }

    /**
     * 提取时间戳
     */
    private fun extractTimestamp(text: String): String? {
        for (pattern in timePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    /**
     * 提取部分信息（用于AI辅助）
     */
    private fun extractPartialInfo(text: String): PartialPaymentInfo? {
        val amount = extractAmount(text)
        val type = if (amount != null) detectTransactionType(text) else null
        val timestamp = extractTimestamp(text)

        return if (amount != null || timestamp != null) {
            PartialPaymentInfo(
                amount = amount,
                type = type,
                timestamp = timestamp
            )
        } else {
            null
        }
    }
}
