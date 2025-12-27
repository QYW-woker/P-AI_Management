package com.lifemanager.app.feature.ai.component

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.core.ai.model.TransactionType
import java.io.File

/**
 * 图片识别组件
 * 支持拍照和从相册选择图片
 */
@Composable
fun ImageRecognitionComponent(
    isProcessing: Boolean,
    recognizedPayment: PaymentInfo?,
    onImageSelected: (Uri) -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit,
    onConfirm: (PaymentInfo) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // 相机权限
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 创建临时文件并启动相机
            tempCameraUri = createTempImageUri(context)
            tempCameraUri?.let { /* 相机launcher会在之后调用 */ }
        }
    }

    // 相册选择
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedBitmap = loadBitmapFromUri(context, it)
            onImageSelected(it)
        }
    }

    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                selectedImageUri = uri
                selectedBitmap = loadBitmapFromUri(context, uri)
                onImageSelected(uri)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 图片预览区域
        if (selectedBitmap != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    selectedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "已选择的图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 处理中指示器
                    if (isProcessing) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("正在识别...")
                                }
                            }
                        }
                    }

                    // 重新选择按钮
                    if (!isProcessing) {
                        IconButton(
                            onClick = {
                                selectedBitmap = null
                                selectedImageUri = null
                                showSourceDialog = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "重新选择",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else {
            // 图片选择区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { showSourceDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击选择图片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "支持支付截图、发票等",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 识别结果
        AnimatedVisibility(visible = recognizedPayment != null) {
            recognizedPayment?.let { payment ->
                PaymentInfoCard(
                    payment = payment,
                    onConfirm = { onConfirm(payment) },
                    onCancel = onCancel
                )
            }
        }
    }

    // 图片来源选择对话框
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("选择图片来源") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("拍照") },
                        leadingContent = {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            // 检查权限
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            tempCameraUri = createTempImageUri(context)
                            tempCameraUri?.let { cameraLauncher.launch(it) }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("从相册选择") },
                        leadingContent = {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 支付信息卡片
 */
@Composable
private fun PaymentInfoCard(
    payment: PaymentInfo,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "识别结果",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 金额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "金额",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "¥${String.format("%.2f", payment.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (payment.type == TransactionType.EXPENSE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            // 类型
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "类型",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (payment.type == TransactionType.EXPENSE) "支出" else "收入",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 商户
            payment.payee?.let { payee ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "商户",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = payee,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }

            // 来源
            payment.paymentMethod?.let { method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "来源",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = getSourceName(method),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onConfirm) {
                    Text("确认记账")
                }
            }
        }
    }
}

/**
 * 获取来源名称
 */
private fun getSourceName(source: String): String {
    return when (source) {
        "WECHAT" -> "微信支付"
        "ALIPAY" -> "支付宝"
        "UNIONPAY" -> "云闪付"
        "JD" -> "京东支付"
        "MEITUAN" -> "美团支付"
        "DOUYIN" -> "抖音支付"
        else -> "其他"
    }
}

/**
 * 创建临时图片URI
 */
private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "camera_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

/**
 * 从URI加载Bitmap
 */
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            // 先获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)

            // 计算采样率，限制最大尺寸为1920
            val maxSize = 1920
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxSize ||
                options.outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            // 重新打开流并解码
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                })
            }
        }
    } catch (e: Exception) {
        null
    }
}
