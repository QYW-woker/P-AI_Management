package com.lifemanager.app.core.notification

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.lifemanager.app.core.ai.model.PaymentInfo
import com.lifemanager.app.data.repository.AIConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 支付通知监听服务
 * 监听微信、支付宝等支付通知，自动解析记账
 */
@AndroidEntryPoint
class PaymentNotificationService : NotificationListenerService() {

    @Inject
    lateinit var notificationParser: PaymentNotificationParser

    @Inject
    lateinit var aiConfigRepository: AIConfigRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "PaymentNotification"

        // 支持的支付APP包名
        private val SUPPORTED_PACKAGES = setOf(
            "com.tencent.mm",               // 微信
            "com.eg.android.AlipayGphone",  // 支付宝
            "com.unionpay",                 // 云闪付
            "com.icbc.mobile",              // 工商银行
            "com.chinamworld.main",         // 建设银行
            "com.android.bankabc",          // 农业银行
            "com.chinaonlinepayment.boc",   // 中国银行
            "cmb.pb",                       // 招商银行
            "com.bankcomm.Bankcomm"         // 交通银行
        )

        // 支付相关关键词
        private val PAYMENT_KEYWORDS = listOf(
            "支付成功", "付款成功", "收款成功", "到账",
            "消费", "支出", "收入", "转账", "红包",
            "扣款", "交易成功", "支付通知"
        )

        // 用于广播解析结果
        private val _paymentEvents = MutableSharedFlow<PaymentInfo>(extraBufferCapacity = 10)
        val paymentEvents: SharedFlow<PaymentInfo> = _paymentEvents
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PaymentNotificationService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "PaymentNotificationService destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 检查功能是否启用
        serviceScope.launch {
            try {
                val featureConfig = aiConfigRepository.getFeatureConfigFlow().first()
                if (!featureConfig.notificationListenerEnabled) {
                    return@launch
                }

                processNotification(sbn)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 通知被移除时不做处理
    }

    /**
     * 处理通知
     */
    private suspend fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // 检查是否是支持的支付APP
        if (packageName !in SUPPORTED_PACKAGES) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 获取通知内容
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullText = buildString {
            if (title.isNotEmpty()) append("$title ")
            if (text.isNotEmpty()) append(text)
            if (bigText.isNotEmpty() && bigText != text) append(" $bigText")
        }

        if (fullText.isBlank()) {
            return
        }

        Log.d(TAG, "Notification from $packageName: $fullText")

        // 检查是否包含支付关键词
        if (!containsPaymentKeyword(fullText)) {
            return
        }

        // 解析支付信息
        val parseResult = notificationParser.parseNotification(
            packageName = packageName,
            title = title,
            content = text,
            bigText = bigText
        )

        parseResult.onSuccess { paymentInfo ->
            Log.d(TAG, "Parsed payment: $paymentInfo")
            _paymentEvents.emit(paymentInfo)
        }.onFailure { error ->
            Log.w(TAG, "Failed to parse payment: ${error.message}")
        }
    }

    /**
     * 检查是否包含支付关键词
     */
    private fun containsPaymentKeyword(text: String): Boolean {
        return PAYMENT_KEYWORDS.any { text.contains(it) }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }
}
