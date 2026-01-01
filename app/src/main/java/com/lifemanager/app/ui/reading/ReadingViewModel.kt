package com.lifemanager.app.ui.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.*
import com.lifemanager.app.domain.usecase.ReadingOverview
import com.lifemanager.app.domain.usecase.ReadingUseCase
import com.lifemanager.app.domain.usecase.YearlyReadingStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

/**
 * 阅读模块ViewModel
 */
@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val readingUseCase: ReadingUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    // 所有书籍
    val allBooks: StateFlow<List<BookEntity>> = readingUseCase.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 正在阅读的书籍
    val readingBooks: StateFlow<List<BookEntity>> = readingUseCase.getReadingBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 已读书籍
    val finishedBooks: StateFlow<List<BookEntity>> = readingUseCase.getFinishedBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 想读书籍
    val wishlistBooks: StateFlow<List<BookEntity>> = readingUseCase.getWishlistBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 收藏书籍
    val favoriteBooks: StateFlow<List<BookEntity>> = readingUseCase.getFavoriteBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 阅读目标
    val currentYearGoal: StateFlow<ReadingGoalEntity?> = readingUseCase.getCurrentYearGoal()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // 书架列表
    val bookShelves: StateFlow<List<BookShelfEntity>> = readingUseCase.getAllBookShelves()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 阅读概览
    private val _overview = MutableStateFlow(ReadingOverview())
    val overview: StateFlow<ReadingOverview> = _overview.asStateFlow()

    // 年度统计
    private val _yearlyStats = MutableStateFlow(YearlyReadingStats(Year.now().value))
    val yearlyStats: StateFlow<YearlyReadingStats> = _yearlyStats.asStateFlow()

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<BookEntity>>(emptyList())
    val searchResults: StateFlow<List<BookEntity>> = _searchResults.asStateFlow()

    // 当前选中的书籍详情
    private val _selectedBook = MutableStateFlow<BookEntity?>(null)
    val selectedBook: StateFlow<BookEntity?> = _selectedBook.asStateFlow()

    // 书籍笔记
    private val _bookNotes = MutableStateFlow<List<ReadingNoteEntity>>(emptyList())
    val bookNotes: StateFlow<List<ReadingNoteEntity>> = _bookNotes.asStateFlow()

    // 收藏的笔记
    val favoriteNotes: StateFlow<List<ReadingNoteEntity>> = readingUseCase.getFavoriteNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadOverview()
        loadYearlyStats()
    }

    /**
     * 加载阅读概览
     */
    private fun loadOverview() {
        viewModelScope.launch {
            try {
                _overview.value = readingUseCase.getReadingOverview()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 加载年度统计
     */
    private fun loadYearlyStats(year: Int = Year.now().value) {
        viewModelScope.launch {
            try {
                _yearlyStats.value = readingUseCase.getYearlyReadingStats(year)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadOverview()
        loadYearlyStats()
    }

    /**
     * 切换当前显示的状态筛选
     */
    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(currentStatusFilter = status) }
    }

    /**
     * 搜索书籍
     */
    fun searchBooks(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            readingUseCase.searchBooks(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    /**
     * 加载书籍详情
     */
    fun loadBookDetail(bookId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                _selectedBook.value = readingUseCase.getBook(bookId)

                // 加载书籍笔记
                readingUseCase.getNotesByBook(bookId).collect { notes ->
                    _bookNotes.value = notes
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 添加书籍
     */
    fun addBook(
        title: String,
        author: String = "",
        translator: String = "",
        publisher: String = "",
        publishYear: Int? = null,
        isbn: String = "",
        coverImage: String = "",
        totalPages: Int = 0,
        source: String = BookSource.BOUGHT,
        format: String = BookFormat.PAPER,
        price: Double = 0.0,
        status: String = ReadingStatus.UNREAD,
        onSuccess: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bookId = readingUseCase.addBook(
                    title = title,
                    author = author,
                    translator = translator,
                    publisher = publisher,
                    publishYear = publishYear,
                    isbn = isbn,
                    coverImage = coverImage,
                    totalPages = totalPages,
                    source = source,
                    format = format,
                    price = price,
                    status = status
                )
                _uiState.update { it.copy(message = "添加成功") }
                loadOverview()
                onSuccess?.invoke(bookId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "添加失败") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 更新书籍
     */
    fun updateBook(book: BookEntity) {
        viewModelScope.launch {
            try {
                readingUseCase.updateBook(book)
                _uiState.update { it.copy(message = "更新成功") }
                loadOverview()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "更新失败") }
            }
        }
    }

    /**
     * 删除书籍
     */
    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            try {
                readingUseCase.deleteBook(bookId)
                _uiState.update { it.copy(message = "删除成功") }
                loadOverview()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "删除失败") }
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(bookId: Long) {
        viewModelScope.launch {
            try {
                readingUseCase.toggleFavorite(bookId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 更新阅读进度
     */
    fun updateProgress(bookId: Long, currentPage: Int) {
        viewModelScope.launch {
            try {
                readingUseCase.updateReadingProgress(bookId, currentPage)
                _uiState.update { it.copy(message = "进度已更新") }
                loadOverview()
                loadYearlyStats()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 开始阅读
     */
    fun startReading(bookId: Long) {
        viewModelScope.launch {
            try {
                readingUseCase.startReading(bookId)
                loadOverview()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 完成阅读
     */
    fun finishReading(bookId: Long, rating: Int = 0, shortReview: String = "") {
        viewModelScope.launch {
            try {
                readingUseCase.finishReading(bookId, rating, shortReview)
                _uiState.update { it.copy(message = "已标记为读完") }
                loadOverview()
                loadYearlyStats()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 放弃阅读
     */
    fun abandonReading(bookId: Long) {
        viewModelScope.launch {
            try {
                readingUseCase.abandonReading(bookId)
                _uiState.update { it.copy(message = "已标记为弃读") }
                loadOverview()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 记录阅读会话
     */
    fun recordReadingSession(
        bookId: Long,
        startPage: Int,
        endPage: Int,
        duration: Int = 0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                readingUseCase.recordReadingSession(
                    bookId = bookId,
                    startPage = startPage,
                    endPage = endPage,
                    duration = duration,
                    notes = notes
                )
                _uiState.update { it.copy(message = "阅读记录已保存") }
                loadOverview()
                loadYearlyStats()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 添加读书笔记
     */
    fun addNote(
        bookId: Long,
        noteType: String = NoteType.THOUGHT,
        content: String,
        excerpt: String = "",
        pageNumber: Int? = null,
        chapter: String = ""
    ) {
        viewModelScope.launch {
            try {
                readingUseCase.addReadingNote(
                    bookId = bookId,
                    noteType = noteType,
                    content = content,
                    excerpt = excerpt,
                    pageNumber = pageNumber,
                    chapter = chapter
                )
                _uiState.update { it.copy(message = "笔记已保存") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 删除笔记
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                readingUseCase.deleteReadingNote(noteId)
                _uiState.update { it.copy(message = "笔记已删除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 切换笔记收藏
     */
    fun toggleNoteFavorite(noteId: Long) {
        viewModelScope.launch {
            try {
                readingUseCase.toggleNoteFavorite(noteId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 设置年度阅读目标
     */
    fun setYearlyGoal(targetBooks: Int, targetPages: Int = 0, targetMinutes: Int = 0) {
        viewModelScope.launch {
            try {
                readingUseCase.setYearlyGoal(
                    targetBooks = targetBooks,
                    targetPages = targetPages,
                    targetMinutes = targetMinutes
                )
                _uiState.update { it.copy(message = "目标已设置") }
                loadYearlyStats()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 创建书架
     */
    fun createBookShelf(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                readingUseCase.createBookShelf(name, description)
                _uiState.update { it.copy(message = "书架创建成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 阅读模块UI状态
 */
data class ReadingUiState(
    val isLoading: Boolean = false,
    val currentStatusFilter: String? = null,
    val message: String? = null,
    val error: String? = null
)
