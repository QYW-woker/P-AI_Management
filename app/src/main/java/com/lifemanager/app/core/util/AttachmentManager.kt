package com.lifemanager.app.core.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 附件管理工具类
 */
object AttachmentManager {

    private const val ATTACHMENTS_DIR = "transaction_attachments"
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 获取附件存储目录
     */
    private fun getAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, ATTACHMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 创建临时图片文件用于拍照
     */
    fun createTempImageFile(context: Context): File {
        val timestamp = dateFormat.format(Date())
        val fileName = "IMG_${timestamp}.jpg"
        return File(getAttachmentsDir(context), fileName)
    }

    /**
     * 获取FileProvider URI
     */
    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * 复制URI内容到本地文件
     */
    suspend fun copyUriToLocal(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val fileName = "ATTACH_${timestamp}.jpg"
            val destFile = File(getAttachmentsDir(context), fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 删除附件文件
     */
    suspend fun deleteAttachment(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 解析附件JSON字符串为路径列表
     */
    fun parseAttachments(json: String): List<String> {
        return try {
            if (json.isBlank() || json == "[]") {
                emptyList()
            } else {
                Json.decodeFromString<List<String>>(json)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 将路径列表转换为JSON字符串
     */
    fun toAttachmentsJson(paths: List<String>): String {
        return Json.encodeToString(paths)
    }

    /**
     * 获取附件缩略图路径（如果存在）
     */
    fun getThumbnailPath(originalPath: String): String {
        val file = File(originalPath)
        val thumbnailFile = File(file.parent, "thumb_${file.name}")
        return if (thumbnailFile.exists()) thumbnailFile.absolutePath else originalPath
    }

    /**
     * 获取附件总大小（MB）
     */
    fun getAttachmentsSizeMB(context: Context): Double {
        val dir = getAttachmentsDir(context)
        var totalSize = 0L
        dir.listFiles()?.forEach { file ->
            totalSize += file.length()
        }
        return totalSize / (1024.0 * 1024.0)
    }

    /**
     * 清理未关联的附件文件
     */
    suspend fun cleanupOrphanedAttachments(
        context: Context,
        usedPaths: Set<String>
    ): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        val dir = getAttachmentsDir(context)
        dir.listFiles()?.forEach { file ->
            if (!usedPaths.contains(file.absolutePath)) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        deletedCount
    }
}
