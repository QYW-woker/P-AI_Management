package com.lifemanager.app.core.ai.service

import com.google.gson.Gson
import com.lifemanager.app.core.ai.service.api.ChatMessage
import com.lifemanager.app.core.ai.service.api.ChatRequest
import com.lifemanager.app.core.ai.service.api.DeepSeekApi
import com.lifemanager.app.core.database.dao.*
import com.lifemanager.app.core.database.entity.*
import com.lifemanager.app.data.repository.AIConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI数据分析服务
 *
 * 定期分析各模块数据，生成洞察和建议
 * 使用缓存避免频繁调用API
 */
@Singleton
class AIDataAnalysisService @Inject constructor(
    private val api: DeepSeekApi,
    private val configRepository: AIConfigRepository,
    private val aiAnalysisDao: AIAnalysisDao,
    private val transactionDao: DailyTransactionDao,
    private val goalDao: GoalDao,
    private val habitDao: HabitDao,
    private val habitRecordDao: HabitRecordDao,
    private val budgetDao: BudgetDao,
    private val customFieldDao: CustomFieldDao,
    private val healthRecordDao: HealthRecordDao,
    private val gson: Gson
) {
    companion object {
        // 缓存有效期（毫秒）
        private const val CACHE_DURATION_WEEKLY = 7 * 24 * 60 * 60 * 1000L // 7天
        private const val CACHE_DURATION_DAILY = 24 * 60 * 60 * 1000L // 1天

        // 最小更新间隔（避免频繁调用）
        private const val MIN_UPDATE_INTERVAL = 6 * 60 * 60 * 1000L // 6小时
    }

    /**
     * 检查是否需要更新分析
     */
    private suspend fun needsUpdate(
        module: String,
        type: String,
        dataHash: String,
        maxAge: Long = CACHE_DURATION_WEEKLY
    ): Boolean {
        val existing = aiAnalysisDao.getByModuleAndType(module, type)
        if (existing == null) return true

        val age = System.currentTimeMillis() - existing.lastUpdated
        // 如果数据哈希变化或超过有效期，需要更新
        return existing.dataHash != dataHash || age > maxAge
    }

    /**
     * 计算数据哈希
     */
    private fun calculateHash(data: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ==================== 财务分析 ====================

    /**
     * 获取财务分析（从缓存或生成新的）
     */
    fun getFinanceAnalysis(): Flow<List<AIAnalysisEntity>> {
        return aiAnalysisDao.getByModule(AnalysisModule.FINANCE)
    }

    /**
     * 分析财务数据
     */
    suspend fun analyzeFinanceData(forceRefresh: Boolean = false): Result<AIAnalysisEntity> =
        withContext(Dispatchers.IO) {
            try {
                val config = configRepository.getConfig()
                if (!config.isConfigured) {
                    return@withContext Result.failure(Exception("AI未配置"))
                }

                val today = LocalDate.now()
                val monthStart = YearMonth.now().atDay(1).toEpochDay().toInt()
                val monthEnd = YearMonth.now().atEndOfMonth().toEpochDay().toInt()

                // 获取本月交易数据
                val transactions = transactionDao.getTransactionsBetweenDatesSync(monthStart, monthEnd)
                if (transactions.isEmpty()) {
                    return@withContext Result.failure(Exception("暂无交易数据"))
                }

                // 计算数据哈希
                val dataStr = transactions.map { "${it.id}:${it.amount}:${it.type}" }.joinToString(",")
                val dataHash = calculateHash(dataStr)

                // 检查是否需要更新
                if (!forceRefresh && !needsUpdate(AnalysisModule.FINANCE, AnalysisType.WEEKLY_SUMMARY, dataHash)) {
                    val cached = aiAnalysisDao.getByModuleAndType(AnalysisModule.FINANCE, AnalysisType.WEEKLY_SUMMARY)
                    if (cached != null) {
                        return@withContext Result.success(cached)
                    }
                }

                // 统计数据
                val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val balance = income - expense

                // 分类统计
                val categoryExpenses = transactions
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.categoryId }
                    .mapValues { it.value.sumOf { t -> t.amount } }
                    .toList()
                    .sortedByDescending { it.second }

                // 获取分类名称
                val categories = customFieldDao.getAllFieldsSync()
                val categoryMap = categories.associate { it.id to it.name }

                val categoryBreakdown = categoryExpenses.take(5).joinToString("\n") { (catId, amount) ->
                    val name = catId?.let { categoryMap[it] } ?: "未分类"
                    "- $name: ¥${String.format("%.2f", amount)}"
                }

                // 获取预算信息
                val budgets = budgetDao.getAllSync()
                val budgetInfo = if (budgets.isNotEmpty()) {
                    val totalBudget = budgets.sumOf { it.totalBudget }
                    val usageRate = if (totalBudget > 0) (expense / totalBudget * 100) else 0.0
                    "预算总额: ¥${String.format("%.2f", totalBudget)}, 使用率: ${String.format("%.1f", usageRate)}%"
                } else {
                    "暂未设置预算"
                }

                val prompt = """
作为专业财务顾问，请分析以下${today.monthValue}月财务数据并给出洞察：

📊 本月概览：
- 收入: ¥${String.format("%.2f", income)}
- 支出: ¥${String.format("%.2f", expense)}
- 结余: ¥${String.format("%.2f", balance)}
- $budgetInfo

📈 支出分类TOP5：
$categoryBreakdown

请按以下JSON格式返回分析结果：
{
  "title": "简短标题（10字以内）",
  "content": "核心洞察（50字以内，突出最重要的发现）",
  "suggestions": ["建议1", "建议2", "建议3"],
  "score": 财务健康评分(0-100),
  "sentiment": "POSITIVE/NEUTRAL/NEGATIVE",
  "highlights": ["亮点1", "亮点2"],
  "warnings": ["需注意的问题"]
}

只返回JSON，不要其他文字。
""".trimIndent()

                val request = ChatRequest(
                    model = config.model,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.3,
                    maxTokens = 500
                )

                val response = api.chatCompletion(
                    authorization = "Bearer ${config.apiKey}",
                    request = request
                )

                val content = response.choices?.firstOrNull()?.message?.content as? String
                    ?: return@withContext Result.failure(Exception("AI响应为空"))

                val analysis = parseAnalysisResponse(
                    content = content,
                    module = AnalysisModule.FINANCE,
                    type = AnalysisType.WEEKLY_SUMMARY,
                    dataHash = dataHash,
                    periodStart = monthStart,
                    periodEnd = monthEnd
                )

                aiAnalysisDao.insertOrUpdate(analysis)
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== 目标分析 ====================

    /**
     * 获取目标分析
     */
    fun getGoalAnalysis(): Flow<List<AIAnalysisEntity>> {
        return aiAnalysisDao.getByModule(AnalysisModule.GOAL)
    }

    /**
     * 分析目标数据
     */
    suspend fun analyzeGoalData(forceRefresh: Boolean = false): Result<AIAnalysisEntity> =
        withContext(Dispatchers.IO) {
            try {
                val config = configRepository.getConfig()
                if (!config.isConfigured) {
                    return@withContext Result.failure(Exception("AI未配置"))
                }

                // 获取活跃目标
                val goals = goalDao.getActiveGoalsSync()
                if (goals.isEmpty()) {
                    return@withContext Result.failure(Exception("暂无活跃目标"))
                }

                val dataStr = goals.map { "${it.id}:${it.currentValue}:${it.status}" }.joinToString(",")
                val dataHash = calculateHash(dataStr)

                if (!forceRefresh && !needsUpdate(AnalysisModule.GOAL, AnalysisType.WEEKLY_SUMMARY, dataHash)) {
                    val cached = aiAnalysisDao.getByModuleAndType(AnalysisModule.GOAL, AnalysisType.WEEKLY_SUMMARY)
                    if (cached != null) {
                        return@withContext Result.success(cached)
                    }
                }

                val today = LocalDate.now()
                val goalsSummary = goals.take(5).joinToString("\n") { goal ->
                    val progress = if ((goal.targetValue ?: 0.0) > 0) {
                        (goal.currentValue / goal.targetValue!! * 100).toInt()
                    } else 0
                    val deadline = goal.endDate?.let { endDateEpoch ->
                        val daysLeft = endDateEpoch - today.toEpochDay().toInt()
                        if (daysLeft > 0) "剩余${daysLeft}天" else "已过期"
                    } ?: "无截止日期"
                    "- ${goal.title}: 进度${progress}%, $deadline"
                }

                val prompt = """
作为目标达成教练，请分析以下目标进展并给出建议：

🎯 当前目标（共${goals.size}个）：
$goalsSummary

请按以下JSON格式返回：
{
  "title": "简短标题（10字以内）",
  "content": "核心洞察（50字以内）",
  "suggestions": ["建议1", "建议2"],
  "score": 目标执行力评分(0-100),
  "sentiment": "POSITIVE/NEUTRAL/NEGATIVE",
  "priorityGoal": "建议优先关注的目标名称",
  "motivationTip": "一句激励的话"
}

只返回JSON。
""".trimIndent()

                val request = ChatRequest(
                    model = config.model,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.4,
                    maxTokens = 400
                )

                val response = api.chatCompletion(
                    authorization = "Bearer ${config.apiKey}",
                    request = request
                )

                val content = response.choices?.firstOrNull()?.message?.content as? String
                    ?: return@withContext Result.failure(Exception("AI响应为空"))

                val analysis = parseAnalysisResponse(
                    content = content,
                    module = AnalysisModule.GOAL,
                    type = AnalysisType.WEEKLY_SUMMARY,
                    dataHash = dataHash,
                    periodStart = today.toEpochDay().toInt(),
                    periodEnd = today.toEpochDay().toInt()
                )

                aiAnalysisDao.insertOrUpdate(analysis)
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== 习惯分析 ====================

    /**
     * 获取习惯分析
     */
    fun getHabitAnalysis(): Flow<List<AIAnalysisEntity>> {
        return aiAnalysisDao.getByModule(AnalysisModule.HABIT)
    }

    /**
     * 分析习惯数据
     */
    suspend fun analyzeHabitData(forceRefresh: Boolean = false): Result<AIAnalysisEntity> =
        withContext(Dispatchers.IO) {
            try {
                val config = configRepository.getConfig()
                if (!config.isConfigured) {
                    return@withContext Result.failure(Exception("AI未配置"))
                }

                // 获取启用的习惯
                val habits = habitDao.getEnabledSync()
                if (habits.isEmpty()) {
                    return@withContext Result.failure(Exception("暂无习惯数据"))
                }

                val today = LocalDate.now()
                val weekStart = today.minusDays(6).toEpochDay().toInt()
                val todayEpoch = today.toEpochDay().toInt()

                // 获取最近7天的打卡记录
                val records = habitRecordDao.getRecordsInRangeSync(weekStart, todayEpoch)

                val dataStr = habits.map { "${it.id}:${it.name}" }.joinToString(",") +
                        records.map { "${it.habitId}:${it.date}" }.joinToString(",")
                val dataHash = calculateHash(dataStr)

                if (!forceRefresh && !needsUpdate(AnalysisModule.HABIT, AnalysisType.WEEKLY_SUMMARY, dataHash)) {
                    val cached = aiAnalysisDao.getByModuleAndType(AnalysisModule.HABIT, AnalysisType.WEEKLY_SUMMARY)
                    if (cached != null) {
                        return@withContext Result.success(cached)
                    }
                }

                // 计算完成率和连续天数
                val habitStats = habits.map { habit ->
                    val habitRecords = records.filter { it.habitId == habit.id }
                    val checkins = habitRecords.size
                    val completionRate = (checkins.toDouble() / 7 * 100).toInt()
                    // 计算连续打卡天数
                    val streak = calculateStreak(habitRecords.map { it.date }, todayEpoch)
                    Triple(habit.name, completionRate, streak)
                }

                val habitSummary = habitStats.joinToString("\n") { stats ->
                    "- ${stats.first}: 本周完成率${stats.second}%, 连续${stats.third}天"
                }

                val avgCompletion = if (habitStats.isNotEmpty()) {
                    habitStats.map { it.second }.average().toInt()
                } else 0

                val prompt = """
作为习惯养成教练，请分析以下习惯打卡数据：

📅 本周习惯表现（共${habits.size}个习惯）：
$habitSummary

平均完成率：${avgCompletion}%

请按以下JSON格式返回：
{
  "title": "简短标题（10字以内）",
  "content": "核心洞察（50字以内）",
  "suggestions": ["建议1", "建议2"],
  "score": 习惯执行力评分(0-100),
  "sentiment": "POSITIVE/NEUTRAL/NEGATIVE",
  "bestHabit": "表现最好的习惯名称",
  "needsAttention": "需要加强的习惯名称"
}

只返回JSON。
""".trimIndent()

                val request = ChatRequest(
                    model = config.model,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.4,
                    maxTokens = 400
                )

                val response = api.chatCompletion(
                    authorization = "Bearer ${config.apiKey}",
                    request = request
                )

                val content = response.choices?.firstOrNull()?.message?.content as? String
                    ?: return@withContext Result.failure(Exception("AI响应为空"))

                val analysis = parseAnalysisResponse(
                    content = content,
                    module = AnalysisModule.HABIT,
                    type = AnalysisType.WEEKLY_SUMMARY,
                    dataHash = dataHash,
                    periodStart = weekStart,
                    periodEnd = todayEpoch
                )

                aiAnalysisDao.insertOrUpdate(analysis)
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== 健康记录分析 ====================

    /**
     * 获取健康记录分析
     */
    fun getHealthAnalysis(): Flow<List<AIAnalysisEntity>> {
        return aiAnalysisDao.getByModule(AnalysisModule.HEALTH)
    }

    /**
     * 分析健康记录数据
     */
    suspend fun analyzeHealthData(forceRefresh: Boolean = false): Result<AIAnalysisEntity> =
        withContext(Dispatchers.IO) {
            try {
                val config = configRepository.getConfig()
                if (!config.isConfigured) {
                    return@withContext Result.failure(Exception("AI未配置"))
                }

                val today = LocalDate.now()
                val weekStart = today.minusDays(6).toEpochDay().toInt()
                val todayEpoch = today.toEpochDay().toInt()

                // 获取最近7天的健康记录
                val records = healthRecordDao.getByDateRangeSync(weekStart, todayEpoch)
                if (records.isEmpty()) {
                    return@withContext Result.failure(Exception("暂无健康数据"))
                }

                val dataStr = records.map { "${it.id}:${it.recordType}:${it.value}" }.joinToString(",")
                val dataHash = calculateHash(dataStr)

                if (!forceRefresh && !needsUpdate(AnalysisModule.HEALTH, AnalysisType.WEEKLY_SUMMARY, dataHash)) {
                    val cached = aiAnalysisDao.getByModuleAndType(AnalysisModule.HEALTH, AnalysisType.WEEKLY_SUMMARY)
                    if (cached != null) {
                        return@withContext Result.success(cached)
                    }
                }

                // 按类型统计健康数据
                val weightRecords = records.filter { it.recordType == "WEIGHT" }
                val sleepRecords = records.filter { it.recordType == "SLEEP" }
                val exerciseRecords = records.filter { it.recordType == "EXERCISE" }
                val moodRecords = records.filter { it.recordType == "MOOD" }
                val waterRecords = records.filter { it.recordType == "WATER" }
                val stepsRecords = records.filter { it.recordType == "STEPS" }

                // 构建健康数据摘要
                val healthSummary = buildString {
                    // 体重
                    if (weightRecords.isNotEmpty()) {
                        val latestWeight = weightRecords.maxByOrNull { it.date }?.value
                        val avgWeight = weightRecords.map { it.value }.average()
                        appendLine("- 体重: 最新${String.format("%.1f", latestWeight)}kg, 平均${String.format("%.1f", avgWeight)}kg")
                    }

                    // 睡眠
                    if (sleepRecords.isNotEmpty()) {
                        val avgSleep = sleepRecords.map { it.value }.average()
                        val avgQuality = sleepRecords.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()
                        appendLine("- 睡眠: 平均${String.format("%.1f", avgSleep)}小时" +
                                (avgQuality?.let { ", 质量评分${String.format("%.1f", it)}/5" } ?: ""))
                    }

                    // 运动
                    if (exerciseRecords.isNotEmpty()) {
                        val totalExercise = exerciseRecords.sumOf { it.value }
                        val exerciseDays = exerciseRecords.map { it.date }.distinct().size
                        appendLine("- 运动: 总计${totalExercise.toInt()}分钟, ${exerciseDays}天有运动")
                    }

                    // 心情
                    if (moodRecords.isNotEmpty()) {
                        val avgMood = moodRecords.mapNotNull { it.rating }.average()
                        val moodTrend = when {
                            avgMood >= 4.0 -> "积极"
                            avgMood >= 3.0 -> "平稳"
                            else -> "需关注"
                        }
                        appendLine("- 心情: 平均评分${String.format("%.1f", avgMood)}/5 ($moodTrend)")
                    }

                    // 饮水
                    if (waterRecords.isNotEmpty()) {
                        val dailyWater = waterRecords.groupBy { it.date }.map { it.value.sumOf { r -> r.value } }
                        val avgWater = dailyWater.average()
                        appendLine("- 饮水: 日均${avgWater.toInt()}ml")
                    }

                    // 步数
                    if (stepsRecords.isNotEmpty()) {
                        val dailySteps = stepsRecords.groupBy { it.date }.map { it.value.sumOf { r -> r.value } }
                        val avgSteps = dailySteps.average()
                        appendLine("- 步数: 日均${avgSteps.toInt()}步")
                    }
                }

                val prompt = """
作为健康管理顾问，请分析以下近7天的健康数据并给出建议：

📊 健康数据概览：
$healthSummary

请按以下JSON格式返回：
{
  "title": "简短标题（10字以内）",
  "content": "核心洞察（50字以内，突出健康状态和需要改进的方面）",
  "suggestions": ["建议1", "建议2", "建议3"],
  "score": 健康评分(0-100),
  "sentiment": "POSITIVE/NEUTRAL/NEGATIVE",
  "highlights": ["表现好的方面1", "表现好的方面2"],
  "warnings": ["需要注意的问题"],
  "focusArea": "最需要关注的健康领域"
}

只返回JSON，不要其他文字。
""".trimIndent()

                val request = ChatRequest(
                    model = config.model,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.3,
                    maxTokens = 500
                )

                val response = api.chatCompletion(
                    authorization = "Bearer ${config.apiKey}",
                    request = request
                )

                val content = response.choices?.firstOrNull()?.message?.content as? String
                    ?: return@withContext Result.failure(Exception("AI响应为空"))

                val analysis = parseAnalysisResponse(
                    content = content,
                    module = AnalysisModule.HEALTH,
                    type = AnalysisType.WEEKLY_SUMMARY,
                    dataHash = dataHash,
                    periodStart = weekStart,
                    periodEnd = todayEpoch
                )

                aiAnalysisDao.insertOrUpdate(analysis)
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== 综合分析 ====================

    /**
     * 获取综合健康评分
     */
    fun getOverallHealthScore(): Flow<AIAnalysisEntity?> {
        return aiAnalysisDao.getOverallHealthScore()
    }

    /**
     * 生成综合健康评分
     */
    suspend fun generateOverallHealthScore(forceRefresh: Boolean = false): Result<AIAnalysisEntity> =
        withContext(Dispatchers.IO) {
            try {
                val config = configRepository.getConfig()
                if (!config.isConfigured) {
                    return@withContext Result.failure(Exception("AI未配置"))
                }

                // 收集各模块数据
                val financeAnalysis = aiAnalysisDao.getByModuleSync(AnalysisModule.FINANCE).firstOrNull()
                val goalAnalysis = aiAnalysisDao.getByModuleSync(AnalysisModule.GOAL).firstOrNull()
                val habitAnalysis = aiAnalysisDao.getByModuleSync(AnalysisModule.HABIT).firstOrNull()
                val healthAnalysis = aiAnalysisDao.getByModuleSync(AnalysisModule.HEALTH).firstOrNull()

                val dataHash = calculateHash(
                    "${financeAnalysis?.dataHash}:${goalAnalysis?.dataHash}:${habitAnalysis?.dataHash}:${healthAnalysis?.dataHash}"
                )

                if (!forceRefresh && !needsUpdate(AnalysisModule.OVERALL, AnalysisType.HEALTH_SCORE, dataHash)) {
                    val cached = aiAnalysisDao.getByModuleAndType(AnalysisModule.OVERALL, AnalysisType.HEALTH_SCORE)
                    if (cached != null) {
                        return@withContext Result.success(cached)
                    }
                }

                val moduleScores = mutableListOf<String>()
                financeAnalysis?.let { moduleScores.add("财务: ${it.score ?: "未知"}分 - ${it.content}") }
                goalAnalysis?.let { moduleScores.add("目标: ${it.score ?: "未知"}分 - ${it.content}") }
                habitAnalysis?.let { moduleScores.add("习惯: ${it.score ?: "未知"}分 - ${it.content}") }
                healthAnalysis?.let { moduleScores.add("健康: ${it.score ?: "未知"}分 - ${it.content}") }

                if (moduleScores.isEmpty()) {
                    return@withContext Result.failure(Exception("缺少模块分析数据"))
                }

                val prompt = """
作为生活管理顾问，请根据以下各模块分析结果，生成综合生活健康评分：

${moduleScores.joinToString("\n")}

请按以下JSON格式返回：
{
  "title": "生活健康综评",
  "content": "综合评价（30字以内）",
  "score": 综合评分(0-100),
  "sentiment": "POSITIVE/NEUTRAL/NEGATIVE",
  "topPriority": "当前最需要关注的领域",
  "encouragement": "一句鼓励的话（20字以内）"
}

只返回JSON。
""".trimIndent()

                val request = ChatRequest(
                    model = config.model,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.4,
                    maxTokens = 300
                )

                val response = api.chatCompletion(
                    authorization = "Bearer ${config.apiKey}",
                    request = request
                )

                val content = response.choices?.firstOrNull()?.message?.content as? String
                    ?: return@withContext Result.failure(Exception("AI响应为空"))

                val today = LocalDate.now().toEpochDay().toInt()
                val analysis = parseAnalysisResponse(
                    content = content,
                    module = AnalysisModule.OVERALL,
                    type = AnalysisType.HEALTH_SCORE,
                    dataHash = dataHash,
                    periodStart = today,
                    periodEnd = today
                )

                aiAnalysisDao.insertOrUpdate(analysis)
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 触发所有模块分析（后台任务使用）
     */
    suspend fun runScheduledAnalysis() {
        withContext(Dispatchers.IO) {
            try {
                // 检查最后更新时间，避免频繁调用
                val lastUpdate = aiAnalysisDao.getByModuleSync(AnalysisModule.FINANCE)
                    .firstOrNull()?.lastUpdated ?: 0L
                if (System.currentTimeMillis() - lastUpdate < MIN_UPDATE_INTERVAL) {
                    return@withContext
                }

                // 依次分析各模块
                analyzeFinanceData()
                analyzeGoalData()
                analyzeHabitData()
                analyzeHealthData()
                generateOverallHealthScore()
            } catch (e: Exception) {
                // 静默失败，不影响应用运行
            }
        }
    }

    /**
     * 清理过期分析数据
     */
    suspend fun cleanupStaleData() {
        val threshold = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L // 30天
        aiAnalysisDao.deleteStale(threshold)
    }

    /**
     * 解析AI响应为分析实体
     */
    private fun parseAnalysisResponse(
        content: String,
        module: String,
        type: String,
        dataHash: String,
        periodStart: Int,
        periodEnd: Int
    ): AIAnalysisEntity {
        return try {
            val jsonStr = extractJson(content)
            val map = gson.fromJson(jsonStr, Map::class.java) as? Map<String, Any>
                ?: throw Exception("Invalid JSON")

            AIAnalysisEntity(
                module = module,
                analysisType = type,
                title = map["title"] as? String ?: "分析结果",
                content = map["content"] as? String ?: content,
                details = jsonStr,
                score = (map["score"] as? Number)?.toInt(),
                sentiment = map["sentiment"] as? String ?: AnalysisSentiment.NEUTRAL,
                dataHash = dataHash,
                periodStart = periodStart,
                periodEnd = periodEnd,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            AIAnalysisEntity(
                module = module,
                analysisType = type,
                title = "分析结果",
                content = content.take(200),
                dataHash = dataHash,
                periodStart = periodStart,
                periodEnd = periodEnd,
                lastUpdated = System.currentTimeMillis()
            )
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

    /**
     * 计算连续打卡天数
     */
    private fun calculateStreak(dates: List<Int>, today: Int): Int {
        if (dates.isEmpty()) return 0
        val sortedDates = dates.sorted().distinct()
        var streak = 0
        var currentDate = today

        // 从今天往前数，检查连续天数
        while (sortedDates.contains(currentDate)) {
            streak++
            currentDate--
        }
        return streak
    }
}
