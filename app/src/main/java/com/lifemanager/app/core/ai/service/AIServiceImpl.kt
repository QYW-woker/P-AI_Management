package com.lifemanager.app.core.ai.service

import com.google.gson.Gson
import com.lifemanager.app.core.ai.model.*
import com.lifemanager.app.core.ai.service.api.ChatMessage
import com.lifemanager.app.core.ai.service.api.ChatRequest
import com.lifemanager.app.core.ai.service.api.DeepSeekApi
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.data.repository.AIConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI服务实现 - DeepSeek
 */
@Singleton
class AIServiceImpl @Inject constructor(
    private val api: DeepSeekApi,
    private val configRepository: AIConfigRepository,
    private val gson: Gson
) : AIService {

    override fun isConfigured(): Boolean {
        return configRepository.getConfigSync().isConfigured
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage("user", "你好，请回复「连接成功」")
                ),
                maxTokens = 20
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
            if (content != null) {
                Result.success("连接成功: $content")
            } else {
                Result.failure(Exception("响应为空"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun parseCommand(
        text: String,
        categories: List<CustomFieldEntity>
    ): Result<CommandIntent> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            val categoryNames = categories.map { it.name }.joinToString("、")

            val systemPrompt = """
你是一个智能生活助手，负责解析用户的语音命令。请将用户输入解析为JSON格式。

支持的命令类型：
1. 记账（transaction）：金额、类型（income/expense）、分类、备注
2. 待办（todo）：标题、截止日期、提醒时间
3. 日记（diary）：内容、心情评分(1-5)
4. 习惯打卡（habit）：习惯名称、数值
5. 时间追踪（timetrack）：动作(start/stop)、分类
6. 导航（navigate）：目标页面
7. 查询（query）：查询类型

可用的记账分类：$categoryNames

请严格按以下JSON格式返回：
{
  "type": "transaction|todo|diary|habit|timetrack|navigate|query|unknown",
  "data": {
    // 根据类型填充相应字段
  }
}

示例：
输入："今天午饭花了25元"
输出：{"type":"transaction","data":{"transactionType":"expense","amount":25,"category":"餐饮","note":"午饭"}}

输入："明天下午3点开会"
输出：{"type":"todo","data":{"title":"开会","dueDate":"明天","dueTime":"15:00"}}

只返回JSON，不要其他文字。
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", text)
                ),
                temperature = 0.1,
                maxTokens = 300
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("AI响应为空"))

            val intent = parseJsonToIntent(content, text)
            Result.success(intent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun classifyTransaction(
        description: String,
        categories: List<CustomFieldEntity>
    ): Result<Long?> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.success(null)
            }

            val categoryList = categories.map { "${it.id}:${it.name}" }.joinToString("\n")

            val prompt = """
请根据描述选择最匹配的分类ID，只返回数字ID，不要其他内容。

分类列表：
$categoryList

描述：$description

如果无法确定，返回0。
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(ChatMessage("user", prompt)),
                temperature = 0.1,
                maxTokens = 10
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content?.trim()
            val categoryId = content?.toLongOrNull()

            Result.success(if (categoryId == 0L) null else categoryId)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    override suspend fun parsePaymentScreenshot(ocrText: String): Result<PaymentInfo> =
        withContext(Dispatchers.IO) {
            try {
                val config = configRepository.getConfig()
                if (!config.isConfigured) {
                    return@withContext Result.failure(Exception("API Key未配置"))
                }

                val prompt = """
请解析以下支付截图的OCR文本，提取支付信息，返回JSON格式：
{
  "amount": 金额数字,
  "type": "expense"或"income",
  "payee": "商家名称",
  "category": "推测的分类",
  "paymentMethod": "支付方式"
}

OCR文本：
$ocrText

只返回JSON，不要其他文字。
""".trimIndent()

                val request = ChatRequest(
                    model = config.model,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.1,
                    maxTokens = 200
                )

                val response = api.chatCompletion(
                    authorization = "Bearer ${config.apiKey}",
                    request = request
                )

                val content = response.choices?.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(Exception("AI响应为空"))

                val paymentInfo = parseJsonToPaymentInfo(content, ocrText)
                Result.success(paymentInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun parsePaymentNotification(
        notificationText: String,
        packageName: String
    ): Result<PaymentInfo> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            val source = when {
                packageName.contains("tencent.mm") -> "微信"
                packageName.contains("alipay", ignoreCase = true) -> "支付宝"
                else -> "其他"
            }

            val prompt = """
请解析以下${source}支付通知，提取支付信息，返回JSON格式：
{
  "amount": 金额数字,
  "type": "expense"或"income",
  "payee": "商家名称",
  "category": "推测的分类"
}

通知内容：
$notificationText

只返回JSON，不要其他文字。
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(ChatMessage("user", prompt)),
                temperature = 0.1,
                maxTokens = 150
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("AI响应为空"))

            val paymentInfo = parseJsonToPaymentInfo(content, notificationText).copy(
                paymentMethod = source
            )
            Result.success(paymentInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateFinanceAdvice(
        income: Double,
        expense: Double,
        categoryBreakdown: Map<String, Double>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            val breakdown = categoryBreakdown.entries
                .sortedByDescending { it.value }
                .joinToString("\n") { "- ${it.key}: ¥${String.format("%.2f", it.value)}" }

            val prompt = """
作为理财顾问，请根据以下数据给出简短的财务建议（100字以内）：

本月收入：¥${String.format("%.2f", income)}
本月支出：¥${String.format("%.2f", expense)}
结余：¥${String.format("%.2f", income - expense)}

支出分类明细：
$breakdown

请给出具体可行的建议。
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(ChatMessage("user", prompt)),
                temperature = 0.5,
                maxTokens = 200
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("AI响应为空"))

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateReportSummary(
        period: String,
        data: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            val dataStr = data.entries.joinToString("\n") { "${it.key}: ${it.value}" }

            val prompt = """
请为以下${period}数据生成简短摘要（50字以内）：

$dataStr
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(ChatMessage("user", prompt)),
                temperature = 0.5,
                maxTokens = 100
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("AI响应为空"))

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解析JSON为CommandIntent
     */
    private fun parseJsonToIntent(json: String, originalText: String): CommandIntent {
        return try {
            // 提取JSON部分
            val jsonStr = extractJson(json)
            val map = gson.fromJson(jsonStr, Map::class.java) as? Map<String, Any>
                ?: return CommandIntent.Unknown(originalText)

            val type = map["type"] as? String ?: return CommandIntent.Unknown(originalText)
            val data = map["data"] as? Map<String, Any> ?: emptyMap()

            when (type) {
                "transaction" -> parseTransactionIntent(data)
                "todo" -> parseTodoIntent(data)
                "diary" -> parseDiaryIntent(data)
                "habit" -> parseHabitIntent(data)
                "timetrack" -> parseTimeTrackIntent(data)
                "navigate" -> parseNavigateIntent(data)
                "query" -> parseQueryIntent(data)
                else -> CommandIntent.Unknown(originalText)
            }
        } catch (e: Exception) {
            CommandIntent.Unknown(originalText, e.message)
        }
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else {
            text
        }
    }

    private fun parseTransactionIntent(data: Map<String, Any>): CommandIntent.Transaction {
        val typeStr = data["transactionType"] as? String ?: "expense"
        val type = if (typeStr.equals("income", ignoreCase = true))
            TransactionType.INCOME else TransactionType.EXPENSE

        return CommandIntent.Transaction(
            type = type,
            amount = (data["amount"] as? Number)?.toDouble(),
            categoryName = data["category"] as? String,
            note = data["note"] as? String,
            payee = data["payee"] as? String
        )
    }

    private fun parseTodoIntent(data: Map<String, Any>): CommandIntent.Todo {
        return CommandIntent.Todo(
            title = data["title"] as? String ?: "",
            description = data["description"] as? String,
            priority = data["priority"] as? String
        )
    }

    private fun parseDiaryIntent(data: Map<String, Any>): CommandIntent.Diary {
        return CommandIntent.Diary(
            content = data["content"] as? String ?: "",
            mood = (data["mood"] as? Number)?.toInt(),
            weather = data["weather"] as? String
        )
    }

    private fun parseHabitIntent(data: Map<String, Any>): CommandIntent.HabitCheckin {
        return CommandIntent.HabitCheckin(
            habitName = data["habitName"] as? String ?: "",
            value = (data["value"] as? Number)?.toDouble()
        )
    }

    private fun parseTimeTrackIntent(data: Map<String, Any>): CommandIntent.TimeTrack {
        val actionStr = data["action"] as? String ?: "start"
        val action = when (actionStr.lowercase()) {
            "stop" -> TimeTrackAction.STOP
            "pause" -> TimeTrackAction.PAUSE
            "resume" -> TimeTrackAction.RESUME
            else -> TimeTrackAction.START
        }
        return CommandIntent.TimeTrack(
            action = action,
            categoryName = data["category"] as? String,
            note = data["note"] as? String
        )
    }

    private fun parseNavigateIntent(data: Map<String, Any>): CommandIntent.Navigate {
        return CommandIntent.Navigate(
            screen = data["screen"] as? String ?: "home"
        )
    }

    private fun parseQueryIntent(data: Map<String, Any>): CommandIntent.Query {
        val typeStr = data["queryType"] as? String ?: "today_expense"
        val queryType = when (typeStr.lowercase()) {
            "month_expense" -> QueryType.MONTH_EXPENSE
            "month_income" -> QueryType.MONTH_INCOME
            "category_expense" -> QueryType.CATEGORY_EXPENSE
            "habit_streak" -> QueryType.HABIT_STREAK
            "goal_progress" -> QueryType.GOAL_PROGRESS
            "savings_progress" -> QueryType.SAVINGS_PROGRESS
            else -> QueryType.TODAY_EXPENSE
        }
        return CommandIntent.Query(type = queryType)
    }

    private fun parseJsonToPaymentInfo(json: String, rawText: String): PaymentInfo {
        return try {
            val jsonStr = extractJson(json)
            val map = gson.fromJson(jsonStr, Map::class.java) as? Map<String, Any>
                ?: throw Exception("Invalid JSON")

            val typeStr = map["type"] as? String ?: "expense"
            val type = if (typeStr.equals("income", ignoreCase = true))
                TransactionType.INCOME else TransactionType.EXPENSE

            PaymentInfo(
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                type = type,
                payee = map["payee"] as? String,
                category = map["category"] as? String,
                paymentMethod = map["paymentMethod"] as? String,
                rawText = rawText
            )
        } catch (e: Exception) {
            PaymentInfo(
                amount = 0.0,
                type = TransactionType.EXPENSE,
                rawText = rawText
            )
        }
    }
}
