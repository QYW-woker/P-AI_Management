package com.lifemanager.app.feature.habit

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifemanager.app.domain.model.frequencyOptions
import com.lifemanager.app.domain.model.habitColors
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.component.PremiumConfirmButton
import com.lifemanager.app.ui.component.PremiumDismissButton
import com.lifemanager.app.ui.theme.AppColors

/**
 * 添加/编辑习惯对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()

    // 动画效果
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
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
                .fillMaxHeight(0.9f)
                .scale(scale)
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
                        Text(if (editState.isEditing) "编辑习惯" else "添加习惯")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveHabit() },
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

                    // 习惯名称
                    Text(
                        text = "习惯名称",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.name,
                        onValueChange = { viewModel.updateEditName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "如：每天阅读30分钟",
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 描述
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
                        maxLines = 2,
                        minLines = 2,
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 颜色选择
                    Text(
                        text = "颜色",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(habitColors) { (color, _) ->
                            ColorChip(
                                color = color,
                                selected = editState.color == color,
                                onClick = { viewModel.updateEditColor(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 打卡频率
                    Text(
                        text = "打卡频率",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        frequencyOptions.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateEditFrequency(value) }
                                    .background(
                                        if (editState.frequency == value)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = editState.frequency == value,
                                    onClick = { viewModel.updateEditFrequency(value) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }

                    // 如果选择每周X次或每月X次，显示目标次数输入
                    if (editState.frequency == "WEEKLY_TIMES" || editState.frequency == "MONTHLY_TIMES") {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "目标次数",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PremiumTextField(
                            value = editState.targetTimes.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { viewModel.updateEditTargetTimes(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = if (editState.frequency == "WEEKLY_TIMES") "次/周" else "次/月",
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 数值型习惯开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "数值型习惯",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "如：每天喝8杯水、走10000步",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editState.isNumeric,
                            onCheckedChange = { viewModel.updateEditIsNumeric(it) }
                        )
                    }

                    // 数值型习惯设置
                    if (editState.isNumeric) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PremiumTextField(
                                value = editState.targetValue?.toInt()?.toString() ?: "",
                                onValueChange = { value ->
                                    viewModel.updateEditTargetValue(value.toDoubleOrNull())
                                },
                                modifier = Modifier.weight(1f),
                                label = "目标值",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            PremiumTextField(
                                value = editState.unit,
                                onValueChange = { viewModel.updateEditUnit(it) },
                                modifier = Modifier.weight(1f),
                                label = "单位",
                                placeholder = "如：杯、步",
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ColorChip(
    color: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipColor = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(chipColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
