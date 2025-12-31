package com.lifemanager.app.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.component.GlowingButton

/**
 * 注册页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val username by viewModel.registerUsername.collectAsState()
    val email by viewModel.registerEmail.collectAsState()
    val password by viewModel.registerPassword.collectAsState()
    val confirmPassword by viewModel.registerConfirmPassword.collectAsState()
    val agreeToTerms by viewModel.agreeToTerms.collectAsState()
    val passwordVisible by viewModel.passwordVisible.collectAsState()
    val focusManager = LocalFocusManager.current

    // 监听注册成功
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onNavigateToHome()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建账号") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "加入我们",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "创建账号，开启智能生活",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 用户名输入
            PremiumTextField(
                value = username,
                onValueChange = { viewModel.updateRegisterUsername(it) },
                modifier = Modifier.fillMaxWidth(),
                label = "用户名",
                placeholder = "3-20个字符",
                leadingIcon = {
                    Icon(Icons.Outlined.Person, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 邮箱输入
            PremiumTextField(
                value = email,
                onValueChange = { viewModel.updateRegisterEmail(it) },
                modifier = Modifier.fillMaxWidth(),
                label = "邮箱",
                placeholder = "example@email.com",
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 密码输入
            PremiumTextField(
                value = password,
                onValueChange = { viewModel.updateRegisterPassword(it) },
                modifier = Modifier.fillMaxWidth(),
                label = "密码",
                placeholder = "至少6个字符",
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                },
                isPassword = true,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 确认密码输入
            PremiumTextField(
                value = confirmPassword,
                onValueChange = { viewModel.updateRegisterConfirmPassword(it) },
                modifier = Modifier.fillMaxWidth(),
                label = "确认密码",
                placeholder = "再次输入密码",
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                },
                isPassword = true,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (agreeToTerms) viewModel.register()
                    }
                ),
                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                errorMessage = if (confirmPassword.isNotEmpty() && password != confirmPassword) "两次输入的密码不一致" else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 同意协议
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreeToTerms,
                    onCheckedChange = { viewModel.updateAgreeToTerms(it) }
                )
                Text(
                    text = "我已阅读并同意",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "《用户协议》",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToTerms() }
                )
                Text(
                    text = "和",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "《隐私政策》",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToPrivacy() }
                )
            }

            // 错误提示
            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 注册按钮
            if (uiState is AuthUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                GlowingButton(
                    text = "注册",
                    onClick = { viewModel.register() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 返回登录
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已有账号？",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateBack) {
                    Text("立即登录")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
