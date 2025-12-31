package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 健康记录实体类
 *
 * 用于记录用户的各类健康和生活数据
 * 支持多种记录类型：体重、睡眠、运动、心情、饮水等
 */
@Entity(
    tableName = "health_records",
    indices = [
        Index(value = ["recordType"]),
        Index(value = ["date"]),
        Index(value = ["recordType", "date"])
    ]
)
data class HealthRecordEntity(
    // 主键ID
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 记录类型
    // WEIGHT: 体重
    // SLEEP: 睡眠
    // EXERCISE: 运动
    // MOOD: 心情
    // WATER: 饮水
    // BLOOD_PRESSURE: 血压
    // HEART_RATE: 心率
    // STEPS: 步数
    // CUSTOM: 自定义
    val recordType: String,

    // 记录日期，使用epochDay格式
    val date: Int,

    // 记录时间，HH:mm格式（可选）
    val time: String? = null,

    // 主要数值（根据类型不同含义不同）
    // 体重：kg
    // 睡眠：小时
    // 运动：分钟
    // 饮水：毫升
    // 血压：收缩压
    // 心率：bpm
    // 步数：步
    val value: Double,

    // 辅助数值（可选）
    // 睡眠：睡眠质量评分(1-5)
    // 血压：舒张压
    // 运动：消耗卡路里
    val secondaryValue: Double? = null,

    // 心情/状态评分 (1-5)
    // 1: 很差, 2: 较差, 3: 一般, 4: 良好, 5: 很好
    val rating: Int? = null,

    // 分类标签
    // 运动类型：跑步、游泳、骑行、健身、瑜伽等
    // 心情来源：工作、家庭、健康、社交等
    val category: String? = null,

    // 备注说明
    val note: String = "",

    // 单位
    val unit: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 健康记录类型
 */
object HealthRecordType {
    const val WEIGHT = "WEIGHT"           // 体重
    const val SLEEP = "SLEEP"             // 睡眠
    const val EXERCISE = "EXERCISE"       // 运动
    const val MOOD = "MOOD"               // 心情
    const val WATER = "WATER"             // 饮水
    const val BLOOD_PRESSURE = "BLOOD_PRESSURE" // 血压
    const val HEART_RATE = "HEART_RATE"   // 心率
    const val STEPS = "STEPS"             // 步数
    const val CUSTOM = "CUSTOM"           // 自定义

    fun getDisplayName(type: String): String = when (type) {
        WEIGHT -> "体重"
        SLEEP -> "睡眠"
        EXERCISE -> "运动"
        MOOD -> "心情"
        WATER -> "饮水"
        BLOOD_PRESSURE -> "血压"
        HEART_RATE -> "心率"
        STEPS -> "步数"
        CUSTOM -> "自定义"
        else -> type
    }

    fun getUnit(type: String): String = when (type) {
        WEIGHT -> "kg"
        SLEEP -> "小时"
        EXERCISE -> "分钟"
        MOOD -> ""
        WATER -> "ml"
        BLOOD_PRESSURE -> "mmHg"
        HEART_RATE -> "bpm"
        STEPS -> "步"
        else -> ""
    }

    fun getIcon(type: String): String = when (type) {
        WEIGHT -> "⚖️"
        SLEEP -> "😴"
        EXERCISE -> "🏃"
        MOOD -> "😊"
        WATER -> "💧"
        BLOOD_PRESSURE -> "❤️"
        HEART_RATE -> "💓"
        STEPS -> "👣"
        CUSTOM -> "📝"
        else -> "📊"
    }
}

/**
 * 运动类型
 */
object ExerciseCategory {
    const val RUNNING = "RUNNING"         // 跑步
    const val WALKING = "WALKING"         // 步行
    const val CYCLING = "CYCLING"         // 骑行
    const val SWIMMING = "SWIMMING"       // 游泳
    const val GYM = "GYM"                 // 健身
    const val YOGA = "YOGA"               // 瑜伽
    const val BASKETBALL = "BASKETBALL"   // 篮球
    const val FOOTBALL = "FOOTBALL"       // 足球
    const val BADMINTON = "BADMINTON"     // 羽毛球
    const val OTHER = "OTHER"             // 其他

    fun getAllCategories(): List<String> = listOf(
        RUNNING, WALKING, CYCLING, SWIMMING, GYM,
        YOGA, BASKETBALL, FOOTBALL, BADMINTON, OTHER
    )

    fun getDisplayName(category: String): String = when (category) {
        RUNNING -> "跑步"
        WALKING -> "步行"
        CYCLING -> "骑行"
        SWIMMING -> "游泳"
        GYM -> "健身"
        YOGA -> "瑜伽"
        BASKETBALL -> "篮球"
        FOOTBALL -> "足球"
        BADMINTON -> "羽毛球"
        OTHER -> "其他"
        else -> category
    }

    fun getIcon(category: String): String = when (category) {
        RUNNING -> "🏃"
        WALKING -> "🚶"
        CYCLING -> "🚴"
        SWIMMING -> "🏊"
        GYM -> "🏋️"
        YOGA -> "🧘"
        BASKETBALL -> "🏀"
        FOOTBALL -> "⚽"
        BADMINTON -> "🏸"
        OTHER -> "🎯"
        else -> "🏃"
    }
}

/**
 * 心情来源
 */
object MoodSource {
    const val WORK = "WORK"               // 工作
    const val FAMILY = "FAMILY"           // 家庭
    const val HEALTH = "HEALTH"           // 健康
    const val SOCIAL = "SOCIAL"           // 社交
    const val FINANCE = "FINANCE"         // 财务
    const val HOBBY = "HOBBY"             // 爱好
    const val RELATIONSHIP = "RELATIONSHIP" // 感情
    const val OTHER = "OTHER"             // 其他

    fun getDisplayName(source: String): String = when (source) {
        WORK -> "工作"
        FAMILY -> "家庭"
        HEALTH -> "健康"
        SOCIAL -> "社交"
        FINANCE -> "财务"
        HOBBY -> "爱好"
        RELATIONSHIP -> "感情"
        OTHER -> "其他"
        else -> source
    }
}

/**
 * 睡眠质量评级
 */
object SleepQuality {
    const val VERY_POOR = 1   // 很差
    const val POOR = 2        // 较差
    const val NORMAL = 3      // 一般
    const val GOOD = 4        // 良好
    const val EXCELLENT = 5   // 优秀

    fun getDisplayName(rating: Int): String = when (rating) {
        VERY_POOR -> "很差"
        POOR -> "较差"
        NORMAL -> "一般"
        GOOD -> "良好"
        EXCELLENT -> "优秀"
        else -> "未知"
    }

    fun getIcon(rating: Int): String = when (rating) {
        VERY_POOR -> "😫"
        POOR -> "😕"
        NORMAL -> "😐"
        GOOD -> "😊"
        EXCELLENT -> "😴"
        else -> "😐"
    }
}

/**
 * 心情评级
 */
object MoodRating {
    const val VERY_SAD = 1    // 很沮丧
    const val SAD = 2         // 低落
    const val NEUTRAL = 3     // 平静
    const val HAPPY = 4       // 开心
    const val VERY_HAPPY = 5  // 非常开心

    fun getDisplayName(rating: Int): String = when (rating) {
        VERY_SAD -> "很沮丧"
        SAD -> "低落"
        NEUTRAL -> "平静"
        HAPPY -> "开心"
        VERY_HAPPY -> "非常开心"
        else -> "未知"
    }

    fun getIcon(rating: Int): String = when (rating) {
        VERY_SAD -> "😢"
        SAD -> "😔"
        NEUTRAL -> "😐"
        HAPPY -> "😊"
        VERY_HAPPY -> "🥳"
        else -> "😐"
    }
}
