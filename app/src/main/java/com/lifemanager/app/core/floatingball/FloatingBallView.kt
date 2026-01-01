package com.lifemanager.app.core.floatingball

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import kotlin.math.sin
import kotlin.math.cos

/**
 * iOS Siri风格AI悬浮球视图
 *
 * 使用彩色渐变波浪动画，模拟Siri的经典视觉效果
 */
class FloatingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val BALL_SIZE_DP = 64
        private const val OUTER_GLOW_SCALE = 1.5f

        // 状态
        const val STATE_IDLE = 0
        const val STATE_LISTENING = 1
        const val STATE_THINKING = 2
        const val STATE_SUCCESS = 3
        const val STATE_ERROR = 4

        // Siri渐变颜色
        private val SIRI_COLORS = intArrayOf(
            Color.parseColor("#FF2D55"),  // 粉红
            Color.parseColor("#FF3B30"),  // 红色
            Color.parseColor("#FF9500"),  // 橙色
            Color.parseColor("#FFCC00"),  // 黄色
            Color.parseColor("#34C759"),  // 绿色
            Color.parseColor("#00C7BE"),  // 青色
            Color.parseColor("#007AFF"),  // 蓝色
            Color.parseColor("#5856D6"),  // 紫色
            Color.parseColor("#AF52DE"),  // 洋红
            Color.parseColor("#FF2D55")   // 循环回粉红
        )
    }

    // 状态
    private var isListening = false
    private var isProcessing = false
    private var hasError = false
    private var volumeLevel = 0f
    private var currentState = STATE_IDLE

    // 动画参数
    private var wavePhase = 0f
    private var colorRotation = 0f
    private var pulseScale = 1f
    private var glowIntensity = 0.3f
    private var waveAmplitude = 0.05f

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 尺寸
    private val ballSizePx: Int
    private val totalSizePx: Int
    private val centerX: Float
    private val centerY: Float
    private val ballRadius: Float

    // 动画器
    private var waveAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null

    // 波浪路径
    private val wavePath = Path()

    init {
        val density = context.resources.displayMetrics.density
        ballSizePx = (BALL_SIZE_DP * density).toInt()
        totalSizePx = (ballSizePx * OUTER_GLOW_SCALE * 1.2f).toInt()
        layoutParams = LayoutParams(totalSizePx, totalSizePx)

        centerX = totalSizePx / 2f
        centerY = totalSizePx / 2f
        ballRadius = ballSizePx / 2f

        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        setBackgroundColor(Color.TRANSPARENT)

        setupPaints()
        startIdleAnimations()
    }

    private fun setupPaints() {
        paint.style = Paint.Style.FILL
        glowPaint.style = Paint.Style.FILL
        wavePaint.style = Paint.Style.FILL
        wavePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(totalSizePx, totalSizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 绘制外发光
        drawOuterGlow(canvas)

        // 2. 绘制主体圆形背景
        drawMainOrb(canvas)

        // 3. 绘制Siri彩色波浪
        drawSiriWaves(canvas)

        // 4. 绘制状态指示
        drawStateIndicator(canvas)
    }

    /**
     * 绘制外发光效果
     */
    private fun drawOuterGlow(canvas: Canvas) {
        val glowRadius = ballRadius * (1.2f + glowIntensity * 0.3f) * pulseScale

        // 根据状态选择发光颜色
        val glowColor = when (currentState) {
            STATE_LISTENING -> Color.parseColor("#007AFF")
            STATE_THINKING -> Color.parseColor("#AF52DE")
            STATE_SUCCESS -> Color.parseColor("#34C759")
            STATE_ERROR -> Color.parseColor("#FF3B30")
            else -> Color.parseColor("#8E8E93")
        }

        val alpha = (glowIntensity * 80).toInt()
        glowPaint.shader = RadialGradient(
            centerX, centerY, glowRadius,
            intArrayOf(
                Color.argb(alpha, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)),
                Color.argb(alpha / 2, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.4f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, glowRadius, glowPaint)
    }

    /**
     * 绘制主体圆形（深色背景）
     */
    private fun drawMainOrb(canvas: Canvas) {
        val radius = ballRadius * pulseScale

        // 深色渐变背景
        paint.shader = RadialGradient(
            centerX, centerY - radius * 0.3f, radius * 1.2f,
            intArrayOf(
                Color.parseColor("#3A3A3C"),
                Color.parseColor("#2C2C2E"),
                Color.parseColor("#1C1C1E")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.shader = null

        // 内边缘高光
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.shader = LinearGradient(
            centerX, centerY - radius,
            centerX, centerY + radius,
            Color.argb(60, 255, 255, 255),
            Color.argb(20, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius - 1f, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    /**
     * 绘制Siri风格的彩色波浪
     */
    private fun drawSiriWaves(canvas: Canvas) {
        val radius = ballRadius * pulseScale * 0.85f
        val amplitude = radius * waveAmplitude * (1f + volumeLevel * 2f)

        // 保存画布状态，设置圆形裁剪区域
        canvas.save()
        val clipPath = Path()
        clipPath.addCircle(centerX, centerY, ballRadius * pulseScale * 0.95f, Path.Direction.CW)
        canvas.clipPath(clipPath)

        // 绘制多层波浪
        val layerCount = if (isListening || isProcessing) 4 else 2
        for (layer in 0 until layerCount) {
            drawWaveLayer(canvas, radius, amplitude, layer, layerCount)
        }

        canvas.restore()
    }

    /**
     * 绘制单层波浪
     */
    private fun drawWaveLayer(canvas: Canvas, radius: Float, amplitude: Float, layer: Int, totalLayers: Int) {
        val phaseOffset = layer * (Math.PI / totalLayers).toFloat()
        val colorOffset = (colorRotation + layer * 0.25f) % 1f
        val layerAlpha = if (isListening || isProcessing) {
            (0.6f - layer * 0.1f)
        } else {
            (0.3f - layer * 0.08f)
        }

        wavePath.reset()

        // 创建波浪形状
        val segments = 60
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * 2 * Math.PI
            val waveOffset = sin(angle * 3 + wavePhase + phaseOffset) * amplitude +
                    sin(angle * 5 + wavePhase * 1.3f + phaseOffset) * amplitude * 0.5f

            val r = radius + waveOffset
            val x = centerX + (r * cos(angle)).toFloat()
            val y = centerY + (r * sin(angle)).toFloat()

            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }
        wavePath.close()

        // 创建扫描渐变
        val colors = IntArray(SIRI_COLORS.size)
        val positions = FloatArray(SIRI_COLORS.size)
        for (i in SIRI_COLORS.indices) {
            val colorIndex = ((i + (colorOffset * SIRI_COLORS.size).toInt()) % SIRI_COLORS.size)
            val color = SIRI_COLORS[colorIndex]
            colors[i] = Color.argb(
                (layerAlpha * 255).toInt(),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            positions[i] = i.toFloat() / (SIRI_COLORS.size - 1)
        }

        wavePaint.shader = SweepGradient(centerX, centerY, colors, positions)
        canvas.drawPath(wavePath, wavePaint)
    }

    /**
     * 绘制状态指示器
     */
    private fun drawStateIndicator(canvas: Canvas) {
        when (currentState) {
            STATE_LISTENING -> drawListeningIndicator(canvas)
            STATE_THINKING -> drawThinkingIndicator(canvas)
            STATE_SUCCESS -> drawSuccessIndicator(canvas)
            STATE_ERROR -> drawErrorIndicator(canvas)
        }
    }

    private fun drawListeningIndicator(canvas: Canvas) {
        // 绘制麦克风图标样式的音量条
        val barCount = 5
        val barWidth = ballRadius * 0.08f
        val barSpacing = ballRadius * 0.12f
        val maxHeight = ballRadius * 0.5f
        val startX = centerX - (barCount - 1) * barSpacing / 2

        paint.color = Color.WHITE

        for (i in 0 until barCount) {
            val heightFactor = when (i) {
                0, 4 -> 0.3f + volumeLevel * 0.3f + sin(wavePhase + i) * 0.1f
                1, 3 -> 0.5f + volumeLevel * 0.4f + sin(wavePhase + i * 0.8f) * 0.15f
                else -> 0.7f + volumeLevel * 0.3f + sin(wavePhase + i * 0.6f) * 0.2f
            }
            val barHeight = maxHeight * heightFactor

            val x = startX + i * barSpacing
            val rect = RectF(
                x - barWidth / 2,
                centerY - barHeight / 2,
                x + barWidth / 2,
                centerY + barHeight / 2
            )
            canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, paint)
        }
    }

    private fun drawThinkingIndicator(canvas: Canvas) {
        // 绘制旋转的点
        val dotCount = 3
        val dotRadius = ballRadius * 0.06f
        val orbitRadius = ballRadius * 0.25f

        for (i in 0 until dotCount) {
            val angle = colorRotation * 2 * Math.PI + i * (2 * Math.PI / dotCount)
            val x = centerX + (orbitRadius * cos(angle)).toFloat()
            val y = centerY + (orbitRadius * sin(angle)).toFloat()

            val alpha = (0.5f + 0.5f * sin(colorRotation * 4 * Math.PI + i)).toInt() * 255
            paint.color = Color.argb(alpha.coerceIn(100, 255), 255, 255, 255)
            canvas.drawCircle(x, y, dotRadius, paint)
        }
    }

    private fun drawSuccessIndicator(canvas: Canvas) {
        // 绘制对勾
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ballRadius * 0.1f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val path = Path()
        path.moveTo(centerX - ballRadius * 0.25f, centerY)
        path.lineTo(centerX - ballRadius * 0.05f, centerY + ballRadius * 0.2f)
        path.lineTo(centerX + ballRadius * 0.25f, centerY - ballRadius * 0.15f)

        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawErrorIndicator(canvas: Canvas) {
        // 绘制感叹号
        paint.color = Color.WHITE

        // 圆点
        canvas.drawCircle(centerX, centerY + ballRadius * 0.2f, ballRadius * 0.06f, paint)

        // 竖线
        val rect = RectF(
            centerX - ballRadius * 0.05f,
            centerY - ballRadius * 0.25f,
            centerX + ballRadius * 0.05f,
            centerY + ballRadius * 0.08f
        )
        canvas.drawRoundRect(rect, ballRadius * 0.05f, ballRadius * 0.05f, paint)
    }

    // ==================== 状态更新方法 ====================

    fun updateState(isListening: Boolean, isProcessing: Boolean, hasError: Boolean) {
        val wasListening = this.isListening
        val wasProcessing = this.isProcessing

        this.isListening = isListening
        this.isProcessing = isProcessing
        this.hasError = hasError

        currentState = when {
            hasError -> STATE_ERROR
            isProcessing -> STATE_THINKING
            isListening -> STATE_LISTENING
            else -> STATE_IDLE
        }

        // 根据状态调整动画参数
        when (currentState) {
            STATE_LISTENING -> {
                waveAmplitude = 0.12f
                startPulseAnimation(1f, 1.05f, 800)
            }
            STATE_THINKING -> {
                waveAmplitude = 0.08f
                startPulseAnimation(1f, 1.03f, 1200)
            }
            STATE_SUCCESS, STATE_ERROR -> {
                waveAmplitude = 0.05f
                pulseScale = 1f
            }
            else -> {
                waveAmplitude = 0.05f
                startPulseAnimation(1f, 1.02f, 2000)
            }
        }

        invalidate()
    }

    fun setMood(mood: Int) {
        // Siri风格不使用心情，保留接口兼容性
        invalidate()
    }

    fun updateVolumeLevel(level: Float) {
        this.volumeLevel = level.coerceIn(0f, 1f)
        waveAmplitude = 0.08f + level * 0.15f
        invalidate()
    }

    fun showResult(text: String) {
        currentState = STATE_SUCCESS
        invalidate()

        postDelayed({
            currentState = STATE_IDLE
            invalidate()
        }, 2000)
    }

    // ==================== 动画控制 ====================

    private fun startIdleAnimations() {
        startWaveAnimation()
        startColorAnimation()
        startGlowAnimation()
        startPulseAnimation(1f, 1.02f, 2000)
    }

    private fun startWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                wavePhase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startColorAnimation() {
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                colorRotation = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startGlowAnimation() {
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0.3f, 0.6f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                glowIntensity = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startPulseAnimation(from: Float, to: Float, duration: Long) {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                pulseScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator?.cancel()
        colorAnimator?.cancel()
        pulseAnimator?.cancel()
        glowAnimator?.cancel()
    }

    // ==================== 兼容性方法 ====================

    /**
     * 设置自定义头像（Siri风格不使用头像，保留接口兼容性）
     */
    fun setCustomAvatar(path: String?) {
        // Siri风格不支持自定义头像
    }

    /**
     * 清除自定义头像
     */
    fun clearCustomAvatar() {
        // Siri风格不使用头像
    }

    /**
     * 刷新头像（保留接口兼容性）
     */
    fun refreshAvatar() {
        invalidate()
    }
}
