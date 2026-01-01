package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * 书籍数据访问对象
 */
@Dao
interface BookDao {

    // ==================== 书籍 CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookByIdFlow(bookId: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY updatedAt DESC")
    fun getBooksByStatus(status: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY updatedAt DESC")
    suspend fun getBooksByStatusSync(status: String): List<BookEntity>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getBooksByCategory(categoryId: Long): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE rating >= :minRating ORDER BY rating DESC")
    fun getBooksByRating(minRating: Int): Flow<List<BookEntity>>

    @Query("UPDATE books SET currentPage = :page, status = :status, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, page: Int, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE books SET status = :status, finishDate = :finishDate, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun updateBookStatus(bookId: Long, status: String, finishDate: Int? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE books SET rating = :rating, shortReview = :shortReview, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun updateBookRating(bookId: Long, rating: Int, shortReview: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE books SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun updateFavoriteStatus(bookId: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE books SET actualReadingTime = actualReadingTime + :minutes, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun addReadingTime(bookId: Long, minutes: Int, updatedAt: Long = System.currentTimeMillis())

    // ==================== 统计查询 ====================

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getTotalBookCount(): Int

    @Query("SELECT COUNT(*) FROM books WHERE status = :status")
    suspend fun getBookCountByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM books WHERE finishDate BETWEEN :startDate AND :endDate")
    suspend fun getFinishedBookCountInPeriod(startDate: Int, endDate: Int): Int

    @Query("SELECT SUM(totalPages) FROM books WHERE status = 'FINISHED'")
    suspend fun getTotalPagesRead(): Int?

    @Query("SELECT SUM(actualReadingTime) FROM books")
    suspend fun getTotalReadingTime(): Int?

    @Query("SELECT AVG(rating) FROM books WHERE rating > 0")
    suspend fun getAverageRating(): Double?

    // ==================== 阅读记录 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingSession(session: ReadingSessionEntity): Long

    @Update
    suspend fun updateReadingSession(session: ReadingSessionEntity)

    @Delete
    suspend fun deleteReadingSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY date DESC, createdAt DESC")
    fun getReadingSessionsByBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE date = :date ORDER BY createdAt DESC")
    fun getReadingSessionsByDate(date: Int): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getReadingSessionsInPeriod(startDate: Int, endDate: Int): Flow<List<ReadingSessionEntity>>

    @Query("SELECT SUM(duration) FROM reading_sessions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalReadingTimeInPeriod(startDate: Int, endDate: Int): Int?

    @Query("SELECT SUM(endPage - startPage) FROM reading_sessions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalPagesReadInPeriod(startDate: Int, endDate: Int): Int?

    @Query("SELECT date, SUM(duration) as totalDuration FROM reading_sessions WHERE date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date")
    suspend fun getDailyReadingStats(startDate: Int, endDate: Int): List<DailyReadingStats>

    // ==================== 读书笔记 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingNote(note: ReadingNoteEntity): Long

    @Update
    suspend fun updateReadingNote(note: ReadingNoteEntity)

    @Delete
    suspend fun deleteReadingNote(note: ReadingNoteEntity)

    @Query("DELETE FROM reading_notes WHERE id = :noteId")
    suspend fun deleteReadingNoteById(noteId: Long)

    @Query("SELECT * FROM reading_notes WHERE id = :noteId")
    suspend fun getReadingNoteById(noteId: Long): ReadingNoteEntity?

    @Query("SELECT * FROM reading_notes WHERE bookId = :bookId ORDER BY pageNumber ASC, createdAt DESC")
    fun getReadingNotesByBook(bookId: Long): Flow<List<ReadingNoteEntity>>

    @Query("SELECT * FROM reading_notes WHERE bookId = :bookId AND noteType = :type ORDER BY createdAt DESC")
    fun getReadingNotesByType(bookId: Long, type: String): Flow<List<ReadingNoteEntity>>

    @Query("SELECT * FROM reading_notes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteNotes(): Flow<List<ReadingNoteEntity>>

    @Query("SELECT * FROM reading_notes WHERE content LIKE '%' || :query || '%' OR excerpt LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchNotes(query: String): Flow<List<ReadingNoteEntity>>

    @Query("UPDATE reading_notes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateNoteFavoriteStatus(noteId: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM reading_notes WHERE bookId = :bookId")
    suspend fun getNoteCountByBook(bookId: Long): Int

    // ==================== 书架管理 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookShelf(shelf: BookShelfEntity): Long

    @Update
    suspend fun updateBookShelf(shelf: BookShelfEntity)

    @Delete
    suspend fun deleteBookShelf(shelf: BookShelfEntity)

    @Query("SELECT * FROM book_shelves ORDER BY sortOrder ASC")
    fun getAllBookShelves(): Flow<List<BookShelfEntity>>

    @Query("SELECT * FROM book_shelves WHERE id = :shelfId")
    suspend fun getBookShelfById(shelfId: Long): BookShelfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookToShelf(mapping: BookShelfMappingEntity)

    @Query("DELETE FROM book_shelf_mapping WHERE bookId = :bookId AND shelfId = :shelfId")
    suspend fun removeBookFromShelf(bookId: Long, shelfId: Long)

    @Query("SELECT b.* FROM books b INNER JOIN book_shelf_mapping m ON b.id = m.bookId WHERE m.shelfId = :shelfId ORDER BY m.addedAt DESC")
    fun getBooksByShelf(shelfId: Long): Flow<List<BookEntity>>

    @Query("SELECT s.* FROM book_shelves s INNER JOIN book_shelf_mapping m ON s.id = m.shelfId WHERE m.bookId = :bookId")
    fun getShelvesForBook(bookId: Long): Flow<List<BookShelfEntity>>

    @Query("SELECT COUNT(*) FROM book_shelf_mapping WHERE shelfId = :shelfId")
    suspend fun getBookCountInShelf(shelfId: Long): Int

    // ==================== 阅读目标 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingGoal(goal: ReadingGoalEntity): Long

    @Update
    suspend fun updateReadingGoal(goal: ReadingGoalEntity)

    @Query("SELECT * FROM reading_goals WHERE year = :year")
    suspend fun getReadingGoalByYear(year: Int): ReadingGoalEntity?

    @Query("SELECT * FROM reading_goals WHERE year = :year")
    fun getReadingGoalByYearFlow(year: Int): Flow<ReadingGoalEntity?>

    @Query("SELECT * FROM reading_goals ORDER BY year DESC")
    fun getAllReadingGoals(): Flow<List<ReadingGoalEntity>>

    @Query("UPDATE reading_goals SET completedBooks = completedBooks + 1, updatedAt = :updatedAt WHERE year = :year")
    suspend fun incrementCompletedBooks(year: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reading_goals SET completedPages = completedPages + :pages, updatedAt = :updatedAt WHERE year = :year")
    suspend fun addCompletedPages(year: Int, pages: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reading_goals SET completedMinutes = completedMinutes + :minutes, updatedAt = :updatedAt WHERE year = :year")
    suspend fun addCompletedMinutes(year: Int, minutes: Int, updatedAt: Long = System.currentTimeMillis())
}

/**
 * 每日阅读统计结果
 */
data class DailyReadingStats(
    val date: Int,
    val totalDuration: Int
)
