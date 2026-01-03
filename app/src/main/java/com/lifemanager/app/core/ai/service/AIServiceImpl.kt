package com.lifemanager.app.core.ai.service

import com.google.gson.Gson
import com.lifemanager.app.core.ai.model.*
import com.lifemanager.app.core.ai.service.api.ChatRequest
import com.lifemanager.app.core.ai.service.api.ChatResponse
import com.lifemanager.app.core.ai.service.api.DeepSeekApi
import com.lifemanager.app.core.ai.service.api.ChatMessage as ApiChatMessage
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
                    ApiChatMessage("user", "你好，请回复「连接成功」")
                ),
                maxTokens = 20
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
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

            val today = java.time.LocalDate.now()
            val todayEpochDay = today.toEpochDay()

            val systemPrompt = """
你是一个智能生活助手，负责解析用户的语音命令。请将用户输入解析为JSON格式。
今天的日期是：$today（epochDay=$todayEpochDay）

支持的命令类型：
1. 记账（transaction）：包含金额的消费或收入记录
2. 待办（todo）：需要完成的具体任务或安排的事项
3. 目标（goal）：中长期的个人目标（减肥、存钱、学习、健身、戒烟等目标性质的内容）
4. 日记（diary）：纯粹记录心情或感想，不涉及具体事项
5. 习惯打卡（habit）：习惯名称、数值
6. 时间追踪（timetrack）：动作(start/stop)、分类
7. 导航（navigate）：目标页面
8. 查询（query）：查询类型

【重要判断规则 - 请严格遵守】：
- 有金额数字 + 消费/购买行为 → transaction（必须解析日期！）
- 涉及减肥、健身、存钱、学习XX技能、戒烟戒酒、读书N本等目标性描述 → goal（目标）
- 涉及具体事件/活动/任务（开会、约会、去医院、出差等）→ todo（待办事项）
- 纯粹表达心情感受（开心、难过、累了）→ diary
- 涉及提醒、闹钟 → todo（设置reminderMinutesBefore）

【目标vs待办的区分】：
- goal：需要持续努力才能达成的目标，通常有量化指标（减肥10斤、存款5万、跑步1000公里）
- todo：一次性可完成的具体事项（开会、看医生、买菜、坐飞机）

【日期计算规则 - 非常重要】：
- "今天" = $todayEpochDay
- "昨天" = ${todayEpochDay - 1}
- "前天" = ${todayEpochDay - 2}
- "明天" = ${todayEpochDay + 1}
- "后天" = ${todayEpochDay + 2}
- "这个月" = ${today.monthValue}月
- "下个月" = ${today.plusMonths(1).monthValue}月

【多条记录识别规则】：
如果用户输入包含多个不同日期的记录，需要返回multiple类型，data为数组。
例如："昨天今天明天吃饭各10元" → 需要识别为3条记录，分别对应昨天、今天、明天

可用的记账分类：$categoryNames

请严格按以下JSON格式返回：
单条记录：
{
  "type": "transaction|todo|goal|diary|habit|timetrack|navigate|query|unknown",
  "data": {
    // transaction: transactionType, amount, category, note, date(epochDay整数), time
    // todo: title, description, dueDate(epochDay整数), startTime(HH:mm), endTime(HH:mm), location, priority(HIGH/MEDIUM/LOW), quadrant, isAllDay, reminderMinutesBefore
    // goal: goalName, targetAmount, targetUnit, deadline, category
    // diary: content, mood(1-5)
  }
}

多条记录（不同日期/不同事项）：
{
  "type": "multiple",
  "data": [
    {"type":"transaction","data":{...}},
    {"type":"transaction","data":{...}}
  ]
}

【待办事项的四象限判断规则】：
- IMPORTANT_URGENT（重要且紧急）：关键会议、紧急任务、今天必须完成的重要事项、坐飞机、赶火车
- IMPORTANT_NOT_URGENT（重要不紧急）：学习计划、健康检查、长期规划相关
- NOT_IMPORTANT_URGENT（不重要但紧急）：普通会议、一般约会、日常事务
- NOT_IMPORTANT_NOT_URGENT（不重要不紧急）：娱乐活动、闲聊

【时间段识别规则】：
- "八点到九点"/"8点-9点" → startTime:"08:00", endTime:"09:00", isAllDay:false
- "下午两点到四点" → startTime:"14:00", endTime:"16:00", isAllDay:false
- "上午10点" → startTime:"10:00", isAllDay:false
- 没有提及具体时间 → isAllDay:true

【提醒识别规则】：
- "7点提醒我" + 事件在8点 → reminderMinutesBefore: 60
- "提前半小时提醒" → reminderMinutesBefore: 30
- "提前1小时提醒" → reminderMinutesBefore: 60
- 无特别说明的重要事件 → reminderMinutesBefore: 30

示例：
输入："12月1号吃饭100元"
输出：{"type":"transaction","data":{"transactionType":"expense","amount":100,"category":"餐饮","note":"吃饭","date":${java.time.LocalDate.of(today.year, 12, 1).toEpochDay()}}}

输入："昨天今天明天中午吃饭各10元"
输出：{"type":"multiple","data":[{"type":"transaction","data":{"transactionType":"expense","amount":10,"category":"餐饮","note":"中午吃饭","date":${todayEpochDay - 1},"time":"12:00"}},{"type":"transaction","data":{"transactionType":"expense","amount":10,"category":"餐饮","note":"中午吃饭","date":${todayEpochDay},"time":"12:00"}},{"type":"transaction","data":{"transactionType":"expense","amount":10,"category":"餐饮","note":"中午吃饭","date":${todayEpochDay + 1},"time":"12:00"}}]}

输入："这个月减肥10斤"
输出：{"type":"goal","data":{"goalName":"减肥10斤","targetAmount":10,"targetUnit":"斤","deadline":"${today.year}-${today.monthValue}-${today.lengthOfMonth()}","category":"健身"}}

输入："明天八点到九点有一个线上会议"
输出：{"type":"todo","data":{"title":"线上会议","dueDate":${todayEpochDay + 1},"startTime":"08:00","endTime":"09:00","isAllDay":false,"priority":"HIGH","quadrant":"NOT_IMPORTANT_URGENT","reminderMinutesBefore":15}}

输入："后天下午三点在公司开产品评审会"
输出：{"type":"todo","data":{"title":"产品评审会","dueDate":${todayEpochDay + 2},"startTime":"15:00","location":"公司","isAllDay":false,"priority":"HIGH","quadrant":"IMPORTANT_URGENT","reminderMinutesBefore":30}}

输入："明天8点飞机去北京，请在7点提醒我"
输出：{"type":"todo","data":{"title":"坐飞机去北京","dueDate":${todayEpochDay + 1},"startTime":"08:00","location":"北京","isAllDay":false,"priority":"HIGH","quadrant":"IMPORTANT_URGENT","reminderMinutesBefore":60}}

输入："今天很开心"
输出：{"type":"diary","data":{"content":"今天很开心","mood":5}}

只返回JSON，不要其他文字。不要创建重复记录！
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(
                    ApiChatMessage("system", systemPrompt),
                    ApiChatMessage("user", text)
                ),
                temperature = 0.1,
                maxTokens = 300
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
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
                messages = listOf(ApiChatMessage("user", prompt)),
                temperature = 0.1,
                maxTokens = 10
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = (response.choices?.firstOrNull()?.message?.content as? String)?.trim()
            val categoryId = content?.toLongOrNull()

            Result.success(if (categoryId == 0L) null else categoryId)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    /**
     * 使用AI视觉模型识别图片内容
     */
    override suspend fun recognizeImagePayment(
        imageBase64: String,
        categories: List<CustomFieldEntity>
    ): Result<PaymentInfo> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            val categoryNames = if (categories.isNotEmpty()) {
                categories.map { it.name }.joinToString("、")
            } else {
                "餐饮、购物、交通、娱乐、日用、通讯、医疗、教育、其他"
            }

            val prompt = """
请仔细识别这张图片中的支付/消费信息。这可能是：
- 支付宝/微信支付截图
- 银行转账截图
- 购物小票/发票
- 外卖订单截图
- 其他消费凭证

请提取以下信息并返回JSON格式：
{
  "amount": 金额数字（必须是数字，如123.45）,
  "type": "expense"（支出）或"income"（收入）,
  "payee": "商家/收款方名称",
  "category": "从以下分类中选择最匹配的：$categoryNames",
  "paymentMethod": "支付方式（支付宝/微信/银行卡/现金等）",
  "note": "简短描述（如：午餐、打车费等）",
  "date": "日期（如有，格式YYYY-MM-DD）"
}

注意：
1. 金额必须是准确的数字，不要包含货币符号
2. 如果是转账给个人，payee填写对方姓名或昵称
3. 如果无法识别某个字段，该字段填null
4. 只返回JSON，不要其他文字
""".trimIndent()

            // 构建多模态消息内容
            val contentParts = listOf(
                mapOf("type" to "text", "text" to prompt),
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf(
                        "url" to "data:image/jpeg;base64,$imageBase64",
                        "detail" to "high"
                    )
                )
            )

            val request = ChatRequest(
                model = "deepseek-chat",  // 使用支持视觉的模型
                messages = listOf(ApiChatMessage("user", contentParts)),
                temperature = 0.1,
                maxTokens = 500
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
                ?: return@withContext Result.failure(Exception("AI响应为空"))

            val paymentInfo = parseJsonToPaymentInfo(content, "图片识别")
            Result.success(paymentInfo)
        } catch (e: Exception) {
            Result.failure(e)
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
                    messages = listOf(ApiChatMessage("user", prompt)),
                    temperature = 0.1,
                    maxTokens = 200
                )

                val response = api.chatCompletion(
                    authorization = "Bearer ${config.apiKey}",
                    request = request
                )

                val content = response.choices?.firstOrNull()?.message?.content as? String
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
                messages = listOf(ApiChatMessage("user", prompt)),
                temperature = 0.1,
                maxTokens = 150
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
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
                messages = listOf(ApiChatMessage("user", prompt)),
                temperature = 0.5,
                maxTokens = 200
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
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
                messages = listOf(ApiChatMessage("user", prompt)),
                temperature = 0.5,
                maxTokens = 100
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
                ?: return@withContext Result.failure(Exception("AI响应为空"))

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzeGoal(
        title: String,
        description: String,
        category: String,
        goalType: String,
        progress: Float,
        remainingDays: Int?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = configRepository.getConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(Exception("API Key未配置"))
            }

            val progressPercent = (progress * 100).toInt()
            val timeStatus = when {
                remainingDays == null -> "无限期目标"
                remainingDays < 0 -> "已逾期${-remainingDays}天"
                remainingDays == 0 -> "今天截止"
                remainingDays <= 7 -> "还剩${remainingDays}天，时间紧迫"
                remainingDays <= 30 -> "还剩${remainingDays}天"
                else -> "还剩${remainingDays}天，时间充裕"
            }

            val categoryName = when (category) {
                "CAREER" -> "事业"
                "FINANCE" -> "财务"
                "HEALTH" -> "健康"
                "LEARNING" -> "学习"
                "RELATIONSHIP" -> "人际关系"
                "LIFESTYLE" -> "生活方式"
                "HOBBY" -> "兴趣爱好"
                else -> category
            }

            val goalTypeName = when (goalType) {
                "YEARLY" -> "年度目标"
                "QUARTERLY" -> "季度目标"
                "MONTHLY" -> "月度目标"
                "LONG_TERM" -> "长期目标"
                else -> goalType
            }

            val prompt = """
作为个人目标达成顾问，请分析以下目标并给出具体可行的建议（150字以内）：

目标名称：$title
目标描述：${description.ifBlank { "无" }}
分类：$categoryName
类型：$goalTypeName
当前进度：$progressPercent%
时间状态：$timeStatus

请从以下几个方面给出建议：
1. 当前进度是否符合预期
2. 需要调整的地方
3. 具体的下一步行动建议

保持建议简洁实用，直接给出可执行的建议，不要使用模板化语言。
""".trimIndent()

            val request = ChatRequest(
                model = config.model,
                messages = listOf(ApiChatMessage("user", prompt)),
                temperature = 0.6,
                maxTokens = 300
            )

            val response = api.chatCompletion(
                authorization = "Bearer ${config.apiKey}",
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content as? String
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

            when (type) {
                "multiple" -> {
                    // 处理多条记录
                    val dataList = map["data"] as? List<Map<String, Any>> ?: emptyList()
                    val intents = dataList.mapNotNull { item ->
                        val itemType = item["type"] as? String ?: return@mapNotNull null
                        val itemData = item["data"] as? Map<String, Any> ?: emptyMap()
                        parseSingleIntent(itemType, itemData)
                    }
                    if (intents.isNotEmpty()) {
                        CommandIntent.Multiple(intents)
                    } else {
                        CommandIntent.Unknown(originalText)
                    }
                }
                else -> {
                    val data = map["data"] as? Map<String, Any> ?: emptyMap()
                    parseSingleIntent(type, data) ?: CommandIntent.Unknown(originalText)
                }
            }
        } catch (e: Exception) {
            CommandIntent.Unknown(originalText, e.message)
        }
    }

    private fun parseSingleIntent(type: String, data: Map<String, Any>): CommandIntent? {
        return when (type) {
            "transaction" -> parseTransactionIntent(data)
            "todo" -> parseTodoIntent(data)
            "goal" -> parseGoalIntent(data)
            "diary" -> parseDiaryIntent(data)
            "habit" -> parseHabitIntent(data)
            "timetrack" -> parseTimeTrackIntent(data)
            "navigate" -> parseNavigateIntent(data)
            "query" -> parseQueryIntent(data)
            else -> null
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

        // 解析日期 - AI返回的是epochDay整数
        val dateEpochDay = (data["date"] as? Number)?.toInt()

        return CommandIntent.Transaction(
            type = type,
            amount = (data["amount"] as? Number)?.toDouble(),
            categoryName = data["category"] as? String,
            date = dateEpochDay,
            time = data["time"] as? String,
            note = data["note"] as? String,
            payee = data["payee"] as? String
        )
    }

    private fun parseTodoIntent(data: Map<String, Any>): CommandIntent.Todo {
        // 解析日期 - AI返回的是epochDay整数
        val dueDateEpochDay = (data["dueDate"] as? Number)?.toInt()
        val isAllDay = (data["isAllDay"] as? Boolean) ?: (data["startTime"] == null)

        return CommandIntent.Todo(
            title = data["title"] as? String ?: "",
            description = data["description"] as? String,
            dueDate = dueDateEpochDay,
            startTime = data["startTime"] as? String,
            endTime = data["endTime"] as? String,
            dueTime = data["dueTime"] as? String,
            isAllDay = isAllDay,
            location = data["location"] as? String,
            priority = data["priority"] as? String,
            quadrant = data["quadrant"] as? String,
            reminderMinutesBefore = (data["reminderMinutesBefore"] as? Number)?.toInt() ?: 0
        )
    }

    private fun parseDiaryIntent(data: Map<String, Any>): CommandIntent.Diary {
        return CommandIntent.Diary(
            content = data["content"] as? String ?: "",
            mood = (data["mood"] as? Number)?.toInt(),
            weather = data["weather"] as? String
        )
    }

    private fun parseGoalIntent(data: Map<String, Any>): CommandIntent.Goal {
        return CommandIntent.Goal(
            action = GoalAction.CREATE,
            goalName = data["goalName"] as? String,
            progress = (data["progress"] as? Number)?.toDouble()
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
