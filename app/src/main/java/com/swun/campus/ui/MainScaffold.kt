package com.swun.campus.ui

import android.app.Activity
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.academic.AcademicScreen
import com.swun.campus.ui.academic.AttendanceWebScreen
import com.swun.campus.ui.academic.CalendarScreen
import com.swun.campus.ui.academic.GradeScreen
import com.swun.campus.ui.academic.KtkqScheduleLoader
import com.swun.campus.ui.academic.ScheduleScreen
import com.swun.campus.ui.account.AccountScreen
import com.swun.campus.ui.common.GlassTab
import com.swun.campus.ui.common.LiquidGlassBottomBar
import com.swun.campus.ui.life.BusScreen
import com.swun.campus.ui.life.LifeScreen
import com.swun.campus.ui.life.OneCardScreen
import com.swun.campus.ui.life.UtilityScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

object Routes {
    const val LIFE = "life"
    const val ACADEMIC = "academic"
    const val ACCOUNT = "account"
    const val GRADE = "grade"
    const val SCHEDULE = "schedule"
    const val CALENDAR = "calendar"
    const val ATTENDANCE = "academic_attendance"
    const val ATTENDANCE_WEB = "academic_attendance_web"
    const val CARD = "life_card"
    const val CHECK_IN = "life_checkin"
    const val LEAVE = "life_leave"
    const val ELECTRIC = "life_electric"
    const val WATER = "life_water"
    const val BILL = "life_bill"
    const val BUS = "life_bus"

    val subRoutes = setOf(
        GRADE,
        SCHEDULE,
        CALENDAR,
        ATTENDANCE,
        ATTENDANCE_WEB,
        CARD,
        CHECK_IN,
        LEAVE,
        ELECTRIC,
        WATER,
        BILL,
        BUS
    )
}

private val tabs = listOf(
    GlassTab(Routes.LIFE, "生活", Icons.Outlined.Home, Icons.Filled.Home),
    GlassTab(Routes.ACADEMIC, "教务", Icons.Outlined.School, Icons.Filled.School),
    GlassTab(Routes.ACCOUNT, "账号", Icons.Outlined.Person, Icons.Filled.Person)
)

private fun titleFor(route: String): String = when (route) {
    Routes.LIFE -> "生活"
    Routes.ACADEMIC -> "教务"
    Routes.ACCOUNT -> "账号"
    Routes.GRADE -> "成绩查询"
    Routes.SCHEDULE -> "课表查询"
    Routes.CALENDAR -> "校历查询"
    Routes.ATTENDANCE -> "课堂考勤"
    Routes.ATTENDANCE_WEB -> "课堂考勤"
    Routes.CARD -> "校园一卡通"
    Routes.CHECK_IN -> "晚归打卡"
    Routes.LEAVE -> "请销假"
    Routes.ELECTRIC -> "电费充值"
    Routes.WATER -> "淋浴充值"
    Routes.BILL -> "账单查询"
    Routes.BUS -> "校车时刻"
    else -> "民大校园"
}

private fun subtitleFor(route: String, vm: AppViewModel): String? = when (route) {
    Routes.LIFE -> "校园生活服务"
    Routes.ACADEMIC -> vm.currentTerm.label
    Routes.ACCOUNT -> vm.profile?.name?.takeIf { it.isNotBlank() }?.let { "$it · 个人账号" }
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(vm: AppViewModel, snackbarHostState: SnackbarHostState) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.ACADEMIC
    val isSubPage = currentRoute in Routes.subRoutes
    // 底栏玻璃的取样源：主页面内容被录进这一层 layer，底栏再取其中被盖住的部分做模糊
    val hazeState = rememberHazeState()

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    Box(Modifier.fillMaxSize()) {
        // 全局只保留一个 1dp 隐藏 WebView：首页/课表/考勤共用同一份 ktkq 数据，
        // 页面切换时也不销毁 WebView，避免返回时重复加载与主线程卡顿。
        KtkqScheduleLoader(vm)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
            Column {
                if (isSubPage) {
                    TopAppBar(
                        colors = colors,
                        title = {
                            Text(
                                text = titleFor(currentRoute),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        colors = colors,
                        title = {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = titleFor(currentRoute),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                subtitleFor(currentRoute, vm)?.let { subtitle ->
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }
                        }
                    )
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ACADEMIC,
            // 底栏是悬浮玻璃，内容必须能滚到它下面，玻璃才有东西可「透」；
            // 子页面没有底栏，也不把内容录进 layer，避免 WebView 反复重录带来的开销。
            modifier = Modifier
                .padding(innerPadding)
                .then(if (isSubPage) Modifier else Modifier.hazeSource(hazeState)),
            // M3 转场要短促：进入 220ms、退出 140ms，位移只有屏宽的 1/10
            enterTransition = {
                fadeIn(tween(200, easing = FastOutLinearInEasing)) +
                    slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 10 }
            },
            exitTransition = {
                fadeOut(tween(140, easing = FastOutLinearInEasing))
            },
            popEnterTransition = {
                fadeIn(tween(200, easing = FastOutLinearInEasing)) +
                    slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 10 }
            },
            popExitTransition = {
                fadeOut(tween(140, easing = FastOutLinearInEasing)) +
                    slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 10 }
            }
        ) {
            composable(Routes.LIFE) {
                LifeScreen(vm) { route -> navController.navigate(route) }
            }

            composable(Routes.ACADEMIC) {
                AcademicScreen(vm) { route -> navController.navigate(route) }
            }

            composable(Routes.ACCOUNT) {
                AccountScreen(vm)
            }

            composable(Routes.GRADE) { GradeScreen(vm) }

            composable(Routes.SCHEDULE) { ScheduleScreen(vm) }

            composable(Routes.CALENDAR) { CalendarScreen(vm) }

            composable(Routes.ATTENDANCE) {
                AttendanceWebScreen(vm)
            }

            composable(Routes.ATTENDANCE_WEB) { AttendanceWebScreen(vm) }

            composable(Routes.CARD) {
                OneCardScreen(vm) { route -> navController.navigate(route) }
            }

            composable(Routes.ELECTRIC) {
                UtilityScreen(
                    vm = vm,
                    title = "电费充值",
                    subtitle = "学校官方电费系统 · 应用内",
                    url = CampusUrls.CARD_ELECTRIC
                )
            }

            composable(Routes.WATER) {
                UtilityScreen(
                    vm = vm,
                    title = "淋浴充值",
                    subtitle = "学校官方热水系统 · 应用内",
                    url = CampusUrls.CARD_WATER
                )
            }

            composable(Routes.BILL) {
                UtilityScreen(
                    vm = vm,
                    title = "账单查询",
                    subtitle = "一卡通消费流水 · 学校官方数据",
                    url = CampusUrls.CARD_BILL
                )
            }

            composable(Routes.BUS) { BusScreen(vm) }
        }
    }

        // 悬浮底栏画在内容之上（Haze 需要「先内容、后玻璃」的绘制顺序）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (!isSubPage) {
                LiquidGlassBottomBar(
                    tabs = tabs,
                    selectedRoute = currentRoute,
                    hazeState = hazeState,
                    onSelect = { route -> navController.switchTab(route) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.ACADEMIC) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
