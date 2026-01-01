package com.lifemanager.app.feature.finance.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.core.database.entity.FundAccountEntity
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import com.lifemanager.app.domain.repository.FundAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 资金账户详情ViewModel
 */
@HiltViewModel
class FundAccountDetailViewModel @Inject constructor(
    private val fundAccountRepository: FundAccountRepository,
    private val transactionRepository: DailyTransactionRepository
) : ViewModel() {

    private val _account = MutableStateFlow<FundAccountEntity?>(null)
    val account: StateFlow<FundAccountEntity?> = _account.asStateFlow()

    private val _transactions = MutableStateFlow<List<DailyTransactionEntity>>(emptyList())
    val transactions: StateFlow<List<DailyTransactionEntity>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _monthSummary = MutableStateFlow(MonthTransactionSummary())
    val monthSummary: StateFlow<MonthTransactionSummary> = _monthSummary.asStateFlow()

    /**
     * 加载账户信息和交易记录
     */
    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 加载账户信息
                val accountInfo = fundAccountRepository.getById(accountId)
                _account.value = accountInfo

                if (accountInfo != null) {
                    // 加载交易记录
                    loadTransactions(accountId)
                    // 计算本月收支
                    calculateMonthSummary(accountId)
                }
            } catch (e: Exception) {
                // 处理错误
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载账户的交易记录
     */
    private suspend fun loadTransactions(accountId: Long) {
        try {
            // 获取近3个月的交易记录
            val today = LocalDate.now()
            val threeMonthsAgo = today.minusMonths(3)
            val startDate = threeMonthsAgo.toEpochDay().toInt()
            val endDate = today.toEpochDay().toInt()

            val records = transactionRepository.getByAccountId(accountId, startDate, endDate)
            _transactions.value = records.sortedByDescending { it.date * 10000 + (it.time.replace(":", "").toIntOrNull() ?: 0) }
        } catch (e: Exception) {
            _transactions.value = emptyList()
        }
    }

    /**
     * 计算本月收支概览
     */
    private suspend fun calculateMonthSummary(accountId: Long) {
        try {
            val today = LocalDate.now()
            val firstDayOfMonth = today.withDayOfMonth(1)
            val startDate = firstDayOfMonth.toEpochDay().toInt()
            val endDate = today.toEpochDay().toInt()

            val records = transactionRepository.getByAccountId(accountId, startDate, endDate)

            val income = records.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = records.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            _monthSummary.value = MonthTransactionSummary(income = income, expense = expense)
        } catch (e: Exception) {
            _monthSummary.value = MonthTransactionSummary()
        }
    }
}
