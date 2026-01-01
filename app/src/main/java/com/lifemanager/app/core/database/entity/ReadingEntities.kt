package com.lifemanager.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书籍实体类
 *
 * 记录书籍的基本信息和阅读状态
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["status"]),
        Index(value = ["categoryId"]),
        Index(value = ["rating"]),
        Index(value = ["startDate"]),
        Index(value = ["finishDate"])
    ]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 书名
    val title: String,

    // 作者
    val author: String = "",

    // 译者
    val translator: String = "",

    // 出版社
    val publisher: String = "",

    // 出版年份
    val publishYear: Int? = null,

    // ISBN
    val isbn: String = "",

    // 封面图片路径
    val coverImage: String = "",

    // 分类ID（可关联自定义字段）
    val categoryId: Long? = null,

    // 分类名称（冗余存储便于显示）
    val categoryName: String = "",

    // 总页数
    val totalPages: Int = 0,

    // 当前阅读页数
    val currentPage: Int = 0,

    // 阅读状态：UNREAD/READING/FINISHED/ABANDONED/WISH
    val status: String = ReadingStatus.UNREAD,

    // 评分（1-5星，支持半星，用0-10表示）
    val rating: Int = 0,

    // 一句话评价
    val shortReview: String = "",

    // 详细书评
    val fullReview: String = "",

    // 开始阅读日期
    val startDate: Int? = null,

    // 完成阅读日期
    val finishDate: Int? = null,

    // 书籍来源：BOUGHT/BORROWED/GIFT/EBOOK/LIBRARY
    val source: String = BookSource.BOUGHT,

    // 书籍格式：PAPER/EBOOK/AUDIOBOOK
    val format: String = BookFormat.PAPER,

    // 购买价格
    val price: Double = 0.0,

    // 购买地点/平台
    val purchaseLocation: String = "",

    // 标签（JSON数组格式）
    val tags: String = "[]",

    // 是否收藏
    val isFavorite: Boolean = false,

    // 阅读优先级：LOW/MEDIUM/HIGH
    val priority: String = ReadingPriority.MEDIUM,

    // 预计阅读时长（分钟）
    val estimatedReadingTime: Int = 0,

    // 实际阅读时长（分钟）
    val actualReadingTime: Int = 0,

    // 备注
    val notes: String = "",

    // 豆瓣链接
    val doubanUrl: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
) {
    // 计算阅读进度百分比
    val progressPercent: Int
        get() = if (totalPages > 0) (currentPage * 100 / totalPages) else 0

    // 是否已完成
    val isFinished: Boolean
        get() = status == ReadingStatus.FINISHED

    // 是否正在阅读
    val isReading: Boolean
        get() = status == ReadingStatus.READING
}

/**
 * 阅读记录实体类
 *
 * 记录每次阅读的详细信息
 */
@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["date"])
    ]
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 关联书籍ID
    val bookId: Long,

    // 阅读日期
    val date: Int,

    // 开始页码
    val startPage: Int,

    // 结束页码
    val endPage: Int,

    // 阅读时长（分钟）
    val duration: Int = 0,

    // 开始时间
    val startTime: String = "",

    // 结束时间
    val endTime: String = "",

    // 阅读感受
    val notes: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
) {
    // 本次阅读页数
    val pagesRead: Int
        get() = endPage - startPage
}

/**
 * 读书笔记实体类
 *
 * 记录阅读过程中的笔记、摘录、想法
 */
@Entity(
    tableName = "reading_notes",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["noteType"]),
        Index(value = ["pageNumber"]),
        Index(value = ["createdAt"])
    ]
)
data class ReadingNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 关联书籍ID
    val bookId: Long,

    // 笔记类型：EXCERPT/THOUGHT/SUMMARY/QUESTION/VOCABULARY
    val noteType: String = NoteType.THOUGHT,

    // 笔记内容
    val content: String,

    // 原文摘录（摘抄时使用）
    val excerpt: String = "",

    // 页码
    val pageNumber: Int? = null,

    // 章节
    val chapter: String = "",

    // 标签（JSON数组格式）
    val tags: String = "[]",

    // 是否收藏
    val isFavorite: Boolean = false,

    // 是否公开
    val isPublic: Boolean = false,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 书单/书架实体类
 *
 * 用于组织和管理书籍
 */
@Entity(
    tableName = "book_shelves",
    indices = [Index(value = ["name"])]
)
data class BookShelfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 书架名称
    val name: String,

    // 描述
    val description: String = "",

    // 图标
    val icon: String = "bookshelf",

    // 颜色
    val color: String = "#795548",

    // 是否为默认书架
    val isDefault: Boolean = false,

    // 排序顺序
    val sortOrder: Int = 0,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 书籍-书架关联表
 */
@Entity(
    tableName = "book_shelf_mapping",
    primaryKeys = ["bookId", "shelfId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookShelfEntity::class,
            parentColumns = ["id"],
            childColumns = ["shelfId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["shelfId"])
    ]
)
data class BookShelfMappingEntity(
    val bookId: Long,
    val shelfId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * 阅读目标实体类
 */
@Entity(
    tableName = "reading_goals",
    indices = [Index(value = ["year"])]
)
data class ReadingGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 目标年份
    val year: Int,

    // 目标阅读书籍数量
    val targetBooks: Int = 12,

    // 已完成书籍数量
    val completedBooks: Int = 0,

    // 目标阅读页数
    val targetPages: Int = 0,

    // 已阅读页数
    val completedPages: Int = 0,

    // 目标阅读时长（分钟）
    val targetMinutes: Int = 0,

    // 已阅读时长（分钟）
    val completedMinutes: Int = 0,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
) {
    // 书籍完成进度
    val booksProgressPercent: Int
        get() = if (targetBooks > 0) (completedBooks * 100 / targetBooks).coerceAtMost(100) else 0

    // 页数完成进度
    val pagesProgressPercent: Int
        get() = if (targetPages > 0) (completedPages * 100 / targetPages).coerceAtMost(100) else 0
}

// ==================== 枚举常量 ====================

/**
 * 阅读状态
 */
object ReadingStatus {
    const val WISH = "WISH"           // 想读
    const val UNREAD = "UNREAD"       // 未读
    const val READING = "READING"     // 在读
    const val FINISHED = "FINISHED"   // 已读
    const val ABANDONED = "ABANDONED" // 弃读

    val ALL = listOf(WISH, UNREAD, READING, FINISHED, ABANDONED)

    fun getDisplayName(status: String): String = when (status) {
        WISH -> "想读"
        UNREAD -> "未读"
        READING -> "在读"
        FINISHED -> "已读"
        ABANDONED -> "弃读"
        else -> "未知"
    }
}

/**
 * 书籍来源
 */
object BookSource {
    const val BOUGHT = "BOUGHT"       // 购买
    const val BORROWED = "BORROWED"   // 借阅
    const val GIFT = "GIFT"           // 赠送
    const val EBOOK = "EBOOK"         // 电子书
    const val LIBRARY = "LIBRARY"     // 图书馆

    fun getDisplayName(source: String): String = when (source) {
        BOUGHT -> "购买"
        BORROWED -> "借阅"
        GIFT -> "赠送"
        EBOOK -> "电子书"
        LIBRARY -> "图书馆"
        else -> "未知"
    }
}

/**
 * 书籍格式
 */
object BookFormat {
    const val PAPER = "PAPER"         // 纸质书
    const val EBOOK = "EBOOK"         // 电子书
    const val AUDIOBOOK = "AUDIOBOOK" // 有声书

    fun getDisplayName(format: String): String = when (format) {
        PAPER -> "纸质书"
        EBOOK -> "电子书"
        AUDIOBOOK -> "有声书"
        else -> "未知"
    }
}

/**
 * 阅读优先级
 */
object ReadingPriority {
    const val LOW = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH = "HIGH"

    fun getDisplayName(priority: String): String = when (priority) {
        LOW -> "低"
        MEDIUM -> "中"
        HIGH -> "高"
        else -> "未知"
    }
}

/**
 * 笔记类型
 */
object NoteType {
    const val EXCERPT = "EXCERPT"       // 摘录
    const val THOUGHT = "THOUGHT"       // 想法
    const val SUMMARY = "SUMMARY"       // 总结
    const val QUESTION = "QUESTION"     // 疑问
    const val VOCABULARY = "VOCABULARY" // 生词

    fun getDisplayName(type: String): String = when (type) {
        EXCERPT -> "摘录"
        THOUGHT -> "想法"
        SUMMARY -> "总结"
        QUESTION -> "疑问"
        VOCABULARY -> "生词"
        else -> "其他"
    }
}
