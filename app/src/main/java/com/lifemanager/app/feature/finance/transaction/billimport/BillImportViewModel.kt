package com.lifemanager.app.feature.finance.transaction.billimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.DailyTransactionEntity
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.core.database.entity.TransactionSource
import com.lifemanager.app.domain.repository.CustomFieldRepository
import com.lifemanager.app.domain.repository.DailyTransactionRepository
import com.lifemanager.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 账单导入ViewModel
 */
@HiltViewModel
class BillImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customFieldRepository: CustomFieldRepository,
    private val transactionRepository: DailyTransactionRepository
) : ViewModel() {

    private val billParser = BillParser(context)
    private val categoryMatcher = CategoryMatcher()

    // 导入状态
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // 解析后的记录（可编辑）
    private val _parsedRecords = MutableStateFlow<List<ParsedBillRecord>>(emptyList())
    val parsedRecords: StateFlow<List<ParsedBillRecord>> = _parsedRecords.asStateFlow()

    // 收入分类
    private val _incomeCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val incomeCategories: StateFlow<List<CustomFieldEntity>> = _incomeCategories.asStateFlow()

    // 支出分类
    private val _expenseCategories = MutableStateFlow<List<CustomFieldEntity>>(emptyList())
    val expenseCategories: StateFlow<List<CustomFieldEntity>> = _expenseCategories.asStateFlow()

    // 账单来源
    private val _billSource = MutableStateFlow(BillSource.UNKNOWN)
    val billSource: StateFlow<BillSource> = _billSource.asStateFlow()

    // 导入统计
    val importStats: StateFlow<ImportStats> = _parsedRecords.map { records ->
        val selectedRecords = records.filter { it.isSelected }
        ImportStats(
            totalRecords = records.size,
            selectedRecords = selectedRecords.size,
            totalIncome = selectedRecords.filter { it.type == "收入" }.sumOf { it.amount },
            totalExpense = selectedRecords.filter { it.type == "支出" }.sumOf { it.amount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImportStats(0, 0, 0.0, 0.0))

    init {
        loadCategories()
    }

    /**
     * 加载分类数据
     */
    private fun loadCategories() {
        viewModelScope.launch {
            customFieldRepository.getFieldsByModule(ModuleType.INCOME).collect { fields ->
                _incomeCategories.value = fields
            }
        }
        viewModelScope.launch {
            customFieldRepository.getFieldsByModule(ModuleType.EXPENSE).collect { fields ->
                _expenseCategories.value = fields
            }
        }
        // 确保"未分类"分类存在
        ensureUncategorizedCategoryExists()
    }

    /**
     * 确保"未分类"分类存在
     */
    private fun ensureUncategorizedCategoryExists() {
        viewModelScope.launch {
            // 检查支出模块是否有"未分类"
            val expenseFields = _expenseCategories.value
            val hasExpenseUncategorized = expenseFields.any { it.name == "未分类" }
            if (!hasExpenseUncategorized) {
                val uncategorized = CustomFieldEntity(
                    name = "未分类",
                    iconName = "help_outline",
                    color = "#9E9E9E",
                    moduleType = ModuleType.EXPENSE,
                    isEnabled = true,
                    sortOrder = 999
                )
                customFieldRepository.insert(uncategorized)
            }

            // 检查收入模块是否有"未分类"
            val incomeFields = _incomeCategories.value
            val hasIncomeUncategorized = incomeFields.any { it.name == "未分类" }
            if (!hasIncomeUncategorized) {
                val uncategorized = CustomFieldEntity(
                    name = "未分类",
                    iconName = "help_outline",
                    color = "#9E9E9E",
                    moduleType = ModuleType.INCOME,
                    isEnabled = true,
                    sortOrder = 999
                )
                customFieldRepository.insert(uncategorized)
            }
        }
    }

    /**
     * 解析账单文件
     */
    fun parseFile(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState.Parsing

            val result = billParser.parseFile(uri)

            when (result) {
                is BillParseResult.Success -> {
                    _billSource.value = result.source

                    // 合并所有分类用于匹配
                    val allCategories = _incomeCategories.value + _expenseCategories.value

                    // 智能匹配分类
                    val matchedRecords = categoryMatcher.matchCategories(result.records, allCategories)

                    _parsedRecords.value = matchedRecords
                    _importState.value = ImportState.Preview(
                        records = matchedRecords,
                        source = result.source,
                        categories = allCategories
                    )
                }
                is BillParseResult.Error -> {
                    _importState.value = ImportState.Error(result.message)
                }
            }
        }
    }

    /**
     * 切换记录选中状态
     */
    fun toggleRecordSelection(index: Int) {
        val records = _parsedRecords.value.toMutableList()
        if (index in records.indices) {
            records[index] = records[index].copy(isSelected = !records[index].isSelected)
            _parsedRecords.value = records
        }
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        val records = _parsedRecords.value
        val allSelected = records.all { it.isSelected }
        _parsedRecords.value = records.map { it.copy(isSelected = !allSelected) }
    }

    /**
     * 更新记录的分类
     */
    fun updateRecordCategory(index: Int, categoryId: Long) {
        val records = _parsedRecords.value.toMutableList()
        if (index in records.indices) {
            records[index] = records[index].copy(suggestedCategoryId = categoryId)
            _parsedRecords.value = records
        }
    }

    /**
     * 执行导入
     */
    fun executeImport() {
        viewModelScope.launch {
            _importState.value = ImportState.Importing

            try {
                val selectedRecords = _parsedRecords.value.filter { it.isSelected }

                if (selectedRecords.isEmpty()) {
                    _importState.value = ImportState.Error("请至少选择一条记录")
                    return@launch
                }

                // 转换为DailyTransactionEntity
                val transactions = selectedRecords.mapNotNull { record ->
                    convertToTransaction(record)
                }

                // 批量插入
                transactionRepository.insertAll(transactions)

                val totalAmount = selectedRecords.sumOf { it.amount }
                _importState.value = ImportState.Success(
                    importedCount = transactions.size,
                    totalAmount = totalAmount
                )

            } catch (e: Exception) {
                _importState.value = ImportState.Error("导入失败: ${e.message}")
            }
        }
    }

    /**
     * 将解析的记录转换为交易实体
     */
    private fun convertToTransaction(record: ParsedBillRecord): DailyTransactionEntity? {
        return try {
            // 解析日期时间
            val dateTime = parseDateTime(record.datetime)

            val date = dateTime.toLocalDate().toEpochDay().toInt()
            val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))

            val type = if (record.type == "收入") TransactionType.INCOME else TransactionType.EXPENSE

            DailyTransactionEntity(
                type = type,
                amount = record.amount,
                categoryId = record.suggestedCategoryId,
                date = date,
                time = time,
                note = buildNote(record),
                source = TransactionSource.IMPORT
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析日期时间字符串
     */
    private fun parseDateTime(datetime: String): LocalDateTime {
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy年MM月dd日 HH:mm:ss",
            "yyyy年MM月dd日 HH:mm"
        )

        for (pattern in patterns) {
            try {
                return LocalDateTime.parse(datetime.trim(), DateTimeFormatter.ofPattern(pattern))
            } catch (e: Exception) {
                continue
            }
        }

        // 如果都失败了，尝试只解析日期部分
        val datePart = datetime.split(" ").firstOrNull() ?: datetime
        val datePatterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "yyyy年MM月dd日")

        for (pattern in datePatterns) {
            try {
                val date = LocalDate.parse(datePart.trim(), DateTimeFormatter.ofPattern(pattern))
                return date.atStartOfDay()
            } catch (e: Exception) {
                continue
            }
        }

        throw Exception("无法解析日期: $datetime")
    }

    /**
     * 构建备注内容
     */
    private fun buildNote(record: ParsedBillRecord): String {
        val parts = mutableListOf<String>()

        if (record.counterparty.isNotBlank()) {
            parts.add(record.counterparty)
        }
        if (record.goods.isNotBlank() && record.goods != record.counterparty) {
            parts.add(record.goods)
        }
        if (record.note.isNotBlank()) {
            parts.add(record.note)
        }

        val note = parts.joinToString(" - ")

        // 添加来源标记
        val sourceTag = when (record.source) {
            BillSource.WECHAT -> "[微信导入]"
            BillSource.ALIPAY -> "[支付宝导入]"
            else -> "[导入]"
        }

        return if (note.isNotBlank()) "$sourceTag $note" else sourceTag
    }

    /**
     * 重置状态
     */
    fun reset() {
        _importState.value = ImportState.Idle
        _parsedRecords.value = emptyList()
        _billSource.value = BillSource.UNKNOWN
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        if (_importState.value is ImportState.Error) {
            _importState.value = ImportState.Idle
        }
    }
}
