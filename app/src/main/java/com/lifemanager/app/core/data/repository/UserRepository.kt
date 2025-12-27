package com.lifemanager.app.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.lifemanager.app.core.database.dao.UserDao
import com.lifemanager.app.core.database.entity.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户仓库
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        // 检查是否已登录
        val savedUserId = prefs.getLong("current_user_id", -1)
        if (savedUserId != -1L) {
            _isLoggedIn.value = true
        }
    }

    /**
     * 加载当前用户
     */
    suspend fun loadCurrentUser() {
        val savedUserId = prefs.getLong("current_user_id", -1)
        if (savedUserId != -1L) {
            val user = userDao.getById(savedUserId)
            _currentUser.value = user
            _isLoggedIn.value = user != null
        }
    }

    /**
     * 获取当前用户Flow
     */
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        val savedUserId = prefs.getLong("current_user_id", -1)
        return userDao.getByIdFlow(savedUserId)
    }

    /**
     * 注册
     */
    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<UserEntity> {
        // 验证
        if (username.length < 3) {
            return Result.failure(Exception("用户名至少3个字符"))
        }
        if (!isValidEmail(email)) {
            return Result.failure(Exception("邮箱格式不正确"))
        }
        if (password.length < 6) {
            return Result.failure(Exception("密码至少6个字符"))
        }

        // 检查是否已存在
        if (userDao.isUsernameExists(username) > 0) {
            return Result.failure(Exception("用户名已被使用"))
        }
        if (userDao.isEmailExists(email) > 0) {
            return Result.failure(Exception("邮箱已被注册"))
        }

        // 创建用户
        val user = UserEntity(
            username = username,
            email = email,
            passwordHash = hashPassword(password),
            nickname = username
        )
        val userId = userDao.insert(user)
        val savedUser = userDao.getById(userId)!!

        // 自动登录
        saveLoginState(savedUser)

        return Result.success(savedUser)
    }

    /**
     * 登录
     */
    suspend fun login(emailOrUsername: String, password: String): Result<UserEntity> {
        val passwordHash = hashPassword(password)
        val user = userDao.login(emailOrUsername, passwordHash)

        return if (user != null) {
            userDao.updateLastLogin(user.id)
            saveLoginState(user)
            Result.success(user)
        } else {
            Result.failure(Exception("账号或密码错误"))
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        prefs.edit().remove("current_user_id").apply()
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    /**
     * 更新昵称
     */
    suspend fun updateNickname(nickname: String): Result<Unit> {
        val userId = prefs.getLong("current_user_id", -1)
        if (userId == -1L) {
            return Result.failure(Exception("未登录"))
        }
        userDao.updateNickname(userId, nickname)
        loadCurrentUser()
        return Result.success(Unit)
    }

    /**
     * 更新头像
     */
    suspend fun updateAvatar(avatarUrl: String): Result<Unit> {
        val userId = prefs.getLong("current_user_id", -1)
        if (userId == -1L) {
            return Result.failure(Exception("未登录"))
        }
        userDao.updateAvatar(userId, avatarUrl)
        loadCurrentUser()
        return Result.success(Unit)
    }

    /**
     * 检查登录状态
     */
    fun checkLoginState(): Boolean {
        return prefs.getLong("current_user_id", -1) != -1L
    }

    private fun saveLoginState(user: UserEntity) {
        prefs.edit().putLong("current_user_id", user.id).apply()
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
