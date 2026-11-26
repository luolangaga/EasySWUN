package com.swun.campus.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.swun.campus.ui.common.glass.LiquidBottomTab
import com.swun.campus.ui.common.glass.LiquidBottomTabs
import com.swun.campus.ui.common.glass.LocalLiquidBottomTabGhost

/** 悬浮玻璃底栏盖住内容的高度。 */
val CampusTabBarContentPadding: Dp = 96.dp

data class CampusTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/** 内容层录进 Backdrop 用的图层，底栏再从这里取自己盖住的那块做折射。 */
@Composable
fun rememberTabBarBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/**
 * 液态玻璃底栏。组件本体是 Backdrop 官方示例的 LiquidBottomTabs
 * （github.com/Kyant0/AndroidLiquidGlass 的 catalog 示例，见 ui/common/glass），
 * 这里只做一层适配：把项目的 tab 数据、配色和路由接进示例的 content 槽。
 */
@Composable
fun LiquidGlassTabBar(
    tabs: List<CampusTab>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() >= 0.5f
    val contentColor = if (isLight) Color.Black else Color.White
    val selectedIndex = tabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)

    // 示例内部用 snapshotFlow 读这个 lambda，lambda 必须是稳定的、且读到的是 state
    val indexState = remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) { indexState.intValue = selectedIndex }
    val indexProvider = remember(indexState) { { indexState.intValue } }

    LiquidBottomTabs(
        selectedTabIndex = indexProvider,
        onTabSelected = { index -> tabs.getOrNull(index)?.let { onSelect(it.route) } },
        backdrop = backdrop,
        tabsCount = tabs.size,
        modifier = modifier.fillMaxWidth(),
        // 示例默认是 0.4 的白，这里压到 0.16，玻璃背后的内容要能看清
        surfaceAlpha = 0.16f
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == selectedRoute
            // 托盘只画未选中的黑图标；选中项交给胶囊里的折射层，避免黑蓝两枚图标叠在一起
            val ghost = LocalLiquidBottomTabGhost.current
            val visible = if (ghost) selected else !selected
            val alpha = if (visible) 1f else 0f

            LiquidBottomTab(onClick = { onSelect(tab.route) }) {
                Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                    contentDescription = tab.label,
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(alpha)
                )
                BasicText(
                    text = tab.label,
                    style = TextStyle(color = contentColor, fontSize = 11.5.sp),
                    modifier = Modifier.alpha(alpha)
                )
            }
        }
    }
}
