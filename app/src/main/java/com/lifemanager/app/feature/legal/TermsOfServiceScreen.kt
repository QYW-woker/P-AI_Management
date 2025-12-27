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
 * 用户协议页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户协议") },
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
                text = "AI智能生活管家用户服务协议",
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

            TermsSection(
                title = "重要提示",
                content = """
                    欢迎使用AI智能生活管家（以下简称"本应用"）。在使用本应用服务之前，请您务必仔细阅读并充分理解本协议的全部内容，特别是涉及免责条款或限制责任条款等重要内容。

                    当您点击"同意"按钮或以其他方式确认接受本协议，或实际使用本应用服务时，即视为您已阅读、理解并同意接受本协议的约束。
                """.trimIndent()
            )

            TermsSection(
                title = "一、服务说明",
                content = """
                    1. 服务内容
                    本应用是一款智能生活管理工具，提供以下主要功能：
                    • 待办事项管理
                    • 日记记录
                    • 财务记账
                    • 习惯打卡
                    • 目标跟踪
                    • 时间统计
                    • 存钱计划
                    • 数据统计分析

                    2. 服务形式
                    本应用提供免费基础服务，部分高级功能可能需要付费订阅。

                    3. 服务调整
                    我们保留随时修改、升级、暂停或终止部分或全部服务的权利。
                """.trimIndent()
            )

            TermsSection(
                title = "二、账户注册与管理",
                content = """
                    1. 注册条件
                    • 您应当具有完全民事行为能力
                    • 未满18周岁的用户应在监护人陪同下使用

                    2. 账户信息
                    • 您应提供真实、准确的注册信息
                    • 您有责任妥善保管账户密码
                    • 因密码泄露导致的损失由您自行承担

                    3. 账户安全
                    • 如发现账户被盗用，请立即联系我们
                    • 禁止转让、出借或出售账户
                """.trimIndent()
            )

            TermsSection(
                title = "三、用户行为规范",
                content = """
                    使用本应用时，您同意不得：

                    1. 违反法律法规
                    • 发布违法、有害信息
                    • 侵犯他人合法权益
                    • 从事任何违法活动

                    2. 滥用服务
                    • 干扰或破坏服务器和网络
                    • 未经授权访问系统或数据
                    • 恶意注册账户或刷取数据

                    3. 不当使用
                    • 将服务用于商业目的（除非获得授权）
                    • 逆向工程、反编译或解密应用
                    • 传播病毒或恶意代码
                """.trimIndent()
            )

            TermsSection(
                title = "四、知识产权",
                content = """
                    1. 应用权利
                    本应用的所有内容，包括但不限于文字、图片、界面设计、软件代码等，均受知识产权法律保护，归我们或相关权利人所有。

                    2. 用户内容
                    • 您保留对您上传内容的所有权
                    • 您授权我们为提供服务而使用您的内容
                    • 您保证上传内容不侵犯第三方权利

                    3. 禁止行为
                    未经授权，禁止复制、修改、分发本应用的任何部分。
                """.trimIndent()
            )

            TermsSection(
                title = "五、付费服务",
                content = """
                    1. 付费规则
                    • 付费服务的价格和内容将在购买页面明确展示
                    • 支付完成后，服务即时生效

                    2. 退款政策
                    • 虚拟服务一经购买，原则上不予退款
                    • 如遇技术问题导致无法使用，可联系客服处理

                    3. 自动续费
                    • 订阅服务可能包含自动续费
                    • 您可以随时在应用设置中取消自动续费
                    • 取消前已扣费的周期服务仍然有效
                """.trimIndent()
            )

            TermsSection(
                title = "六、免责声明",
                content = """
                    1. 服务稳定性
                    我们将努力保障服务的连续性，但不保证服务不会中断或出错。因系统维护、升级等导致的服务暂停，我们将尽可能提前通知。

                    2. 数据安全
                    虽然我们采取多重措施保护数据，但不能完全排除数据丢失或泄露的风险。建议您定期备份重要数据。

                    3. 第三方服务
                    本应用可能包含第三方服务链接，我们对第三方服务不承担责任。

                    4. 不可抗力
                    因不可抗力（如自然灾害、战争、政策变化等）导致的服务中断或数据损失，我们不承担责任。
                """.trimIndent()
            )

            TermsSection(
                title = "七、责任限制",
                content = """
                    在法律允许的最大范围内：

                    1. 我们对因使用本应用导致的任何间接、附带、特殊或惩罚性损害不承担责任

                    2. 我们的总责任不超过您在过去12个月内支付的服务费用总额

                    3. 对于免费服务，我们不承担任何责任
                """.trimIndent()
            )

            TermsSection(
                title = "八、协议修改",
                content = """
                    1. 我们保留随时修改本协议的权利
                    2. 修改后的协议将在应用内公布
                    3. 重大变更将通过弹窗等方式通知您
                    4. 继续使用服务即视为同意修改后的协议
                """.trimIndent()
            )

            TermsSection(
                title = "九、账户终止",
                content = """
                    1. 您可以随时通过应用设置注销账户
                    2. 如您违反本协议，我们有权暂停或终止您的账户
                    3. 账户终止后，我们可能保留您的部分数据以满足法律要求
                """.trimIndent()
            )

            TermsSection(
                title = "十、争议解决",
                content = """
                    1. 本协议的解释和执行适用中华人民共和国法律
                    2. 如发生争议，双方应首先协商解决
                    3. 协商不成的，任何一方可向有管辖权的人民法院提起诉讼
                """.trimIndent()
            )

            TermsSection(
                title = "十一、联系方式",
                content = """
                    如您对本协议有任何疑问，请通过以下方式联系我们：

                    邮箱：support@lifemanager.app

                    感谢您使用AI智能生活管家！
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TermsSection(
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
