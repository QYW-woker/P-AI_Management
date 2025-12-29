package com.lifemanager.app.feature.finance.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.dao.CustomFieldDao
import com.lifemanager.app.core.database.dao.DailyTransactionDao
import com.lifemanager.app.domain.model.DailyTransactionWithCategory
import com.lifemanager.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记账搜索ViewModel
 */
@HiltViewModel
class AccountingSearchViewModel @Inject constructor(
    private val transactionDao: DailyTransactionDao,
    private val customFieldDao: CustomFieldDao
) : ViewModel() {

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<DailyTransactionWithCategory>>(emptyList())
    val searchResults: StateFlow<List<DailyTransactionWithCategory>> = _searchResults.asStateFlow()

    // 是否正在搜索
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 选中的类型筛选
    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    // 最近搜索
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // 搜索统计
    private val _searchStats = MutableStateFlow(SearchStats())
    val searchStats: StateFlow<SearchStats> = _searchStats.asStateFlow()

    private var searchJob: Job? = null

    /**
     * 更新搜索关键词
     */
    fun updateQuery(query: String) {
        _searchQuery.value = query
        // 延迟搜索，避免频繁请求
        searchJob?.cancel()
        if (query.isNotEmpty()) {
            searchJob = viewModelScope.launch {
                delay(300) // 300ms防抖
                performSearch()
            }
        } else {
            _searchResults.value = emptyList()
            _searchStats.value = SearchStats()
        }
    }

    /**
     * 清除搜索关键词
     */
    fun clearQuery() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchStats.value = SearchStats()
    }

    /**
     * 执行搜索
     */
    fun performSearch() {
        val query = _searchQuery.value
        if (query.isEmpty()) return

        _isSearching.value = true

        viewModelScope.launch {
            try {
                transactionDao.searchByNote(query).collectLatest { transactions ->
                    // 按类型筛选
                    val filtered = if (_selectedType.value != null) {
                        transactions.filter { it.type == _selectedType.value }
                    } else {
                        transactions
                    }

                    val transactionsWithCategory = filtered.map { entity ->
                        val category = entity.categoryId?.let { id ->
                            customFieldDao.getFieldById(id)
                        }
                        DailyTransactionWithCategory(
                            transaction = entity,
                            category = category
                        )
                    }

                    _searchResults.value = transactionsWithCategory

                    // 计算统计
                    val totalIncome = transactionsWithCategory
                        .filter { it.transaction.type == TransactionType.INCOME }
                        .sumOf { it.transaction.amount }
                    val totalExpense = transactionsWithCategory
                        .filter { it.transaction.type == TransactionType.EXPENSE }
                        .sumOf { it.transaction.amount }

                    _searchStats.value = SearchStats(
                        count = transactionsWithCategory.size,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense
                    )

                    _isSearching.value = false

                    // 保存到最近搜索
                    addToRecentSearches(query)
                }
            } catch (e: Exception) {
                _isSearching.value = false
            }
        }
    }

    /**
     * 选择类型筛选
     */
    fun selectType(type: String?) {
        _selectedType.value = type
        if (_searchQuery.value.isNotEmpty()) {
            performSearch()
        }
    }

    /**
     * 添加到最近搜索
     */
    private fun addToRecentSearches(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        if (current.size > 10) {
            current.removeAt(current.size - 1)
        }
        _recentSearches.value = current
    }

    /**
     * 清除最近搜索
     */
    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }
}
