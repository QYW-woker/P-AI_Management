package com.lifemanager.app.core.ocr.di

import android.content.Context
import com.lifemanager.app.core.ai.service.AIService
import com.lifemanager.app.core.ocr.OcrRecognizer
import com.lifemanager.app.core.ocr.ScreenshotParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * OCR模块依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideOcrRecognizer(
        @ApplicationContext context: Context
    ): OcrRecognizer {
        return OcrRecognizer(context)
    }

    @Provides
    @Singleton
    fun provideScreenshotParser(
        ocrRecognizer: OcrRecognizer,
        aiService: AIService
    ): ScreenshotParser {
        return ScreenshotParser(ocrRecognizer, aiService)
    }
}
