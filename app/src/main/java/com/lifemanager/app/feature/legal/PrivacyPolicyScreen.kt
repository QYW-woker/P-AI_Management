package com.lifemanager.app.feature.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 隐私政策页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐私政策") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                .padding(16.dp)
        ) {
            Text(
                text = "AI智能生活管家隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "更新日期：2024年1月1日\n生效日期：2024年1月1日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            PolicySection(
                title = "引言",
                content = """
                    欢迎使用AI智能生活管家（以下简称"本应用"）。我们非常重视您的隐私保护和个人信息安全。本隐私政策将向您说明我们如何收集、使用、存储和保护您的个人信息。

                    请您在使用本应用前，仔细阅读并充分理解本隐私政策的全部内容。一旦您开始使用本应用，即表示您已阅读并同意本隐私政策。
                """.trimIndent()
            )

            PolicySection(
                title = "一、我们收集的信息",
                content = """
                    1. 账户信息
                    当您注册账户时，我们会收集您的用户名、邮箱地址等信息，用于创建和管理您的账户。

                    2. 使用数据
                    • 待办事项：您创建的任务标题、描述、截止日期等
                    • 日记内容：您记录的日记文本、心情状态、天气信息
                    • 财务数据：您记录的收入、支出、资产等财务信息
                    • 习惯数据：您设置的习惯目标和打卡记录
                    • 目标数据：您设定的目标及进度信息

                    3. 设备信息
                    为了优化服务，我们可能收集设备型号、操作系统版本等基本信息。
                """.trimIndent()
            )

            PolicySection(
                title = "二、信息的使用",
                content = """
                    我们收集的信息将用于：

                    1. 提供核心功能服务
                    • 同步和备份您的数据
                    • 发送提醒通知
                    • 生成统计分析报告

                    2. 改进服务质量
                    • 分析使用趋势，优化产品功能
                    • 修复技术问题

                    3. 个性化体验
                    • 根据您的使用习惯提供智能建议
                    • 定制化推荐内容
                """.trimIndent()
            )

            PolicySection(
                title = "三、信息存储与保护",
                content = """
                    1. 存储方式
                    • 本地存储：您的数据主要存储在设备本地数据库中
                    • 云端备份：如启用备份功能，数据将加密后存储至安全的云服务器

                    2. 安全措施
                    • 采用行业标准的加密技术保护数据传输
                    • 对敏感数据进行加密存储
                    • 定期进行安全审计和漏洞检测

                    3. 存储期限
                    • 账户数据：在您主动删除账户前持续保存
                    • 使用记录：保存至您主动清除或账户注销
                """.trimIndent()
            )

            PolicySection(
                title = "四、信息共享与披露",
                content = """
                    我们承诺不会出售您的个人信息。仅在以下情况下可能共享：

                    1. 获得您的明确同意
                    2. 法律法规要求或政府部门依法要求
                    3. 为保护用户或公众的安全和权益所必需

                    如涉及第三方服务（如云存储），我们会确保其遵守相应的隐私保护标准。
                """.trimIndent()
            )

            PolicySection(
                title = "五、您的权利",
                content = """
                    您对您的个人信息享有以下权利：

                    1. 访问权：查看我们收集的您的信息
                    2. 更正权：修改不准确的个人信息
                    3. 删除权：删除您的账户和相关数据
                    4. 导出权：导出您的个人数据
                    5. 撤回同意：随时撤回对数据收集的同意

                    您可以通过应用内设置或联系我们行使上述权利。
                """.trimIndent()
            )

            PolicySection(
                title = "六、未成年人保护",
                content = """
                    本应用不针对未满14周岁的未成年人。如果您是未成年人的监护人，发现您的孩子未经同意使用了本应用，请联系我们删除相关信息。
                """.trimIndent()
            )

            PolicySection(
                title = "七、隐私政策更新",
                content = """
                    我们可能会不时更新本隐私政策。更新后的政策将在应用内公布，重大变更我们会以弹窗等方式通知您。建议您定期查阅本政策。
                """.trimIndent()
            )

            PolicySection(
                title = "八、联系我们",
                content = """
                    如您对本隐私政策有任何疑问、意见或建议，可通过以下方式联系我们：

                    邮箱：support@lifemanager.app

                    我们将在15个工作日内回复您的请求。
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}
