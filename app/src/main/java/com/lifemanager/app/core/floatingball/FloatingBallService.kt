package com.lifemanager.app.core.floatingball

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.lifemanager.app.MainActivity
import com.lifemanager.app.R
import com.lifemanager.app.core.ai.model.CommandIntent
import com.lifemanager.app.core.ai.model.ExecutionResult
import com.lifemanager.app.core.voice.VoiceCommandExecutor
import com.lifemanager.app.core.voice.VoiceCommandProcessor
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

    @Inject
    lateinit var voiceCommandProcessor: VoiceCommandProcessor

    @Inject
    lateinit var voiceCommandExecutor: VoiceCommandExecutor

    private lateinit var windowManager: WindowManager
    private var floatingBallView: FloatingBallView? = null
    private var confirmationView: FloatingConfirmationView? = null
    private var isListening = false
    private var pendingIntent: CommandIntent? = null
    private var pendingText: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // WakeLock 保持服务运行
    private var wakeLock: PowerManager.WakeLock? = null

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
        acquireWakeLock()
        observeVoiceState()
    }

    /**
     * 获取WakeLock保持服务运行
     */
    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "LifeManager:FloatingBallWakeLock"
            )
            wakeLock?.setReferenceCounted(false)
        }
        wakeLock?.acquire(10 * 60 * 1000L) // 10分钟后自动释放，服务会定期续期
    }

    /**
     * 释放WakeLock
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                acquireWakeLock() // 续期WakeLock
                showFloatingBall()
            }
            ACTION_STOP -> {
                hideFloatingBall()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 当任务被移除时（用户从最近任务中滑掉app），重启服务
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 设置闹钟在1秒后重启服务
        val restartIntent = Intent(this, FloatingBallService::class.java).apply {
            action = ACTION_START
        }
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            restartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000,
            pendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        hideConfirmationDialog()
        hideFloatingBall()
        releaseWakeLock()
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

        // 计算悬浮球尺寸 (56dp * 1.5 波纹空间)
        val density = resources.displayMetrics.density
        val ballSizePx = (56 * 1.5f * density).toInt()

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
            // 使用固定尺寸确保悬浮球可见
            width = ballSizePx
            height = ballSizePx
            // 初始位置：屏幕右侧中间
            x = resources.displayMetrics.widthPixels - ballSizePx - 20
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

                // 识别成功后处理结果
                if (state is VoiceRecognitionState.Result) {
                    floatingBallView?.showResult(state.text)
                    processVoiceResult(state.text)
                }

                // 识别错误时显示Toast
                if (state is VoiceRecognitionState.Error) {
                    showToast("语音识别失败: ${state.message}")
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

    /**
     * 处理语音识别结果
     */
    private fun processVoiceResult(text: String) {
        serviceScope.launch {
            try {
                // 使用AI解析语音命令
                val parseResult = voiceCommandProcessor.process(text)

                parseResult.fold(
                    onSuccess = { intent ->
                        pendingIntent = intent
                        pendingText = text
                        showConfirmationDialog(text, intent)
                    },
                    onFailure = { e ->
                        showToast("命令解析失败: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                showToast("处理失败: ${e.message}")
            }
        }
    }

    /**
     * 显示确认弹窗
     */
    private fun showConfirmationDialog(originalText: String, intent: CommandIntent) {
        mainHandler.post {
            hideConfirmationDialog()

            confirmationView = FloatingConfirmationView(this).apply {
                updateContent(originalText, intent)
                setOnConfirmListener {
                    executeCommand()
                }
                setOnCancelListener {
                    hideConfirmationDialog()
                    voiceRecognitionManager.resetState()
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
                flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
            }

            try {
                windowManager.addView(confirmationView, layoutParams)
            } catch (e: Exception) {
                showToast("无法显示确认窗口")
            }
        }
    }

    /**
     * 隐藏确认弹窗
     */
    private fun hideConfirmationDialog() {
        confirmationView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // 忽略
            }
        }
        confirmationView = null
    }

    /**
     * 执行命令
     */
    private fun executeCommand() {
        val intent = pendingIntent ?: return
        hideConfirmationDialog()

        serviceScope.launch {
            try {
                val result = voiceCommandExecutor.execute(intent)

                mainHandler.post {
                    when (result) {
                        is ExecutionResult.Success -> {
                            showToast("✅ ${result.message}")
                        }
                        is ExecutionResult.Failure -> {
                            showToast("❌ ${result.message}")
                        }
                        is ExecutionResult.NeedMoreInfo -> {
                            showToast("⚠️ ${result.prompt}")
                        }
                        is ExecutionResult.NeedConfirmation -> {
                            showToast("ℹ️ ${result.previewMessage}")
                        }
                        is ExecutionResult.NotRecognized -> {
                            showToast("❓ 无法识别: ${result.originalText}")
                        }
                    }
                }

                voiceRecognitionManager.resetState()
            } catch (e: Exception) {
                mainHandler.post {
                    showToast("执行失败: ${e.message}")
                }
            }
        }

        pendingIntent = null
        pendingText = ""
    }

    /**
     * 显示Toast
     */
    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
