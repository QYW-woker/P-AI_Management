package com.lifemanager.app.core.floatingball

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import com.lifemanager.app.MainActivity
import com.lifemanager.app.R
import com.lifemanager.app.core.voice.VoiceRecognitionManager
import com.lifemanager.app.core.voice.VoiceRecognitionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * 悬浮球前台服务
 * 提供全局语音输入功能
 */
@AndroidEntryPoint
class FloatingBallService : Service() {

    @Inject
    lateinit var voiceRecognitionManager: VoiceRecognitionManager

    private lateinit var windowManager: WindowManager
    private var floatingBallView: FloatingBallView? = null
    private var isListening = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_ball_channel"
        private const val CHANNEL_NAME = "语音助手"

        const val ACTION_START = "com.lifemanager.app.FLOATING_BALL_START"
        const val ACTION_STOP = "com.lifemanager.app.FLOATING_BALL_STOP"

        fun start(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        observeVoiceState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                showFloatingBall()
            }
            ACTION_STOP -> {
                hideFloatingBall()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingBall()
        serviceScope.cancel()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "语音助手悬浮球服务"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, FloatingBallService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("语音助手")
            .setContentText("点击悬浮球开始语音输入")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * 显示悬浮球
     */
    private fun showFloatingBall() {
        if (floatingBallView != null) return

        floatingBallView = FloatingBallView(this).apply {
            setOnClickListener {
                toggleVoiceRecognition()
            }
            setOnLongClickListener {
                openApp()
                true
            }
        }

        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            gravity = Gravity.TOP or Gravity.START
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = resources.displayMetrics.widthPixels - 200
            y = resources.displayMetrics.heightPixels / 3
        }

        windowManager.addView(floatingBallView, layoutParams)
        setupDragBehavior(floatingBallView!!, layoutParams)
    }

    /**
     * 隐藏悬浮球
     */
    private fun hideFloatingBall() {
        floatingBallView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // 忽略移除失败
            }
        }
        floatingBallView = null
    }

    /**
     * 设置拖拽行为
     */
    private fun setupDragBehavior(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (dx * dx + dy * dy > 100) {  // 移动超过10像素
                        isDragging = true
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                    } else {
                        // 贴边吸附
                        val screenWidth = resources.displayMetrics.widthPixels
                        val snapToLeft = params.x < screenWidth / 2
                        params.x = if (snapToLeft) 0 else screenWidth - view.width
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 切换语音识别
     */
    private fun toggleVoiceRecognition() {
        if (isListening) {
            voiceRecognitionManager.stopListening()
        } else {
            voiceRecognitionManager.startListening()
        }
    }

    /**
     * 打开APP
     */
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "ai_assistant")
        }
        startActivity(intent)
    }

    /**
     * 监听语音状态
     */
    private fun observeVoiceState() {
        serviceScope.launch {
            voiceRecognitionManager.state.collectLatest { state ->
                isListening = state is VoiceRecognitionState.Listening ||
                        state is VoiceRecognitionState.PartialResult

                floatingBallView?.updateState(
                    isListening = isListening,
                    isProcessing = state is VoiceRecognitionState.Processing,
                    hasError = state is VoiceRecognitionState.Error
                )

                // 识别成功后显示结果
                if (state is VoiceRecognitionState.Result) {
                    floatingBallView?.showResult(state.text)
                }
            }
        }

        // 监听音量
        serviceScope.launch {
            voiceRecognitionManager.volumeLevel.collectLatest { level ->
                floatingBallView?.updateVolumeLevel(level)
            }
        }
    }
}
