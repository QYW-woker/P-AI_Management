package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI分析结果缓存实体
 *
 * 用于存储各模块的AI分析结果，避免频繁调用API
 * 分析结果按模块和类型分类存储，定期更新
 */
@Entity(
    tableName = "ai_analysis",
    indices = [
        Index(value = ["module", "analysisType"], unique = true),
        Index(value = ["lastUpdated"])
    ]
)
data class AIAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * 模块名称
     * FINANCE - 财务模块
     * GOAL - 目标模块
     * HABIT - 习惯模块
     * TIME - 时间统计模块
     * DIARY - 日记模块
     * SAVINGS - 存钱计划模块
     * HEALTH - 健康记录模块
     * OVERALL - 综合分析
     */
    val module: String,

    /**
     * 分析类型
     * WEEKLY_SUMMARY - 周度总结
     * MONTHLY_SUMMARY - 月度总结
     * TREND_ANALYSIS - 趋势分析
     * ANOMALY_DETECTION - 异常检测
     * SUGGESTION - 建议
     * HEALTH_SCORE - 健康评分
     */
    val analysisType: String,

    /**
     * 分析结果标题
     */
    val title: String,

    /**
     * 分析结果内容（主要洞察）
     */
    val content: String,

    /**
     * 详细分析（JSON格式，包含更多细节）
     */
    val details: String = "{}",

    /**
     * 评分（0-100，可选）
     */
    val score: Int? = null,

    /**
     * 情感倾向
     * POSITIVE - 积极
     * NEUTRAL - 中性
     * NEGATIVE - 需要注意
     */
    val sentiment: String = "NEUTRAL",

    /**
     * 数据哈希（用于判断数据是否变化）
     */
    val dataHash: String = "",

    /**
     * 分析的数据时间范围开始
     */
    val periodStart: Int = 0,

    /**
     * 分析的数据时间范围结束
     */
    val periodEnd: Int = 0,

    /**
     * 最后更新时间
     */
    val lastUpdated: Long = System.currentTimeMillis(),

    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 分析模块枚举
 */
object AnalysisModule {
    const val FINANCE = "FINANCE"
    const val GOAL = "GOAL"
    const val HABIT = "HABIT"
    const val TIME = "TIME"
    const val DIARY = "DIARY"
    const val SAVINGS = "SAVINGS"
    const val HEALTH = "HEALTH"
    const val OVERALL = "OVERALL"
}

/**
 * 分析类型枚举
 */
object AnalysisType {
    const val WEEKLY_SUMMARY = "WEEKLY_SUMMARY"
    const val MONTHLY_SUMMARY = "MONTHLY_SUMMARY"
    const val TREND_ANALYSIS = "TREND_ANALYSIS"
    const val ANOMALY_DETECTION = "ANOMALY_DETECTION"
    const val SUGGESTION = "SUGGESTION"
    const val HEALTH_SCORE = "HEALTH_SCORE"
}

/**
 * 情感倾向枚举
 */
object AnalysisSentiment {
    const val POSITIVE = "POSITIVE"
    const val NEUTRAL = "NEUTRAL"
    const val NEGATIVE = "NEGATIVE"
}
