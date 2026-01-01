package com.lifemanager.app.core.ai.model

/**
 * 命令执行结果
 */
sealed class ExecutionResult {

    /**
     * 执行成功
     */
    data class Success(
        val message: String,
        val data: Any? = null
    ) : ExecutionResult()

    /**
     * 需要确认
     */
    data class NeedConfirmation(
        val intent: CommandIntent,
        val previewMessage: String
    ) : ExecutionResult()

    /**
     * 需要更多信息
     */
    data class NeedMoreInfo(
        val intent: CommandIntent,
        val missingFields: List<String>,
        val prompt: String
    ) : ExecutionResult()

    /**
     * 执行失败
     */
    data class Failure(
        val message: String,
        val error: Throwable? = null
    ) : ExecutionResult()

    /**
     * 无法识别
     */
    data class NotRecognized(
        val originalText: String,
        val suggestions: List<String> = emptyList()
    ) : ExecutionResult()

    /**
     * 批量执行成功
     */
    data class MultipleAdded(
        val count: Int,
        val summary: String
    ) : ExecutionResult()
}

/**
 * 支付信息（从截图/通知解析）
 */
data class PaymentInfo(
    val amount: Double,
    val type: TransactionType,
    val payee: String? = null,          // 商家名称
    val category: String? = null,        // 分类
    val timestamp: Long? = null,         // 时间戳
    val orderId: String? = null,         // 订单号
    val paymentMethod: String? = null,   // 支付方式（微信/支付宝等）
    val rawText: String? = null          // 原始文本
)

/**
 * 分析报告
 */
data class AnalysisReport(
    val title: String,
    val summary: String,
    val highlights: List<String>,
    val suggestions: List<String>,
    val data: Map<String, Any> = emptyMap()
)
