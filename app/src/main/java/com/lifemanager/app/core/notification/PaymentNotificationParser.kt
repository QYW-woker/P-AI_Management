package com.lifemanager.app.core.notification

import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.core.ai.model.TransactionType
import com.lifemanager.app.core.ai.service.AIService
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 支付通知解析器
 * 解析微信、支付宝等支付通知中的交易信息
 */
@Singleton
class PaymentNotificationParser @Inject constructor(
    private val aiService: AIService
) {
    // 金额匹配模式
    private val amountPatterns = listOf(
        Pattern.compile("[¥￥]\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)\\s*元"),
        Pattern.compile("金额[：:]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)(?=\\s*(?:元|¥|￥|$))")
    )

    /**
     * 解析通知
     */
    suspend fun parseNotification(
        packageName: String,
        title: String,
        content: String,
        bigText: String = ""
    ): Result<PaymentInfo> {
        val fullText = "$title $content $bigText"

        // 1. 尝试本地规则解析
        val localResult = parseWithLocalRules(packageName, fullText)
        if (localResult != null) {
            return Result.success(localResult)
        }

        // 2. 使用AI解析
        return aiService.parsePaymentNotification(fullText, packageName)
    }

    /**
     * 使用本地规则解析
     */
    private fun parseWithLocalRules(packageName: String, text: String): PaymentInfo? {
        val amount = extractAmount(text) ?: return null
        val type = detectTransactionType(text)
        val payee = extractPayee(text)
        val paymentMethod = getPaymentMethod(packageName)

        return PaymentInfo(
            amount = amount,
            type = type,
            payee = payee,
            category = null,
            timestamp = System.currentTimeMillis(),
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
            while (matcher.find()) {
                val amountStr = matcher.group(1)
                val amount = amountStr?.toDoubleOrNull()
                if (amount != null && amount > 0 && amount < 1000000) {
                    return amount
                }
            }
        }
        return null
    }

    /**
     * 检测交易类型
     */
    private fun detectTransactionType(text: String): TransactionType {
        val expenseIndicators = listOf(
            "支付成功", "付款成功", "消费", "支出", "扣款",
            "向", "付给", "支付给", "已付款", "已扣款"
        )
        val incomeIndicators = listOf(
            "收款成功", "到账", "收入", "转入", "收到",
            "红包", "退款", "已收款", "转账收款"
        )

        val hasExpense = expenseIndicators.any { text.contains(it) }
        val hasIncome = incomeIndicators.any { text.contains(it) }

        return when {
            hasIncome && !hasExpense -> TransactionType.INCOME
            hasExpense && !hasIncome -> TransactionType.EXPENSE
            text.contains("+") && !text.contains("-") -> TransactionType.INCOME
            text.contains("-") && !text.contains("+") -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }
    }

    /**
     * 提取收款方/付款方
     */
    private fun extractPayee(text: String): String? {
        val patterns = listOf(
            Pattern.compile("(?:向|付款给|转账给|支付给)\\s*[\"「【]?([^\"」】\\s]+)[\"」】]?"),
            Pattern.compile("(?:来自|收到)\\s*[\"「【]?([^\"」】\\s]+)[\"」】]?\\s*(?:的|转账)?"),
            Pattern.compile("(?:商户|店铺|商家)[：:]?\\s*([^\\s\\n]+)"),
            Pattern.compile("[【\\[]([^】\\]]+)[】\\]]")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val payee = matcher.group(1)?.trim()
                if (!payee.isNullOrBlank() && payee.length in 1..30) {
                    // 过滤无意义的词
                    if (payee !in listOf("支付", "付款", "收款", "成功", "通知")) {
                        return payee
                    }
                }
            }
        }
        return null
    }

    /**
     * 获取支付方式
     */
    private fun getPaymentMethod(packageName: String): String {
        return when (packageName) {
            "com.tencent.mm" -> "微信支付"
            "com.eg.android.AlipayGphone" -> "支付宝"
            "com.unionpay" -> "云闪付"
            "com.icbc.mobile" -> "工商银行"
            "com.chinamworld.main" -> "建设银行"
            "com.android.bankabc" -> "农业银行"
            "com.chinaonlinepayment.boc" -> "中国银行"
            "cmb.pb" -> "招商银行"
            "com.bankcomm.Bankcomm" -> "交通银行"
            else -> "其他"
        }
    }
}
