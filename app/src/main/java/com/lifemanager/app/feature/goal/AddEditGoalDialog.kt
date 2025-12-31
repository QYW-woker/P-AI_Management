package com.lifemanager.app.feature.goal

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifemanager.app.domain.model.goalCategoryOptions
import com.lifemanager.app.domain.model.goalTypeOptions
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.theme.AppColors

/**
 * 添加/编辑目标对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalDialog(
    viewModel: GoalViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()

    var targetValueText by remember(editState.targetValue) {
        mutableStateOf(editState.targetValue?.toString() ?: "")
    }

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
                        Text(if (editState.isEditing) "编辑目标" else "创建目标")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveGoal() },
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

                    // 目标标题
                    Text(
                        text = "目标标题 *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.title,
                        onValueChange = { viewModel.updateEditTitle(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "如：存款10万元、学习一门新语言",
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 目标描述
                    Text(
                        text = "目标描述",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = editState.description,
                        onValueChange = { viewModel.updateEditDescription(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "详细描述你的目标...",
                        maxLines = 3,
                        minLines = 2,
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 目标类型
                    Text(
                        text = "目标周期",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goalTypeOptions.forEach { (value, label) ->
                            if (editState.goalType == value) {
                                Button(
                                    onClick = { viewModel.updateEditGoalType(value) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(label)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.updateEditGoalType(value) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 目标分类
                    Text(
                        text = "目标分类",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goalCategoryOptions.forEach { (value, label) ->
                            if (editState.category == value) {
                                Button(
                                    onClick = { viewModel.updateEditCategory(value) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(label)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.updateEditCategory(value) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 进度类型
                    Text(
                        text = "进度类型",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editState.progressType == "PERCENTAGE") {
                            Button(
                                onClick = { viewModel.updateEditProgressType("PERCENTAGE") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("百分比")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.updateEditProgressType("PERCENTAGE") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("百分比")
                            }
                        }
                        if (editState.progressType == "NUMERIC") {
                            Button(
                                onClick = { viewModel.updateEditProgressType("NUMERIC") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("数值型")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.updateEditProgressType("NUMERIC") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("数值型")
                            }
                        }
                    }

                    // 数值型目标设置
                    if (editState.progressType == "NUMERIC") {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumTextField(
                                value = targetValueText,
                                onValueChange = { value ->
                                    targetValueText = value.filter { it.isDigit() || it == '.' }
                                    targetValueText.toDoubleOrNull()?.let {
                                        viewModel.updateEditTargetValue(it)
                                    }
                                },
                                modifier = Modifier.weight(2f),
                                label = "目标数值 *",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )

                            PremiumTextField(
                                value = editState.unit,
                                onValueChange = { viewModel.updateEditUnit(it) },
                                modifier = Modifier.weight(1f),
                                label = "单位",
                                placeholder = "如：元、本",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
