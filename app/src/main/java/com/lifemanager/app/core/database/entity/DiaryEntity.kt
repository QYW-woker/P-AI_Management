package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 日记实体类
 *
 * 用于记录日常生活点滴
 * 支持AI情绪分析和主题标签
 * 可附加图片、语音等多媒体
 * 支持位置记录
 */
@Entity(
    tableName = "diaries",
    indices = [
        Index(value = ["date"], unique = true),
        Index(value = ["moodScore"]),
        Index(value = ["createdAt"])
    ]
)
data class DiaryEntity(
    // 主键ID
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 日期，epochDay格式
    // 每天只能有一篇日记
    val date: Int,

    // 日记标题
    val title: String = "",

    // 日记内容
    val content: String,

    // 心情评分 (1-5)
    // 1: 很差, 2: 较差, 3: 一般, 4: 较好, 5: 很好
    val moodScore: Int? = null,

    // AI分析的情绪标签，JSON数组格式
    // 如: ["开心", "感恩", "期待"]
    val moodTags: String = "[]",

    // AI分类的主题标签，JSON数组格式
    // 如: ["工作", "学习", "社交"]
    val topicTags: String = "[]",

    // 附件路径（图片/语音/视频），JSON数组格式
    val attachments: String = "[]",

    // 天气
    val weather: String? = null,

    // ==================== 位置信息 ====================

    // 位置名称（如：北京市朝阳区）
    val locationName: String? = null,

    // 详细地址
    val locationAddress: String? = null,

    // 纬度
    val latitude: Double? = null,

    // 经度
    val longitude: Double? = null,

    // POI名称（如：咖啡馆名称）
    val poiName: String? = null,

    // ==================== 旧字段（保留兼容） ====================

    // 位置（兼容旧数据）
    val location: String? = null,

    // 睡眠时长（分钟）
    val sleepMinutes: Int? = null,

    // ==================== 时间戳 ====================

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis(),

    // 是否已删除（软删除）
    val isDeleted: Boolean = false,

    // 是否收藏
    val isFavorite: Boolean = false,

    // 是否私密（需要额外验证）
    val isPrivate: Boolean = false,

    // 字数统计
    val wordCount: Int = 0
)

/**
 * 心情评分枚举
 */
object MoodScore {
    const val VERY_BAD = 1      // 很差
    const val BAD = 2           // 较差
    const val NORMAL = 3        // 一般
    const val GOOD = 4          // 较好
    const val VERY_GOOD = 5     // 很好
}

/**
 * 天气选项
 */
object Weather {
    const val SUNNY = "SUNNY"           // 晴天
    const val CLOUDY = "CLOUDY"         // 多云
    const val OVERCAST = "OVERCAST"     // 阴天
    const val LIGHT_RAIN = "LIGHT_RAIN" // 小雨
    const val RAINY = "RAINY"           // 雨天
    const val HEAVY_RAIN = "HEAVY_RAIN" // 大雨
    const val THUNDERSTORM = "THUNDERSTORM" // 雷雨
    const val SNOWY = "SNOWY"           // 雪天
    const val WINDY = "WINDY"           // 大风
    const val FOGGY = "FOGGY"           // 雾天
    const val HAZY = "HAZY"             // 霾

    fun getIcon(weather: String): String = when (weather) {
        SUNNY -> "☀️"
        CLOUDY -> "⛅"
        OVERCAST -> "☁️"
        LIGHT_RAIN -> "🌦️"
        RAINY -> "🌧️"
        HEAVY_RAIN -> "⛈️"
        THUNDERSTORM -> "🌩️"
        SNOWY -> "❄️"
        WINDY -> "💨"
        FOGGY -> "🌫️"
        HAZY -> "😷"
        else -> "🌤️"
    }

    fun getName(weather: String): String = when (weather) {
        SUNNY -> "晴天"
        CLOUDY -> "多云"
        OVERCAST -> "阴天"
        LIGHT_RAIN -> "小雨"
        RAINY -> "雨天"
        HEAVY_RAIN -> "大雨"
        THUNDERSTORM -> "雷雨"
        SNOWY -> "雪天"
        WINDY -> "大风"
        FOGGY -> "雾天"
        HAZY -> "霾"
        else -> "未知"
    }

    fun getAll(): List<Pair<String, String>> = listOf(
        SUNNY to "晴天",
        CLOUDY to "多云",
        OVERCAST to "阴天",
        LIGHT_RAIN to "小雨",
        RAINY to "雨天",
        HEAVY_RAIN to "大雨",
        THUNDERSTORM to "雷雨",
        SNOWY to "雪天",
        WINDY to "大风",
        FOGGY to "雾天",
        HAZY to "霾"
    )
}

/**
 * 心情图标
 */
object MoodIcon {
    fun getIcon(score: Int): String = when (score) {
        1 -> "😢"
        2 -> "😞"
        3 -> "😐"
        4 -> "😊"
        5 -> "😄"
        else -> "😶"
    }

    fun getName(score: Int): String = when (score) {
        1 -> "很差"
        2 -> "较差"
        3 -> "一般"
        4 -> "较好"
        5 -> "很好"
        else -> "未知"
    }

    fun getColor(score: Int): Long = when (score) {
        1 -> 0xFF9E9E9E  // 灰色
        2 -> 0xFFFF9800  // 橙色
        3 -> 0xFFFFC107  // 黄色
        4 -> 0xFF8BC34A  // 浅绿
        5 -> 0xFF4CAF50  // 绿色
        else -> 0xFF9E9E9E
    }
}

/**
 * 位置数据类
 */
data class DiaryLocation(
    val name: String,           // 位置名称
    val address: String? = null, // 详细地址
    val latitude: Double,       // 纬度
    val longitude: Double,      // 经度
    val poiName: String? = null // POI名称
) {
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return poiName ?: name
    }

    /**
     * 转换为实体字段
     */
    fun toEntityFields(): Map<String, Any?> = mapOf(
        "locationName" to name,
        "locationAddress" to address,
        "latitude" to latitude,
        "longitude" to longitude,
        "poiName" to poiName
    )

    companion object {
        /**
         * 从实体创建
         */
        fun fromEntity(entity: DiaryEntity): DiaryLocation? {
            if (entity.latitude == null || entity.longitude == null) return null
            return DiaryLocation(
                name = entity.locationName ?: "",
                address = entity.locationAddress,
                latitude = entity.latitude,
                longitude = entity.longitude,
                poiName = entity.poiName
            )
        }
    }
}
