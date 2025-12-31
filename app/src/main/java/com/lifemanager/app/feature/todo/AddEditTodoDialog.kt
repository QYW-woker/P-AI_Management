package com.lifemanager.app.feature.todo

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifemanager.app.domain.model.priorityList
import com.lifemanager.app.domain.model.quadrantList
import com.lifemanager.app.ui.component.DatePickerButton
import com.lifemanager.app.ui.component.DatePickerDialog
import com.lifemanager.app.ui.component.TimePickerButton
import com.lifemanager.app.ui.component.TimePickerDialog
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalTime

/**
 * 添加/编辑待办对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoDialog(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 将epochDay转换为LocalDate
    val selectedDate = editState.dueDate?.let { LocalDate.ofEpochDay(it.toLong()) }
    // 将时间字符串转换为LocalTime
    val selectedTime = editState.dueTime?.let {
        try {
            val parts = it.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) { null }
    }

    // 动画效果
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val dialogScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialogScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .scale(dialogScale)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = AppColors.Primary.copy(alpha = 0.2f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f),
                            AppColors.Primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(if (editState.isEditing) "编辑待办" else "添加待办")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveTodo() },
                            enabled = !editState.isSaving
                        ) {
                            if (editState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("保存")
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 错误提示
                    editState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 标题输入
                    Text(
                        text = "标题",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.title,
                        onValueChange = { viewModel.updateEditTitle(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "输入待办事项",
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 描述输入
                    Text(
                        text = "描述（可选）",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.description,
                        onValueChange = { viewModel.updateEditDescription(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "添加描述",
                        maxLines = 3,
                        minLines = 2,
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 优先级选择
                    Text(
                        text = "优先级",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        priorityList.forEach { priority ->
                            PriorityChip(
                                name = priority.name,
                                color = Color(priority.color),
                                selected = editState.priority == priority.code,
                                onClick = { viewModel.updateEditPriority(priority.code) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 四象限选择
                    Text(
                        text = "四象限分类（可选）",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quadrantList.take(2).forEach { quadrant ->
                                QuadrantChip(
                                    name = quadrant.name,
                                    description = quadrant.description,
                                    color = Color(quadrant.color),
                                    selected = editState.quadrant == quadrant.code,
                                    onClick = {
                                        viewModel.updateEditQuadrant(
                                            if (editState.quadrant == quadrant.code) null else quadrant.code
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quadrantList.drop(2).forEach { quadrant ->
                                QuadrantChip(
                                    name = quadrant.name,
                                    description = quadrant.description,
                                    color = Color(quadrant.color),
                                    selected = editState.quadrant == quadrant.code,
                                    onClick = {
                                        viewModel.updateEditQuadrant(
                                            if (editState.quadrant == quadrant.code) null else quadrant.code
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 截止时间
                    Text(
                        text = "截止时间（可选）",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 日期选择按钮
                        DatePickerButton(
                            selectedDate = selectedDate,
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                            placeholder = "选择日期"
                        )

                        // 时间选择按钮
                        TimePickerButton(
                            selectedTime = selectedTime,
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            placeholder = "选择时间"
                        )
                    }

                    // 清除日期时间按钮
                    if (editState.dueDate != null || editState.dueTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.updateEditDueDate(null)
                                viewModel.updateEditDueTime(null)
                            }
                        ) {
                            Text("清除截止时间", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                viewModel.updateEditDueDate(date.toEpochDay().toInt())
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // 时间选择器对话框
    if (showTimePicker) {
        TimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                viewModel.updateEditDueTime(String.format("%02d:%02d", time.hour, time.minute))
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun PriorityChip(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) color.copy(alpha = 0.2f) else Color.Transparent
    val contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = ButtonDefaults.outlinedButtonBorder.takeIf { !selected }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun QuadrantChip(
    name: String,
    description: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(color)
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
