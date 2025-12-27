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
 * OCR识别结果
 */
data class OcrResult(
    /** 完整识别文本 */
    val fullText: String,
    /** 按行分割的文本 */
    val lines: List<String>,
    /** 文本块列表（包含位置信息） */
    val blocks: List<TextBlock>,
    /** 识别置信度（0-1） */
    val confidence: Float
)

/**
 * 文本块
 */
data class TextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val lines: List<TextLine>
)

/**
 * 文本行
 */
data class TextLine(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val elements: List<TextElement>
)

/**
 * 文本元素（单词/字符）
 */
data class TextElement(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val confidence: Float
)

/**
 * OCR识别器
 * 使用ML Kit进行中文文字识别
 */
@Singleton
class OcrRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /**
     * 从Bitmap识别文字
     */
    suspend fun recognizeFromBitmap(bitmap: Bitmap): Result<OcrResult> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = performRecognition(inputImage)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从Uri识别文字
     */
    suspend fun recognizeFromUri(uri: Uri): Result<OcrResult> {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val result = performRecognition(inputImage)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从文件路径识别文字
     */
    suspend fun recognizeFromPath(path: String): Result<OcrResult> {
        return recognizeFromUri(Uri.parse("file://$path"))
    }

    /**
     * 执行OCR识别
     */
    private suspend fun performRecognition(inputImage: InputImage): OcrResult {
        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        TextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            lines = block.lines.map { line ->
                                TextLine(
                                    text = line.text,
                                    boundingBox = line.boundingBox,
                                    elements = line.elements.map { element ->
                                        TextElement(
                                            text = element.text,
                                            boundingBox = element.boundingBox,
                                            confidence = element.confidence ?: 0.9f
                                        )
                                    }
                                )
                            }
                        )
                    }

                    val lines = blocks.flatMap { it.lines.map { line -> line.text } }
                    val avgConfidence = blocks.flatMap { block ->
                        block.lines.flatMap { line ->
                            line.elements.map { it.confidence }
                        }
                    }.average().toFloat().takeIf { !it.isNaN() } ?: 0.9f

                    val result = OcrResult(
                        fullText = visionText.text,
                        lines = lines,
                        blocks = blocks,
                        confidence = avgConfidence
                    )

                    continuation.resume(result)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }

            continuation.invokeOnCancellation {
                // ML Kit没有取消方法，但我们可以忽略结果
            }
        }
    }

    /**
     * 释放资源
     */
    fun close() {
        textRecognizer.close()
    }
}
