package com.lifemanager.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.lifemanager.app.feature.home.CleanHomeScreen
import com.lifemanager.app.feature.finance.income.MonthlyIncomeExpenseScreen
import com.lifemanager.app.feature.finance.income.FieldManagementScreen
import com.lifemanager.app.feature.finance.asset.MonthlyAssetScreen
import com.lifemanager.app.feature.finance.expense.MonthlyExpenseScreen
import com.lifemanager.app.feature.finance.transaction.DailyTransactionScreen
import com.lifemanager.app.feature.todo.CleanTodoScreen
import com.lifemanager.app.feature.todo.CleanTodoDetailScreen
import com.lifemanager.app.feature.diary.CleanDiaryScreen
import com.lifemanager.app.feature.diary.CleanEditDiaryScreen
import com.lifemanager.app.feature.timetrack.TimeTrackScreen
import com.lifemanager.app.feature.habit.CleanHabitScreen
import com.lifemanager.app.feature.habit.CleanHabitDetailScreen
import com.lifemanager.app.feature.habit.CleanEditHabitScreen
import com.lifemanager.app.feature.savings.CleanSavingsPlanScreen
import com.lifemanager.app.feature.savings.CleanSavingsPlanDetailScreen
import com.lifemanager.app.feature.goal.GoalScreen
import com.lifemanager.app.feature.goal.GoalDetailScreen
import com.lifemanager.app.feature.goal.AddEditGoalScreen
import com.lifemanager.app.feature.datacenter.DataCenterScreen
import com.lifemanager.app.feature.profile.ProfileScreen
import com.lifemanager.app.feature.settings.SettingsScreen
import com.lifemanager.app.feature.auth.LoginScreen
import com.lifemanager.app.feature.auth.RegisterScreen
import com.lifemanager.app.feature.legal.PrivacyPolicyScreen
import com.lifemanager.app.feature.legal.TermsOfServiceScreen
import com.lifemanager.app.feature.ai.AISettingsScreen
import com.lifemanager.app.feature.ai.AIAssistantScreen
import com.lifemanager.app.feature.ai.VoiceInputScreen
import com.lifemanager.app.feature.budget.BudgetScreen
import com.lifemanager.app.feature.finance.transaction.billimport.BillImportScreen
import com.lifemanager.app.feature.finance.accounting.AccountingMainScreen
import com.lifemanager.app.feature.finance.accounting.AccountingCalendarScreen
import com.lifemanager.app.feature.finance.accounting.AccountingSearchScreen
import com.lifemanager.app.feature.finance.ledger.LedgerManagementScreen
import com.lifemanager.app.feature.finance.recurring.RecurringTransactionScreen
import com.lifemanager.app.feature.finance.account.FundAccountScreen
import com.lifemanager.app.feature.finance.account.FundAccountDetailScreen
import com.lifemanager.app.feature.finance.statistics.StatisticsScreen
import com.lifemanager.app.feature.health.CleanHealthRecordScreen
import com.lifemanager.app.feature.health.CleanHealthRecordDetailScreen
import com.lifemanager.app.ui.reading.ReadingScreen
import com.lifemanager.app.ui.reading.BookDetailScreen

/**
 * 窗口尺寸类型
 */
enum class WindowSizeClass {
    COMPACT,    // 手机竖屏 < 600dp
    MEDIUM,     // 手机横屏/小平板 600-840dp
    EXPANDED    // 大平板/桌面 > 840dp
}

/**
 * 获取当前窗口尺寸类型
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp < 600 -> WindowSizeClass.COMPACT
        configuration.screenWidthDp < 840 -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

/**
 * 自适应导航容器
 *
 * 根据屏幕尺寸自动选择底部导航、侧边栏或抽屉导航
 */
@Composable
fun AdaptiveNavigation(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 判断当前是否在主页面（显示底部导航的页面）
    val isMainScreen = bottomNavItems.any { it.route == currentRoute }

    when (windowSizeClass) {
        WindowSizeClass.COMPACT -> {
            // 手机：底部导航栏
            Scaffold(
                bottomBar = {
                    if (isMainScreen) {
                        NavigationBar {
                            bottomNavItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(Screen.Home.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == item.route) {
                                                item.selectedIcon
                                            } else {
                                                item.icon
                                            },
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                },
                modifier = modifier
            ) { paddingValues ->
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        WindowSizeClass.MEDIUM -> {
            // 平板/横屏：侧边导航栏（收起状态）
            Row(modifier = modifier.fillMaxSize()) {
                if (isMainScreen) {
                    NavigationRail {
                        Spacer(modifier = Modifier.height(8.dp))
                        bottomNavItems.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Home.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == item.route) {
                                            item.selectedIcon
                                        } else {
                                            item.icon
                                        },
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        WindowSizeClass.EXPANDED -> {
            // 大平板/桌面：侧边导航栏（展开状态）
            PermanentNavigationDrawer(
                drawerContent = {
                    if (isMainScreen) {
                        PermanentDrawerSheet(modifier = Modifier.width(240.dp)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AI生活管家",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            bottomNavItems.forEach { item ->
                                NavigationDrawerItem(
                                    label = { Text(item.label) },
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(Screen.Home.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == item.route) {
                                                item.selectedIcon
                                            } else {
                                                item.icon
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                },
                modifier = modifier
            ) {
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 应用导航图
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // 首页 - 使用简洁设计版本
        composable(Screen.Home.route) {
            CleanHomeScreen(
                onNavigateToModule = { route ->
                    // 检查是否是底部导航栏的主页面路由
                    val isMainScreenRoute = bottomNavItems.any { it.route == route }
                    if (isMainScreenRoute) {
                        // 使用与底部导航栏一致的导航方式
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(route)
                    }
                }
            )
        }

        // 月度收支
        composable(Screen.MonthlyIncomeExpense.route) {
            MonthlyIncomeExpenseScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFieldManagement = {
                    navController.navigate(Screen.FieldManagement.createRoute("INCOME"))
                }
            )
        }

        // 字段管理（简化路由，不带参数）
        composable("field_management") {
            FieldManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 月度资产
        composable(Screen.MonthlyAsset.route) {
            MonthlyAssetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 月度开销
        composable(Screen.MonthlyExpense.route) {
            MonthlyExpenseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 日常记账
        composable(Screen.DailyTransaction.route) {
            DailyTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToImport = { navController.navigate(Screen.BillImport.route) },
                onNavigateToCategoryManagement = { navController.navigate("field_management") }
            )
        }

        // 账单导入
        composable(Screen.BillImport.route) {
            BillImportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 预算管理
        composable(Screen.Budget.route) {
            BudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 记账主界面
        composable(Screen.AccountingMain.route) {
            AccountingMainScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToCalendar = { navController.navigate(Screen.AccountingCalendar.route) },
                onNavigateToSearch = { navController.navigate(Screen.AccountingSearch.route) },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                },
                onNavigateToLedgerManagement = { navController.navigate(Screen.LedgerManagement.route) },
                onNavigateToAssetManagement = { navController.navigate(Screen.MonthlyAsset.route) },
                onNavigateToRecurringTransaction = { navController.navigate(Screen.RecurringTransaction.route) },
                onNavigateToCategoryManagement = { navController.navigate("field_management") },
                onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                onNavigateToImport = { navController.navigate(Screen.BillImport.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDailyTransaction = { navController.navigate(Screen.DailyTransaction.route) },
                onNavigateToFundAccount = { navController.navigate(Screen.FundAccount.route) }
            )
        }

        // 记账日历
        composable(Screen.AccountingCalendar.route) {
            AccountingCalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddTransaction = { navController.navigate(Screen.DailyTransaction.route) }
            )
        }

        // 记账搜索
        composable(Screen.AccountingSearch.route) {
            AccountingSearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 账本管理
        composable(Screen.LedgerManagement.route) {
            LedgerManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 周期记账
        composable(Screen.RecurringTransaction.route) {
            RecurringTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 资金账户
        composable(Screen.FundAccount.route) {
            FundAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { accountId ->
                    navController.navigate(Screen.FundAccountDetail.createRoute(accountId))
                }
            )
        }

        // 资金账户详情
        composable(
            route = Screen.FundAccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            FundAccountDetailScreen(
                accountId = accountId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 统计分析
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 目标管理
        composable(Screen.Goal.route) {
            GoalScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { goalId ->
                    navController.navigate(Screen.GoalDetail.createRoute(goalId))
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.AddGoal.route)
                },
                onNavigateToAddMultiLevel = {
                    navController.navigate(Screen.AddMultiLevelGoal.route)
                }
            )
        }

        // 添加单级目标
        composable(Screen.AddGoal.route) {
            AddEditGoalScreen(
                goalId = null,
                isMultiLevel = false,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 添加多级目标
        composable(Screen.AddMultiLevelGoal.route) {
            AddEditGoalScreen(
                goalId = null,
                isMultiLevel = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 编辑目标
        composable(
            route = Screen.EditGoal.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("id") ?: 0L
            AddEditGoalScreen(
                goalId = goalId,
                isMultiLevel = false,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 目标详情
        composable(
            route = Screen.GoalDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("id") ?: 0L
            GoalDetailScreen(
                goalId = goalId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.EditGoal.createRoute(id))
                }
            )
        }

        // 数据中心
        composable(Screen.DataCenter.route) {
            DataCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 个人中心
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        // 添加收支
        composable(
            route = Screen.AddIncomeExpense.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "INCOME"
            // TODO: 实现添加收支页面
            PlaceholderScreen(title = "添加${if (type == "INCOME") "收入" else "支出"}")
        }

        // 字段管理（带参数）
        composable(
            route = Screen.FieldManagement.route,
            arguments = listOf(navArgument("moduleType") { type = NavType.StringType })
        ) {
            FieldManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 待办 - 使用简洁设计版本
        composable(Screen.Todo.route) {
            CleanTodoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { todoId ->
                    navController.navigate(Screen.TodoDetail.createRoute(todoId))
                }
            )
        }

        // 待办详情 - 使用简洁设计版本
        composable(
            route = Screen.TodoDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getLong("id") ?: 0L
            CleanTodoDetailScreen(
                todoId = todoId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGoal = { goalId ->
                    navController.navigate(Screen.GoalDetail.createRoute(goalId))
                }
            )
        }

        // 日记 - 使用简洁设计版本
        composable(Screen.Diary.route) {
            CleanDiaryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { date ->
                    navController.navigate(Screen.EditDiary.createRoute(date))
                }
            )
        }

        // 日记编辑 - 全屏编辑页面
        composable(
            route = Screen.EditDiary.route,
            arguments = listOf(navArgument("date") { type = NavType.IntType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getInt("date") ?: 0
            CleanEditDiaryScreen(
                diaryDate = if (date == 0) null else date,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 时间统计
        composable(Screen.TimeTrack.route) {
            TimeTrackScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 习惯打卡 - 使用简洁设计版本
        composable(Screen.Habit.route) {
            CleanHabitScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { habitId ->
                    navController.navigate(Screen.HabitDetail.createRoute(habitId))
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.EditHabit.createNewRoute())
                }
            )
        }

        // 习惯详情页
        composable(
            route = Screen.HabitDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("id") ?: 0L
            CleanHabitDetailScreen(
                habitId = habitId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.EditHabit.createRoute(id))
                }
            )
        }

        // 习惯编辑页
        composable(
            route = Screen.EditHabit.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("id") ?: 0L
            CleanEditHabitScreen(
                habitId = habitId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 存钱计划 - 使用简洁设计版本
        composable(Screen.SavingsPlan.route) {
            CleanSavingsPlanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { planId ->
                    navController.navigate(Screen.SavingsPlanDetail.createRoute(planId))
                }
            )
        }

        // 存钱计划详情
        composable(
            route = Screen.SavingsPlanDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("id") ?: 0L
            CleanSavingsPlanDetailScreen(
                planId = planId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    // TODO: Navigate to edit screen when created
                }
            )
        }

        // 健康记录 - 使用简洁设计版本
        composable(Screen.HealthRecord.route) {
            CleanHealthRecordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { recordId ->
                    navController.navigate(Screen.HealthRecordDetail.createRoute(recordId))
                }
            )
        }

        // 健康记录详情
        composable(
            route = Screen.HealthRecordDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("id") ?: 0L
            CleanHealthRecordDetailScreen(
                recordId = recordId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 阅读
        composable(Screen.Reading.route) {
            ReadingScreen(
                onNavigateToBookDetail = { bookId ->
                    navController.navigate(Screen.BookDetail.createRoute(bookId))
                },
                onNavigateToAddBook = { },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 书籍详情
        composable(
            route = Screen.BookDetail.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            BookDetailScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 设置
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTerms = { navController.navigate(Screen.TermsOfService.route) },
                onNavigateToAISettings = { navController.navigate(Screen.AISettings.route) }
            )
        }

        // AI设置
        composable(Screen.AISettings.route) {
            AISettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // AI语音助手
        composable(Screen.AIAssistant.route) {
            AIAssistantScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.AISettings.route) },
                onExecuteIntent = { intent ->
                    // 根据意图类型导航到对应页面或执行操作
                    // 这里可以在后续完善具体的命令执行逻辑
                }
            )
        }

        // 登录
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTerms = { navController.navigate(Screen.TermsOfService.route) }
            )
        }

        // 注册
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTerms = { navController.navigate(Screen.TermsOfService.route) }
            )
        }

        // 隐私政策
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 用户协议
        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * 占位页面
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
