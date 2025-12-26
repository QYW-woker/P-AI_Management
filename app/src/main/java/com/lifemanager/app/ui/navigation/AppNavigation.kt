package com.lifemanager.app.ui.navigation

import androidx.compose.foundation.layout.*
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
import com.lifemanager.app.feature.home.HomeScreen
import com.lifemanager.app.feature.finance.income.MonthlyIncomeExpenseScreen
import com.lifemanager.app.feature.finance.income.FieldManagementScreen

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
        // 首页
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToModule = { route ->
                    navController.navigate(route)
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

        // 目标管理
        composable(Screen.Goal.route) {
            // TODO: 实现目标管理页面
            PlaceholderScreen(title = "目标管理")
        }

        // 数据中心
        composable(Screen.DataCenter.route) {
            // TODO: 实现数据中心页面
            PlaceholderScreen(title = "数据中心")
        }

        // 个人中心
        composable(Screen.Profile.route) {
            // TODO: 实现个人中心页面
            PlaceholderScreen(title = "个人中心")
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

        // 待办
        composable(Screen.Todo.route) {
            PlaceholderScreen(title = "待办记事")
        }

        // 日记
        composable(Screen.Diary.route) {
            PlaceholderScreen(title = "日记")
        }

        // 时间统计
        composable(Screen.TimeTrack.route) {
            PlaceholderScreen(title = "时间统计")
        }

        // 习惯打卡
        composable(Screen.Habit.route) {
            PlaceholderScreen(title = "习惯打卡")
        }

        // 存钱计划
        composable(Screen.SavingsPlan.route) {
            PlaceholderScreen(title = "存钱计划")
        }

        // 设置
        composable(Screen.Settings.route) {
            PlaceholderScreen(title = "设置")
        }

        // AI设置
        composable(Screen.AISettings.route) {
            PlaceholderScreen(title = "AI设置")
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
