package com.lifemanager.app.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifemanager.app.core.util.AttachmentManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * 附件选择器组件
 */
@Composable
fun AttachmentPicker(
    attachments: List<String>,
    onAttachmentsChanged: (List<String>) -> Unit,
    maxAttachments: Int = 5,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    // 相机拍照结果
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            val newList = attachments.toMutableList()
            newList.add(tempPhotoFile!!.absolutePath)
            onAttachmentsChanged(newList)
        }
    }

    // 图库选择结果
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val localPath = AttachmentManager.copyUriToLocal(context, it)
                if (localPath != null) {
                    val newList = attachments.toMutableList()
                    newList.add(localPath)
                    onAttachmentsChanged(newList)
                }
            }
        }
    }

    Column(modifier = modifier) {
        // 标题和添加按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "小票/凭证（${attachments.size}/$maxAttachments）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (attachments.size < maxAttachments) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "添加附件",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("拍照") },
                            onClick = {
                                showMenu = false
                                tempPhotoFile = AttachmentManager.createTempImageFile(context)
                                val uri = AttachmentManager.getFileUri(context, tempPhotoFile!!)
                                cameraLauncher.launch(uri)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("从相册选择") },
                            onClick = {
                                showMenu = false
                                galleryLauncher.launch("image/*")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 附件预览列表
        if (attachments.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (attachments.size < maxAttachments) {
                            showMenu = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击添加小票或凭证",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 附件列表
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                attachments.forEachIndexed { index, path ->
                    AttachmentThumbnail(
                        path = path,
                        onClick = { selectedImageIndex = index },
                        onDelete = {
                            scope.launch {
                                AttachmentManager.deleteAttachment(path)
                                val newList = attachments.toMutableList()
                                newList.removeAt(index)
                                onAttachmentsChanged(newList)
                            }
                        }
                    )
                }

                // 添加更多按钮
                if (attachments.size < maxAttachments) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 图片预览对话框
    if (selectedImageIndex != null) {
        ImagePreviewDialog(
            imagePath = attachments[selectedImageIndex!!],
            onDismiss = { selectedImageIndex = null },
            onDelete = {
                scope.launch {
                    val path = attachments[selectedImageIndex!!]
                    AttachmentManager.deleteAttachment(path)
                    val newList = attachments.toMutableList()
                    newList.removeAt(selectedImageIndex!!)
                    onAttachmentsChanged(newList)
                    selectedImageIndex = null
                }
            }
        )
    }
}

/**
 * 附件缩略图
 */
@Composable
private fun AttachmentThumbnail(
    path: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(path))
                .crossfade(true)
                .build(),
            contentDescription = "附件图片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 图片预览对话框
 */
@Composable
private fun ImagePreviewDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("图片预览") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "预览图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        }
    )
}

/**
 * 只读附件查看器（用于已保存的交易）
 */
@Composable
fun AttachmentViewer(
    attachments: List<String>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    if (attachments.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = "附件（${attachments.size}）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            attachments.forEachIndexed { index, path ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIndex = index }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(path))
                            .crossfade(true)
                            .build(),
                        contentDescription = "附件图片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    // 预览对话框
    if (selectedIndex != null) {
        AlertDialog(
            onDismissRequest = { selectedIndex = null },
            title = { Text("图片预览") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(attachments[selectedIndex!!]))
                            .crossfade(true)
                            .build(),
                        contentDescription = "预览图片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedIndex = null }) {
                    Text("关闭")
                }
            }
        )
    }
}
