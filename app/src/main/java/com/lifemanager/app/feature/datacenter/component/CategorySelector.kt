package com.lifemanager.app.feature.datacenter.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifemanager.app.core.database.entity.CustomFieldEntity

/**
 * 分类选择器组件
 *
 * 支持多选分类筛选，以下拉菜单形式展示
 *
 * @param title 选择器标题（如"收入"、"支出"）
 * @param categories 可选分类列表
 * @param selectedIds 已选分类ID集合（空表示全选）
 * @param onSelectionChange 选择变更回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    title: String,
    categories: List<CustomFieldEntity>,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val allSelected = selectedIds.isEmpty() || selectedIds.size == categories.size

    Column(modifier = modifier) {
        // 下拉触发按钮
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (allSelected) {
                        "全部$title"
                    } else {
                        "${selectedIds.size}个$title"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 下拉菜单对话框
        if (expanded) {
            CategorySelectionDialog(
                title = "选择$title",
                categories = categories,
                selectedIds = selectedIds,
                onSelectionChange = onSelectionChange,
                onDismiss = { expanded = false }
            )
        }
    }
}

/**
 * 分类选择对话框
 */
@Composable
private fun CategorySelectionDialog(
    title: String,
    categories: List<CustomFieldEntity>,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val allSelected = selectedIds.isEmpty() || selectedIds.size == categories.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 全选选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = {
                            onSelectionChange(emptySet())
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "全选",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Divider()

                // 分类列表
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedIds.isEmpty() || selectedIds.contains(category.id)
                        CategoryCheckItem(
                            category = category,
                            isSelected = isSelected,
                            onToggle = {
                                val newSelection = if (selectedIds.isEmpty()) {
                                    // 当前是全选状态，取消选中这一项
                                    categories.map { it.id }.toSet() - category.id
                                } else if (isSelected) {
                                    // 取消选中
                                    val remaining = selectedIds - category.id
                                    // 如果取消后没有选中项，恢复全选
                                    if (remaining.isEmpty()) emptySet() else remaining
                                } else {
                                    // 添加选中
                                    selectedIds + category.id
                                }
                                onSelectionChange(newSelection)
                            }
                        )
                    }
                }

                // 确定按钮
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("确定")
                }
            }
        }
    }
}

/**
 * 分类选择项
 */
@Composable
private fun CategoryCheckItem(
    category: CustomFieldEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    // 获取卡通图标
    val emoji = com.lifemanager.app.ui.component.CategoryIcons.getIcon(
        name = category.name,
        iconName = category.iconName,
        moduleType = category.moduleType
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Spacer(modifier = Modifier.width(8.dp))

        // 卡通图标 + 颜色标记
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(parseHexColor(category.color)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 解析十六进制颜色字符串
 */
private fun parseHexColor(hexColor: String): Color {
    return try {
        val colorString = if (hexColor.startsWith("#")) hexColor.substring(1) else hexColor
        Color(android.graphics.Color.parseColor("#$colorString"))
    } catch (e: Exception) {
        Color.Gray
    }
}

/**
 * 组合分类选择器（收入+支出）
 */
@Composable
fun CombinedCategorySelector(
    incomeCategories: List<CustomFieldEntity>,
    expenseCategories: List<CustomFieldEntity>,
    selectedIncomeIds: Set<Long>,
    selectedExpenseIds: Set<Long>,
    onIncomeSelectionChange: (Set<Long>) -> Unit,
    onExpenseSelectionChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategorySelector(
            title = "收入",
            categories = incomeCategories,
            selectedIds = selectedIncomeIds,
            onSelectionChange = onIncomeSelectionChange,
            modifier = Modifier.weight(1f)
        )
        CategorySelector(
            title = "支出",
            categories = expenseCategories,
            selectedIds = selectedExpenseIds,
            onSelectionChange = onExpenseSelectionChange,
            modifier = Modifier.weight(1f)
        )
    }
}
