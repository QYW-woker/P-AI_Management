package com.lifemanager.app.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR文字识别管理器
 * 使用ML Kit进行中文文字识别
 */
@Singleton
class OcrManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 中文文字识别器
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /**
     * 从Bitmap识别文字
     * @param bitmap 图片Bitmap
     * @return 识别到的文字
     */
    suspend fun recognizeFromBitmap(bitmap: Bitmap): Result<OcrResult> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val text = processImage(inputImage)
            Result.success(OcrResult(
                rawText = text,
                lines = text.lines().filter { it.isNotBlank() }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从URI识别文字
     * @param uri 图片URI
     * @return 识别到的文字
     */
    suspend fun recognizeFromUri(uri: Uri): Result<OcrResult> {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val text = processImage(inputImage)
            Result.success(OcrResult(
                rawText = text,
                lines = text.lines().filter { it.isNotBlank() }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 处理图片并提取文字
     */
    private suspend fun processImage(inputImage: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    /**
     * 释放资源
     */
    fun close() {
        recognizer.close()
    }
}

/**
 * OCR识别结果
 */
data class OcrResult(
    val rawText: String,
    val lines: List<String>
)
