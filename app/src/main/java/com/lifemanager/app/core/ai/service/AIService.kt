package com.lifemanager.app.core.ai.service

import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.core.database.entity.CustomFieldEntity

/**
 * AI服务接口
 */
interface AIService {

    /**
     * 检查服务是否已配置
     */
    fun isConfigured(): Boolean

    /**
     * 测试API连接
     */
    suspend fun testConnection(): Result<String>

    /**
     * 解析语音/文本命令
     * @param text 用户输入的文本
     * @param categories 可用的分类列表（用于智能分类）
     * @return 解析后的命令意图
     */
    suspend fun parseCommand(
        text: String,
        categories: List<CustomFieldEntity> = emptyList()
    ): Result<CommandIntent>

    /**
     * 智能分类
     * @param description 交易描述
     * @param categories 可用分类列表
     * @return 匹配的分类ID
     */
    suspend fun classifyTransaction(
        description: String,
        categories: List<CustomFieldEntity>
    ): Result<Long?>

    /**
     * 解析支付截图文本
     * @param ocrText OCR识别的文本
     * @return 解析后的支付信息
     */
    suspend fun parsePaymentScreenshot(ocrText: String): Result<PaymentInfo>

    /**
     * 解析支付通知
     * @param notificationText 通知内容
     * @param packageName 来源APP包名
     * @return 解析后的支付信息
     */
    suspend fun parsePaymentNotification(
        notificationText: String,
        packageName: String
    ): Result<PaymentInfo>

    /**
     * 生成财务分析建议
     * @param income 收入总额
     * @param expense 支出总额
     * @param categoryBreakdown 分类明细
     * @return 分析建议文本
     */
    suspend fun generateFinanceAdvice(
        income: Double,
        expense: Double,
        categoryBreakdown: Map<String, Double>
    ): Result<String>

    /**
     * 生成周报/月报摘要
     */
    suspend fun generateReportSummary(
        period: String,
        data: Map<String, Any>
    ): Result<String>
}
