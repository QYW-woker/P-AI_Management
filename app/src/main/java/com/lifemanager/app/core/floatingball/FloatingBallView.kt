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
 * 白衣仙女风格AI悬浮球视图
 *
 * 像素级还原可爱的白衣仙女形象
 * 长发飘飘，身着白色汉服，头戴小皇冠
 */
class FloatingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val BALL_SIZE_DP = 72  // 增大尺寸以显示更多细节
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
    private var sleeveWaveProgress = 0f
    private var glowPulse = 0f
    private var rippleScale = 1f

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val robePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 仙女配色 - 像素级还原
    private val colorHairDark = Color.parseColor("#3D3D3D")        // 深色头发
    private val colorHairHighlight = Color.parseColor("#5C5C5C")  // 头发高光
    private val colorSkin = Color.parseColor("#FDF5F0")           // 白皙肌肤
    private val colorSkinShadow = Color.parseColor("#F0E6E0")     // 肌肤阴影
    private val colorCheek = Color.parseColor("#FFDDDD")          // 淡粉腮红
    private val colorEyeBrown = Color.parseColor("#4A3728")       // 棕色眼睛
    private val colorEyeHighlight = Color.parseColor("#FFFFFF")   // 眼睛高光
    private val colorLipPink = Color.parseColor("#E8B4B4")        // 淡粉唇色

    // 汉服配色
    private val colorRobeWhite = Color.parseColor("#FAFAFA")      // 白色外袍
    private val colorRobeInner = Color.parseColor("#F5F5F5")      // 内衬
    private val colorRobeShadow = Color.parseColor("#E8E8E8")     // 袍子阴影
    private val colorBeltGray = Color.parseColor("#9E9E9E")       // 灰色腰带
    private val colorBeltTassel = Color.parseColor("#D4AF37")     // 金色流苏
    private val colorTiara = Color.parseColor("#E8E8E8")          // 银色头冠
    private val colorTiaraGem = Color.parseColor("#FFE4E1")       // 粉色宝石

    // 光晕效果
    private val colorGlowInner = Color.parseColor("#FFFFFF")
    private val colorGlowOuter = Color.parseColor("#E8F4FF")

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
    private var sleeveAnimator: ValueAnimator? = null
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
        val breathScale = 1f + sin(breathProgress) * 0.015f

        canvas.save()
        canvas.scale(breathScale, breathScale, centerX, floatY)

        // 1. 绘制外层光晕
        drawOuterGlow(canvas, centerX, floatY)

        // 2. 绘制波纹（录音时）
        if (isListening && rippleScale > 1f) {
            drawRipples(canvas, centerX, floatY)
        }

        // 3. 绘制内层光晕
        drawInnerGlow(canvas, centerX, floatY)

        // 4. 绘制头发（后层）
        drawHairBack(canvas, centerX, floatY)

        // 5. 绘制身体和汉服
        drawRobe(canvas, centerX, floatY)

        // 6. 绘制脖子
        drawNeck(canvas, centerX, floatY)

        // 7. 绘制头部
        drawHead(canvas, centerX, floatY)

        // 8. 绘制头发（前层）
        drawHairFront(canvas, centerX, floatY)

        // 9. 绘制脸部
        drawFace(canvas, centerX, floatY)

        // 10. 绘制头冠
        drawTiara(canvas, centerX, floatY)

        // 11. 绘制飘动的衣袖
        drawSleeves(canvas, centerX, floatY)

        // 12. 绘制状态装饰
        drawStatusDecoration(canvas, centerX, floatY)

        canvas.restore()
    }

    /**
     * 绘制外层光晕
     */
    private fun drawOuterGlow(canvas: Canvas, cx: Float, cy: Float) {
        val glowRadius = ballSizePx * 0.55f
        val pulseRadius = glowRadius * (1f + glowPulse * 0.1f)

        glowPaint.shader = RadialGradient(
            cx, cy, pulseRadius,
            intArrayOf(
                Color.argb(60, 255, 255, 255),
                Color.argb(30, 232, 244, 255),
                Color.argb(0, 232, 244, 255)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, pulseRadius, glowPaint)
        glowPaint.shader = null
    }

    /**
     * 绘制内层光晕
     */
    private fun drawInnerGlow(canvas: Canvas, cx: Float, cy: Float) {
        val glowRadius = ballSizePx * 0.4f

        glowPaint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(
                Color.argb(100, 255, 255, 255),
                Color.argb(50, 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)
        glowPaint.shader = null
    }

    /**
     * 绘制波纹
     */
    private fun drawRipples(canvas: Canvas, cx: Float, cy: Float) {
        for (i in 0..2) {
            val scale = rippleScale + i * 0.1f
            val alpha = ((1f - (scale - 1f) / (RIPPLE_MAX_SCALE - 1f)) * 0.2f * 255).toInt()
            glowPaint.color = Color.argb(alpha.coerceIn(0, 255), 200, 230, 255)
            canvas.drawCircle(cx, cy, ballSizePx * 0.4f * scale, glowPaint)
        }
    }

    /**
     * 绘制后层头发
     */
    private fun drawHairBack(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.22f
        val headCenterY = cy - ballSizePx * 0.12f

        hairPaint.color = colorHairDark
        hairPaint.style = Paint.Style.FILL

        // 左侧长发
        val leftHairPath = Path().apply {
            val waveOffset = sin(hairWaveProgress) * ballSizePx * 0.02f

            moveTo(cx - headRadius * 0.8f, headCenterY - headRadius * 0.3f)
            quadTo(
                cx - headRadius * 1.3f + waveOffset, headCenterY + ballSizePx * 0.1f,
                cx - headRadius * 1.1f + waveOffset * 0.5f, headCenterY + ballSizePx * 0.35f
            )
            quadTo(
                cx - headRadius * 0.9f + waveOffset, headCenterY + ballSizePx * 0.45f,
                cx - headRadius * 0.7f, headCenterY + ballSizePx * 0.5f
            )
            lineTo(cx - headRadius * 0.5f, headCenterY + ballSizePx * 0.15f)
            close()
        }
        canvas.drawPath(leftHairPath, hairPaint)

        // 右侧长发
        val rightHairPath = Path().apply {
            val waveOffset = sin(hairWaveProgress + Math.PI.toFloat()) * ballSizePx * 0.02f

            moveTo(cx + headRadius * 0.8f, headCenterY - headRadius * 0.3f)
            quadTo(
                cx + headRadius * 1.3f + waveOffset, headCenterY + ballSizePx * 0.1f,
                cx + headRadius * 1.1f + waveOffset * 0.5f, headCenterY + ballSizePx * 0.35f
            )
            quadTo(
                cx + headRadius * 0.9f + waveOffset, headCenterY + ballSizePx * 0.45f,
                cx + headRadius * 0.7f, headCenterY + ballSizePx * 0.5f
            )
            lineTo(cx + headRadius * 0.5f, headCenterY + ballSizePx * 0.15f)
            close()
        }
        canvas.drawPath(rightHairPath, hairPaint)
    }

    /**
     * 绘制汉服袍子
     */
    private fun drawRobe(canvas: Canvas, cx: Float, cy: Float) {
        val robeTop = cy + ballSizePx * 0.02f
        val robeBottom = cy + ballSizePx * 0.42f
        val robeWidth = ballSizePx * 0.28f

        // 外袍 - 白色
        robePaint.color = colorRobeWhite
        robePaint.style = Paint.Style.FILL

        val robePath = Path().apply {
            moveTo(cx - robeWidth * 0.5f, robeTop)
            lineTo(cx + robeWidth * 0.5f, robeTop)
            // 右侧
            quadTo(cx + robeWidth * 0.8f, robeTop + ballSizePx * 0.1f,
                   cx + robeWidth * 1.0f, robeBottom)
            // 底部
            quadTo(cx, robeBottom + ballSizePx * 0.03f,
                   cx - robeWidth * 1.0f, robeBottom)
            // 左侧
            quadTo(cx - robeWidth * 0.8f, robeTop + ballSizePx * 0.1f,
                   cx - robeWidth * 0.5f, robeTop)
            close()
        }
        canvas.drawPath(robePath, robePaint)

        // 内衬 - V领设计
        robePaint.color = colorRobeInner
        val innerPath = Path().apply {
            moveTo(cx - robeWidth * 0.2f, robeTop)
            lineTo(cx, robeTop + ballSizePx * 0.08f)
            lineTo(cx + robeWidth * 0.2f, robeTop)
            lineTo(cx + robeWidth * 0.15f, robeTop + ballSizePx * 0.02f)
            lineTo(cx, robeTop + ballSizePx * 0.1f)
            lineTo(cx - robeWidth * 0.15f, robeTop + ballSizePx * 0.02f)
            close()
        }
        canvas.drawPath(innerPath, robePaint)

        // 腰带
        robePaint.color = colorBeltGray
        val beltY = robeTop + ballSizePx * 0.12f
        val beltRect = RectF(
            cx - robeWidth * 0.55f, beltY,
            cx + robeWidth * 0.55f, beltY + ballSizePx * 0.035f
        )
        canvas.drawRoundRect(beltRect, ballSizePx * 0.01f, ballSizePx * 0.01f, robePaint)

        // 腰带结
        robePaint.color = colorRobeWhite
        canvas.drawCircle(cx, beltY + ballSizePx * 0.017f, ballSizePx * 0.025f, robePaint)

        // 流苏
        robePaint.color = colorBeltTassel
        robePaint.strokeWidth = ballSizePx * 0.008f
        robePaint.style = Paint.Style.STROKE
        val tasselOffset = sin(hairWaveProgress) * ballSizePx * 0.01f
        canvas.drawLine(cx, beltY + ballSizePx * 0.04f,
                       cx + tasselOffset, beltY + ballSizePx * 0.1f, robePaint)
        canvas.drawLine(cx - ballSizePx * 0.01f, beltY + ballSizePx * 0.04f,
                       cx - ballSizePx * 0.01f + tasselOffset * 0.8f, beltY + ballSizePx * 0.09f, robePaint)
        robePaint.style = Paint.Style.FILL
    }

    /**
     * 绘制脖子
     */
    private fun drawNeck(canvas: Canvas, cx: Float, cy: Float) {
        skinPaint.color = colorSkin
        skinPaint.style = Paint.Style.FILL

        val neckWidth = ballSizePx * 0.06f
        val neckTop = cy - ballSizePx * 0.02f
        val neckBottom = cy + ballSizePx * 0.04f

        canvas.drawRect(cx - neckWidth, neckTop, cx + neckWidth, neckBottom, skinPaint)
    }

    /**
     * 绘制头部
     */
    private fun drawHead(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.22f
        val headCenterY = cy - ballSizePx * 0.12f

        // 头部阴影
        skinPaint.color = colorSkinShadow
        canvas.drawCircle(cx + 1, headCenterY + 1, headRadius, skinPaint)

        // 头部主体 - 稍微椭圆形
        skinPaint.color = colorSkin
        val headRect = RectF(
            cx - headRadius, headCenterY - headRadius * 1.05f,
            cx + headRadius, headCenterY + headRadius * 0.95f
        )
        canvas.drawOval(headRect, skinPaint)
    }

    /**
     * 绘制前层头发
     */
    private fun drawHairFront(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.22f
        val headCenterY = cy - ballSizePx * 0.12f

        hairPaint.color = colorHairDark
        hairPaint.style = Paint.Style.FILL

        // 刘海 - 中分造型
        val bangsPath = Path().apply {
            // 左侧刘海
            moveTo(cx - headRadius * 0.05f, headCenterY - headRadius * 0.6f)
            quadTo(cx - headRadius * 0.5f, headCenterY - headRadius * 0.9f,
                   cx - headRadius * 0.9f, headCenterY - headRadius * 0.4f)
            quadTo(cx - headRadius * 0.95f, headCenterY - headRadius * 0.1f,
                   cx - headRadius * 0.85f, headCenterY + headRadius * 0.1f)
            lineTo(cx - headRadius * 0.7f, headCenterY - headRadius * 0.1f)
            quadTo(cx - headRadius * 0.4f, headCenterY - headRadius * 0.5f,
                   cx - headRadius * 0.05f, headCenterY - headRadius * 0.6f)
            close()
        }
        canvas.drawPath(bangsPath, hairPaint)

        // 右侧刘海
        val bangsPathRight = Path().apply {
            moveTo(cx + headRadius * 0.05f, headCenterY - headRadius * 0.6f)
            quadTo(cx + headRadius * 0.5f, headCenterY - headRadius * 0.9f,
                   cx + headRadius * 0.9f, headCenterY - headRadius * 0.4f)
            quadTo(cx + headRadius * 0.95f, headCenterY - headRadius * 0.1f,
                   cx + headRadius * 0.85f, headCenterY + headRadius * 0.1f)
            lineTo(cx + headRadius * 0.7f, headCenterY - headRadius * 0.1f)
            quadTo(cx + headRadius * 0.4f, headCenterY - headRadius * 0.5f,
                   cx + headRadius * 0.05f, headCenterY - headRadius * 0.6f)
            close()
        }
        canvas.drawPath(bangsPathRight, hairPaint)

        // 头顶发型
        val topHairPath = Path().apply {
            moveTo(cx - headRadius * 0.3f, headCenterY - headRadius * 0.95f)
            quadTo(cx, headCenterY - headRadius * 1.15f,
                   cx + headRadius * 0.3f, headCenterY - headRadius * 0.95f)
            quadTo(cx, headCenterY - headRadius * 0.85f,
                   cx - headRadius * 0.3f, headCenterY - headRadius * 0.95f)
            close()
        }
        canvas.drawPath(topHairPath, hairPaint)

        // 头发高光
        hairPaint.color = colorHairHighlight
        hairPaint.style = Paint.Style.STROKE
        hairPaint.strokeWidth = ballSizePx * 0.01f
        val highlightPath = Path().apply {
            moveTo(cx - headRadius * 0.4f, headCenterY - headRadius * 0.7f)
            quadTo(cx - headRadius * 0.2f, headCenterY - headRadius * 0.85f,
                   cx, headCenterY - headRadius * 0.75f)
        }
        canvas.drawPath(highlightPath, hairPaint)
        hairPaint.style = Paint.Style.FILL
    }

    /**
     * 绘制脸部特征
     */
    private fun drawFace(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.22f
        val headCenterY = cy - ballSizePx * 0.12f

        // 眼睛参数
        val eyeOffsetX = headRadius * 0.38f
        val eyeOffsetY = headRadius * 0.05f
        val eyeWidth = headRadius * 0.22f
        val eyeHeight = headRadius * 0.28f

        // 眨眼效果
        val eyeScaleY = 1f - blinkProgress * 0.85f

        // 绘制双眼
        for (side in listOf(-1f, 1f)) {
            val eyeCx = cx + eyeOffsetX * side
            val eyeCy = headCenterY + eyeOffsetY

            canvas.save()
            canvas.scale(1f, eyeScaleY, eyeCx, eyeCy)

            // 眼白
            eyePaint.color = Color.WHITE
            val eyeRect = RectF(
                eyeCx - eyeWidth, eyeCy - eyeHeight,
                eyeCx + eyeWidth, eyeCy + eyeHeight
            )
            canvas.drawOval(eyeRect, eyePaint)

            // 瞳孔 - 大而圆润的棕色眼睛
            eyePaint.color = colorEyeBrown
            val pupilRadius = eyeWidth * 0.7f
            canvas.drawCircle(eyeCx, eyeCy + eyeHeight * 0.1f, pupilRadius, eyePaint)

            // 瞳孔中心（深色）
            eyePaint.color = Color.parseColor("#2D1F14")
            canvas.drawCircle(eyeCx, eyeCy + eyeHeight * 0.1f, pupilRadius * 0.5f, eyePaint)

            // 大高光
            eyePaint.color = colorEyeHighlight
            canvas.drawCircle(eyeCx - eyeWidth * 0.25f, eyeCy - eyeHeight * 0.15f,
                            pupilRadius * 0.35f, eyePaint)
            // 小高光
            canvas.drawCircle(eyeCx + eyeWidth * 0.2f, eyeCy + eyeHeight * 0.2f,
                            pupilRadius * 0.15f, eyePaint)

            canvas.restore()
        }

        // 腮红
        eyePaint.color = Color.argb(50, 255, 180, 180)
        val cheekRadius = headRadius * 0.1f
        canvas.drawCircle(cx - headRadius * 0.5f, headCenterY + headRadius * 0.35f, cheekRadius, eyePaint)
        canvas.drawCircle(cx + headRadius * 0.5f, headCenterY + headRadius * 0.35f, cheekRadius, eyePaint)

        // 嘴巴 - 小巧的粉色嘴唇
        drawMouth(canvas, cx, headCenterY, headRadius)
    }

    /**
     * 绘制嘴巴
     */
    private fun drawMouth(canvas: Canvas, cx: Float, headCenterY: Float, headRadius: Float) {
        val mouthY = headCenterY + headRadius * 0.5f

        paint.color = colorLipPink
        paint.style = Paint.Style.FILL

        when (currentMood) {
            MOOD_VERY_GOOD -> {
                // 开心微笑
                val smilePath = Path().apply {
                    moveTo(cx - headRadius * 0.12f, mouthY)
                    quadTo(cx, mouthY + headRadius * 0.1f, cx + headRadius * 0.12f, mouthY)
                    quadTo(cx, mouthY + headRadius * 0.05f, cx - headRadius * 0.12f, mouthY)
                }
                canvas.drawPath(smilePath, paint)
            }
            MOOD_GOOD -> {
                // 温柔微笑
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = headRadius * 0.04f
                paint.strokeCap = Paint.Cap.ROUND
                val smilePath = Path().apply {
                    moveTo(cx - headRadius * 0.08f, mouthY)
                    quadTo(cx, mouthY + headRadius * 0.06f, cx + headRadius * 0.08f, mouthY)
                }
                canvas.drawPath(smilePath, paint)
            }
            MOOD_NORMAL -> {
                // 平静 - 小巧嘴唇
                canvas.drawOval(
                    cx - headRadius * 0.05f, mouthY - headRadius * 0.02f,
                    cx + headRadius * 0.05f, mouthY + headRadius * 0.02f,
                    paint
                )
            }
            MOOD_BAD -> {
                // 担忧
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = headRadius * 0.04f
                val sadPath = Path().apply {
                    moveTo(cx - headRadius * 0.06f, mouthY + headRadius * 0.02f)
                    quadTo(cx, mouthY - headRadius * 0.03f, cx + headRadius * 0.06f, mouthY + headRadius * 0.02f)
                }
                canvas.drawPath(sadPath, paint)
            }
            MOOD_VERY_BAD -> {
                // 难过
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = headRadius * 0.04f
                val sadPath = Path().apply {
                    moveTo(cx - headRadius * 0.08f, mouthY + headRadius * 0.04f)
                    quadTo(cx, mouthY - headRadius * 0.05f, cx + headRadius * 0.08f, mouthY + headRadius * 0.04f)
                }
                canvas.drawPath(sadPath, paint)
            }
        }
        paint.style = Paint.Style.FILL
    }

    /**
     * 绘制头冠
     */
    private fun drawTiara(canvas: Canvas, cx: Float, cy: Float) {
        val headRadius = ballSizePx * 0.22f
        val headCenterY = cy - ballSizePx * 0.12f
        val tiaraY = headCenterY - headRadius * 0.85f

        paint.color = colorTiara
        paint.style = Paint.Style.FILL

        // 头冠主体
        val tiaraPath = Path().apply {
            moveTo(cx - headRadius * 0.3f, tiaraY + headRadius * 0.1f)
            lineTo(cx - headRadius * 0.2f, tiaraY - headRadius * 0.15f)
            lineTo(cx - headRadius * 0.08f, tiaraY)
            lineTo(cx, tiaraY - headRadius * 0.25f)  // 中间尖顶
            lineTo(cx + headRadius * 0.08f, tiaraY)
            lineTo(cx + headRadius * 0.2f, tiaraY - headRadius * 0.15f)
            lineTo(cx + headRadius * 0.3f, tiaraY + headRadius * 0.1f)
            close()
        }
        canvas.drawPath(tiaraPath, paint)

        // 中心宝石
        paint.color = colorTiaraGem
        canvas.drawCircle(cx, tiaraY - headRadius * 0.1f, headRadius * 0.05f, paint)

        // 宝石高光
        paint.color = Color.WHITE
        canvas.drawCircle(cx - headRadius * 0.015f, tiaraY - headRadius * 0.115f, headRadius * 0.02f, paint)
    }

    /**
     * 绘制飘动的衣袖
     */
    private fun drawSleeves(canvas: Canvas, cx: Float, cy: Float) {
        val robeTop = cy + ballSizePx * 0.05f
        val sleeveWave = sin(sleeveWaveProgress) * ballSizePx * 0.02f

        robePaint.color = colorRobeWhite
        robePaint.style = Paint.Style.FILL

        // 左袖
        val leftSleevePath = Path().apply {
            moveTo(cx - ballSizePx * 0.2f, robeTop)
            quadTo(cx - ballSizePx * 0.35f + sleeveWave, robeTop + ballSizePx * 0.05f,
                   cx - ballSizePx * 0.4f + sleeveWave * 1.5f, robeTop + ballSizePx * 0.15f)
            quadTo(cx - ballSizePx * 0.38f + sleeveWave, robeTop + ballSizePx * 0.2f,
                   cx - ballSizePx * 0.32f, robeTop + ballSizePx * 0.18f)
            lineTo(cx - ballSizePx * 0.22f, robeTop + ballSizePx * 0.08f)
            close()
        }
        canvas.drawPath(leftSleevePath, robePaint)

        // 右袖
        val rightSleevePath = Path().apply {
            moveTo(cx + ballSizePx * 0.2f, robeTop)
            quadTo(cx + ballSizePx * 0.35f - sleeveWave, robeTop + ballSizePx * 0.05f,
                   cx + ballSizePx * 0.4f - sleeveWave * 1.5f, robeTop + ballSizePx * 0.15f)
            quadTo(cx + ballSizePx * 0.38f - sleeveWave, robeTop + ballSizePx * 0.2f,
                   cx + ballSizePx * 0.32f, robeTop + ballSizePx * 0.18f)
            lineTo(cx + ballSizePx * 0.22f, robeTop + ballSizePx * 0.08f)
            close()
        }
        canvas.drawPath(rightSleevePath, robePaint)

        // 小手 - 肤色
        skinPaint.color = colorSkin
        canvas.drawCircle(cx - ballSizePx * 0.38f + sleeveWave * 1.5f, robeTop + ballSizePx * 0.16f,
                         ballSizePx * 0.025f, skinPaint)
        canvas.drawCircle(cx + ballSizePx * 0.38f - sleeveWave * 1.5f, robeTop + ballSizePx * 0.16f,
                         ballSizePx * 0.025f, skinPaint)
    }

    /**
     * 绘制状态装饰
     */
    private fun drawStatusDecoration(canvas: Canvas, cx: Float, cy: Float) {
        when (currentState) {
            STATE_LISTENING -> drawListeningEffect(canvas, cx, cy)
            STATE_THINKING -> drawThinkingEffect(canvas, cx, cy)
            STATE_SUCCESS -> drawSuccessEffect(canvas, cx, cy)
            STATE_ERROR -> drawErrorEffect(canvas, cx, cy)
        }
    }

    private fun drawListeningEffect(canvas: Canvas, cx: Float, cy: Float) {
        // 音符飘动
        paint.color = Color.parseColor("#90CAF9")
        paint.textSize = ballSizePx * 0.1f
        val offset = sin(hairWaveProgress * 2) * ballSizePx * 0.03f
        canvas.drawText("♪", cx + ballSizePx * 0.35f, cy - ballSizePx * 0.25f + offset, paint)
        canvas.drawText("♫", cx - ballSizePx * 0.38f, cy - ballSizePx * 0.2f - offset, paint)
    }

    private fun drawThinkingEffect(canvas: Canvas, cx: Float, cy: Float) {
        // 思考泡泡
        paint.color = Color.WHITE
        val offset = sin(hairWaveProgress) * ballSizePx * 0.01f
        canvas.drawCircle(cx + ballSizePx * 0.32f, cy - ballSizePx * 0.32f + offset, ballSizePx * 0.025f, paint)
        canvas.drawCircle(cx + ballSizePx * 0.38f, cy - ballSizePx * 0.4f + offset, ballSizePx * 0.035f, paint)
        canvas.drawCircle(cx + ballSizePx * 0.45f, cy - ballSizePx * 0.5f + offset, ballSizePx * 0.05f, paint)

        paint.color = Color.parseColor("#90A4AE")
        paint.textSize = ballSizePx * 0.05f
        canvas.drawText("...", cx + ballSizePx * 0.42f, cy - ballSizePx * 0.48f + offset, paint)
    }

    private fun drawSuccessEffect(canvas: Canvas, cx: Float, cy: Float) {
        // 闪烁星星
        paint.color = Color.parseColor("#FFD700")
        val offset = sin(glowPulse * 4) * ballSizePx * 0.02f
        drawStar(canvas, cx - ballSizePx * 0.35f, cy - ballSizePx * 0.35f + offset, ballSizePx * 0.05f)
        drawStar(canvas, cx + ballSizePx * 0.38f, cy - ballSizePx * 0.3f - offset, ballSizePx * 0.04f)
        drawStar(canvas, cx + ballSizePx * 0.08f, cy - ballSizePx * 0.48f + offset * 0.5f, ballSizePx * 0.035f)
    }

    private fun drawErrorEffect(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = Color.parseColor("#FFCDD2")
        paint.textSize = ballSizePx * 0.12f
        canvas.drawText("?", cx + ballSizePx * 0.32f, cy - ballSizePx * 0.28f, paint)
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
        startSleeveAnimation()
        startGlowAnimation()
    }

    private fun startBlinkAnimation() {
        blinkAnimator?.cancel()
        blinkAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 180
            startDelay = 4000
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
            duration = 4000
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
        floatAnimator = ValueAnimator.ofFloat(-ballSizePx * 0.02f, ballSizePx * 0.02f).apply {
            duration = 2500
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
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                hairWaveProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startSleeveAnimation() {
        sleeveAnimator?.cancel()
        sleeveAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                sleeveWaveProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startGlowAnimation() {
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 3500
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
        sleeveAnimator?.cancel()
        glowAnimator?.cancel()
        pulseAnimator?.cancel()
    }
}
