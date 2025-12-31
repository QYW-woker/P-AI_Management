package com.lifemanager.app.core.floatingball

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.math.sin
import kotlin.math.cos

/**
 * 卡通风格AI悬浮球视图
 *
 * 采用可爱的卡通AI助手形象，支持多种表情和动画效果
 */
class FloatingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val BALL_SIZE_DP = 64
        private const val RIPPLE_MAX_SCALE = 1.6f

        // 表情状态
        const val EXPRESSION_IDLE = 0      // 默认微笑
        const val EXPRESSION_LISTENING = 1 // 认真听
        const val EXPRESSION_THINKING = 2  // 思考中
        const val EXPRESSION_HAPPY = 3     // 开心
        const val EXPRESSION_ERROR = 4     // 困惑/错误
        const val EXPRESSION_SLEEPING = 5  // 休眠
    }

    // 状态
    private var isListening = false
    private var isProcessing = false
    private var hasError = false
    private var volumeLevel = 0f
    private var currentExpression = EXPRESSION_IDLE

    // 动画参数
    private var blinkProgress = 0f      // 眨眼进度 0-1
    private var breathProgress = 0f     // 呼吸进度 0-2π
    private var bounceOffset = 0f       // 弹跳偏移
    private var antennaAngle = 0f       // 天线摆动角度
    private var eyeSparkle = 0f         // 眼睛闪烁
    private var mouthOpenness = 0f      // 嘴巴张开程度
    private var cheekBlush = 0f         // 脸红程度
    private var rippleScale = 1f
    private var rippleAlpha = 0f

    // 画笔
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bodyGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2C3E50")
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E74C3C")
    }

    private val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val antennaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f
    }

    private val antennaGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#20000000")
    }

    // 颜色主题
    private val colorBodyIdle = Color.parseColor("#667EEA")       // 渐变紫蓝
    private val colorBodyIdleLight = Color.parseColor("#764BA2")
    private val colorBodyListening = Color.parseColor("#11998E")  // 渐变青绿
    private val colorBodyListeningLight = Color.parseColor("#38EF7D")
    private val colorBodyProcessing = Color.parseColor("#F093FB") // 渐变粉紫
    private val colorBodyProcessingLight = Color.parseColor("#F5576C")
    private val colorBodyError = Color.parseColor("#FF6B6B")      // 渐变红
    private val colorBodyErrorLight = Color.parseColor("#EE5A24")
    private val colorCheek = Color.parseColor("#40FFB6C1")        // 腮红粉

    // 尺寸
    private val ballSizePx: Int
    private val totalSizePx: Int
    private val centerX: Float
    private val centerY: Float
    private val radius: Float

    // 动画器
    private var blinkAnimator: ValueAnimator? = null
    private var breathAnimator: ValueAnimator? = null
    private var bounceAnimator: ValueAnimator? = null
    private var antennaAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var expressionAnimator: AnimatorSet? = null

    init {
        val density = context.resources.displayMetrics.density
        ballSizePx = (BALL_SIZE_DP * density).toInt()

        // 设置视图大小（包含波纹和天线空间）
        totalSizePx = (ballSizePx * RIPPLE_MAX_SCALE * 1.2f).toInt()
        layoutParams = LayoutParams(totalSizePx, totalSizePx)

        centerX = totalSizePx / 2f
        centerY = totalSizePx / 2f + ballSizePx * 0.1f // 稍微下移留天线空间
        radius = ballSizePx / 2f

        outlinePaint.strokeWidth = radius * 0.04f
        mouthPaint.strokeWidth = radius * 0.08f
        antennaPaint.strokeWidth = radius * 0.08f

        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        setBackgroundColor(Color.TRANSPARENT)

        // 启动基础动画
        startIdleAnimations()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(totalSizePx, totalSizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val breathOffset = sin(breathProgress) * radius * 0.03f
        val currentCenterY = centerY + bounceOffset + breathOffset

        // 绘制阴影
        drawShadow(canvas, centerX, currentCenterY + radius * 0.9f)

        // 绘制波纹（录音时）
        if (isListening && rippleScale > 1f) {
            drawRipples(canvas, centerX, currentCenterY)
        }

        // 绘制天线
        drawAntenna(canvas, centerX, currentCenterY - radius)

        // 绘制身体
        drawBody(canvas, centerX, currentCenterY)

        // 绘制眼睛
        drawEyes(canvas, centerX, currentCenterY)

        // 绘制腮红
        drawCheeks(canvas, centerX, currentCenterY)

        // 绘制嘴巴
        drawMouth(canvas, centerX, currentCenterY)

        // 绘制高光
        drawBodyHighlight(canvas, centerX, currentCenterY)
    }

    /**
     * 绘制阴影
     */
    private fun drawShadow(canvas: Canvas, cx: Float, cy: Float) {
        val shadowRadius = radius * 0.7f * (1f - bounceOffset / (radius * 0.3f)).coerceIn(0.5f, 1f)
        canvas.drawOval(
            cx - shadowRadius,
            cy - shadowRadius * 0.3f,
            cx + shadowRadius,
            cy + shadowRadius * 0.3f,
            shadowPaint
        )
    }

    /**
     * 绘制波纹
     */
    private fun drawRipples(canvas: Canvas, cx: Float, cy: Float) {
        val baseColor = getBodyColor().first
        for (i in 0..2) {
            val scale = rippleScale + i * 0.15f
            val alpha = ((1f - (scale - 1f) / (RIPPLE_MAX_SCALE - 1f)) * 0.3f * 255).toInt()
            ripplePaint.color = baseColor
            ripplePaint.alpha = alpha.coerceIn(0, 255)
            canvas.drawCircle(cx, cy, radius * scale, ripplePaint)
        }
    }

    /**
     * 绘制天线
     */
    private fun drawAntenna(canvas: Canvas, cx: Float, topY: Float) {
        val (color1, _) = getBodyColor()
        antennaPaint.color = color1

        // 天线杆
        val antennaHeight = radius * 0.35f
        val antennaEndX = cx + sin(Math.toRadians(antennaAngle.toDouble())).toFloat() * radius * 0.15f
        val antennaEndY = topY - antennaHeight

        canvas.drawLine(cx, topY, antennaEndX, antennaEndY, antennaPaint)

        // 天线顶部发光球
        val glowRadius = radius * 0.12f
        val glowAlpha = (0.5f + sin(breathProgress * 2) * 0.5f)

        // 外发光
        antennaGlowPaint.shader = RadialGradient(
            antennaEndX, antennaEndY, glowRadius * 2,
            Color.argb((glowAlpha * 100).toInt(), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(antennaEndX, antennaEndY, glowRadius * 2, antennaGlowPaint)

        // 核心球
        antennaGlowPaint.shader = null
        antennaGlowPaint.color = if (isListening) {
            Color.parseColor("#00FF88")
        } else if (isProcessing) {
            Color.parseColor("#FFD700")
        } else {
            Color.WHITE
        }
        canvas.drawCircle(antennaEndX, antennaEndY, glowRadius, antennaGlowPaint)
    }

    /**
     * 绘制身体
     */
    private fun drawBody(canvas: Canvas, cx: Float, cy: Float) {
        val (color1, color2) = getBodyColor()

        // 渐变效果
        bodyGradientPaint.shader = LinearGradient(
            cx - radius, cy - radius,
            cx + radius, cy + radius,
            color1, color2,
            Shader.TileMode.CLAMP
        )

        // 音量响应缩放
        val volumeScale = 1f + volumeLevel * 0.08f
        canvas.drawCircle(cx, cy, radius * volumeScale, bodyGradientPaint)

        // 轮廓
        outlinePaint.color = Color.argb(50, 0, 0, 0)
        canvas.drawCircle(cx, cy, radius * volumeScale, outlinePaint)
    }

    /**
     * 绘制身体高光
     */
    private fun drawBodyHighlight(canvas: Canvas, cx: Float, cy: Float) {
        // 顶部高光
        val highlightPath = Path()
        val highlightRadius = radius * 0.7f
        val highlightCenterY = cy - radius * 0.3f

        highlightPaint.shader = RadialGradient(
            cx - radius * 0.2f, highlightCenterY,
            highlightRadius,
            Color.argb(80, 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx - radius * 0.2f, highlightCenterY, highlightRadius * 0.5f, highlightPaint)
        highlightPaint.shader = null
    }

    /**
     * 绘制眼睛
     */
    private fun drawEyes(canvas: Canvas, cx: Float, cy: Float) {
        val eyeOffsetX = radius * 0.28f
        val eyeOffsetY = -radius * 0.1f
        val eyeRadius = radius * 0.22f

        // 计算眨眼效果
        val eyeScaleY = 1f - blinkProgress * 0.9f

        for (side in listOf(-1f, 1f)) {
            val eyeCx = cx + eyeOffsetX * side
            val eyeCy = cy + eyeOffsetY

            // 保存画布状态
            canvas.save()
            canvas.scale(1f, eyeScaleY, eyeCx, eyeCy)

            when (currentExpression) {
                EXPRESSION_HAPPY -> {
                    // 开心的弯眼睛 ^_^
                    drawHappyEye(canvas, eyeCx, eyeCy, eyeRadius, side)
                }
                EXPRESSION_ERROR -> {
                    // 困惑的X眼睛
                    drawConfusedEye(canvas, eyeCx, eyeCy, eyeRadius)
                }
                EXPRESSION_SLEEPING -> {
                    // 睡眠的横线眼睛
                    drawSleepingEye(canvas, eyeCx, eyeCy, eyeRadius)
                }
                else -> {
                    // 正常圆眼睛
                    drawNormalEye(canvas, eyeCx, eyeCy, eyeRadius, side)
                }
            }

            canvas.restore()
        }
    }

    /**
     * 绘制正常眼睛
     */
    private fun drawNormalEye(canvas: Canvas, cx: Float, cy: Float, radius: Float, side: Float) {
        // 眼白
        eyePaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, radius, eyePaint)

        // 瞳孔（根据状态略微移动）
        val pupilOffsetX = if (isListening) side * radius * 0.1f else 0f
        val pupilRadius = radius * 0.55f

        pupilPaint.color = Color.parseColor("#2C3E50")
        canvas.drawCircle(cx + pupilOffsetX, cy, pupilRadius, pupilPaint)

        // 瞳孔内圈
        pupilPaint.color = Color.parseColor("#1A252F")
        canvas.drawCircle(cx + pupilOffsetX, cy, pupilRadius * 0.6f, pupilPaint)

        // 高光
        highlightPaint.color = Color.WHITE
        highlightPaint.shader = null
        canvas.drawCircle(cx + pupilOffsetX - radius * 0.2f, cy - radius * 0.2f, radius * 0.2f, highlightPaint)

        // 小高光
        canvas.drawCircle(cx + pupilOffsetX + radius * 0.15f, cy + radius * 0.1f, radius * 0.08f, highlightPaint)
    }

    /**
     * 绘制开心眼睛 ^_^
     */
    private fun drawHappyEye(canvas: Canvas, cx: Float, cy: Float, radius: Float, side: Float) {
        mouthPaint.color = Color.parseColor("#2C3E50")
        mouthPaint.strokeWidth = radius * 0.35f
        mouthPaint.style = Paint.Style.STROKE

        val path = Path()
        path.moveTo(cx - radius * 0.8f, cy + radius * 0.2f)
        path.quadTo(cx, cy - radius * 0.6f, cx + radius * 0.8f, cy + radius * 0.2f)
        canvas.drawPath(path, mouthPaint)

        mouthPaint.strokeWidth = this.radius * 0.08f
    }

    /**
     * 绘制困惑眼睛
     */
    private fun drawConfusedEye(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        mouthPaint.color = Color.parseColor("#2C3E50")
        mouthPaint.strokeWidth = radius * 0.3f

        // X形状
        val offset = radius * 0.5f
        canvas.drawLine(cx - offset, cy - offset, cx + offset, cy + offset, mouthPaint)
        canvas.drawLine(cx - offset, cy + offset, cx + offset, cy - offset, mouthPaint)

        mouthPaint.strokeWidth = this.radius * 0.08f
    }

    /**
     * 绘制睡眠眼睛
     */
    private fun drawSleepingEye(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        mouthPaint.color = Color.parseColor("#2C3E50")
        mouthPaint.strokeWidth = radius * 0.25f
        canvas.drawLine(cx - radius * 0.7f, cy, cx + radius * 0.7f, cy, mouthPaint)
        mouthPaint.strokeWidth = this.radius * 0.08f
    }

    /**
     * 绘制腮红
     */
    private fun drawCheeks(canvas: Canvas, cx: Float, cy: Float) {
        if (cheekBlush <= 0) return

        val cheekOffsetX = radius * 0.55f
        val cheekOffsetY = radius * 0.25f
        val cheekRadius = radius * 0.15f

        cheekPaint.color = Color.argb((cheekBlush * 100).toInt(), 255, 182, 193)

        for (side in listOf(-1f, 1f)) {
            canvas.drawCircle(
                cx + cheekOffsetX * side,
                cy + cheekOffsetY,
                cheekRadius,
                cheekPaint
            )
        }
    }

    /**
     * 绘制嘴巴
     */
    private fun drawMouth(canvas: Canvas, cx: Float, cy: Float) {
        val mouthY = cy + radius * 0.35f
        mouthPaint.color = Color.parseColor("#E74C3C")
        mouthPaint.style = Paint.Style.STROKE

        when (currentExpression) {
            EXPRESSION_HAPPY -> {
                // 大笑嘴巴
                val path = Path()
                val mouthWidth = radius * 0.5f
                path.moveTo(cx - mouthWidth, mouthY - radius * 0.05f)
                path.quadTo(cx, mouthY + radius * 0.25f, cx + mouthWidth, mouthY - radius * 0.05f)
                canvas.drawPath(path, mouthPaint)
            }
            EXPRESSION_LISTENING -> {
                // 小O嘴
                mouthPaint.style = Paint.Style.FILL
                val ovalRadius = radius * 0.12f + mouthOpenness * radius * 0.08f
                canvas.drawOval(
                    cx - ovalRadius * 0.7f,
                    mouthY - ovalRadius,
                    cx + ovalRadius * 0.7f,
                    mouthY + ovalRadius,
                    mouthPaint
                )
            }
            EXPRESSION_THINKING -> {
                // 波浪嘴
                val path = Path()
                val waveWidth = radius * 0.35f
                path.moveTo(cx - waveWidth, mouthY)
                path.cubicTo(
                    cx - waveWidth * 0.5f, mouthY - radius * 0.1f,
                    cx + waveWidth * 0.5f, mouthY + radius * 0.1f,
                    cx + waveWidth, mouthY
                )
                canvas.drawPath(path, mouthPaint)
            }
            EXPRESSION_ERROR -> {
                // 波浪嘴（下垂）
                val path = Path()
                val mouthWidth = radius * 0.3f
                path.moveTo(cx - mouthWidth, mouthY)
                path.quadTo(cx, mouthY + radius * 0.15f, cx + mouthWidth, mouthY)
                canvas.drawPath(path, mouthPaint)
            }
            EXPRESSION_SLEEPING -> {
                // 小气泡Z
                mouthPaint.style = Paint.Style.STROKE
                mouthPaint.textSize = radius * 0.3f
                // 简单横线
                canvas.drawLine(cx - radius * 0.15f, mouthY, cx + radius * 0.15f, mouthY, mouthPaint)
            }
            else -> {
                // 默认微笑
                val path = Path()
                val smileWidth = radius * 0.35f
                path.moveTo(cx - smileWidth, mouthY)
                path.quadTo(cx, mouthY + radius * 0.15f, cx + smileWidth, mouthY)
                canvas.drawPath(path, mouthPaint)
            }
        }
    }

    /**
     * 获取当前状态的身体颜色
     */
    private fun getBodyColor(): Pair<Int, Int> {
        return when {
            hasError -> Pair(colorBodyError, colorBodyErrorLight)
            isProcessing -> Pair(colorBodyProcessing, colorBodyProcessingLight)
            isListening -> Pair(colorBodyListening, colorBodyListeningLight)
            else -> Pair(colorBodyIdle, colorBodyIdleLight)
        }
    }

    /**
     * 更新状态
     */
    fun updateState(isListening: Boolean, isProcessing: Boolean, hasError: Boolean) {
        val wasListening = this.isListening

        this.isListening = isListening
        this.isProcessing = isProcessing
        this.hasError = hasError

        // 更新表情
        currentExpression = when {
            hasError -> EXPRESSION_ERROR
            isProcessing -> EXPRESSION_THINKING
            isListening -> EXPRESSION_LISTENING
            else -> EXPRESSION_IDLE
        }

        // 更新腮红
        cheekBlush = when {
            isListening -> 0.8f
            isProcessing -> 0.5f
            else -> 0f
        }

        if (isListening && !wasListening) {
            startListeningAnimations()
        } else if (!isListening && wasListening) {
            stopListeningAnimations()
        }

        if (isProcessing) {
            startThinkingAnimation()
        }

        invalidate()
    }

    /**
     * 更新音量级别
     */
    fun updateVolumeLevel(level: Float) {
        this.volumeLevel = level.coerceIn(0f, 1f)
        mouthOpenness = level
        invalidate()
    }

    /**
     * 显示识别结果
     */
    fun showResult(text: String) {
        // 显示开心表情
        currentExpression = EXPRESSION_HAPPY
        cheekBlush = 1f
        invalidate()

        postDelayed({
            currentExpression = EXPRESSION_IDLE
            cheekBlush = 0f
            invalidate()
        }, 2000)

        Toast.makeText(context, "识别结果: $text", Toast.LENGTH_SHORT).show()
    }

    /**
     * 启动空闲动画
     */
    private fun startIdleAnimations() {
        // 眨眼动画
        startBlinkAnimation()

        // 呼吸动画
        startBreathAnimation()

        // 天线摆动
        startAntennaAnimation()
    }

    /**
     * 眨眼动画
     */
    private fun startBlinkAnimation() {
        blinkAnimator?.cancel()

        blinkAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 150
            startDelay = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animator ->
                blinkProgress = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    /**
     * 呼吸动画
     */
    private fun startBreathAnimation() {
        breathAnimator?.cancel()

        breathAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                breathProgress = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    /**
     * 天线摆动动画
     */
    private fun startAntennaAnimation() {
        antennaAnimator?.cancel()

        antennaAnimator = ValueAnimator.ofFloat(-15f, 15f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                antennaAngle = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    /**
     * 开始监听动画
     */
    private fun startListeningAnimations() {
        // 脉冲波纹
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, RIPPLE_MAX_SCALE).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                rippleScale = animator.animatedValue as Float
                rippleAlpha = 1f - (rippleScale - 1f) / (RIPPLE_MAX_SCALE - 1f)
                invalidate()
            }

            start()
        }

        // 轻微弹跳
        bounceAnimator?.cancel()
        bounceAnimator = ValueAnimator.ofFloat(0f, -radius * 0.08f, 0f).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                bounceOffset = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    /**
     * 停止监听动画
     */
    private fun stopListeningAnimations() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        rippleScale = 1f
        rippleAlpha = 0f

        bounceAnimator?.cancel()
        bounceAnimator = null
        bounceOffset = 0f

        invalidate()
    }

    /**
     * 思考动画
     */
    private fun startThinkingAnimation() {
        // 天线快速摆动
        antennaAnimator?.cancel()
        antennaAnimator = ValueAnimator.ofFloat(-25f, 25f).apply {
            duration = 400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener { animator ->
                antennaAngle = animator.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blinkAnimator?.cancel()
        breathAnimator?.cancel()
        bounceAnimator?.cancel()
        antennaAnimator?.cancel()
        pulseAnimator?.cancel()
        expressionAnimator?.cancel()
    }
}
