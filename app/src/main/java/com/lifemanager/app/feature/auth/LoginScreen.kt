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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.ui.component.PremiumTextField
import com.lifemanager.app.ui.component.GlowingButton

/**
 * 登录页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()
    val passwordVisible by viewModel.passwordVisible.collectAsState()
    val focusManager = LocalFocusManager.current

    // 监听登录成功
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onNavigateToHome()
            viewModel.resetState()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = "AI智能生活管家",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "让AI帮你管理生活",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 邮箱/用户名输入
            PremiumTextField(
                value = email,
                onValueChange = { viewModel.updateLoginEmail(it) },
                modifier = Modifier.fillMaxWidth(),
                label = "邮箱或用户名",
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
                onValueChange = { viewModel.updateLoginPassword(it) },
                modifier = Modifier.fillMaxWidth(),
                label = "密码",
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
                        viewModel.login()
                    }
                )
            )

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

            // 登录按钮
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
                    text = "登录",
                    onClick = { viewModel.login() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 注册入口
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "还没有账号？",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text("立即注册")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 其他登录方式
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "  其他方式  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 第三方登录按钮（占位）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* TODO: 微信登录 */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "微信登录",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(
                    onClick = { /* TODO: 手机号登录 */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "手机号登录",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 协议提示
            Text(
                text = "登录即表示同意",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                Text(
                    text = "《用户协议》",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToTerms() }
                )
                Text(
                    text = " 和 ",
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
