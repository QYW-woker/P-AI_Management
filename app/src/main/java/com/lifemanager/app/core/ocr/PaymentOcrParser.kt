package com.lifemanager.app.core.ocr

import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.core.ai.model.TransactionType
import com.lifemanager.app.core.ai.service.AIService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 支付截图OCR解析器
 * 解析微信、支付宝等支付截图
 */
@Singleton
class PaymentOcrParser @Inject constructor(
    private val aiService: AIService
) {
    // 金额正则表达式
    private val amountPatterns = listOf(
        Pattern.compile("""[¥￥][\s]*([\d,]+\.?\d*)"""),  // ¥100.00
        Pattern.compile("""([\d,]+\.?\d*)[\s]*[元块]"""),  // 100.00元
        Pattern.compile("""金额[：:\s]*([\d,]+\.?\d*)"""),  // 金额：100.00
        Pattern.compile("""支付金额[：:\s]*([\d,]+\.?\d*)"""),  // 支付金额：100.00
        Pattern.compile("""实付[：:\s]*[¥￥]?([\d,]+\.?\d*)"""),  // 实付：100.00
        Pattern.compile("""付款金额[：:\s]*[¥￥]?([\d,]+\.?\d*)""")  // 付款金额：100.00
    )

    // 商户名称正则表达式
    private val merchantPatterns = listOf(
        Pattern.compile("""收款方[：:\s]*(.+)"""),
        Pattern.compile("""商户名称[：:\s]*(.+)"""),
        Pattern.compile("""商家[：:\s]*(.+)"""),
        Pattern.compile("""付款给[：:\s]*(.+)"""),
        Pattern.compile("""转账给[：:\s]*(.+)""")
    )

    // 时间正则表达式
    private val timePatterns = listOf(
        Pattern.compile("""(\d{4}[-/年]\d{1,2}[-/月]\d{1,2}[日]?\s*\d{1,2}:\d{2}(?::\d{2})?)"""),
        Pattern.compile("""(\d{1,2}[-/月]\d{1,2}[日]?\s*\d{1,2}:\d{2})"""),
        Pattern.compile("""支付时间[：:\s]*(.+)"""),
        Pattern.compile("""交易时间[：:\s]*(.+)""")
    )

    // 支付类型关键词
    private val expenseKeywords = listOf(
        "支付成功", "付款成功", "已付款", "支出", "消费", "购买",
        "转账给", "付给", "支付", "付款", "扫码付"
    )
    private val incomeKeywords = listOf(
        "收款成功", "已收款", "收入", "收到", "转入", "到账",
        "收钱", "红包", "奖励", "返现"
    )

    /**
     * 解析OCR识别结果
     * @param ocrResult OCR识别结果
     * @return 解析出的支付信息
     */
    suspend fun parseOcrResult(ocrResult: OcrResult): Result<PaymentInfo> {
        val text = ocrResult.rawText

        // 先尝试本地规则解析
        val localResult = parseLocally(text)
        if (localResult != null && localResult.amount > 0) {
            return Result.success(localResult)
        }

        // 本地解析失败，尝试AI解析
        return aiService.parsePaymentScreenshot(text)
    }

    /**
     * 使用本地规则解析
     */
    private fun parseLocally(text: String): PaymentInfo? {
        val amount = extractAmount(text)
        if (amount == null || amount <= 0) {
            return null
        }

        val isIncome = incomeKeywords.any { text.contains(it) }
        val isExpense = expenseKeywords.any { text.contains(it) }

        // 判断类型
        val type = when {
            isIncome && !isExpense -> TransactionType.INCOME
            isExpense && !isIncome -> TransactionType.EXPENSE
            isIncome && isExpense -> {
                // 都包含时，根据优先级判断
                if (text.indexOf(incomeKeywords.first { text.contains(it) }) <
                    text.indexOf(expenseKeywords.first { text.contains(it) })) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
            }
            else -> TransactionType.EXPENSE // 默认为支出
        }

        val merchant = extractMerchant(text)
        val time = extractTime(text)
        val source = detectSource(text)

        return PaymentInfo(
            amount = amount,
            type = type,
            payee = merchant,
            timestamp = time?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            paymentMethod = source,
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
                val amountStr = matcher.group(1)?.replace(",", "")
                amountStr?.toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    /**
     * 提取商户名称
     */
    private fun extractMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (!merchant.isNullOrBlank()) {
                    // 清理商户名称
                    return merchant.take(50) // 限制长度
                }
            }
        }
        return null
    }

    /**
     * 提取时间
     */
    private fun extractTime(text: String): LocalDateTime? {
        for (pattern in timePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val timeStr = matcher.group(1)
                return parseDateTime(timeStr)
            }
        }
        return null
    }

    /**
     * 解析日期时间字符串
     */
    private fun parseDateTime(timeStr: String?): LocalDateTime? {
        if (timeStr.isNullOrBlank()) return null

        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"),
            DateTimeFormatter.ofPattern("MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
        )

        val cleanTimeStr = timeStr.trim()

        for (formatter in formatters) {
            try {
                // 对于不包含年份的格式，补充当前年份
                val fullTimeStr = if (cleanTimeStr.length < 10) {
                    "${LocalDateTime.now().year}年$cleanTimeStr"
                } else {
                    cleanTimeStr
                }
                return LocalDateTime.parse(fullTimeStr, formatter)
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    /**
     * 检测支付来源
     */
    private fun detectSource(text: String): String {
        return when {
            text.contains("微信") || text.contains("WeChat") -> "WECHAT"
            text.contains("支付宝") || text.contains("Alipay") -> "ALIPAY"
            text.contains("云闪付") || text.contains("银联") -> "UNIONPAY"
            text.contains("京东") -> "JD"
            text.contains("美团") -> "MEITUAN"
            text.contains("抖音") || text.contains("抖音支付") -> "DOUYIN"
            else -> "UNKNOWN"
        }
    }
}
