package com.lifemanager.app.feature.finance.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.ModuleType
import com.lifemanager.app.domain.usecase.CustomFieldUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 字段管理ViewModel
 *
 * 管理收支类别字段的添加、编辑、删除和排序
 */
@HiltViewModel
class FieldManagementViewModel @Inject constructor(
    private val useCase: CustomFieldUseCase
) : ViewModel() {

    // 当前选中的模块类型 (0: 收入, 1: 支出)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // 收入类别字段列表
    val incomeFields: StateFlow<List<CustomFieldEntity>> = useCase.getIncomeFields()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 支出类别字段列表
    val expenseFields: StateFlow<List<CustomFieldEntity>> = useCase.getExpenseFields()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 编辑状态
    private val _editState = MutableStateFlow(FieldEditState())
    val editState: StateFlow<FieldEditState> = _editState.asStateFlow()

    // 是否显示编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 操作提示信息
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    /**
     * 切换标签页
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * 获取当前模块类型
     */
    private fun getCurrentModuleType(): String {
        return if (_selectedTab.value == 0) ModuleType.INCOME else ModuleType.EXPENSE
    }

    /**
     * 显示添加字段对话框
     */
    fun showAddDialog() {
        _editState.value = FieldEditState(
            moduleType = getCurrentModuleType(),
            isEditing = false
        )
        _showEditDialog.value = true
    }

    /**
     * 显示编辑字段对话框
     */
    fun showEditDialog(field: CustomFieldEntity) {
        _editState.value = FieldEditState(
            id = field.id,
            moduleType = field.moduleType,
            name = field.name,
            iconName = field.iconName,
            color = field.color,
            isPreset = field.isPreset,
            isEditing = true
        )
        _showEditDialog.value = true
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _editState.value = FieldEditState()
    }

    /**
     * 更新字段名称
     */
    fun updateName(name: String) {
        _editState.value = _editState.value.copy(name = name)
    }

    /**
     * 更新字段图标
     */
    fun updateIcon(iconName: String) {
        _editState.value = _editState.value.copy(iconName = iconName)
    }

    /**
     * 更新字段颜色
     */
    fun updateColor(color: String) {
        _editState.value = _editState.value.copy(color = color)
    }

    /**
     * 保存字段
     */
    fun saveField() {
        val state = _editState.value

        // 验证
        if (state.name.isBlank()) {
            _editState.value = state.copy(error = "请输入类别名称")
            return
        }

        _editState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                if (state.isEditing) {
                    // 更新现有字段
                    useCase.updateField(
                        CustomFieldEntity(
                            id = state.id,
                            moduleType = state.moduleType,
                            name = state.name,
                            iconName = state.iconName,
                            color = state.color,
                            isPreset = state.isPreset
                        )
                    )
                    _snackbarMessage.value = "类别已更新"
                } else {
                    // 添加新字段
                    useCase.addField(
                        moduleType = state.moduleType,
                        name = state.name,
                        iconName = state.iconName,
                        color = state.color
                    )
                    _snackbarMessage.value = "类别已添加"
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
     * 切换字段启用状态
     */
    fun toggleFieldEnabled(field: CustomFieldEntity) {
        viewModelScope.launch {
            try {
                useCase.toggleFieldEnabled(field.id, !field.isEnabled)
                _snackbarMessage.value = if (field.isEnabled) "已禁用" else "已启用"
            } catch (e: Exception) {
                _snackbarMessage.value = "操作失败"
            }
        }
    }

    /**
     * 删除字段
     */
    fun deleteField(field: CustomFieldEntity) {
        if (field.isPreset) {
            _snackbarMessage.value = "预设类别不能删除，但可以禁用"
            return
        }

        viewModelScope.launch {
            try {
                val success = useCase.deleteField(field.id)
                if (success) {
                    _snackbarMessage.value = "类别已删除"
                } else {
                    _snackbarMessage.value = "删除失败"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 清除提示信息
     */
    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

/**
 * 字段编辑状态
 */
data class FieldEditState(
    val id: Long = 0,
    val moduleType: String = ModuleType.INCOME,
    val name: String = "",
    val iconName: String = "category",
    val color: String = "#2196F3",
    val isPreset: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
