package com.lifemanager.app.core.floatingball

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.math.sin
import kotlin.math.cos

/**
 * 白衣小仙女风格AI悬浮球视图
 *
 * 像素级还原参考图中的Q版小仙女形象
 * 特点：大头小身、扁平化设计、简洁可爱
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
    private var blinkProgress = 0f
    private var breathProgress = 0f
    private var floatOffset = 0f
    private var hairWaveProgress = 0f
    private var glowPulse = 0f
    private var rippleScale = 1f

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 配色 - 完全匹配参考图
    private val colorHairBlack = Color.parseColor("#2D2D2D")       // 黑色头发
    private val colorSkin = Color.parseColor("#FFEEE6")            // 肤色（偏暖白）
    private val colorCheek = Color.parseColor("#FFCCCC")           // 腮红
    private val colorEyeBlack = Color.parseColor("#3D3D3D")        // 眼睛黑色
    private val colorMouthPink = Color.parseColor("#E8A0A0")       // 嘴巴粉色
    private val colorDressWhite = Color.parseColor("#FFFFFF")      // 白色裙子
    private val colorDressShadow = Color.parseColor("#F0F0F0")     // 裙子阴影
    private val colorBeltGray = Color.parseColor("#9E9E9E")        // 灰色腰带
    private val colorPendant = Color.parseColor("#DAA520")         // 金色挂坠
    private val colorTiara = Color.parseColor("#D0D0D0")           // 银色头冠
    private val colorTiaraCenter = Color.parseColor("#4A4A4A")     // 头冠中心
    private val colorGlow = Color.parseColor("#FFFFFF")            // 光晕

    // 尺寸
    private val ballSizePx: Int
    private val totalSizePx: Int
    private val centerX: Float
    private val centerY: Float

    // 动画器
    private var blinkAnimator: ValueAnimator? = null
    private var breathAnimator: ValueAnimator? = null
    private var floatAnimator: ValueAnimator? = null
    private var hairAnimator: ValueAnimator? = null
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

        startIdleAnimations()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(totalSizePx, totalSizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val floatY = centerY + floatOffset
        val breathScale = 1f + sin(breathProgress) * 0.01f

        canvas.save()
        canvas.scale(breathScale, breathScale, centerX, floatY)

        // 绘制顺序（从后到前）
        drawGlow(canvas, centerX, floatY)

        if (isListening && rippleScale > 1f) {
            drawRipples(canvas, centerX, floatY)
        }

        drawBackHair(canvas, centerX, floatY)      // 后面的头发
        drawDress(canvas, centerX, floatY)          // 白色裙子
        drawHead(canvas, centerX, floatY)           // 头部（脸）
        drawFrontHair(canvas, centerX, floatY)      // 前面的头发（刘海）
        drawFace(canvas, centerX, floatY)           // 五官
        drawTiara(canvas, centerX, floatY)          // 头冠
        drawArms(canvas, centerX, floatY)           // 小手臂

        drawStatusEffect(canvas, centerX, floatY)

        canvas.restore()
    }

    /**
     * 绘制光晕背景
     */
    private fun drawGlow(canvas: Canvas, cx: Float, cy: Float) {
        val glowRadius = ballSizePx * 0.5f * (1f + glowPulse * 0.08f)

        paint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(
                Color.argb(80, 255, 255, 255),
                Color.argb(30, 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
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
     * 绘制后面的长发
     */
    private fun drawBackHair(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = colorHairBlack
        paint.style = Paint.Style.FILL

        val headTop = cy - ballSizePx * 0.28f
        val hairWave = sin(hairWaveProgress) * ballSizePx * 0.015f

        // 左侧长发
        val leftHair = Path().apply {
            moveTo(cx - ballSizePx * 0.18f, headTop + ballSizePx * 0.1f)
            // 外侧曲线
            quadTo(
                cx - ballSizePx * 0.28f + hairWave, cy + ballSizePx * 0.1f,
                cx - ballSizePx * 0.24f + hairWave * 0.5f, cy + ballSizePx * 0.32f
            )
            // 发尾
            quadTo(
                cx - ballSizePx * 0.2f, cy + ballSizePx * 0.35f,
                cx - ballSizePx * 0.14f, cy + ballSizePx * 0.3f
            )
            // 内侧
            lineTo(cx - ballSizePx * 0.1f, cy + ballSizePx * 0.05f)
            close()
        }
        canvas.drawPath(leftHair, paint)

        // 右侧长发
        val rightHair = Path().apply {
            moveTo(cx + ballSizePx * 0.18f, headTop + ballSizePx * 0.1f)
            quadTo(
                cx + ballSizePx * 0.28f - hairWave, cy + ballSizePx * 0.1f,
                cx + ballSizePx * 0.24f - hairWave * 0.5f, cy + ballSizePx * 0.32f
            )
            quadTo(
                cx + ballSizePx * 0.2f, cy + ballSizePx * 0.35f,
                cx + ballSizePx * 0.14f, cy + ballSizePx * 0.3f
            )
            lineTo(cx + ballSizePx * 0.1f, cy + ballSizePx * 0.05f)
            close()
        }
        canvas.drawPath(rightHair, paint)
    }

    /**
     * 绘制白色A字裙
     */
    private fun drawDress(canvas: Canvas, cx: Float, cy: Float) {
        val dressTop = cy + ballSizePx * 0.02f
        val dressBottom = cy + ballSizePx * 0.38f

        // 白色裙身 - 简单的梯形/A字形
        paint.color = colorDressWhite
        paint.style = Paint.Style.FILL

        val dressPath = Path().apply {
            // 领口（窄）
            moveTo(cx - ballSizePx * 0.08f, dressTop)
            lineTo(cx + ballSizePx * 0.08f, dressTop)
            // 右侧裙摆（宽）
            lineTo(cx + ballSizePx * 0.2f, dressBottom)
            // 底部裙摆 - 略带弧度
            quadTo(cx, dressBottom + ballSizePx * 0.02f, cx - ballSizePx * 0.2f, dressBottom)
            // 左侧回到领口
            close()
        }
        canvas.drawPath(dressPath, paint)

        // 裙子阴影（左侧）
        paint.color = colorDressShadow
        val shadowPath = Path().apply {
            moveTo(cx - ballSizePx * 0.08f, dressTop)
            lineTo(cx - ballSizePx * 0.03f, dressTop)
            lineTo(cx - ballSizePx * 0.1f, dressBottom)
            lineTo(cx - ballSizePx * 0.2f, dressBottom)
            close()
        }
        canvas.drawPath(shadowPath, paint)

        // 灰色腰带
        paint.color = colorBeltGray
        val beltY = dressTop + ballSizePx * 0.06f
        val beltHeight = ballSizePx * 0.03f
        canvas.drawRect(
            cx - ballSizePx * 0.12f, beltY,
            cx + ballSizePx * 0.12f, beltY + beltHeight,
            paint
        )

        // 金色挂坠
        paint.color = colorPendant
        canvas.drawCircle(cx, beltY + beltHeight + ballSizePx * 0.03f, ballSizePx * 0.018f, paint)
    }

    /**
     * 绘制头部（椭圆形脸）
     */
    private fun drawHead(canvas: Canvas, cx: Float, cy: Float) {
        val headCenterY = cy - ballSizePx * 0.12f
        val headRadiusX = ballSizePx * 0.2f   // 横向半径
        val headRadiusY = ballSizePx * 0.22f  // 纵向半径（稍高）

        // 脸部（肤色椭圆）
        paint.color = colorSkin
        paint.style = Paint.Style.FILL

        val faceRect = RectF(
            cx - headRadiusX, headCenterY - headRadiusY,
            cx + headRadiusX, headCenterY + headRadiusY * 0.9f
        )
        canvas.drawOval(faceRect, paint)
    }

    /**
     * 绘制前面的头发（刘海和头顶）
     */
    private fun drawFrontHair(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = colorHairBlack
        paint.style = Paint.Style.FILL

        val headCenterY = cy - ballSizePx * 0.12f
        val headTop = headCenterY - ballSizePx * 0.22f

        // 头顶发型（覆盖头部上半部分）
        val topHair = Path().apply {
            // 从左边开始
            moveTo(cx - ballSizePx * 0.2f, headCenterY - ballSizePx * 0.05f)
            // 左侧弧线向上
            quadTo(cx - ballSizePx * 0.22f, headTop, cx - ballSizePx * 0.1f, headTop - ballSizePx * 0.04f)
            // 头顶中间
            quadTo(cx, headTop - ballSizePx * 0.06f, cx + ballSizePx * 0.1f, headTop - ballSizePx * 0.04f)
            // 右侧弧线向下
            quadTo(cx + ballSizePx * 0.22f, headTop, cx + ballSizePx * 0.2f, headCenterY - ballSizePx * 0.05f)
            // 右侧边缘向下延伸
            lineTo(cx + ballSizePx * 0.19f, headCenterY + ballSizePx * 0.08f)
            // 右侧刘海内边
            lineTo(cx + ballSizePx * 0.12f, headCenterY - ballSizePx * 0.02f)
            // 中分线右侧
            lineTo(cx + ballSizePx * 0.01f, headTop + ballSizePx * 0.08f)
            // 中分线左侧
            lineTo(cx - ballSizePx * 0.01f, headTop + ballSizePx * 0.08f)
            // 左侧刘海内边
            lineTo(cx - ballSizePx * 0.12f, headCenterY - ballSizePx * 0.02f)
            // 左侧边缘
            lineTo(cx - ballSizePx * 0.19f, headCenterY + ballSizePx * 0.08f)
            close()
        }
        canvas.drawPath(topHair, paint)
    }

    /**
     * 绘制五官
     */
    private fun drawFace(canvas: Canvas, cx: Float, cy: Float) {
        val headCenterY = cy - ballSizePx * 0.12f

        // 眼睛位置
        val eyeY = headCenterY + ballSizePx * 0.02f
        val eyeSpacing = ballSizePx * 0.09f
        val eyeRadius = ballSizePx * 0.035f

        // 眨眼时眼睛变成线
        val eyeScaleY = 1f - blinkProgress * 0.9f

        paint.color = colorEyeBlack
        paint.style = Paint.Style.FILL

        // 左眼
        canvas.save()
        canvas.scale(1f, eyeScaleY, cx - eyeSpacing, eyeY)
        canvas.drawCircle(cx - eyeSpacing, eyeY, eyeRadius, paint)
        canvas.restore()

        // 右眼
        canvas.save()
        canvas.scale(1f, eyeScaleY, cx + eyeSpacing, eyeY)
        canvas.drawCircle(cx + eyeSpacing, eyeY, eyeRadius, paint)
        canvas.restore()

        // 眼睛高光（不眨眼时才显示）
        if (blinkProgress < 0.3f) {
            paint.color = Color.WHITE
            val highlightRadius = eyeRadius * 0.35f
            val highlightOffsetX = -eyeRadius * 0.25f
            val highlightOffsetY = -eyeRadius * 0.25f
            canvas.drawCircle(cx - eyeSpacing + highlightOffsetX, eyeY + highlightOffsetY, highlightRadius, paint)
            canvas.drawCircle(cx + eyeSpacing + highlightOffsetX, eyeY + highlightOffsetY, highlightRadius, paint)
        }

        // 腮红
        paint.color = Color.argb(60, 255, 180, 180)
        val cheekY = eyeY + ballSizePx * 0.06f
        val cheekRadius = ballSizePx * 0.035f
        canvas.drawCircle(cx - eyeSpacing - ballSizePx * 0.03f, cheekY, cheekRadius, paint)
        canvas.drawCircle(cx + eyeSpacing + ballSizePx * 0.03f, cheekY, cheekRadius, paint)

        // 嘴巴
        drawMouth(canvas, cx, headCenterY)
    }

    /**
     * 绘制嘴巴
     */
    private fun drawMouth(canvas: Canvas, cx: Float, headCenterY: Float) {
        val mouthY = headCenterY + ballSizePx * 0.12f

        paint.color = colorMouthPink
        paint.style = Paint.Style.FILL

        when (currentMood) {
            MOOD_VERY_GOOD -> {
                // 开心大笑 - 弧形嘴
                val smilePath = Path().apply {
                    moveTo(cx - ballSizePx * 0.04f, mouthY)
                    quadTo(cx, mouthY + ballSizePx * 0.035f, cx + ballSizePx * 0.04f, mouthY)
                    quadTo(cx, mouthY + ballSizePx * 0.02f, cx - ballSizePx * 0.04f, mouthY)
                }
                canvas.drawPath(smilePath, paint)
            }
            MOOD_GOOD -> {
                // 微笑 - 小弧线
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = ballSizePx * 0.015f
                paint.strokeCap = Paint.Cap.ROUND
                val smilePath = Path().apply {
                    moveTo(cx - ballSizePx * 0.025f, mouthY)
                    quadTo(cx, mouthY + ballSizePx * 0.02f, cx + ballSizePx * 0.025f, mouthY)
                }
                canvas.drawPath(smilePath, paint)
                paint.style = Paint.Style.FILL
            }
            MOOD_NORMAL -> {
                // 普通 - 小圆点/椭圆
                canvas.drawOval(
                    cx - ballSizePx * 0.018f, mouthY - ballSizePx * 0.008f,
                    cx + ballSizePx * 0.018f, mouthY + ballSizePx * 0.008f,
                    paint
                )
            }
            MOOD_BAD, MOOD_VERY_BAD -> {
                // 难过 - 倒弧线
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = ballSizePx * 0.015f
                paint.strokeCap = Paint.Cap.ROUND
                val sadPath = Path().apply {
                    moveTo(cx - ballSizePx * 0.025f, mouthY + ballSizePx * 0.01f)
                    quadTo(cx, mouthY - ballSizePx * 0.015f, cx + ballSizePx * 0.025f, mouthY + ballSizePx * 0.01f)
                }
                canvas.drawPath(sadPath, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    /**
     * 绘制头冠
     */
    private fun drawTiara(canvas: Canvas, cx: Float, cy: Float) {
        val headCenterY = cy - ballSizePx * 0.12f
        val tiaraY = headCenterY - ballSizePx * 0.22f

        // 头冠底座（银色）
        paint.color = colorTiara
        paint.style = Paint.Style.FILL

        // 简单的三角形头冠
        val tiaraPath = Path().apply {
            // 底部左
            moveTo(cx - ballSizePx * 0.08f, tiaraY + ballSizePx * 0.04f)
            // 左边尖
            lineTo(cx - ballSizePx * 0.06f, tiaraY - ballSizePx * 0.01f)
            // 中间最高点
            lineTo(cx, tiaraY - ballSizePx * 0.05f)
            // 右边尖
            lineTo(cx + ballSizePx * 0.06f, tiaraY - ballSizePx * 0.01f)
            // 底部右
            lineTo(cx + ballSizePx * 0.08f, tiaraY + ballSizePx * 0.04f)
            close()
        }
        canvas.drawPath(tiaraPath, paint)

        // 中心装饰（深色）
        paint.color = colorTiaraCenter
        val centerPath = Path().apply {
            moveTo(cx, tiaraY - ballSizePx * 0.03f)
            lineTo(cx - ballSizePx * 0.025f, tiaraY + ballSizePx * 0.02f)
            lineTo(cx + ballSizePx * 0.025f, tiaraY + ballSizePx * 0.02f)
            close()
        }
        canvas.drawPath(centerPath, paint)
    }

    /**
     * 绘制小手臂
     */
    private fun drawArms(canvas: Canvas, cx: Float, cy: Float) {
        val armY = cy + ballSizePx * 0.12f
        val armWave = sin(hairWaveProgress) * ballSizePx * 0.01f

        // 白色袖子
        paint.color = colorDressWhite
        paint.style = Paint.Style.FILL

        // 左袖
        val leftArm = Path().apply {
            moveTo(cx - ballSizePx * 0.12f, armY - ballSizePx * 0.02f)
            quadTo(
                cx - ballSizePx * 0.2f + armWave, armY,
                cx - ballSizePx * 0.22f + armWave, armY + ballSizePx * 0.08f
            )
            lineTo(cx - ballSizePx * 0.18f + armWave, armY + ballSizePx * 0.1f)
            lineTo(cx - ballSizePx * 0.1f, armY + ballSizePx * 0.03f)
            close()
        }
        canvas.drawPath(leftArm, paint)

        // 右袖
        val rightArm = Path().apply {
            moveTo(cx + ballSizePx * 0.12f, armY - ballSizePx * 0.02f)
            quadTo(
                cx + ballSizePx * 0.2f - armWave, armY,
                cx + ballSizePx * 0.22f - armWave, armY + ballSizePx * 0.08f
            )
            lineTo(cx + ballSizePx * 0.18f - armWave, armY + ballSizePx * 0.1f)
            lineTo(cx + ballSizePx * 0.1f, armY + ballSizePx * 0.03f)
            close()
        }
        canvas.drawPath(rightArm, paint)

        // 小手（肤色圆点）
        paint.color = colorSkin
        canvas.drawCircle(cx - ballSizePx * 0.2f + armWave, armY + ballSizePx * 0.09f, ballSizePx * 0.022f, paint)
        canvas.drawCircle(cx + ballSizePx * 0.2f - armWave, armY + ballSizePx * 0.09f, ballSizePx * 0.022f, paint)
    }

    /**
     * 绘制状态效果
     */
    private fun drawStatusEffect(canvas: Canvas, cx: Float, cy: Float) {
        when (currentState) {
            STATE_LISTENING -> {
                // 音符
                paint.color = Color.parseColor("#90CAF9")
                paint.textSize = ballSizePx * 0.1f
                val offset = sin(hairWaveProgress * 2) * ballSizePx * 0.02f
                canvas.drawText("♪", cx + ballSizePx * 0.3f, cy - ballSizePx * 0.3f + offset, paint)
            }
            STATE_THINKING -> {
                // 思考泡泡
                paint.color = Color.WHITE
                val offset = sin(hairWaveProgress) * ballSizePx * 0.01f
                canvas.drawCircle(cx + ballSizePx * 0.28f, cy - ballSizePx * 0.35f + offset, ballSizePx * 0.02f, paint)
                canvas.drawCircle(cx + ballSizePx * 0.34f, cy - ballSizePx * 0.42f + offset, ballSizePx * 0.03f, paint)
                canvas.drawCircle(cx + ballSizePx * 0.4f, cy - ballSizePx * 0.5f + offset, ballSizePx * 0.04f, paint)
            }
            STATE_SUCCESS -> {
                // 星星
                paint.color = Color.parseColor("#FFD700")
                val offset = sin(glowPulse * 4) * ballSizePx * 0.015f
                drawStar(canvas, cx - ballSizePx * 0.3f, cy - ballSizePx * 0.38f + offset, ballSizePx * 0.04f)
                drawStar(canvas, cx + ballSizePx * 0.32f, cy - ballSizePx * 0.35f - offset, ballSizePx * 0.03f)
            }
            STATE_ERROR -> {
                paint.color = Color.parseColor("#FFCDD2")
                paint.textSize = ballSizePx * 0.1f
                canvas.drawText("?", cx + ballSizePx * 0.28f, cy - ballSizePx * 0.3f, paint)
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
        startBlinkAnimation()
        startBreathAnimation()
        startFloatAnimation()
        startHairAnimation()
        startGlowAnimation()
    }

    private fun startBlinkAnimation() {
        blinkAnimator?.cancel()
        blinkAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 150
            startDelay = 3500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                blinkProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startBreathAnimation() {
        breathAnimator?.cancel()
        breathAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 3500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                breathProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startFloatAnimation() {
        floatAnimator?.cancel()
        floatAnimator = ValueAnimator.ofFloat(-ballSizePx * 0.015f, ballSizePx * 0.015f).apply {
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

    private fun startHairAnimation() {
        hairAnimator?.cancel()
        hairAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                hairWaveProgress = it.animatedValue as Float
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
        blinkAnimator?.cancel()
        breathAnimator?.cancel()
        floatAnimator?.cancel()
        hairAnimator?.cancel()
        glowAnimator?.cancel()
        pulseAnimator?.cancel()
    }
}
