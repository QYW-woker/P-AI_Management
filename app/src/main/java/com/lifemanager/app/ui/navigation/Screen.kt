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

    /** 月度定投 */
    object MonthlyInvestment : Screen("monthly_investment")

    /** 月度统计（统一入口） */
    object MonthlyStatistics : Screen("monthly_statistics")

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

    /** 资金账户 */
    object FundAccount : Screen("fund_account")

    /** 资金账户详情 */
    object FundAccountDetail : Screen("fund_account_detail/{accountId}") {
        fun createRoute(accountId: Long) = "fund_account_detail/$accountId"
    }

    /** 统计分析 */
    object Statistics : Screen("statistics")

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

    /** 添加单级目标 */
    object AddGoal : Screen("add_goal")

    /** 添加多级目标 */
    object AddMultiLevelGoal : Screen("add_multi_level_goal")

    /** 编辑目标 */
    object EditGoal : Screen("edit_goal/{id}") {
        fun createRoute(id: Long) = "edit_goal/$id"
    }

    /** 目标详情 */
    object GoalDetail : Screen("goal_detail/{id}") {
        fun createRoute(id: Long) = "goal_detail/$id"
    }

    // ==================== 存钱模块 ====================

    /** 存钱总览 */
    object SavingsOverview : Screen("savings_overview")

    /** 快速存钱 */
    object QuickSavings : Screen("quick_savings/{planId}") {
        fun createRoute(planId: Long? = null) = if (planId != null) "quick_savings/$planId" else "quick_savings/-1"
    }

    /** 存钱计划详情 */
    object SavingsPlanDetail : Screen("savings_plan_detail/{id}") {
        fun createRoute(id: Long) = "savings_plan_detail/$id"
    }

    /** 添加存钱计划 */
    object AddSavingsPlan : Screen("add_savings_plan")

    /** 编辑存钱计划 */
    object EditSavingsPlan : Screen("edit_savings_plan/{id}") {
        fun createRoute(id: Long) = "edit_savings_plan/$id"
    }

    /** 存钱记录详情 */
    object SavingsRecordDetail : Screen("savings_record_detail/{id}") {
        fun createRoute(id: Long) = "savings_record_detail/$id"
    }

    // ==================== 其他模块 ====================

    /** 待办记事 */
    object Todo : Screen("todo")

    /** 待办详情 */
    object TodoDetail : Screen("todo_detail/{id}") {
        fun createRoute(id: Long) = "todo_detail/$id"
    }

    /** 日记 */
    object Diary : Screen("diary")

    /** 日记详情 */
    object DiaryDetail : Screen("diary_detail/{id}") {
        fun createRoute(id: Long) = "diary_detail/$id"
    }

    /** 日记编辑 */
    object EditDiary : Screen("edit_diary/{date}") {
        fun createRoute(date: Int) = "edit_diary/$date"
        fun createNewRoute() = "edit_diary/0"
    }

    /** 时间统计 */
    object TimeTrack : Screen("time_track")

    /** 时间记录详情 */
    object TimeTrackDetail : Screen("time_track_detail/{id}") {
        fun createRoute(id: Long) = "time_track_detail/$id"
    }

    /** 习惯打卡 */
    object Habit : Screen("habit")

    /** 习惯详情 */
    object HabitDetail : Screen("habit_detail/{id}") {
        fun createRoute(id: Long) = "habit_detail/$id"
    }

    /** 习惯编辑 */
    object EditHabit : Screen("edit_habit/{id}") {
        fun createRoute(id: Long) = "edit_habit/$id"
        fun createNewRoute() = "edit_habit/0"
    }

    /** 存钱计划 */
    object SavingsPlan : Screen("savings_plan")

    /** 健康记录 */
    object HealthRecord : Screen("health_record")

    /** 健康记录详情 */
    object HealthRecordDetail : Screen("health_record_detail/{id}") {
        fun createRoute(id: Long) = "health_record_detail/$id"
    }

    /** 阅读 */
    object Reading : Screen("reading")

    /** 书籍详情 */
    object BookDetail : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }

    /** 交易详情 */
    object TransactionDetail : Screen("transaction_detail/{id}") {
        fun createRoute(id: Long) = "transaction_detail/$id"
    }

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
