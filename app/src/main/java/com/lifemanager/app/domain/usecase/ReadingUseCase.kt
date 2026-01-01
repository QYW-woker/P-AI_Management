package com.lifemanager.app.domain.usecase

import com.lifemanager.app.core.database.dao.DailyReadingStats
import com.lifemanager.app.core.database.entity.*
import com.lifemanager.app.data.repository.ReadingRepository
import com.lifemanager.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 阅读模块用例类
 *
 * 提供书籍管理、阅读记录、笔记等业务逻辑
 */
@Singleton
class ReadingUseCase @Inject constructor(
    private val readingRepository: ReadingRepository
) {

    // ==================== 书籍管理 ====================

    /**
     * 添加新书
     */
    suspend fun addBook(
        title: String,
        author: String = "",
        translator: String = "",
        publisher: String = "",
        publishYear: Int? = null,
        isbn: String = "",
        coverImage: String = "",
        categoryId: Long? = null,
        categoryName: String = "",
        totalPages: Int = 0,
        source: String = BookSource.BOUGHT,
        format: String = BookFormat.PAPER,
        price: Double = 0.0,
        purchaseLocation: String = "",
        tags: String = "[]",
        priority: String = ReadingPriority.MEDIUM,
        notes: String = "",
        doubanUrl: String = "",
        status: String = ReadingStatus.UNREAD
    ): Long {
        val book = BookEntity(
            title = title,
            author = author,
            translator = translator,
            publisher = publisher,
            publishYear = publishYear,
            isbn = isbn,
            coverImage = coverImage,
            categoryId = categoryId,
            categoryName = categoryName,
            totalPages = totalPages,
            status = status,
            source = source,
            format = format,
            price = price,
            purchaseLocation = purchaseLocation,
            tags = tags,
            priority = priority,
            notes = notes,
            doubanUrl = doubanUrl
        )
        return readingRepository.insertBook(book)
    }

    /**
     * 更新书籍信息
     */
    suspend fun updateBook(book: BookEntity) {
        readingRepository.updateBook(book.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 删除书籍
     */
    suspend fun deleteBook(bookId: Long) {
        readingRepository.deleteBookById(bookId)
    }

    /**
     * 获取书籍详情
     */
    suspend fun getBook(bookId: Long): BookEntity? {
        return readingRepository.getBookById(bookId)
    }

    /**
     * 获取书籍详情Flow
     */
    fun getBookFlow(bookId: Long): Flow<BookEntity?> {
        return readingRepository.getBookByIdFlow(bookId)
    }

    /**
     * 获取所有书籍
     */
    fun getAllBooks(): Flow<List<BookEntity>> {
        return readingRepository.getAllBooks()
    }

    /**
     * 按状态获取书籍
     */
    fun getBooksByStatus(status: String): Flow<List<BookEntity>> {
        return readingRepository.getBooksByStatus(status)
    }

    /**
     * 获取按状态分组的书籍
     */
    fun getBooksGroupedByStatus(): Flow<Map<String, List<BookEntity>>> {
        return readingRepository.getAllBooks().map { books ->
            books.groupBy { it.status }
        }
    }

    /**
     * 获取正在阅读的书籍
     */
    fun getReadingBooks(): Flow<List<BookEntity>> {
        return readingRepository.getBooksByStatus(ReadingStatus.READING)
    }

    /**
     * 获取已完成的书籍
     */
    fun getFinishedBooks(): Flow<List<BookEntity>> {
        return readingRepository.getBooksByStatus(ReadingStatus.FINISHED)
    }

    /**
     * 获取想读的书籍
     */
    fun getWishlistBooks(): Flow<List<BookEntity>> {
        return readingRepository.getBooksByStatus(ReadingStatus.WISH)
    }

    /**
     * 获取收藏的书籍
     */
    fun getFavoriteBooks(): Flow<List<BookEntity>> {
        return readingRepository.getFavoriteBooks()
    }

    /**
     * 搜索书籍
     */
    fun searchBooks(query: String): Flow<List<BookEntity>> {
        return readingRepository.searchBooks(query)
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(bookId: Long) {
        val book = readingRepository.getBookById(bookId)
        book?.let {
            readingRepository.updateFavoriteStatus(bookId, !it.isFavorite)
        }
    }

    // ==================== 阅读进度 ====================

    /**
     * 更新阅读进度
     */
    suspend fun updateReadingProgress(bookId: Long, currentPage: Int) {
        val book = readingRepository.getBookById(bookId) ?: return
        val now = System.currentTimeMillis()
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()

        // 更新状态
        val newStatus = when {
            currentPage >= book.totalPages && book.totalPages > 0 -> ReadingStatus.FINISHED
            currentPage > 0 -> ReadingStatus.READING
            else -> book.status
        }

        // 获取上次阅读页数
        val pagesRead = currentPage - book.currentPage

        // 更新书籍进度
        val finishDate = if (newStatus == ReadingStatus.FINISHED) today else null
        readingRepository.updateReadingProgress(bookId, currentPage, newStatus)
        if (finishDate != null) {
            readingRepository.updateBookStatus(bookId, newStatus, finishDate)
        }

        // 更新年度目标
        if (newStatus == ReadingStatus.FINISHED && book.status != ReadingStatus.FINISHED) {
            val year = Year.now().value
            readingRepository.incrementCompletedBooks(year)
        }

        if (pagesRead > 0) {
            val year = Year.now().value
            readingRepository.addCompletedPages(year, pagesRead)
        }
    }

    /**
     * 开始阅读
     */
    suspend fun startReading(bookId: Long) {
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
        val book = readingRepository.getBookById(bookId) ?: return

        if (book.status == ReadingStatus.UNREAD || book.status == ReadingStatus.WISH) {
            readingRepository.updateBook(
                book.copy(
                    status = ReadingStatus.READING,
                    startDate = today,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 完成阅读
     */
    suspend fun finishReading(bookId: Long, rating: Int = 0, shortReview: String = "") {
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
        val book = readingRepository.getBookById(bookId) ?: return

        readingRepository.updateBook(
            book.copy(
                status = ReadingStatus.FINISHED,
                currentPage = book.totalPages,
                finishDate = today,
                rating = rating,
                shortReview = shortReview,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 更新年度目标
        if (book.status != ReadingStatus.FINISHED) {
            val year = Year.now().value
            readingRepository.incrementCompletedBooks(year)
        }
    }

    /**
     * 放弃阅读
     */
    suspend fun abandonReading(bookId: Long) {
        readingRepository.updateBookStatus(bookId, ReadingStatus.ABANDONED)
    }

    /**
     * 重新阅读
     */
    suspend fun restartReading(bookId: Long) {
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
        val book = readingRepository.getBookById(bookId) ?: return

        readingRepository.updateBook(
            book.copy(
                status = ReadingStatus.READING,
                currentPage = 0,
                startDate = today,
                finishDate = null,
                rating = 0,
                shortReview = "",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // ==================== 阅读记录 ====================

    /**
     * 记录阅读会话
     */
    suspend fun recordReadingSession(
        bookId: Long,
        startPage: Int,
        endPage: Int,
        duration: Int = 0,
        startTime: String = "",
        endTime: String = "",
        notes: String = ""
    ): Long {
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()

        val session = ReadingSessionEntity(
            bookId = bookId,
            date = today,
            startPage = startPage,
            endPage = endPage,
            duration = duration,
            startTime = startTime,
            endTime = endTime,
            notes = notes
        )

        // 更新书籍的阅读时长
        if (duration > 0) {
            readingRepository.addReadingTime(bookId, duration)

            // 更新年度目标
            val year = Year.now().value
            readingRepository.addCompletedMinutes(year, duration)
        }

        // 更新阅读进度
        if (endPage > 0) {
            updateReadingProgress(bookId, endPage)
        }

        return readingRepository.insertReadingSession(session)
    }

    /**
     * 获取书籍的阅读记录
     */
    fun getReadingSessionsByBook(bookId: Long): Flow<List<ReadingSessionEntity>> {
        return readingRepository.getReadingSessionsByBook(bookId)
    }

    /**
     * 获取某日期的阅读记录
     */
    fun getReadingSessionsByDate(date: Int): Flow<List<ReadingSessionEntity>> {
        return readingRepository.getReadingSessionsByDate(date)
    }

    /**
     * 获取每日阅读统计
     */
    suspend fun getDailyReadingStats(startDate: Int, endDate: Int): List<DailyReadingStats> {
        return readingRepository.getDailyReadingStats(startDate, endDate)
    }

    // ==================== 读书笔记 ====================

    /**
     * 添加读书笔记
     */
    suspend fun addReadingNote(
        bookId: Long,
        noteType: String = NoteType.THOUGHT,
        content: String,
        excerpt: String = "",
        pageNumber: Int? = null,
        chapter: String = "",
        tags: String = "[]"
    ): Long {
        val note = ReadingNoteEntity(
            bookId = bookId,
            noteType = noteType,
            content = content,
            excerpt = excerpt,
            pageNumber = pageNumber,
            chapter = chapter,
            tags = tags
        )
        return readingRepository.insertReadingNote(note)
    }

    /**
     * 更新读书笔记
     */
    suspend fun updateReadingNote(note: ReadingNoteEntity) {
        readingRepository.updateReadingNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 删除读书笔记
     */
    suspend fun deleteReadingNote(noteId: Long) {
        readingRepository.deleteReadingNoteById(noteId)
    }

    /**
     * 获取书籍的所有笔记
     */
    fun getNotesByBook(bookId: Long): Flow<List<ReadingNoteEntity>> {
        return readingRepository.getReadingNotesByBook(bookId)
    }

    /**
     * 按类型获取笔记
     */
    fun getNotesByType(bookId: Long, type: String): Flow<List<ReadingNoteEntity>> {
        return readingRepository.getReadingNotesByType(bookId, type)
    }

    /**
     * 获取收藏的笔记
     */
    fun getFavoriteNotes(): Flow<List<ReadingNoteEntity>> {
        return readingRepository.getFavoriteNotes()
    }

    /**
     * 搜索笔记
     */
    fun searchNotes(query: String): Flow<List<ReadingNoteEntity>> {
        return readingRepository.searchNotes(query)
    }

    /**
     * 切换笔记收藏状态
     */
    suspend fun toggleNoteFavorite(noteId: Long) {
        val note = readingRepository.getReadingNoteById(noteId)
        note?.let {
            readingRepository.updateNoteFavoriteStatus(noteId, !it.isFavorite)
        }
    }

    // ==================== 书架管理 ====================

    /**
     * 创建书架
     */
    suspend fun createBookShelf(
        name: String,
        description: String = "",
        icon: String = "bookshelf",
        color: String = "#795548"
    ): Long {
        val shelf = BookShelfEntity(
            name = name,
            description = description,
            icon = icon,
            color = color
        )
        return readingRepository.insertBookShelf(shelf)
    }

    /**
     * 获取所有书架
     */
    fun getAllBookShelves(): Flow<List<BookShelfEntity>> {
        return readingRepository.getAllBookShelves()
    }

    /**
     * 获取书架中的书籍
     */
    fun getBooksByShelf(shelfId: Long): Flow<List<BookEntity>> {
        return readingRepository.getBooksByShelf(shelfId)
    }

    /**
     * 添加书籍到书架
     */
    suspend fun addBookToShelf(bookId: Long, shelfId: Long) {
        readingRepository.addBookToShelf(bookId, shelfId)
    }

    /**
     * 从书架移除书籍
     */
    suspend fun removeBookFromShelf(bookId: Long, shelfId: Long) {
        readingRepository.removeBookFromShelf(bookId, shelfId)
    }

    // ==================== 阅读目标 ====================

    /**
     * 设置年度阅读目标
     */
    suspend fun setYearlyGoal(
        year: Int = Year.now().value,
        targetBooks: Int = 12,
        targetPages: Int = 0,
        targetMinutes: Int = 0
    ): Long {
        val existingGoal = readingRepository.getReadingGoalByYear(year)

        return if (existingGoal != null) {
            readingRepository.updateReadingGoal(
                existingGoal.copy(
                    targetBooks = targetBooks,
                    targetPages = targetPages,
                    targetMinutes = targetMinutes,
                    updatedAt = System.currentTimeMillis()
                )
            )
            existingGoal.id
        } else {
            val goal = ReadingGoalEntity(
                year = year,
                targetBooks = targetBooks,
                targetPages = targetPages,
                targetMinutes = targetMinutes
            )
            readingRepository.insertReadingGoal(goal)
        }
    }

    /**
     * 获取当年阅读目标
     */
    fun getCurrentYearGoal(): Flow<ReadingGoalEntity?> {
        return readingRepository.getReadingGoalByYearFlow(Year.now().value)
    }

    /**
     * 获取所有阅读目标
     */
    fun getAllReadingGoals(): Flow<List<ReadingGoalEntity>> {
        return readingRepository.getAllReadingGoals()
    }

    // ==================== 统计分析 ====================

    /**
     * 获取阅读概览统计
     */
    suspend fun getReadingOverview(): ReadingOverview {
        val totalBooks = readingRepository.getTotalBookCount()
        val readingCount = readingRepository.getBookCountByStatus(ReadingStatus.READING)
        val finishedCount = readingRepository.getBookCountByStatus(ReadingStatus.FINISHED)
        val wishCount = readingRepository.getBookCountByStatus(ReadingStatus.WISH)
        val totalPages = readingRepository.getTotalPagesRead()
        val totalMinutes = readingRepository.getTotalReadingTime()
        val avgRating = readingRepository.getAverageRating()

        return ReadingOverview(
            totalBooks = totalBooks,
            readingCount = readingCount,
            finishedCount = finishedCount,
            wishCount = wishCount,
            abandonedCount = readingRepository.getBookCountByStatus(ReadingStatus.ABANDONED),
            totalPagesRead = totalPages,
            totalReadingMinutes = totalMinutes,
            averageRating = avgRating
        )
    }

    /**
     * 获取年度阅读统计
     */
    suspend fun getYearlyReadingStats(year: Int = Year.now().value): YearlyReadingStats {
        val startDate = (year * 10000 + 101) // YYYYMMDD格式：年初
        val endDate = (year * 10000 + 1231) // YYYYMMDD格式：年末

        val finishedBooks = readingRepository.getFinishedBookCountInPeriod(startDate, endDate)
        val pagesRead = readingRepository.getTotalPagesReadInPeriod(startDate, endDate)
        val minutesRead = readingRepository.getTotalReadingTimeInPeriod(startDate, endDate)

        val goal = readingRepository.getReadingGoalByYear(year)

        return YearlyReadingStats(
            year = year,
            booksFinished = finishedBooks,
            pagesRead = pagesRead,
            minutesRead = minutesRead,
            targetBooks = goal?.targetBooks ?: 0,
            targetPages = goal?.targetPages ?: 0,
            targetMinutes = goal?.targetMinutes ?: 0
        )
    }
}

/**
 * 阅读概览统计
 */
data class ReadingOverview(
    val totalBooks: Int = 0,
    val readingCount: Int = 0,
    val finishedCount: Int = 0,
    val wishCount: Int = 0,
    val abandonedCount: Int = 0,
    val totalPagesRead: Int = 0,
    val totalReadingMinutes: Int = 0,
    val averageRating: Double = 0.0
) {
    val totalReadingHours: Double get() = totalReadingMinutes / 60.0
}

/**
 * 年度阅读统计
 */
data class YearlyReadingStats(
    val year: Int,
    val booksFinished: Int = 0,
    val pagesRead: Int = 0,
    val minutesRead: Int = 0,
    val targetBooks: Int = 0,
    val targetPages: Int = 0,
    val targetMinutes: Int = 0
) {
    val booksProgressPercent: Int
        get() = if (targetBooks > 0) (booksFinished * 100 / targetBooks).coerceAtMost(100) else 0

    val pagesProgressPercent: Int
        get() = if (targetPages > 0) (pagesRead * 100 / targetPages).coerceAtMost(100) else 0

    val minutesProgressPercent: Int
        get() = if (targetMinutes > 0) (minutesRead * 100 / targetMinutes).coerceAtMost(100) else 0

    val hoursRead: Double get() = minutesRead / 60.0
}
