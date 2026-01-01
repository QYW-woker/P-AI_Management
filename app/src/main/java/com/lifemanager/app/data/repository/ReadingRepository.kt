package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.BookDao
import com.lifemanager.app.core.database.dao.DailyReadingStats
import com.lifemanager.app.core.database.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 阅读模块数据仓库
 *
 * 提供书籍、阅读记录、笔记等数据的访问接口
 */
@Singleton
class ReadingRepository @Inject constructor(
    private val bookDao: BookDao
) {

    // ==================== 书籍管理 ====================

    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    suspend fun deleteBookById(bookId: Long) = bookDao.deleteBookById(bookId)

    suspend fun getBookById(bookId: Long): BookEntity? = bookDao.getBookById(bookId)

    fun getBookByIdFlow(bookId: Long): Flow<BookEntity?> = bookDao.getBookByIdFlow(bookId)

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBooksByStatus(status: String): Flow<List<BookEntity>> = bookDao.getBooksByStatus(status)

    suspend fun getBooksByStatusSync(status: String): List<BookEntity> = bookDao.getBooksByStatusSync(status)

    fun getFavoriteBooks(): Flow<List<BookEntity>> = bookDao.getFavoriteBooks()

    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.searchBooks(query)

    fun getBooksByCategory(categoryId: Long): Flow<List<BookEntity>> = bookDao.getBooksByCategory(categoryId)

    fun getBooksByRating(minRating: Int): Flow<List<BookEntity>> = bookDao.getBooksByRating(minRating)

    suspend fun updateReadingProgress(bookId: Long, page: Int, status: String) =
        bookDao.updateReadingProgress(bookId, page, status)

    suspend fun updateBookStatus(bookId: Long, status: String, finishDate: Int? = null) =
        bookDao.updateBookStatus(bookId, status, finishDate)

    suspend fun updateBookRating(bookId: Long, rating: Int, shortReview: String) =
        bookDao.updateBookRating(bookId, rating, shortReview)

    suspend fun updateFavoriteStatus(bookId: Long, isFavorite: Boolean) =
        bookDao.updateFavoriteStatus(bookId, isFavorite)

    suspend fun addReadingTime(bookId: Long, minutes: Int) =
        bookDao.addReadingTime(bookId, minutes)

    // ==================== 统计查询 ====================

    suspend fun getTotalBookCount(): Int = bookDao.getTotalBookCount()

    suspend fun getBookCountByStatus(status: String): Int = bookDao.getBookCountByStatus(status)

    suspend fun getFinishedBookCountInPeriod(startDate: Int, endDate: Int): Int =
        bookDao.getFinishedBookCountInPeriod(startDate, endDate)

    suspend fun getTotalPagesRead(): Int = bookDao.getTotalPagesRead() ?: 0

    suspend fun getTotalReadingTime(): Int = bookDao.getTotalReadingTime() ?: 0

    suspend fun getAverageRating(): Double = bookDao.getAverageRating() ?: 0.0

    // ==================== 阅读记录 ====================

    suspend fun insertReadingSession(session: ReadingSessionEntity): Long =
        bookDao.insertReadingSession(session)

    suspend fun updateReadingSession(session: ReadingSessionEntity) =
        bookDao.updateReadingSession(session)

    suspend fun deleteReadingSession(session: ReadingSessionEntity) =
        bookDao.deleteReadingSession(session)

    fun getReadingSessionsByBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        bookDao.getReadingSessionsByBook(bookId)

    fun getReadingSessionsByDate(date: Int): Flow<List<ReadingSessionEntity>> =
        bookDao.getReadingSessionsByDate(date)

    fun getReadingSessionsInPeriod(startDate: Int, endDate: Int): Flow<List<ReadingSessionEntity>> =
        bookDao.getReadingSessionsInPeriod(startDate, endDate)

    suspend fun getTotalReadingTimeInPeriod(startDate: Int, endDate: Int): Int =
        bookDao.getTotalReadingTimeInPeriod(startDate, endDate) ?: 0

    suspend fun getTotalPagesReadInPeriod(startDate: Int, endDate: Int): Int =
        bookDao.getTotalPagesReadInPeriod(startDate, endDate) ?: 0

    suspend fun getDailyReadingStats(startDate: Int, endDate: Int): List<DailyReadingStats> =
        bookDao.getDailyReadingStats(startDate, endDate)

    // ==================== 读书笔记 ====================

    suspend fun insertReadingNote(note: ReadingNoteEntity): Long =
        bookDao.insertReadingNote(note)

    suspend fun updateReadingNote(note: ReadingNoteEntity) =
        bookDao.updateReadingNote(note)

    suspend fun deleteReadingNote(note: ReadingNoteEntity) =
        bookDao.deleteReadingNote(note)

    suspend fun deleteReadingNoteById(noteId: Long) =
        bookDao.deleteReadingNoteById(noteId)

    suspend fun getReadingNoteById(noteId: Long): ReadingNoteEntity? =
        bookDao.getReadingNoteById(noteId)

    fun getReadingNotesByBook(bookId: Long): Flow<List<ReadingNoteEntity>> =
        bookDao.getReadingNotesByBook(bookId)

    fun getReadingNotesByType(bookId: Long, type: String): Flow<List<ReadingNoteEntity>> =
        bookDao.getReadingNotesByType(bookId, type)

    fun getFavoriteNotes(): Flow<List<ReadingNoteEntity>> =
        bookDao.getFavoriteNotes()

    fun searchNotes(query: String): Flow<List<ReadingNoteEntity>> =
        bookDao.searchNotes(query)

    suspend fun updateNoteFavoriteStatus(noteId: Long, isFavorite: Boolean) =
        bookDao.updateNoteFavoriteStatus(noteId, isFavorite)

    suspend fun getNoteCountByBook(bookId: Long): Int =
        bookDao.getNoteCountByBook(bookId)

    // ==================== 书架管理 ====================

    suspend fun insertBookShelf(shelf: BookShelfEntity): Long =
        bookDao.insertBookShelf(shelf)

    suspend fun updateBookShelf(shelf: BookShelfEntity) =
        bookDao.updateBookShelf(shelf)

    suspend fun deleteBookShelf(shelf: BookShelfEntity) =
        bookDao.deleteBookShelf(shelf)

    fun getAllBookShelves(): Flow<List<BookShelfEntity>> =
        bookDao.getAllBookShelves()

    suspend fun getBookShelfById(shelfId: Long): BookShelfEntity? =
        bookDao.getBookShelfById(shelfId)

    suspend fun addBookToShelf(bookId: Long, shelfId: Long) =
        bookDao.addBookToShelf(BookShelfMappingEntity(bookId, shelfId))

    suspend fun removeBookFromShelf(bookId: Long, shelfId: Long) =
        bookDao.removeBookFromShelf(bookId, shelfId)

    fun getBooksByShelf(shelfId: Long): Flow<List<BookEntity>> =
        bookDao.getBooksByShelf(shelfId)

    fun getShelvesForBook(bookId: Long): Flow<List<BookShelfEntity>> =
        bookDao.getShelvesForBook(bookId)

    suspend fun getBookCountInShelf(shelfId: Long): Int =
        bookDao.getBookCountInShelf(shelfId)

    // ==================== 阅读目标 ====================

    suspend fun insertReadingGoal(goal: ReadingGoalEntity): Long =
        bookDao.insertReadingGoal(goal)

    suspend fun updateReadingGoal(goal: ReadingGoalEntity) =
        bookDao.updateReadingGoal(goal)

    suspend fun getReadingGoalByYear(year: Int): ReadingGoalEntity? =
        bookDao.getReadingGoalByYear(year)

    fun getReadingGoalByYearFlow(year: Int): Flow<ReadingGoalEntity?> =
        bookDao.getReadingGoalByYearFlow(year)

    fun getAllReadingGoals(): Flow<List<ReadingGoalEntity>> =
        bookDao.getAllReadingGoals()

    suspend fun incrementCompletedBooks(year: Int) =
        bookDao.incrementCompletedBooks(year)

    suspend fun addCompletedPages(year: Int, pages: Int) =
        bookDao.addCompletedPages(year, pages)

    suspend fun addCompletedMinutes(year: Int, minutes: Int) =
        bookDao.addCompletedMinutes(year, minutes)
}
