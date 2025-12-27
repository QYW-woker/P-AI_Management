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
import android.widget.FrameLayout
import android.widget.Toast

/**
 * 悬浮球视图
 * 自定义View实现悬浮球效果
 */
class FloatingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val BALL_SIZE_DP = 56
        private const val RIPPLE_MAX_SCALE = 1.5f
    }

    // 状态
    private var isListening = false
    private var isProcessing = false
    private var hasError = false
    private var volumeLevel = 0f

    // 画笔
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 动画相关
    private var pulseAnimator: ValueAnimator? = null
    private var rippleScale = 1f
    private var rippleAlpha = 0f

    // 颜色
    private val colorIdle = Color.parseColor("#6200EE")       // 主题色
    private val colorListening = Color.parseColor("#03DAC5")  // 录音中
    private val colorProcessing = Color.parseColor("#FF9800") // 处理中
    private val colorError = Color.parseColor("#F44336")      // 错误

    // 尺寸
    private val ballSizePx: Int
    private val centerX: Float
    private val centerY: Float
    private val radius: Float

    init {
        val density = context.resources.displayMetrics.density
        ballSizePx = (BALL_SIZE_DP * density).toInt()

        // 设置视图大小（包含波纹空间）
        val totalSize = (ballSizePx * RIPPLE_MAX_SCALE).toInt()
        layoutParams = LayoutParams(totalSize, totalSize)

        centerX = totalSize / 2f
        centerY = totalSize / 2f
        radius = ballSizePx / 2f

        setWillNotDraw(false)
        isClickable = true
        isFocusable = true

        // 设置背景透明
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制波纹（录音时）
        if (isListening && rippleScale > 1f) {
            ripplePaint.color = colorListening
            ripplePaint.alpha = (rippleAlpha * 255).toInt()
            canvas.drawCircle(centerX, centerY, radius * rippleScale, ripplePaint)
        }

        // 绘制主球体
        ballPaint.color = when {
            hasError -> colorError
            isProcessing -> colorProcessing
            isListening -> colorListening
            else -> colorIdle
        }

        // 音量响应缩放
        val volumeScale = 1f + volumeLevel * 0.15f
        canvas.drawCircle(centerX, centerY, radius * volumeScale, ballPaint)

        // 绘制麦克风图标
        drawMicIcon(canvas, centerX, centerY, radius * 0.5f)
    }

    /**
     * 绘制麦克风图标
     */
    private fun drawMicIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.strokeWidth = size * 0.15f
        iconPaint.style = if (isListening) Paint.Style.STROKE else Paint.Style.FILL

        // 麦克风主体
        val micWidth = size * 0.4f
        val micHeight = size * 0.6f

        val micRect = RectF(
            cx - micWidth / 2,
            cy - micHeight / 2 - size * 0.1f,
            cx + micWidth / 2,
            cy + micHeight / 2 - size * 0.1f
        )
        canvas.drawRoundRect(micRect, micWidth / 2, micWidth / 2, iconPaint)

        // 麦克风架
        iconPaint.style = Paint.Style.STROKE
        val standRadius = size * 0.35f
        val standTop = cy - size * 0.15f
        val standBottom = cy + size * 0.25f

        // 弧形
        val arcRect = RectF(
            cx - standRadius,
            standTop,
            cx + standRadius,
            standTop + standRadius * 2
        )
        canvas.drawArc(arcRect, 0f, 180f, false, iconPaint)

        // 底座线
        canvas.drawLine(cx, standTop + standRadius, cx, standBottom + size * 0.1f, iconPaint)
        canvas.drawLine(
            cx - size * 0.2f, standBottom + size * 0.1f,
            cx + size * 0.2f, standBottom + size * 0.1f,
            iconPaint
        )
    }

    /**
     * 更新状态
     */
    fun updateState(isListening: Boolean, isProcessing: Boolean, hasError: Boolean) {
        val wasListening = this.isListening

        this.isListening = isListening
        this.isProcessing = isProcessing
        this.hasError = hasError

        if (isListening && !wasListening) {
            startPulseAnimation()
        } else if (!isListening && wasListening) {
            stopPulseAnimation()
        }

        invalidate()
    }

    /**
     * 更新音量级别
     */
    fun updateVolumeLevel(level: Float) {
        this.volumeLevel = level.coerceIn(0f, 1f)
        invalidate()
    }

    /**
     * 显示识别结果
     */
    fun showResult(text: String) {
        Toast.makeText(context, "识别结果: $text", Toast.LENGTH_SHORT).show()
    }

    /**
     * 开始脉冲动画
     */
    private fun startPulseAnimation() {
        stopPulseAnimation()

        pulseAnimator = ValueAnimator.ofFloat(1f, RIPPLE_MAX_SCALE).apply {
            duration = 1000
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
    }

    /**
     * 停止脉冲动画
     */
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        rippleScale = 1f
        rippleAlpha = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulseAnimation()
    }
}
