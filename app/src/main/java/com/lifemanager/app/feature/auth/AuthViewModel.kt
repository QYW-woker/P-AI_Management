package com.lifemanager.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifemanager.app.core.data.repository.UserRepository
import com.lifemanager.app.core.database.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 认证ViewModel
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = userRepository.isLoggedIn
    val currentUser: StateFlow<UserEntity?> = userRepository.currentUser

    // 登录表单
    private val _loginEmail = MutableStateFlow("")
    val loginEmail: StateFlow<String> = _loginEmail.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    // 注册表单
    private val _registerUsername = MutableStateFlow("")
    val registerUsername: StateFlow<String> = _registerUsername.asStateFlow()

    private val _registerEmail = MutableStateFlow("")
    val registerEmail: StateFlow<String> = _registerEmail.asStateFlow()

    private val _registerPassword = MutableStateFlow("")
    val registerPassword: StateFlow<String> = _registerPassword.asStateFlow()

    private val _registerConfirmPassword = MutableStateFlow("")
    val registerConfirmPassword: StateFlow<String> = _registerConfirmPassword.asStateFlow()

    private val _agreeToTerms = MutableStateFlow(false)
    val agreeToTerms: StateFlow<Boolean> = _agreeToTerms.asStateFlow()

    // 密码可见性
    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.loadCurrentUser()
        }
    }

    // 登录表单更新
    fun updateLoginEmail(value: String) {
        _loginEmail.value = value
    }

    fun updateLoginPassword(value: String) {
        _loginPassword.value = value
    }

    // 注册表单更新
    fun updateRegisterUsername(value: String) {
        _registerUsername.value = value
    }

    fun updateRegisterEmail(value: String) {
        _registerEmail.value = value
    }

    fun updateRegisterPassword(value: String) {
        _registerPassword.value = value
    }

    fun updateRegisterConfirmPassword(value: String) {
        _registerConfirmPassword.value = value
    }

    fun updateAgreeToTerms(value: Boolean) {
        _agreeToTerms.value = value
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }

    /**
     * 登录
     */
    fun login() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val email = _loginEmail.value.trim()
            val password = _loginPassword.value

            if (email.isEmpty()) {
                _uiState.value = AuthUiState.Error("请输入邮箱或用户名")
                return@launch
            }
            if (password.isEmpty()) {
                _uiState.value = AuthUiState.Error("请输入密码")
                return@launch
            }

            userRepository.login(email, password).fold(
                onSuccess = {
                    clearForms()
                    _uiState.value = AuthUiState.Success("登录成功")
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "登录失败")
                }
            )
        }
    }

    /**
     * 注册
     */
    fun register() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val username = _registerUsername.value.trim()
            val email = _registerEmail.value.trim()
            val password = _registerPassword.value
            val confirmPassword = _registerConfirmPassword.value

            // 验证
            if (username.isEmpty()) {
                _uiState.value = AuthUiState.Error("请输入用户名")
                return@launch
            }
            if (email.isEmpty()) {
                _uiState.value = AuthUiState.Error("请输入邮箱")
                return@launch
            }
            if (password.isEmpty()) {
                _uiState.value = AuthUiState.Error("请输入密码")
                return@launch
            }
            if (password != confirmPassword) {
                _uiState.value = AuthUiState.Error("两次输入的密码不一致")
                return@launch
            }
            if (!_agreeToTerms.value) {
                _uiState.value = AuthUiState.Error("请阅读并同意用户协议和隐私政策")
                return@launch
            }

            userRepository.register(username, email, password).fold(
                onSuccess = {
                    clearForms()
                    _uiState.value = AuthUiState.Success("注册成功")
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "注册失败")
                }
            )
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        userRepository.logout()
        _uiState.value = AuthUiState.Idle
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * 清空表单
     */
    private fun clearForms() {
        _loginEmail.value = ""
        _loginPassword.value = ""
        _registerUsername.value = ""
        _registerEmail.value = ""
        _registerPassword.value = ""
        _registerConfirmPassword.value = ""
        _agreeToTerms.value = false
        _passwordVisible.value = false
    }
}

/**
 * 认证UI状态
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
