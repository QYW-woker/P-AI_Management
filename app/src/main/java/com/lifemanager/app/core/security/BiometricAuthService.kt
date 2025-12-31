package com.lifemanager.app.core.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生物识别认证服务
 *
 * 支持指纹、面部识别等生物识别方式
 */
@Singleton
class BiometricAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val biometricManager = BiometricManager.from(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: Flow<AuthState> = _authState.asStateFlow()

    companion object {
        // 认证类型
        const val AUTH_TYPE_BIOMETRIC = BiometricManager.Authenticators.BIOMETRIC_STRONG
        const val AUTH_TYPE_WEAK = BiometricManager.Authenticators.BIOMETRIC_WEAK
        const val AUTH_TYPE_CREDENTIAL = BiometricManager.Authenticators.DEVICE_CREDENTIAL
        const val AUTH_TYPE_ALL = AUTH_TYPE_BIOMETRIC or AUTH_TYPE_CREDENTIAL
    }

    /**
     * 检查生物识别是否可用
     */
    fun checkBiometricAvailability(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(AUTH_TYPE_BIOMETRIC)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.Unsupported
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.Unknown
            else -> BiometricAvailability.Unknown
        }
    }

    /**
     * 检查设备凭据是否可用
     */
    fun checkDeviceCredentialAvailability(): Boolean {
        return biometricManager.canAuthenticate(AUTH_TYPE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * 获取支持的认证方式
     */
    fun getSupportedAuthTypes(): List<AuthType> {
        val types = mutableListOf<AuthType>()

        if (biometricManager.canAuthenticate(AUTH_TYPE_BIOMETRIC) == BiometricManager.BIOMETRIC_SUCCESS) {
            types.add(AuthType.Fingerprint)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ 可能支持面部识别
                types.add(AuthType.Face)
            }
        }

        if (biometricManager.canAuthenticate(AUTH_TYPE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            types.add(AuthType.Pin)
            types.add(AuthType.Pattern)
            types.add(AuthType.Password)
        }

        return types
    }

    /**
     * 执行生物识别认证
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        config: AuthConfig = AuthConfig()
    ): AuthResult {
        val availability = checkBiometricAvailability()
        if (availability != BiometricAvailability.Available && !config.allowDeviceCredential) {
            return AuthResult.Error(
                errorCode = -1,
                errorMessage = getAvailabilityMessage(availability)
            )
        }

        val resultChannel = Channel<AuthResult>()

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                _authState.value = AuthState.Authenticated
                resultChannel.trySend(AuthResult.Success(result.authenticationType))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                _authState.value = AuthState.Failed(errorCode, errString.toString())
                val result = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> AuthResult.Cancelled
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> AuthResult.Lockout(errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT)
                    else -> AuthResult.Error(errorCode, errString.toString())
                }
                resultChannel.trySend(result)
            }

            override fun onAuthenticationFailed() {
                _authState.value = AuthState.Failed(-1, "认证失败，请重试")
                // 不发送结果，等待用户重试或取消
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val authenticators = if (config.allowDeviceCredential) {
            AUTH_TYPE_BIOMETRIC or AUTH_TYPE_CREDENTIAL
        } else {
            AUTH_TYPE_BIOMETRIC
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.title)
            .setSubtitle(config.subtitle)
            .setDescription(config.description)
            .setAllowedAuthenticators(authenticators)
            .apply {
                if (!config.allowDeviceCredential) {
                    setNegativeButtonText(config.negativeButtonText)
                }
            }
            .setConfirmationRequired(config.confirmationRequired)
            .build()

        _authState.value = AuthState.Authenticating
        prompt.authenticate(promptInfo)

        return resultChannel.receive()
    }

    /**
     * 取消认证
     */
    fun cancelAuthentication() {
        _authState.value = AuthState.Idle
    }

    /**
     * 重置认证状态
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    /**
     * 获取可用性提示消息
     */
    private fun getAvailabilityMessage(availability: BiometricAvailability): String {
        return when (availability) {
            BiometricAvailability.Available -> "生物识别可用"
            BiometricAvailability.NoHardware -> "此设备不支持生物识别"
            BiometricAvailability.HardwareUnavailable -> "生物识别硬件暂时不可用"
            BiometricAvailability.NoneEnrolled -> "请先在系统设置中注册指纹或面部"
            BiometricAvailability.SecurityUpdateRequired -> "需要安全更新才能使用生物识别"
            BiometricAvailability.Unsupported -> "生物识别不受支持"
            BiometricAvailability.Unknown -> "生物识别状态未知"
        }
    }

    /**
     * 获取推荐的认证方式描述
     */
    fun getRecommendedAuthDescription(): String {
        val types = getSupportedAuthTypes()
        return when {
            types.contains(AuthType.Fingerprint) && types.contains(AuthType.Face) -> "指纹或面容"
            types.contains(AuthType.Fingerprint) -> "指纹"
            types.contains(AuthType.Face) -> "面容"
            types.isNotEmpty() -> "设备密码"
            else -> "无可用认证方式"
        }
    }
}

/**
 * 生物识别可用性状态
 */
sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    object NoHardware : BiometricAvailability()
    object HardwareUnavailable : BiometricAvailability()
    object NoneEnrolled : BiometricAvailability()
    object SecurityUpdateRequired : BiometricAvailability()
    object Unsupported : BiometricAvailability()
    object Unknown : BiometricAvailability()
}

/**
 * 认证类型
 */
sealed class AuthType {
    object Fingerprint : AuthType()
    object Face : AuthType()
    object Iris : AuthType()
    object Pin : AuthType()
    object Pattern : AuthType()
    object Password : AuthType()
}

/**
 * 认证状态
 */
sealed class AuthState {
    object Idle : AuthState()
    object Authenticating : AuthState()
    object Authenticated : AuthState()
    data class Failed(val errorCode: Int, val message: String) : AuthState()
}

/**
 * 认证结果
 */
sealed class AuthResult {
    data class Success(val authenticationType: Int) : AuthResult()
    object Cancelled : AuthResult()
    data class Lockout(val isPermanent: Boolean) : AuthResult()
    data class Error(val errorCode: Int, val errorMessage: String) : AuthResult()
}

/**
 * 认证配置
 */
data class AuthConfig(
    val title: String = "身份验证",
    val subtitle: String = "使用生物识别验证您的身份",
    val description: String = "请验证以继续操作",
    val negativeButtonText: String = "取消",
    val allowDeviceCredential: Boolean = true,
    val confirmationRequired: Boolean = false
)

/**
 * 应用锁设置
 */
data class AppLockSettings(
    val isEnabled: Boolean = false,
    val authOnLaunch: Boolean = true,
    val authOnResume: Boolean = false,
    val lockTimeout: Long = 5 * 60 * 1000, // 5分钟
    val allowedAuthTypes: Set<String> = setOf("BIOMETRIC", "CREDENTIAL"),
    val sensitiveDataProtection: Boolean = true
)
