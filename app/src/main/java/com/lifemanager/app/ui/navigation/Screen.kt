package com.lifemanager.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用路由定义
 *
 * 定义所有页面的路由路径
 */
sealed class Screen(val route: String) {

    // ==================== 主页面（底部导航） ====================

    /** 首页 */
    object Home : Screen("home")

    /** 数据中心 */
    object DataCenter : Screen("data_center")

    /** 目标管理 */
    object Goal : Screen("goal")

    /** 个人中心 */
    object Profile : Screen("profile")

    // ==================== 财务模块 ====================

    /** 月度收支 */
    object MonthlyIncomeExpense : Screen("monthly_income_expense")

    /** 月度资产 */
    object MonthlyAsset : Screen("monthly_asset")

    /** 月度开销 */
    object MonthlyExpense : Screen("monthly_expense")

    /** 日常记账 */
    object DailyTransaction : Screen("daily_transaction")

    /** 账单导入 */
    object BillImport : Screen("bill_import")

    /** 预算管理 */
    object Budget : Screen("budget")

    /** 记账主界面 */
    object AccountingMain : Screen("accounting_main")

    /** 记账日历 */
    object AccountingCalendar : Screen("accounting_calendar")

    /** 记账搜索 */
    object AccountingSearch : Screen("accounting_search")

    /** 账本管理 */
    object LedgerManagement : Screen("ledger_management")

    /** 周期记账 */
    object RecurringTransaction : Screen("recurring_transaction")

    // ==================== 详情/编辑页面 ====================

    /** 添加收支记录 */
    object AddIncomeExpense : Screen("add_income_expense/{type}") {
        fun createRoute(type: String) = "add_income_expense/$type"
    }

    /** 收支记录详情 */
    object IncomeExpenseDetail : Screen("income_expense_detail/{id}") {
        fun createRoute(id: Long) = "income_expense_detail/$id"
    }

    /** 字段管理 */
    object FieldManagement : Screen("field_management/{moduleType}") {
        fun createRoute(moduleType: String) = "field_management/$moduleType"
    }

    // ==================== 目标模块 ====================

    /** 添加目标 */
    object AddGoal : Screen("add_goal")

    /** 目标详情 */
    object GoalDetail : Screen("goal_detail/{id}") {
        fun createRoute(id: Long) = "goal_detail/$id"
    }

    // ==================== 其他模块 ====================

    /** 待办记事 */
    object Todo : Screen("todo")

    /** 日记 */
    object Diary : Screen("diary")

    /** 时间统计 */
    object TimeTrack : Screen("time_track")

    /** 习惯打卡 */
    object Habit : Screen("habit")

    /** 存钱计划 */
    object SavingsPlan : Screen("savings_plan")

    // ==================== 设置 ====================

    /** 设置页面 */
    object Settings : Screen("settings")

    /** AI设置 */
    object AISettings : Screen("ai_settings")

    /** AI助手 */
    object AIAssistant : Screen("ai_assistant")

    // ==================== 认证模块 ====================

    /** 登录页面 */
    object Login : Screen("login")

    /** 注册页面 */
    object Register : Screen("register")

    // ==================== 法律页面 ====================

    /** 隐私政策 */
    object PrivacyPolicy : Screen("privacy_policy")

    /** 用户协议 */
    object TermsOfService : Screen("terms_of_service")
}

/**
 * 底部导航项
 */
data class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

/**
 * 底部导航项列表
 */
val bottomNavItems = listOf(
    NavigationItem(
        route = Screen.Home.route,
        icon = Icons.Default.Home,
        selectedIcon = Icons.Filled.Home,
        label = "首页"
    ),
    NavigationItem(
        route = Screen.AccountingMain.route,
        icon = Icons.Default.AccountBalance,
        selectedIcon = Icons.Filled.AccountBalance,
        label = "记账"
    ),
    NavigationItem(
        route = Screen.Goal.route,
        icon = Icons.Default.Flag,
        selectedIcon = Icons.Filled.Flag,
        label = "目标"
    ),
    NavigationItem(
        route = Screen.DataCenter.route,
        icon = Icons.Default.Analytics,
        selectedIcon = Icons.Filled.Analytics,
        label = "数据"
    ),
    NavigationItem(
        route = Screen.Profile.route,
        icon = Icons.Default.Person,
        selectedIcon = Icons.Filled.Person,
        label = "我的"
    )
)
