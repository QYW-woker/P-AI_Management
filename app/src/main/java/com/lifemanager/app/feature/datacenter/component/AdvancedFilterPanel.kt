@file:OptIn(ExperimentalMaterial3Api::class)

package com.lifemanager.app.feature.datacenter.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifemanager.app.feature.datacenter.model.*

/**
 * 高级筛选面板
 */
@Composable
fun AdvancedFilterPanel(
    filterState: DataCenterFilterState,
    isExpanded: Boolean,
    filterPresets: List<FilterPreset>,
    selectedPreset: FilterPreset?,
    onToggleExpand: () -> Unit,
    onApplyPreset: (FilterPreset) -> Unit,
    onSavePreset: (String, String) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onUpdateModules: (Set<DataModule>) -> Unit,
    onUpdateCompareMode: (CompareMode) -> Unit,
    onUpdateGranularity: (AggregateGranularity) -> Unit,
    onUpdateSortMode: (SortMode) -> Unit,
    onUpdateAmountRange: (Double?, Double?) -> Unit,
    onUpdateSearchKeyword: (String) -> Unit,
    onUpdateShowTopN: (Int) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var presetDescription by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        // 筛选预设快捷选择
        FilterPresetSelector(
            presets = filterPresets,
            selectedPreset = selectedPreset,
            onSelectPreset = onApplyPreset,
            onSavePreset = { showSavePresetDialog = true },
            onDeletePreset = onDeletePreset
        )

        // 展开/折叠高级筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "高级筛选",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (hasActiveFilters(filterState)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("已启用")
                    }
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开"
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 模块筛选
                    ModuleFilter(
                        selectedModules = filterState.selectedModules,
                        onSelectionChange = onUpdateModules
                    )

                    Divider()

                    // 对比模式
                    CompareModeSelector(
                        selectedMode = filterState.compareMode,
                        onModeChange = onUpdateCompareMode
                    )

                    Divider()

                    // 聚合粒度
                    GranularitySelector(
                        selectedGranularity = filterState.aggregateGranularity,
                        onGranularityChange = onUpdateGranularity
                    )

                    Divider()

                    // 排序方式
                    SortModeSelector(
                        selectedSort = filterState.sortMode,
                        onSortChange = onUpdateSortMode
                    )

                    Divider()

                    // 金额范围筛选
                    AmountRangeFilter(
                        minAmount = filterState.minAmount,
                        maxAmount = filterState.maxAmount,
                        onRangeChange = onUpdateAmountRange
                    )

                    Divider()

                    // 搜索关键词
                    SearchKeywordInput(
                        keyword = filterState.searchKeyword,
                        onKeywordChange = onUpdateSearchKeyword
                    )

                    Divider()

                    // 显示数量
                    ShowTopNSelector(
                        currentN = filterState.showTopN,
                        onNChange = onUpdateShowTopN
                    )

                    // 重置按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onResetFilters) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重置筛选")
                        }
                    }
                }
            }
        }
    }

    // 保存预设对话框
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("保存筛选预设") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("预设名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = presetDescription,
                        onValueChange = { presetDescription = it },
                        label = { Text("描述（可选）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSavePreset(presetName, presetDescription)
                            presetName = ""
                            presetDescription = ""
                            showSavePresetDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 判断是否有激活的筛选条件
 */
private fun hasActiveFilters(state: DataCenterFilterState): Boolean {
    return state.selectedModules != setOf(DataModule.ALL) ||
           state.compareMode != CompareMode.NONE ||
           state.minAmount != null ||
           state.maxAmount != null ||
           state.searchKeyword.isNotBlank() ||
           state.showTopN != 10
}

/**
 * 筛选预设选择器
 */
@Composable
private fun FilterPresetSelector(
    presets: List<FilterPreset>,
    selectedPreset: FilterPreset?,
    onSelectPreset: (FilterPreset) -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: (Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<FilterPreset?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "快捷预设",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onSavePreset,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "保存当前筛选为预设",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { preset ->
                PresetChip(
                    preset = preset,
                    isSelected = selectedPreset?.id == preset.id,
                    onClick = { onSelectPreset(preset) },
                    onLongClick = if (!preset.isDefault) {
                        { showDeleteDialog = preset }
                    } else null
                )
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { preset ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除预设") },
            text = { Text("确定要删除预设「${preset.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePreset(preset.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 预设标签
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetChip(
    preset: FilterPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(preset.name) },
        leadingIcon = if (preset.isDefault) {
            {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

/**
 * 模块筛选
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleFilter(
    selectedModules: Set<DataModule>,
    onSelectionChange: (Set<DataModule>) -> Unit
) {
    Column {
        Text(
            text = "数据模块",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataModule.values().forEach { module ->
                val isSelected = selectedModules.contains(module) ||
                    (module == DataModule.ALL && selectedModules.isEmpty())

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSelection = if (module == DataModule.ALL) {
                            setOf(DataModule.ALL)
                        } else {
                            val current = selectedModules - DataModule.ALL
                            if (current.contains(module)) {
                                val remaining = current - module
                                if (remaining.isEmpty()) setOf(DataModule.ALL) else remaining
                            } else {
                                current + module
                            }
                        }
                        onSelectionChange(newSelection)
                    },
                    label = { Text(module.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = module.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * 对比模式选择器
 */
@Composable
private fun CompareModeSelector(
    selectedMode: CompareMode,
    onModeChange: (CompareMode) -> Unit
) {
    Column {
        Text(
            text = "对比模式",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CompareMode.values().toList()) { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode.displayName) }
                )
            }
        }
    }
}

/**
 * 聚合粒度选择器
 */
@Composable
private fun GranularitySelector(
    selectedGranularity: AggregateGranularity,
    onGranularityChange: (AggregateGranularity) -> Unit
) {
    Column {
        Text(
            text = "聚合粒度",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AggregateGranularity.values().toList()) { granularity ->
                FilterChip(
                    selected = selectedGranularity == granularity,
                    onClick = { onGranularityChange(granularity) },
                    label = { Text(granularity.displayName) }
                )
            }
        }
    }
}

/**
 * 排序方式选择器
 */
@Composable
private fun SortModeSelector(
    selectedSort: SortMode,
    onSortChange: (SortMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "排序方式",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true }
            ) {
                Text(selectedSort.displayName)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SortMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.displayName) },
                        onClick = {
                            onSortChange(mode)
                            expanded = false
                        },
                        leadingIcon = if (selectedSort == mode) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * 金额范围筛选
 */
@Composable
private fun AmountRangeFilter(
    minAmount: Double?,
    maxAmount: Double?,
    onRangeChange: (Double?, Double?) -> Unit
) {
    var minText by remember(minAmount) { mutableStateOf(minAmount?.toString() ?: "") }
    var maxText by remember(maxAmount) { mutableStateOf(maxAmount?.toString() ?: "") }

    Column {
        Text(
            text = "金额范围",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minText,
                onValueChange = {
                    minText = it
                    val min = it.toDoubleOrNull()
                    onRangeChange(min, maxAmount)
                },
                label = { Text("最小金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Text("至")
            OutlinedTextField(
                value = maxText,
                onValueChange = {
                    maxText = it
                    val max = it.toDoubleOrNull()
                    onRangeChange(minAmount, max)
                },
                label = { Text("最大金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 搜索关键词输入
 */
@Composable
private fun SearchKeywordInput(
    keyword: String,
    onKeywordChange: (String) -> Unit
) {
    Column {
        Text(
            text = "搜索关键词",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            placeholder = { Text("输入关键词筛选...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = if (keyword.isNotBlank()) {
                {
                    IconButton(onClick = { onKeywordChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除"
                        )
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 显示数量选择器
 */
@Composable
private fun ShowTopNSelector(
    currentN: Int,
    onNChange: (Int) -> Unit
) {
    val options = listOf(5, 10, 15, 20, 50, 100)

    Column {
        Text(
            text = "显示数量",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { n ->
                FilterChip(
                    selected = currentN == n,
                    onClick = { onNChange(n) },
                    label = { Text("前$n") }
                )
            }
        }
    }
}

/**
 * 数据模块对应的图标
 */
private val DataModule.icon: ImageVector
    get() = when (this) {
        DataModule.FINANCE -> Icons.Default.AccountBalance
        DataModule.TODO -> Icons.Default.CheckCircle
        DataModule.HABIT -> Icons.Default.EventRepeat
        DataModule.TIME -> Icons.Default.Timer
        DataModule.DIARY -> Icons.Default.Book
        DataModule.GOAL -> Icons.Default.Flag
        DataModule.ASSET -> Icons.Default.Savings
        DataModule.ALL -> Icons.Default.Dashboard
    }
