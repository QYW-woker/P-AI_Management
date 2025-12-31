package com.lifemanager.app.feature.finance.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.AccountType
import com.lifemanager.app.core.database.entity.FundAccountEntity
import com.lifemanager.app.domain.repository.FundAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 资金账户管理ViewModel
 */
@HiltViewModel
class FundAccountViewModel @Inject constructor(
    private val repository: FundAccountRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<FundAccountUiState>(FundAccountUiState.Loading)
    val uiState: StateFlow<FundAccountUiState> = _uiState.asStateFlow()

    // 账户列表
    private val _accounts = MutableStateFlow<List<FundAccountEntity>>(emptyList())
    val accounts: StateFlow<List<FundAccountEntity>> = _accounts.asStateFlow()

    // 按类型分组的账户
    private val _groupedAccounts = MutableStateFlow<Map<String, List<FundAccountEntity>>>(emptyMap())
    val groupedAccounts: StateFlow<Map<String, List<FundAccountEntity>>> = _groupedAccounts.asStateFlow()

    // 资产概览
    private val _assetSummary = MutableStateFlow(AssetSummary())
    val assetSummary: StateFlow<AssetSummary> = _assetSummary.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(FundAccountEditState())
    val editState: StateFlow<FundAccountEditState> = _editState.asStateFlow()

    // 对话框状态
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private var accountToDelete: Long? = null

    init {
        loadAccounts()
    }

    /**
     * 加载账户列表
     */
    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = FundAccountUiState.Loading
            try {
                repository.getAllEnabled()
                    .catch { e ->
                        _uiState.value = FundAccountUiState.Error(e.message ?: "加载失败")
                    }
                    .collect { accountList ->
                        _accounts.value = accountList
                        _groupedAccounts.value = accountList.groupBy { it.accountType }
                        _uiState.value = FundAccountUiState.Success
                        loadAssetSummary()
                    }
            } catch (e: Exception) {
                _uiState.value = FundAccountUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 加载资产概览
     */
    private fun loadAssetSummary() {
        viewModelScope.launch {
            try {
                val assets = repository.getTotalAssets()
                val liabilities = repository.getTotalLiabilities()
                val netWorth = repository.getNetWorth()
                _assetSummary.value = AssetSummary(
                    totalAssets = assets,
                    totalLiabilities = liabilities,
                    netWorth = netWorth
                )
            } catch (e: Exception) {
                // 加载失败不影响主界面
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadAccounts()
    }

    /**
     * 显示添加账户对话框
     */
    fun showAddDialog() {
        _editState.value = FundAccountEditState()
        _showEditDialog.value = true
    }

    /**
     * 显示编辑账户对话框
     */
    fun showEditDialog(accountId: Long) {
        viewModelScope.launch {
            val account = repository.getById(accountId)
            if (account != null) {
                _editState.value = FundAccountEditState(
                    id = account.id,
                    name = account.name,
                    accountType = account.accountType,
                    bankCode = account.bankCode ?: "",
                    cardNumber = account.cardNumber ?: "",
                    balance = account.balance.toString(),
                    creditLimit = account.creditLimit?.toString() ?: "",
                    billDay = account.billDay?.toString() ?: "",
                    repaymentDay = account.repaymentDay?.toString() ?: "",
                    note = account.note,
                    includeInTotal = account.includeInTotal,
                    isEditing = true
                )
                _showEditDialog.value = true
            }
        }
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editState.value = FundAccountEditState()
    }

    /**
     * 更新编辑状态
     */
    fun updateEditName(name: String) {
        _editState.value = _editState.value.copy(name = name, error = null)
    }

    fun updateEditAccountType(type: String) {
        _editState.value = _editState.value.copy(accountType = type)
    }

    fun updateEditBankCode(bankCode: String) {
        _editState.value = _editState.value.copy(bankCode = bankCode)
    }

    fun updateEditCardNumber(cardNumber: String) {
        _editState.value = _editState.value.copy(cardNumber = cardNumber)
    }

    fun updateEditBalance(balance: String) {
        _editState.value = _editState.value.copy(balance = balance, error = null)
    }

    fun updateEditCreditLimit(limit: String) {
        _editState.value = _editState.value.copy(creditLimit = limit)
    }

    fun updateEditBillDay(day: String) {
        _editState.value = _editState.value.copy(billDay = day)
    }

    fun updateEditRepaymentDay(day: String) {
        _editState.value = _editState.value.copy(repaymentDay = day)
    }

    fun updateEditNote(note: String) {
        _editState.value = _editState.value.copy(note = note)
    }

    fun updateEditIncludeInTotal(include: Boolean) {
        _editState.value = _editState.value.copy(includeInTotal = include)
    }

    /**
     * 保存账户
     */
    fun saveAccount() {
        val state = _editState.value

        // 验证
        if (state.name.isBlank()) {
            _editState.value = state.copy(error = "请输入账户名称")
            return
        }

        val balance = state.balance.toDoubleOrNull()
        if (balance == null) {
            _editState.value = state.copy(error = "请输入有效的余额")
            return
        }

        viewModelScope.launch {
            _editState.value = state.copy(isSaving = true, error = null)
            try {
                // 检查账户名是否重复
                if (repository.isNameExists(state.name.trim(), state.id)) {
                    _editState.value = state.copy(isSaving = false, error = "账户名称已存在")
                    return@launch
                }

                val account = FundAccountEntity(
                    id = state.id,
                    name = state.name.trim(),
                    accountType = state.accountType,
                    bankCode = state.bankCode.ifBlank { null },
                    cardNumber = state.cardNumber.ifBlank { null },
                    balance = balance,
                    creditLimit = state.creditLimit.toDoubleOrNull(),
                    billDay = state.billDay.toIntOrNull()?.coerceIn(1, 31),
                    repaymentDay = state.repaymentDay.toIntOrNull()?.coerceIn(1, 31),
                    note = state.note.trim(),
                    includeInTotal = state.includeInTotal
                )

                if (state.isEditing) {
                    repository.update(account)
                } else {
                    repository.insert(account)
                }

                hideEditDialog()
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 显示删除确认
     */
    fun showDeleteConfirm(accountId: Long) {
        accountToDelete = accountId
        _showDeleteDialog.value = true
    }

    /**
     * 隐藏删除确认
     */
    fun hideDeleteConfirm() {
        _showDeleteDialog.value = false
        accountToDelete = null
    }

    /**
     * 确认删除
     */
    fun confirmDelete() {
        val id = accountToDelete ?: return
        viewModelScope.launch {
            try {
                repository.disable(id) // 使用软删除
                hideDeleteConfirm()
            } catch (e: Exception) {
                _uiState.value = FundAccountUiState.Error(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 快速更新余额
     */
    fun updateBalance(accountId: Long, newBalance: Double) {
        viewModelScope.launch {
            try {
                repository.updateBalance(accountId, newBalance)
            } catch (e: Exception) {
                _uiState.value = FundAccountUiState.Error(e.message ?: "更新失败")
            }
        }
    }

    /**
     * 获取账户类型显示名称
     */
    fun getAccountTypeName(type: String): String = AccountType.getDisplayName(type)

    /**
     * 获取账户类型图标
     */
    fun getAccountTypeIcon(type: String): String = AccountType.getIcon(type)

    /**
     * 是否为负债账户
     */
    fun isDebtAccount(type: String): Boolean = AccountType.isDebtAccount(type)
}

/**
 * UI状态
 */
sealed class FundAccountUiState {
    object Loading : FundAccountUiState()
    object Success : FundAccountUiState()
    data class Error(val message: String) : FundAccountUiState()
}

/**
 * 资产概览
 */
data class AssetSummary(
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0
)

/**
 * 编辑状态
 */
data class FundAccountEditState(
    val id: Long = 0,
    val name: String = "",
    val accountType: String = AccountType.BANK_CARD,
    val bankCode: String = "",
    val cardNumber: String = "",
    val balance: String = "0",
    val creditLimit: String = "",
    val billDay: String = "",
    val repaymentDay: String = "",
    val note: String = "",
    val includeInTotal: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
