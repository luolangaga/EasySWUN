package com.swun.campus.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** 悬浮底栏盖住内容的高度，主页面滚动到底时用它留出底部空白。 */
val LiquidGlassBarContentPadding: Dp = 88.dp

data class GlassTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val BarHeight: Dp = 62.dp
private val BarInnerPadding: Dp = 6.dp
private val IndicatorHeight: Dp = 50.dp
private val IndicatorGap: Dp = 8.dp

/**
 * 液态玻璃底栏：
 * 1. 真正的背景模糊（Haze 把 NavHost 的内容录进一层 layer，底栏再取其中被盖住的部分做高斯模糊）；
 * 2. 玻璃本体叠一层左上入射的镜面渐变 + 细噪声，模拟磨砂玻璃的颗粒；
 * 3. 1dp 渐变描边做「光边」，上缘亮、下缘暗，看起来有厚度；
 * 4. 选中态是一枚会弹性滑动的玻璃胶囊，两端的弹簧阻尼不同，切换时会先拉长再回弹。
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun LiquidGlassBottomBar(
    tabs: List<GlassTab>,
    selectedRoute: String,
    hazeState: HazeState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(50)

    // 玻璃底色：浅色主题用带一点冷调的雾白，深色主题用近黑的蓝灰，alpha 决定内容透出来的程度
    val glassTint = if (dark) {
        Color(0xFF0B1217).copy(alpha = 0.42f)
    } else {
        Color(0xFFF1F6FA).copy(alpha = 0.56f)
    }
    // 镜面高光：光从左上来，向右下渐隐，玻璃才有「厚度」
    val sheen = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (dark) 0.16f else 0.42f),
        0.42f to Color.White.copy(alpha = if (dark) 0.04f else 0.10f),
        1f to Color.White.copy(alpha = if (dark) 0.02f else 0.02f),
        start = Offset.Zero,
        end = Offset.Infinite
    )
    val hazeStyle = HazeStyle(
        // 26dp 左右是「看得清轮廓、但看不清内容」的甜点区
        blurRadius = 26.dp,
        tints = listOf(HazeTint(glassTint), HazeTint(sheen)),
        noiseFactor = 0.05f,
        // Android 11 及以下没有 RenderEffect 模糊，退化成接近不透明的磨砂底，保证可读性
        fallbackTint = HazeTint(if (dark) Color(0xF210171B) else Color(0xEBFFFFFF))
    )

    val rimColors = if (dark) {
        listOf(
            Color.White.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.22f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.94f),
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.58f)
        )
    }

    val indicatorFill = Brush.verticalGradient(
        listOf(
            if (dark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.86f),
            if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.58f)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = true,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .hazeEffect(state = hazeState, style = hazeStyle) {
                // 半分辨率取样：模糊本来就看不出细节，像素量却只有 1/4，滑动时更稳
                inputScale = HazeInputScale.Fixed(0.5f)
            }
            .drawWithContent {
                // 玻璃的高光与描边先画，再让胶囊和图标盖在上面，避免光泽糊到文字上

                // 顶缘一道很软的镜面反光，模拟光从左上打到玻璃上
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (dark) 0.10f else 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.18f, 0f),
                        radius = size.width * 0.40f
                    )
                )

                val stroke = 1.dp.toPx()
                // 外缘光边：左上最亮，向右下渐隐，再在底部点亮一点
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = rimColors,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius((size.height - stroke) / 2f),
                    style = Stroke(width = stroke)
                )
                // 内缘高光：只在上半部分可见，强化玻璃折射的观感
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.22f else 0.45f),
                        0.35f to Color.Transparent
                    ),
                    topLeft = Offset(stroke * 1.5f, stroke * 1.5f),
                    size = Size(size.width - stroke * 3f, size.height - stroke * 3f),
                    cornerRadius = CornerRadius((size.height - stroke * 3f) / 2f),
                    style = Stroke(width = stroke)
                )
                // 底缘内阴影，让玻璃有厚度而不是一张白纸
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.90f to Color.Transparent,
                        1f to Color.Black.copy(alpha = if (dark) 0.05f else 0.03f)
                    ),
                    topLeft = Offset(stroke * 1.5f, stroke * 1.5f),
                    size = Size(size.width - stroke * 3f, size.height - stroke * 3f),
                    cornerRadius = CornerRadius((size.height - stroke * 3f) / 2f),
                    style = Stroke(width = stroke * 3f)
                )

                drawContent()
            }
    ) {
        val itemWidth = (maxWidth - BarInnerPadding * 2) / tabs.size
        val selectedIndex = tabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)

        // 胶囊的左右边缘各自用一条弹簧，阻尼不同 → 移动时先被「拉长」，再回弹成胶囊
        val leftEdge = remember { Animatable(0.dp, Dp.VectorConverter) }
        val rightEdge = remember { Animatable(0.dp, Dp.VectorConverter) }
        var measured by remember { mutableStateOf(false) }

        LaunchedEffect(selectedIndex, itemWidth) {
            val left = BarInnerPadding + itemWidth * selectedIndex + IndicatorGap / 2
            val right = BarInnerPadding + itemWidth * (selectedIndex + 1) - IndicatorGap / 2
            if (!measured) {
                leftEdge.snapTo(left)
                rightEdge.snapTo(right)
                measured = true
            } else {
                coroutineScope {
                    launch {
                        leftEdge.animateTo(
                            targetValue = left,
                            animationSpec = spring(
                                dampingRatio = 0.62f,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        rightEdge.animateTo(
                            targetValue = right,
                            animationSpec = spring(
                                dampingRatio = 0.86f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(x = leftEdge.value, y = (BarHeight - IndicatorHeight) / 2)
                    .width((rightEdge.value - leftEdge.value).coerceAtLeast(0.dp))
                    .height(IndicatorHeight)
                    // 胶囊自己再带一点微投影，浅色背景下才看得出它是「浮」在玻璃上的一层
                    .shadow(
                        elevation = 3.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.14f)
                    )
                    .background(indicatorFill, shape)
                    .drawWithContent {
                        drawContent()
                        val stroke = 1.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (dark) 0.18f else 0.75f),
                                    Color.Black.copy(alpha = if (dark) 0.10f else 0.06f)
                                )
                            ),
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius((size.height - stroke) / 2f),
                            style = Stroke(width = stroke)
                        )
                    }
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = tab.route == selectedRoute
                    val progress by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(durationMillis = 260),
                        label = "tabProgress"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 260),
                        label = "tabColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelect(tab.route) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    val scale = 0.94f + 0.06f * progress
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
