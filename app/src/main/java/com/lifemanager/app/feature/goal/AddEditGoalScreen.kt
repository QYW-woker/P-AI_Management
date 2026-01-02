package com.lifemanager.app.feature.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.domain.model.SubGoalEditState
import com.lifemanager.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 新建/编辑目标页面
 *
 * @param goalId 编辑时的目标ID，null表示新建
 * @param isMultiLevel 是否为多级目标（带子目标）
 * @param onNavigateBack 返回回调
 * @param viewModel ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    goalId: Long? = null,
    isMultiLevel: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val isEditing = goalId != null && goalId > 0

    // 表单状态
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("FINANCE") }
    var goalType by remember { mutableStateOf("SAVINGS") }
    var targetValue by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("元") }
    var progressType by remember { mutableStateOf("NUMERIC") }
    var deadline by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // 子目标列表（仅多级目标使用）
    var subGoals by remember { mutableStateOf<List<SubGoalEditState>>(emptyList()) }
    var showAddSubGoalDialog by remember { mutableStateOf(false) }
    var editingSubGoal by remember { mutableStateOf<SubGoalEditState?>(null) }

    // 加载现有目标数据（编辑模式）
    LaunchedEffect(goalId) {
        if (isEditing && goalId != null) {
            viewModel.getGoalById(goalId).collect { goal ->
                goal?.let {
                    title = it.title
                    description = it.description
                    category = it.category
                    goalType = it.goalType
                    targetValue = it.targetValue?.toString() ?: ""
                    unit = it.unit
                    progressType = it.progressType
                    it.endDate?.let { epochDay ->
                        deadline = LocalDate.ofEpochDay(epochDay.toLong())
                    }
                }
            }
        }
    }

    val categories = listOf(
        "CAREER" to "职业发展",
        "FINANCE" to "财务目标",
        "HEALTH" to "健康运动",
        "LEARNING" to "学习成长",
        "RELATIONSHIP" to "人际关系",
        "LIFESTYLE" to "生活方式",
        "HOBBY" to "兴趣爱好",
        "OTHER" to "其他"
    )

    val goalTypes = listOf(
        "SAVINGS" to "存钱目标",
        "HABIT" to "习惯养成",
        "ACHIEVEMENT" to "成就达成",
        "LEARNING" to "学习目标",
        "FITNESS" to "健身目标",
        "OTHER" to "其他目标"
    )

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = if (isEditing) "编辑目标" else "新建目标",
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isBlank()) {
                                error = "请输入目标名称"
                                return@TextButton
                            }
                            // 多级目标必须至少有一个子目标
                            if (isMultiLevel && subGoals.isEmpty()) {
                                error = "多级目标至少需要添加一个子目标"
                                return@TextButton
                            }
                            isLoading = true
                            error = null

                            val deadlineInt = deadline?.toEpochDay()?.toInt()

                            if (isEditing && goalId != null) {
                                viewModel.updateGoal(
                                    id = goalId,
                                    title = title,
                                    description = description,
                                    category = category,
                                    goalType = goalType,
                                    targetValue = targetValue.toDoubleOrNull(),
                                    unit = unit,
                                    progressType = progressType,
                                    deadline = deadlineInt
                                )
                            } else if (isMultiLevel) {
                                // 创建多级目标
                                viewModel.createGoalWithSubGoals(
                                    title = title,
                                    description = description,
                                    category = category,
                                    goalType = goalType,
                                    targetValue = targetValue.toDoubleOrNull(),
                                    unit = unit,
                                    progressType = progressType,
                                    deadline = deadlineInt,
                                    subGoals = subGoals
                                )
                            } else {
                                // 创建单级目标
                                viewModel.createGoal(
                                    title = title,
                                    description = description,
                                    category = category,
                                    goalType = goalType,
                                    targetValue = targetValue.toDoubleOrNull(),
                                    unit = unit,
                                    progressType = progressType,
                                    deadline = deadlineInt
                                )
                            }
                            onNavigateBack()
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
        ) {
            // 错误提示
            error?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = AppShapes.Medium
                ) {
                    Row(
                        modifier = Modifier.padding(AppDimens.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 目标名称
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("目标名称 *") },
                placeholder = { Text("例如：存款10万元") },
                singleLine = true,
                shape = AppShapes.Medium
            )

            // 目标描述
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("目标描述") },
                placeholder = { Text("描述一下这个目标...") },
                minLines = 2,
                maxLines = 4,
                shape = AppShapes.Medium
            )

            // 分类选择
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = categories.find { it.first == category }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("分类") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    shape = AppShapes.Medium
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                category = value
                                showCategoryDropdown = false
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(value))
                                )
                            }
                        )
                    }
                }
            }

            // 目标类型
            ExposedDropdownMenuBox(
                expanded = showTypeDropdown,
                onExpandedChange = { showTypeDropdown = it }
            ) {
                OutlinedTextField(
                    value = goalTypes.find { it.first == goalType }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("目标类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                    shape = AppShapes.Medium
                )
                ExposedDropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false }
                ) {
                    goalTypes.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                goalType = value
                                showTypeDropdown = false
                            }
                        )
                    }
                }
            }

            // 目标值和单位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.weight(2f),
                    label = { Text("目标数值") },
                    placeholder = { Text("例如：100000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = AppShapes.Medium
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("单位") },
                    placeholder = { Text("元") },
                    singleLine = true,
                    shape = AppShapes.Medium
                )
            }

            // 截止日期
            OutlinedTextField(
                value = deadline?.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                label = { Text("截止日期") },
                placeholder = { Text("选择截止日期（可选）") },
                trailingIcon = {
                    Row {
                        if (deadline != null) {
                            IconButton(onClick = { deadline = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期")
                        }
                    }
                },
                shape = AppShapes.Medium,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // 进度类型选择
            SectionTitle(title = "进度跟踪方式", centered = false)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                FilterChip(
                    selected = progressType == "NUMERIC",
                    onClick = { progressType = "NUMERIC" },
                    label = { Text("数值进度") },
                    leadingIcon = if (progressType == "NUMERIC") {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = progressType == "PERCENTAGE",
                    onClick = { progressType = "PERCENTAGE" },
                    label = { Text("百分比进度") },
                    leadingIcon = if (progressType == "PERCENTAGE") {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }

            // 子目标管理（仅多级目标显示）
            if (isMultiLevel) {
                Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

                Divider()

                Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SectionTitle(title = "子目标", centered = false)
                        Text(
                            text = "已添加 ${subGoals.size} 个子目标",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddSubGoalDialog = true },
                        shape = AppShapes.Medium
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加子目标")
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

                // 子目标列表
                if (subGoals.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = AppShapes.Medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountTree,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "尚未添加子目标",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击上方按钮添加子目标",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subGoals.forEachIndexed { index, subGoal ->
                            SubGoalCard(
                                subGoal = subGoal,
                                index = index + 1,
                                onEdit = { editingSubGoal = subGoal },
                                onDelete = { subGoals = subGoals.filter { it.tempId != subGoal.tempId } }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))
        }
    }

    // 添加/编辑子目标对话框
    if (showAddSubGoalDialog || editingSubGoal != null) {
        SubGoalDialog(
            subGoal = editingSubGoal,
            onDismiss = {
                showAddSubGoalDialog = false
                editingSubGoal = null
            },
            onSave = { newSubGoal ->
                if (editingSubGoal != null) {
                    // 编辑现有子目标
                    subGoals = subGoals.map {
                        if (it.tempId == editingSubGoal!!.tempId) newSubGoal else it
                    }
                } else {
                    // 添加新子目标
                    subGoals = subGoals + newSubGoal
                }
                showAddSubGoalDialog = false
                editingSubGoal = null
            }
        )
    }

    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadline?.toEpochDay()?.times(86400000)
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            deadline = LocalDate.ofEpochDay(it / 86400000)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "CAREER" -> Color(0xFF2196F3)
        "FINANCE" -> Color(0xFF4CAF50)
        "HEALTH" -> Color(0xFFE91E63)
        "LEARNING" -> Color(0xFFFF9800)
        "RELATIONSHIP" -> Color(0xFF9C27B0)
        "LIFESTYLE" -> Color(0xFF00BCD4)
        "HOBBY" -> Color(0xFFFF5722)
        else -> Color.Gray
    }
}

/**
 * 子目标卡片
 */
@Composable
private fun SubGoalCard(
    subGoal: SubGoalEditState,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))

            // 子目标信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subGoal.title.ifBlank { "未命名子目标" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subGoal.targetValue != null) {
                    Text(
                        text = "目标: ${subGoal.targetValue}${subGoal.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 操作按钮
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 子目标编辑对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubGoalDialog(
    subGoal: SubGoalEditState?,
    onDismiss: () -> Unit,
    onSave: (SubGoalEditState) -> Unit
) {
    val isEditing = subGoal != null

    var title by remember { mutableStateOf(subGoal?.title ?: "") }
    var description by remember { mutableStateOf(subGoal?.description ?: "") }
    var targetValue by remember { mutableStateOf(subGoal?.targetValue?.toString() ?: "") }
    var unit by remember { mutableStateOf(subGoal?.unit ?: "") }
    var progressType by remember { mutableStateOf(subGoal?.progressType ?: "PERCENTAGE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "编辑子目标" else "添加子目标",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                // 子目标名称
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("子目标名称 *") },
                    placeholder = { Text("例如：每日运动30分钟") },
                    singleLine = true,
                    shape = AppShapes.Medium
                )

                // 描述（可选）
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("描述（可选）") },
                    minLines = 2,
                    maxLines = 3,
                    shape = AppShapes.Medium
                )

                // 目标值和单位
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.weight(2f),
                        label = { Text("目标数值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = AppShapes.Medium
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("单位") },
                        singleLine = true,
                        shape = AppShapes.Medium
                    )
                }

                // 进度类型
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    FilterChip(
                        selected = progressType == "NUMERIC",
                        onClick = { progressType = "NUMERIC" },
                        label = { Text("数值") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = progressType == "PERCENTAGE",
                        onClick = { progressType = "PERCENTAGE" },
                        label = { Text("百分比") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            SubGoalEditState(
                                tempId = subGoal?.tempId ?: System.currentTimeMillis(),
                                title = title.trim(),
                                description = description.trim(),
                                targetValue = targetValue.toDoubleOrNull(),
                                unit = unit.trim(),
                                progressType = progressType
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
