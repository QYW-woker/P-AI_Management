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
 * 可爱小人风格AI悬浮球视图
 *
 * 采用可爱的小人形象，支持根据用户心情显示不同表情
 * 设计灵感参考 HelloKite 风格
 */
class FloatingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val BALL_SIZE_DP = 60
        private const val RIPPLE_MAX_SCALE = 1.5f

        // 心情等级 (与日记心情同步)
        const val MOOD_VERY_BAD = 1    // 很差
        const val MOOD_BAD = 2         // 较差
        const val MOOD_NORMAL = 3      // 一般
        const val MOOD_GOOD = 4        // 较好
        const val MOOD_VERY_GOOD = 5   // 很好

        // 状态
        const val STATE_IDLE = 0       // 空闲
        const val STATE_LISTENING = 1  // 监听中
        const val STATE_THINKING = 2   // 思考中
        const val STATE_SUCCESS = 3    // 成功
        const val STATE_ERROR = 4      // 错误
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
    private var bounceOffset = 0f
    private var waveProgress = 0f
    private var sparkleProgress = 0f
    private var headTilt = 0f
    private var armWave = 0f
    private var rippleScale = 1f

    // 画笔
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accessoryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#20000000")
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // 可爱配色方案
    private val colorSkin = Color.parseColor("#FFE4C4")          // 皮肤色
    private val colorSkinShadow = Color.parseColor("#DBC4A4")    // 皮肤阴影
    private val colorHairBrown = Color.parseColor("#5D4037")     // 棕色头发
    private val colorHairBlack = Color.parseColor("#2C2C2C")     // 黑色头发
    private val colorCheek = Color.parseColor("#FFCCCC")         // 腮红
    private val colorEyeWhite = Color.WHITE                      // 眼白
    private val colorEyeBlack = Color.parseColor("#2C3E50")      // 瞳孔
    private val colorMouthHappy = Color.parseColor("#E57373")    // 开心嘴巴
    private val colorMouthSad = Color.parseColor("#90A4AE")      // 难过嘴巴

    // 身体渐变色 - 根据状态变化
    private val colorBodyIdle = Color.parseColor("#7C4DFF")      // 紫色
    private val colorBodyIdleLight = Color.parseColor("#B47CFF")
    private val colorBodyListening = Color.parseColor("#00BCD4") // 青色
    private val colorBodyListeningLight = Color.parseColor("#4DD0E1")
    private val colorBodyThinking = Color.parseColor("#FF9800")  // 橙色
    private val colorBodyThinkingLight = Color.parseColor("#FFB74D")
    private val colorBodySuccess = Color.parseColor("#4CAF50")   // 绿色
    private val colorBodySuccessLight = Color.parseColor("#81C784")
    private val colorBodyError = Color.parseColor("#F44336")     // 红色
    private val colorBodyErrorLight = Color.parseColor("#E57373")

    // 尺寸
    private val ballSizePx: Int
    private val totalSizePx: Int
    private val centerX: Float
    private val centerY: Float

    // 动画器
    private var blinkAnimator: ValueAnimator? = null
    private var breathAnimator: ValueAnimator? = null
    private var bounceAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null
    private var sparkleAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null

    init {
        val density = context.resources.displayMetrics.density
        ballSizePx = (BALL_SIZE_DP * density).toInt()
        totalSizePx = (ballSizePx * RIPPLE_MAX_SCALE * 1.3f).toInt()
        layoutParams = LayoutParams(totalSizePx, totalSizePx)

        centerX = totalSizePx / 2f
        centerY = totalSizePx / 2f + ballSizePx * 0.05f

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

        val breathOffset = sin(breathProgress) * ballSizePx * 0.02f
        val currentCenterY = centerY + bounceOffset + breathOffset

        // 绘制阴影
        drawShadow(canvas, centerX, currentCenterY + ballSizePx * 0.45f)

        // 绘制波纹（录音时）
        if (isListening && rippleScale > 1f) {
            drawRipples(canvas, centerX, currentCenterY)
        }

        // 绘制身体（衣服部分）
        drawBody(canvas, centerX, currentCenterY)

        // 绘制头部
        drawHead(canvas, centerX, currentCenterY)

        // 绘制头发
        drawHair(canvas, centerX, currentCenterY)

        // 绘制脸部特征
        drawFace(canvas, centerX, currentCenterY)

        // 绘制手臂
        drawArms(canvas, centerX, currentCenterY)

        // 绘制状态装饰
        drawStatusDecoration(canvas, centerX, currentCenterY)
    }

    /**
     * 绘制阴影
     */
    private fun drawShadow(canvas: Canvas, cx: Float, cy: Float) {
        val shadowRadiusX = ballSizePx * 0.25f
        val shadowRadiusY = ballSizePx * 0.08f
        canvas.drawOval(
            cx - shadowRadiusX,
            cy - shadowRadiusY,
            cx + shadowRadiusX,
            cy + shadowRadiusY,
            shadowPaint
        )
    }

    /**
     * 绘制波纹
     */
    private fun drawRipples(canvas: Canvas, cx: Float, cy: Float) {
        val baseColor = getBodyColors().first
        for (i in 0..2) {
            val scale = rippleScale + i * 0.12f
            val alpha = ((1f - (scale - 1f) / (RIPPLE_MAX_SCALE - 1f)) * 0.25f * 255).toInt()
            ripplePaint.color = baseColor
            ripplePaint.alpha = alpha.coerceIn(0, 255)
            canvas.drawCircle(cx, cy, ballSizePx * 0.4f * scale, ripplePaint)
        }
    }

    /**
     * 绘制身体（衣服）
     */
    private fun drawBody(canvas: Canvas, cx: Float, cy: Float) {
        val (color1, color2) = getBodyColors()

        val bodyTop = cy + ballSizePx * 0.08f
        val bodyBottom = cy + ballSizePx * 0.4f
        val bodyWidth = ballSizePx * 0.35f

        // 渐变效果
        bodyPaint.shader = LinearGradient(
            cx - bodyWidth, bodyTop,
            cx + bodyWidth, bodyBottom,
            color1, color2,
            Shader.TileMode.CLAMP
        )

        // 身体形状 - 可爱的梯形
        val bodyPath = Path().apply {
            moveTo(cx - bodyWidth * 0.7f, bodyTop)
            lineTo(cx + bodyWidth * 0.7f, bodyTop)
            lineTo(cx + bodyWidth, bodyBottom)
            quadTo(cx, bodyBottom + ballSizePx * 0.05f, cx - bodyWidth, bodyBottom)
            close()
        }

        canvas.drawPath(bodyPath, bodyPaint)
        bodyPaint.shader = null
    }

    /**
     * 绘制头部
     */
    private fun drawHead(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.28f
        val headCenterY = cy - ballSizePx * 0.05f + headTilt * 2

        // 头部阴影
        facePaint.color = colorSkinShadow
        canvas.drawCircle(cx + 2, headCenterY + 2, headRadius, facePaint)

        // 头部主体
        facePaint.color = colorSkin
        canvas.drawCircle(cx, headCenterY, headRadius, facePaint)
    }

    /**
     * 绘制头发
     */
    private fun drawHair(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.28f
        val headCenterY = cy - ballSizePx * 0.05f + headTilt * 2

        accessoryPaint.color = colorHairBrown
        accessoryPaint.style = Paint.Style.FILL

        // 刘海
        val hairPath = Path().apply {
            moveTo(cx - headRadius * 0.9f, headCenterY - headRadius * 0.3f)
            quadTo(cx - headRadius * 0.5f, headCenterY - headRadius * 1.1f,
                   cx, headCenterY - headRadius * 0.85f)
            quadTo(cx + headRadius * 0.5f, headCenterY - headRadius * 1.1f,
                   cx + headRadius * 0.9f, headCenterY - headRadius * 0.3f)
            quadTo(cx + headRadius * 0.7f, headCenterY - headRadius * 0.6f,
                   cx, headCenterY - headRadius * 0.5f)
            quadTo(cx - headRadius * 0.7f, headCenterY - headRadius * 0.6f,
                   cx - headRadius * 0.9f, headCenterY - headRadius * 0.3f)
            close()
        }
        canvas.drawPath(hairPath, accessoryPaint)

        // 小呆毛
        val ahogePath = Path().apply {
            moveTo(cx - headRadius * 0.1f, headCenterY - headRadius * 0.85f)
            quadTo(cx + headRadius * 0.1f, headCenterY - headRadius * 1.3f,
                   cx + headRadius * 0.3f + sin(waveProgress) * headRadius * 0.1f,
                   headCenterY - headRadius * 1.1f)
        }
        accessoryPaint.style = Paint.Style.STROKE
        accessoryPaint.strokeWidth = ballSizePx * 0.04f
        accessoryPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(ahogePath, accessoryPaint)
        accessoryPaint.style = Paint.Style.FILL
    }

    /**
     * 绘制脸部特征
     */
    private fun drawFace(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.28f
        val headCenterY = cy - ballSizePx * 0.05f + headTilt * 2

        // 眼睛位置
        val eyeOffsetX = headRadius * 0.35f
        val eyeOffsetY = headRadius * 0.05f
        val eyeRadius = headRadius * 0.18f

        // 眨眼效果
        val eyeScaleY = 1f - blinkProgress * 0.9f

        for (side in listOf(-1f, 1f)) {
            val eyeCx = cx + eyeOffsetX * side
            val eyeCy = headCenterY + eyeOffsetY

            canvas.save()
            canvas.scale(1f, eyeScaleY, eyeCx, eyeCy)

            when (currentMood) {
                MOOD_VERY_GOOD -> drawSparkleEyes(canvas, eyeCx, eyeCy, eyeRadius, side)
                MOOD_GOOD -> drawHappyEyes(canvas, eyeCx, eyeCy, eyeRadius)
                MOOD_NORMAL -> drawNormalEyes(canvas, eyeCx, eyeCy, eyeRadius, side)
                MOOD_BAD -> drawSadEyes(canvas, eyeCx, eyeCy, eyeRadius)
                MOOD_VERY_BAD -> drawVeryBadEyes(canvas, eyeCx, eyeCy, eyeRadius)
            }

            canvas.restore()
        }

        // 腮红
        drawCheeks(canvas, cx, headCenterY, headRadius)

        // 嘴巴
        drawMouth(canvas, cx, headCenterY, headRadius)
    }

    /**
     * 绘制闪亮眼睛 (很开心)
     */
    private fun drawSparkleEyes(canvas: Canvas, cx: Float, cy: Float, radius: Float, side: Float) {
        // 眼白
        eyePaint.color = colorEyeWhite
        canvas.drawCircle(cx, cy, radius, eyePaint)

        // 大瞳孔
        eyePaint.color = colorEyeBlack
        canvas.drawCircle(cx, cy, radius * 0.7f, eyePaint)

        // 星星高光
        starPaint.color = Color.WHITE
        val starSize = radius * 0.35f
        val sparkleOffset = sin(sparkleProgress) * radius * 0.1f
        drawStar(canvas, cx - radius * 0.2f + sparkleOffset, cy - radius * 0.2f, starSize, 4)
        drawStar(canvas, cx + radius * 0.15f, cy + radius * 0.1f, starSize * 0.5f, 4)
    }

    /**
     * 绘制开心眼睛
     */
    private fun drawHappyEyes(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // 弯弯的眼睛 ^_^
        mouthPaint.color = colorEyeBlack
        mouthPaint.style = Paint.Style.STROKE
        mouthPaint.strokeWidth = radius * 0.4f
        mouthPaint.strokeCap = Paint.Cap.ROUND

        val path = Path().apply {
            moveTo(cx - radius * 0.8f, cy + radius * 0.2f)
            quadTo(cx, cy - radius * 0.6f, cx + radius * 0.8f, cy + radius * 0.2f)
        }
        canvas.drawPath(path, mouthPaint)
    }

    /**
     * 绘制普通眼睛
     */
    private fun drawNormalEyes(canvas: Canvas, cx: Float, cy: Float, radius: Float, side: Float) {
        // 眼白
        eyePaint.color = colorEyeWhite
        canvas.drawCircle(cx, cy, radius, eyePaint)

        // 瞳孔
        eyePaint.color = colorEyeBlack
        val pupilOffsetX = if (isListening) side * radius * 0.15f else 0f
        canvas.drawCircle(cx + pupilOffsetX, cy, radius * 0.6f, eyePaint)

        // 高光
        eyePaint.color = Color.WHITE
        canvas.drawCircle(cx + pupilOffsetX - radius * 0.25f, cy - radius * 0.2f, radius * 0.25f, eyePaint)
        canvas.drawCircle(cx + pupilOffsetX + radius * 0.1f, cy + radius * 0.1f, radius * 0.1f, eyePaint)
    }

    /**
     * 绘制难过眼睛
     */
    private fun drawSadEyes(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // 眼白
        eyePaint.color = colorEyeWhite
        canvas.drawCircle(cx, cy, radius, eyePaint)

        // 瞳孔（略微向下看）
        eyePaint.color = colorEyeBlack
        canvas.drawCircle(cx, cy + radius * 0.15f, radius * 0.55f, eyePaint)

        // 高光
        eyePaint.color = Color.WHITE
        canvas.drawCircle(cx - radius * 0.2f, cy, radius * 0.2f, eyePaint)
    }

    /**
     * 绘制很难过眼睛
     */
    private fun drawVeryBadEyes(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // 横线眼睛 >_<
        mouthPaint.color = colorEyeBlack
        mouthPaint.style = Paint.Style.STROKE
        mouthPaint.strokeWidth = radius * 0.35f
        mouthPaint.strokeCap = Paint.Cap.ROUND

        // X形或者悲伤弧线
        val offset = radius * 0.5f
        canvas.drawLine(cx - offset, cy - offset * 0.5f, cx + offset, cy + offset * 0.3f, mouthPaint)
        canvas.drawLine(cx - offset, cy + offset * 0.3f, cx + offset, cy - offset * 0.5f, mouthPaint)
    }

    /**
     * 绘制腮红
     */
    private fun drawCheeks(canvas: Canvas, cx: Float, headCenterY: Float, headRadius: Float) {
        val blushAlpha = when (currentMood) {
            MOOD_VERY_GOOD -> 180
            MOOD_GOOD -> 150
            MOOD_NORMAL -> 100
            MOOD_BAD -> 50
            else -> 30
        }

        cheekPaint.color = Color.argb(blushAlpha, 255, 182, 193)
        val cheekRadius = headRadius * 0.12f
        val cheekOffsetX = headRadius * 0.55f
        val cheekOffsetY = headRadius * 0.25f

        canvas.drawCircle(cx - cheekOffsetX, headCenterY + cheekOffsetY, cheekRadius, cheekPaint)
        canvas.drawCircle(cx + cheekOffsetX, headCenterY + cheekOffsetY, cheekRadius, cheekPaint)
    }

    /**
     * 绘制嘴巴
     */
    private fun drawMouth(canvas: Canvas, cx: Float, headCenterY: Float, headRadius: Float) {
        val mouthY = headCenterY + headRadius * 0.4f
        mouthPaint.strokeCap = Paint.Cap.ROUND

        when (currentMood) {
            MOOD_VERY_GOOD -> {
                // 大笑 :D
                mouthPaint.color = colorMouthHappy
                mouthPaint.style = Paint.Style.FILL
                val path = Path().apply {
                    moveTo(cx - headRadius * 0.3f, mouthY - headRadius * 0.05f)
                    quadTo(cx, mouthY + headRadius * 0.2f, cx + headRadius * 0.3f, mouthY - headRadius * 0.05f)
                    quadTo(cx, mouthY + headRadius * 0.05f, cx - headRadius * 0.3f, mouthY - headRadius * 0.05f)
                }
                canvas.drawPath(path, mouthPaint)
            }
            MOOD_GOOD -> {
                // 微笑 :)
                mouthPaint.color = colorMouthHappy
                mouthPaint.style = Paint.Style.STROKE
                mouthPaint.strokeWidth = headRadius * 0.06f
                val path = Path().apply {
                    moveTo(cx - headRadius * 0.2f, mouthY)
                    quadTo(cx, mouthY + headRadius * 0.12f, cx + headRadius * 0.2f, mouthY)
                }
                canvas.drawPath(path, mouthPaint)
            }
            MOOD_NORMAL -> {
                // 平静 :-|
                mouthPaint.color = colorMouthHappy
                mouthPaint.style = Paint.Style.STROKE
                mouthPaint.strokeWidth = headRadius * 0.05f
                canvas.drawLine(cx - headRadius * 0.12f, mouthY, cx + headRadius * 0.12f, mouthY, mouthPaint)
            }
            MOOD_BAD -> {
                // 难过 :(
                mouthPaint.color = colorMouthSad
                mouthPaint.style = Paint.Style.STROKE
                mouthPaint.strokeWidth = headRadius * 0.06f
                val path = Path().apply {
                    moveTo(cx - headRadius * 0.18f, mouthY + headRadius * 0.05f)
                    quadTo(cx, mouthY - headRadius * 0.08f, cx + headRadius * 0.18f, mouthY + headRadius * 0.05f)
                }
                canvas.drawPath(path, mouthPaint)
            }
            MOOD_VERY_BAD -> {
                // 很难过 :'(
                mouthPaint.color = colorMouthSad
                mouthPaint.style = Paint.Style.STROKE
                mouthPaint.strokeWidth = headRadius * 0.06f
                val path = Path().apply {
                    moveTo(cx - headRadius * 0.2f, mouthY + headRadius * 0.08f)
                    quadTo(cx, mouthY - headRadius * 0.12f, cx + headRadius * 0.2f, mouthY + headRadius * 0.08f)
                }
                canvas.drawPath(path, mouthPaint)

                // 泪滴
                if (currentState == STATE_IDLE || currentState == STATE_ERROR) {
                    eyePaint.color = Color.parseColor("#64B5F6")
                    canvas.drawCircle(cx - headRadius * 0.5f, headCenterY + headRadius * 0.45f, headRadius * 0.06f, eyePaint)
                }
            }
        }
    }

    /**
     * 绘制手臂
     */
    private fun drawArms(canvas: Canvas, cx: Float, cy: Float) {
        val bodyWidth = ballSizePx * 0.35f
        val armLength = ballSizePx * 0.15f
        val armY = cy + ballSizePx * 0.15f

        accessoryPaint.color = colorSkin
        accessoryPaint.style = Paint.Style.STROKE
        accessoryPaint.strokeWidth = ballSizePx * 0.06f
        accessoryPaint.strokeCap = Paint.Cap.ROUND

        // 左手
        val leftArmAngle = -30f + armWave * 15f
        val leftEndX = cx - bodyWidth * 0.8f - armLength * cos(Math.toRadians(leftArmAngle.toDouble())).toFloat()
        val leftEndY = armY + armLength * sin(Math.toRadians(leftArmAngle.toDouble())).toFloat()
        canvas.drawLine(cx - bodyWidth * 0.7f, armY, leftEndX, leftEndY, accessoryPaint)

        // 右手
        val rightArmAngle = 30f - armWave * 15f
        val rightEndX = cx + bodyWidth * 0.8f + armLength * cos(Math.toRadians(rightArmAngle.toDouble())).toFloat()
        val rightEndY = armY + armLength * sin(Math.toRadians(rightArmAngle.toDouble())).toFloat()
        canvas.drawLine(cx + bodyWidth * 0.7f, armY, rightEndX, rightEndY, accessoryPaint)

        // 手掌
        accessoryPaint.style = Paint.Style.FILL
        canvas.drawCircle(leftEndX, leftEndY, ballSizePx * 0.04f, accessoryPaint)
        canvas.drawCircle(rightEndX, rightEndY, ballSizePx * 0.04f, accessoryPaint)
    }

    /**
     * 绘制状态装饰
     */
    private fun drawStatusDecoration(canvas: Canvas, cx: Float, cy: Float) {
        when (currentState) {
            STATE_LISTENING -> {
                // 音符装饰
                drawMusicNotes(canvas, cx, cy)
            }
            STATE_THINKING -> {
                // 思考泡泡
                drawThinkingBubbles(canvas, cx, cy)
            }
            STATE_SUCCESS -> {
                // 星星
                drawSuccessStars(canvas, cx, cy)
            }
            STATE_ERROR -> {
                // 问号
                drawErrorMark(canvas, cx, cy)
            }
        }
    }

    /**
     * 绘制音符
     */
    private fun drawMusicNotes(canvas: Canvas, cx: Float, cy: Float) {
        accessoryPaint.color = getBodyColors().first
        accessoryPaint.style = Paint.Style.FILL
        accessoryPaint.textSize = ballSizePx * 0.15f

        val offset = sin(waveProgress * 2) * ballSizePx * 0.05f
        canvas.drawText("♪", cx + ballSizePx * 0.35f, cy - ballSizePx * 0.25f + offset, accessoryPaint)
        canvas.drawText("♫", cx - ballSizePx * 0.4f, cy - ballSizePx * 0.2f - offset, accessoryPaint)
    }

    /**
     * 绘制思考泡泡
     */
    private fun drawThinkingBubbles(canvas: Canvas, cx: Float, cy: Float) {
        accessoryPaint.color = Color.WHITE
        accessoryPaint.style = Paint.Style.FILL

        val offset = sin(waveProgress) * ballSizePx * 0.02f
        canvas.drawCircle(cx + ballSizePx * 0.35f, cy - ballSizePx * 0.35f + offset, ballSizePx * 0.04f, accessoryPaint)
        canvas.drawCircle(cx + ballSizePx * 0.42f, cy - ballSizePx * 0.45f + offset, ballSizePx * 0.06f, accessoryPaint)
        canvas.drawCircle(cx + ballSizePx * 0.5f, cy - ballSizePx * 0.55f + offset, ballSizePx * 0.08f, accessoryPaint)

        // 省略号
        accessoryPaint.color = Color.parseColor("#90A4AE")
        accessoryPaint.textSize = ballSizePx * 0.08f
        canvas.drawText("...", cx + ballSizePx * 0.44f, cy - ballSizePx * 0.52f + offset, accessoryPaint)
    }

    /**
     * 绘制成功星星
     */
    private fun drawSuccessStars(canvas: Canvas, cx: Float, cy: Float) {
        starPaint.color = Color.parseColor("#FFD700")
        val starOffset = sin(sparkleProgress) * ballSizePx * 0.03f

        drawStar(canvas, cx - ballSizePx * 0.4f, cy - ballSizePx * 0.35f + starOffset, ballSizePx * 0.08f, 5)
        drawStar(canvas, cx + ballSizePx * 0.38f, cy - ballSizePx * 0.3f - starOffset, ballSizePx * 0.06f, 5)
        drawStar(canvas, cx + ballSizePx * 0.1f, cy - ballSizePx * 0.5f + starOffset * 0.5f, ballSizePx * 0.05f, 5)
    }

    /**
     * 绘制错误标记
     */
    private fun drawErrorMark(canvas: Canvas, cx: Float, cy: Float) {
        accessoryPaint.color = Color.parseColor("#F44336")
        accessoryPaint.style = Paint.Style.FILL
        accessoryPaint.textSize = ballSizePx * 0.2f
        canvas.drawText("?", cx + ballSizePx * 0.35f, cy - ballSizePx * 0.25f, accessoryPaint)
    }

    /**
     * 绘制星星
     */
    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, points: Int) {
        val path = Path()
        val innerRadius = radius * 0.4f

        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) radius else innerRadius
            val angle = Math.PI / points * i - Math.PI / 2
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, starPaint)
    }

    /**
     * 获取身体颜色
     */
    private fun getBodyColors(): Pair<Int, Int> {
        return when (currentState) {
            STATE_LISTENING -> Pair(colorBodyListening, colorBodyListeningLight)
            STATE_THINKING -> Pair(colorBodyThinking, colorBodyThinkingLight)
            STATE_SUCCESS -> Pair(colorBodySuccess, colorBodySuccessLight)
            STATE_ERROR -> Pair(colorBodyError, colorBodyErrorLight)
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

    /**
     * 设置心情 (1-5)
     */
    fun setMood(mood: Int) {
        currentMood = mood.coerceIn(MOOD_VERY_BAD, MOOD_VERY_GOOD)
        invalidate()
    }

    /**
     * 更新音量级别
     */
    fun updateVolumeLevel(level: Float) {
        this.volumeLevel = level.coerceIn(0f, 1f)
        armWave = level
        invalidate()
    }

    /**
     * 显示识别结果
     */
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

    /**
     * 启动空闲动画
     */
    private fun startIdleAnimations() {
        startBlinkAnimation()
        startBreathAnimation()
        startWaveAnimation()
        startSparkleAnimation()
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
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                breathProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                waveProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startSparkleAnimation() {
        sparkleAnimator?.cancel()
        sparkleAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                sparkleProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startListeningAnimations() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, RIPPLE_MAX_SCALE).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                rippleScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        bounceAnimator?.cancel()
        bounceAnimator = ValueAnimator.ofFloat(0f, -ballSizePx * 0.03f, 0f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                bounceOffset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopListeningAnimations() {
        pulseAnimator?.cancel()
        rippleScale = 1f
        bounceAnimator?.cancel()
        bounceOffset = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blinkAnimator?.cancel()
        breathAnimator?.cancel()
        bounceAnimator?.cancel()
        waveAnimator?.cancel()
        sparkleAnimator?.cancel()
        pulseAnimator?.cancel()
    }
}
