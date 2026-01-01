package com.lifemanager.app.core.floatingball

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.lifemanager.app.R
import java.io.File
import kotlin.math.sin
import kotlin.math.cos

/**
 * 白衣小仙女风格AI悬浮球视图
 *
 * 使用图片资源显示可爱的小仙女形象
 */
class FloatingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val BALL_SIZE_DP = 72
        private const val RIPPLE_MAX_SCALE = 1.4f

        // 心情等级
        const val MOOD_VERY_BAD = 1
        const val MOOD_BAD = 2
        const val MOOD_NORMAL = 3
        const val MOOD_GOOD = 4
        const val MOOD_VERY_GOOD = 5

        // 状态
        const val STATE_IDLE = 0
        const val STATE_LISTENING = 1
        const val STATE_THINKING = 2
        const val STATE_SUCCESS = 3
        const val STATE_ERROR = 4
    }

    // 状态
    private var isListening = false
    private var isProcessing = false
    private var hasError = false
    private var volumeLevel = 0f
    private var currentMood = MOOD_NORMAL
    private var currentState = STATE_IDLE

    // 动画参数
    private var floatOffset = 0f
    private var breathScale = 1f
    private var glowPulse = 0f
    private var rippleScale = 1f

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 仙女图片
    private var fairyDrawable: Drawable? = null
    private var customAvatarBitmap: Bitmap? = null
    private var customAvatarPath: String? = null

    // 尺寸
    private val ballSizePx: Int
    private val totalSizePx: Int
    private val centerX: Float
    private val centerY: Float

    // 动画器
    private var breathAnimator: ValueAnimator? = null
    private var floatAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null

    init {
        val density = context.resources.displayMetrics.density
        ballSizePx = (BALL_SIZE_DP * density).toInt()
        totalSizePx = (ballSizePx * RIPPLE_MAX_SCALE * 1.2f).toInt()
        layoutParams = LayoutParams(totalSizePx, totalSizePx)

        centerX = totalSizePx / 2f
        centerY = totalSizePx / 2f

        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        setBackgroundColor(Color.TRANSPARENT)

        // 加载仙女图片
        loadFairyImage()

        startIdleAnimations()
    }

    /**
     * 加载仙女图片资源
     */
    private fun loadFairyImage() {
        // 先尝试加载自定义头像
        loadCustomAvatarFromPrefs()

        // 如果没有自定义头像，加载默认图片
        if (customAvatarBitmap == null) {
            try {
                fairyDrawable = ContextCompat.getDrawable(context, R.drawable.ic_fairy_assistant)
            } catch (e: Exception) {
                fairyDrawable = null
            }
        }
    }

    /**
     * 从配置中加载自定义头像
     */
    private fun loadCustomAvatarFromPrefs() {
        try {
            val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
            val path = prefs.getString("custom_avatar_path", null)
            if (path != null) {
                setCustomAvatar(path)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置自定义头像
     * @param path 图片文件路径
     */
    fun setCustomAvatar(path: String?) {
        if (path == customAvatarPath) return

        customAvatarPath = path

        // 回收旧的bitmap
        customAvatarBitmap?.recycle()
        customAvatarBitmap = null

        if (path != null) {
            try {
                val file = File(path)
                if (file.exists()) {
                    // 加载并缩放图片
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(path, options)

                    // 计算缩放比例
                    val targetSize = ballSizePx
                    val sampleSize = calculateInSampleSize(options, targetSize, targetSize)

                    options.inJustDecodeBounds = false
                    options.inSampleSize = sampleSize

                    val bitmap = BitmapFactory.decodeFile(path, options)
                    if (bitmap != null) {
                        // 缩放到目标尺寸
                        customAvatarBitmap = Bitmap.createScaledBitmap(
                            bitmap,
                            targetSize,
                            targetSize,
                            true
                        )
                        if (bitmap != customAvatarBitmap) {
                            bitmap.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                customAvatarBitmap = null
            }
        }

        invalidate()
    }

    /**
     * 计算图片采样率
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 清除自定义头像，使用默认形象
     */
    fun clearCustomAvatar() {
        customAvatarPath = null
        customAvatarBitmap?.recycle()
        customAvatarBitmap = null

        // 重新加载默认图片
        try {
            fairyDrawable = ContextCompat.getDrawable(context, R.drawable.ic_fairy_assistant)
        } catch (e: Exception) {
            fairyDrawable = null
        }

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(totalSizePx, totalSizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val floatY = centerY + floatOffset

        canvas.save()
        canvas.scale(breathScale, breathScale, centerX, floatY)

        // 1. 绘制光晕背景
        drawGlow(canvas, centerX, floatY)

        // 2. 绘制波纹（录音时）
        if (isListening && rippleScale > 1f) {
            drawRipples(canvas, centerX, floatY)
        }

        // 3. 绘制仙女图片或备用图形
        drawFairy(canvas, centerX, floatY)

        // 4. 绘制状态效果
        drawStatusEffect(canvas, centerX, floatY)

        canvas.restore()
    }

    /**
     * 绘制光晕背景
     */
    private fun drawGlow(canvas: Canvas, cx: Float, cy: Float) {
        val glowRadius = ballSizePx * 0.55f * (1f + glowPulse * 0.1f)

        paint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(
                Color.argb(60, 255, 255, 255),
                Color.argb(25, 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowRadius, paint)
        paint.shader = null
    }

    /**
     * 绘制波纹（录音时）
     */
    private fun drawRipples(canvas: Canvas, cx: Float, cy: Float) {
        for (i in 0..2) {
            val scale = rippleScale + i * 0.1f
            val alpha = ((1f - (scale - 1f) / (RIPPLE_MAX_SCALE - 1f)) * 0.15f * 255).toInt()
            paint.color = Color.argb(alpha.coerceIn(0, 255), 200, 220, 255)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, ballSizePx * 0.4f * scale, paint)
        }
    }

    /**
     * 绘制仙女（图片或备用方案）
     */
    private fun drawFairy(canvas: Canvas, cx: Float, cy: Float) {
        val imgSize = (ballSizePx * 0.85f).toInt()
        val left = (cx - imgSize / 2f).toInt()
        val top = (cy - imgSize / 2f).toInt()

        // 优先使用自定义头像
        val customBitmap = customAvatarBitmap
        if (customBitmap != null && !customBitmap.isRecycled) {
            val destRect = RectF(left.toFloat(), top.toFloat(), (left + imgSize).toFloat(), (top + imgSize).toFloat())
            paint.isFilterBitmap = true
            canvas.drawBitmap(customBitmap, null, destRect, paint)
            return
        }

        // 其次使用默认drawable
        val drawable = fairyDrawable
        if (drawable != null) {
            drawable.setBounds(left, top, left + imgSize, top + imgSize)
            drawable.draw(canvas)
        } else {
            // 备用方案：简单的卡通形象
            drawFallbackFairy(canvas, cx, cy)
        }
    }

    /**
     * 备用绘制方案（当图片资源不存在时）
     */
    private fun drawFallbackFairy(canvas: Canvas, cx: Float, cy: Float) {
        // 头发（后面）
        paint.color = Color.parseColor("#2D2D2D")
        paint.style = Paint.Style.FILL

        // 左侧长发
        val leftHair = Path().apply {
            moveTo(cx - ballSizePx * 0.15f, cy - ballSizePx * 0.15f)
            quadTo(cx - ballSizePx * 0.25f, cy + ballSizePx * 0.1f,
                   cx - ballSizePx * 0.2f, cy + ballSizePx * 0.3f)
            lineTo(cx - ballSizePx * 0.1f, cy + ballSizePx * 0.25f)
            close()
        }
        canvas.drawPath(leftHair, paint)

        // 右侧长发
        val rightHair = Path().apply {
            moveTo(cx + ballSizePx * 0.15f, cy - ballSizePx * 0.15f)
            quadTo(cx + ballSizePx * 0.25f, cy + ballSizePx * 0.1f,
                   cx + ballSizePx * 0.2f, cy + ballSizePx * 0.3f)
            lineTo(cx + ballSizePx * 0.1f, cy + ballSizePx * 0.25f)
            close()
        }
        canvas.drawPath(rightHair, paint)

        // 白色裙子
        paint.color = Color.WHITE
        val dress = Path().apply {
            moveTo(cx - ballSizePx * 0.08f, cy + ballSizePx * 0.02f)
            lineTo(cx + ballSizePx * 0.08f, cy + ballSizePx * 0.02f)
            lineTo(cx + ballSizePx * 0.18f, cy + ballSizePx * 0.35f)
            quadTo(cx, cy + ballSizePx * 0.37f, cx - ballSizePx * 0.18f, cy + ballSizePx * 0.35f)
            close()
        }
        canvas.drawPath(dress, paint)

        // 腰带
        paint.color = Color.parseColor("#9E9E9E")
        canvas.drawRect(
            cx - ballSizePx * 0.1f, cy + ballSizePx * 0.08f,
            cx + ballSizePx * 0.1f, cy + ballSizePx * 0.11f,
            paint
        )

        // 金色挂坠
        paint.color = Color.parseColor("#DAA520")
        canvas.drawCircle(cx, cy + ballSizePx * 0.14f, ballSizePx * 0.015f, paint)

        // 脸部
        paint.color = Color.parseColor("#FFEEE6")
        canvas.drawOval(
            cx - ballSizePx * 0.17f, cy - ballSizePx * 0.28f,
            cx + ballSizePx * 0.17f, cy + ballSizePx * 0.08f,
            paint
        )

        // 头发（前面）
        paint.color = Color.parseColor("#2D2D2D")
        val frontHair = Path().apply {
            moveTo(cx - ballSizePx * 0.17f, cy - ballSizePx * 0.05f)
            quadTo(cx - ballSizePx * 0.19f, cy - ballSizePx * 0.25f, cx - ballSizePx * 0.08f, cy - ballSizePx * 0.3f)
            quadTo(cx, cy - ballSizePx * 0.32f, cx + ballSizePx * 0.08f, cy - ballSizePx * 0.3f)
            quadTo(cx + ballSizePx * 0.19f, cy - ballSizePx * 0.25f, cx + ballSizePx * 0.17f, cy - ballSizePx * 0.05f)
            lineTo(cx + ballSizePx * 0.12f, cy - ballSizePx * 0.02f)
            lineTo(cx + ballSizePx * 0.01f, cy - ballSizePx * 0.18f)
            lineTo(cx - ballSizePx * 0.01f, cy - ballSizePx * 0.18f)
            lineTo(cx - ballSizePx * 0.12f, cy - ballSizePx * 0.02f)
            close()
        }
        canvas.drawPath(frontHair, paint)

        // 头冠
        paint.color = Color.parseColor("#D0D0D0")
        val tiara = Path().apply {
            moveTo(cx - ballSizePx * 0.07f, cy - ballSizePx * 0.26f)
            lineTo(cx - ballSizePx * 0.05f, cy - ballSizePx * 0.3f)
            lineTo(cx, cy - ballSizePx * 0.34f)
            lineTo(cx + ballSizePx * 0.05f, cy - ballSizePx * 0.3f)
            lineTo(cx + ballSizePx * 0.07f, cy - ballSizePx * 0.26f)
            close()
        }
        canvas.drawPath(tiara, paint)

        // 头冠中心
        paint.color = Color.parseColor("#4A4A4A")
        val tiaraCenter = Path().apply {
            moveTo(cx, cy - ballSizePx * 0.32f)
            lineTo(cx - ballSizePx * 0.02f, cy - ballSizePx * 0.27f)
            lineTo(cx + ballSizePx * 0.02f, cy - ballSizePx * 0.27f)
            close()
        }
        canvas.drawPath(tiaraCenter, paint)

        // 眼睛
        paint.color = Color.parseColor("#3D3D3D")
        canvas.drawCircle(cx - ballSizePx * 0.07f, cy - ballSizePx * 0.08f, ballSizePx * 0.028f, paint)
        canvas.drawCircle(cx + ballSizePx * 0.07f, cy - ballSizePx * 0.08f, ballSizePx * 0.028f, paint)

        // 眼睛高光
        paint.color = Color.WHITE
        canvas.drawCircle(cx - ballSizePx * 0.075f, cy - ballSizePx * 0.088f, ballSizePx * 0.01f, paint)
        canvas.drawCircle(cx + ballSizePx * 0.065f, cy - ballSizePx * 0.088f, ballSizePx * 0.01f, paint)

        // 腮红
        paint.color = Color.argb(50, 255, 180, 180)
        canvas.drawCircle(cx - ballSizePx * 0.11f, cy - ballSizePx * 0.02f, ballSizePx * 0.03f, paint)
        canvas.drawCircle(cx + ballSizePx * 0.11f, cy - ballSizePx * 0.02f, ballSizePx * 0.03f, paint)

        // 嘴巴
        paint.color = Color.parseColor("#E8A0A0")
        canvas.drawOval(
            cx - ballSizePx * 0.015f, cy + ballSizePx * 0.0f,
            cx + ballSizePx * 0.015f, cy + ballSizePx * 0.015f,
            paint
        )

        // 小手
        paint.color = Color.parseColor("#FFEEE6")
        canvas.drawCircle(cx - ballSizePx * 0.18f, cy + ballSizePx * 0.18f, ballSizePx * 0.02f, paint)
        canvas.drawCircle(cx + ballSizePx * 0.18f, cy + ballSizePx * 0.18f, ballSizePx * 0.02f, paint)
    }

    /**
     * 绘制状态效果
     */
    private fun drawStatusEffect(canvas: Canvas, cx: Float, cy: Float) {
        when (currentState) {
            STATE_LISTENING -> {
                paint.color = Color.parseColor("#90CAF9")
                paint.textSize = ballSizePx * 0.1f
                val offset = sin(glowPulse * 2) * ballSizePx * 0.02f
                canvas.drawText("♪", cx + ballSizePx * 0.32f, cy - ballSizePx * 0.28f + offset, paint)
            }
            STATE_THINKING -> {
                paint.color = Color.WHITE
                val offset = sin(glowPulse) * ballSizePx * 0.01f
                canvas.drawCircle(cx + ballSizePx * 0.28f, cy - ballSizePx * 0.32f + offset, ballSizePx * 0.02f, paint)
                canvas.drawCircle(cx + ballSizePx * 0.34f, cy - ballSizePx * 0.4f + offset, ballSizePx * 0.03f, paint)
                canvas.drawCircle(cx + ballSizePx * 0.4f, cy - ballSizePx * 0.48f + offset, ballSizePx * 0.04f, paint)
            }
            STATE_SUCCESS -> {
                paint.color = Color.parseColor("#FFD700")
                val offset = sin(glowPulse * 4) * ballSizePx * 0.015f
                drawStar(canvas, cx - ballSizePx * 0.3f, cy - ballSizePx * 0.35f + offset, ballSizePx * 0.04f)
                drawStar(canvas, cx + ballSizePx * 0.32f, cy - ballSizePx * 0.32f - offset, ballSizePx * 0.03f)
            }
            STATE_ERROR -> {
                paint.color = Color.parseColor("#FFCDD2")
                paint.textSize = ballSizePx * 0.1f
                canvas.drawText("?", cx + ballSizePx * 0.28f, cy - ballSizePx * 0.28f, paint)
            }
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) size else size * 0.4f
            val angle = Math.PI / 5 * i - Math.PI / 2
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    // ==================== 状态更新方法 ====================

    fun updateState(isListening: Boolean, isProcessing: Boolean, hasError: Boolean) {
        val wasListening = this.isListening

        this.isListening = isListening
        this.isProcessing = isProcessing
        this.hasError = hasError

        currentState = when {
            hasError -> STATE_ERROR
            isProcessing -> STATE_THINKING
            isListening -> STATE_LISTENING
            else -> STATE_IDLE
        }

        if (isListening && !wasListening) {
            startListeningAnimations()
        } else if (!isListening && wasListening) {
            stopListeningAnimations()
        }

        invalidate()
    }

    fun setMood(mood: Int) {
        currentMood = mood.coerceIn(MOOD_VERY_BAD, MOOD_VERY_GOOD)
        invalidate()
    }

    fun updateVolumeLevel(level: Float) {
        this.volumeLevel = level.coerceIn(0f, 1f)
        invalidate()
    }

    fun showResult(text: String) {
        currentState = STATE_SUCCESS
        currentMood = MOOD_VERY_GOOD
        invalidate()

        postDelayed({
            currentState = STATE_IDLE
            currentMood = MOOD_NORMAL
            invalidate()
        }, 2500)

        Toast.makeText(context, "识别结果: $text", Toast.LENGTH_SHORT).show()
    }

    // ==================== 动画控制 ====================

    private fun startIdleAnimations() {
        startBreathAnimation()
        startFloatAnimation()
        startGlowAnimation()
    }

    private fun startBreathAnimation() {
        breathAnimator?.cancel()
        breathAnimator = ValueAnimator.ofFloat(1f, 1.02f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                breathScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startFloatAnimation() {
        floatAnimator?.cancel()
        floatAnimator = ValueAnimator.ofFloat(-ballSizePx * 0.02f, ballSizePx * 0.02f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                floatOffset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startGlowAnimation() {
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                glowPulse = sin(it.animatedValue as Float)
                invalidate()
            }
            start()
        }
    }

    private fun startListeningAnimations() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, RIPPLE_MAX_SCALE).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                rippleScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopListeningAnimations() {
        pulseAnimator?.cancel()
        rippleScale = 1f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        breathAnimator?.cancel()
        floatAnimator?.cancel()
        glowAnimator?.cancel()
        pulseAnimator?.cancel()

        // 回收自定义头像bitmap
        customAvatarBitmap?.recycle()
        customAvatarBitmap = null
    }

    /**
     * 刷新头像（当设置更改时调用）
     */
    fun refreshAvatar() {
        loadCustomAvatarFromPrefs()
        invalidate()
    }
}
