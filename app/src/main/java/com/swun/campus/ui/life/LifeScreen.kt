package com.swun.campus.ui.life

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.Routes
import com.swun.campus.ui.common.LiquidGlassBarContentPadding
import com.swun.campus.ui.web.CampusWebView

@Composable
fun LifeScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    var officialUrl by remember { mutableStateOf<String?>(null) }
    var officialTitle by remember { mutableStateOf("官方页面") }
    var officialScript by remember { mutableStateOf<String?>(null) }
    var officialWebView by remember { mutableStateOf<WebView?>(null) }

    val context = LocalContext.current
    val startCheckIn: () -> Unit = {
        officialTitle = "晚归打卡"
        // 直接进学校官方的定位打卡页；不再先停在学生平台首页、
        // 等脚本去找“定位打卡”入口，用户点一次就到位。
        officialUrl = CampusUrls.CHECKIN_PAGE
        officialScript = null
    }
    val locationGranted: () -> Boolean = {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            locationGranted()
        if (granted) {
            startCheckIn()
        } else {
            Toast.makeText(
                context,
                "晚归打卡需要定位权限，请在系统设置中允许定位",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Text(
            text = "校园生活",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "一卡通、缴费、校车与学工事务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        LifeFeatureCard(
            title = "校园一卡通",
            subtitle = "二维码 · 余额 · 在线充值",
            icon = Icons.Outlined.CreditCard,
            onClick = { onOpen(Routes.CARD) }
        )

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            LifeSmallCard(
                title = "晚归打卡",
                subtitle = "定位 · 官方打卡",
                icon = Icons.Outlined.MyLocation,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (locationGranted()) {
                        startCheckIn()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )
            Spacer(Modifier.width(12.dp))
            LifeSmallCard(
                title = "请销假",
                subtitle = "民大学工 · 请假/销假",
                icon = Icons.Outlined.Assignment,
                modifier = Modifier.weight(1f),
                onClick = {
                    officialTitle = "请销假"
                    // 直接落到学生平台手机版的请销假页，不再先展示平台首页。
                    officialUrl = CampusUrls.LEAVE_MOBILE
                    officialScript = null
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "缴费与出行",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            LifeSmallCard(
                title = "电费充值",
                subtitle = "宿舍电费 · 官方充值",
                icon = Icons.Outlined.Bolt,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Routes.ELECTRIC) }
            )
            Spacer(Modifier.width(12.dp))
            LifeSmallCard(
                title = "淋浴充值",
                subtitle = "热水 / 淋浴 · 官方充值",
                icon = Icons.Outlined.WaterDrop,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Routes.WATER) }
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            LifeSmallCard(
                title = "校车时刻",
                subtitle = "三校区通勤 · 下一班",
                icon = Icons.Outlined.DirectionsBus,
                modifier = Modifier.weight(1f),
                onClick = { onOpen(Routes.BUS) }
            )
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "一卡通、电费、淋浴和账单均走学校 ykt 官方数据，缴费提交也在学校官方页面完成；本应用不保存本地记录。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(LiquidGlassBarContentPadding))
    }

    val url = officialUrl
    if (url != null) {
        Dialog(
            onDismissRequest = { officialUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = officialTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "学校官方页面 · 应用内",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { officialWebView?.reload() }) { Text("刷新") }
                        TextButton(onClick = { officialUrl = null }) { Text("关闭") }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CampusWebView(
                            url = url,
                            cookies = vm.sessionCookies(),
                            cookieHosts = listOf(
                                "authserver.swun.edu.cn",
                                "zxtjfx.swun.edu.cn",
                                "gyglxt.swun.edu.cn"
                            ),
                            clearCookieHosts = emptyList(),
                            mobileLayout = true,
                            ignoreHttpCodes = setOf(403, 412),
                            injectedScript = officialScript,
                            onWebViewReady = { officialWebView = it }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 官方学生平台首页有「定位打卡 / 请销假」入口。
 * 进入官方页后直接点中对应入口，不再多经过一层自绘页面。
 */
private fun clickByTextScript(target: String): String {
    val escaped = target.replace("\\", "\\\\").replace("\"", "\\\"")
    return """
        (function () {
          var target = "$escaped";
          var tries = 0;
          function go() {
            var nodes = document.querySelectorAll('div,span,p,li,button,a');
            var best = null;
            var bestArea = 1e9;
            for (var i = 0; i < nodes.length; i++) {
              var el = nodes[i];
              var text = (el.textContent || '').trim();
              if (text === target && el.offsetParent !== null) {
                var r = el.getBoundingClientRect();
                var area = r.width * r.height;
                if (area > 0 && area < bestArea) { best = el; bestArea = area; }
              }
            }
            if (best) { best.click(); return; }
            if (tries++ < 20) setTimeout(go, 350);
          }
          setTimeout(go, 1000);
        })();
    """
}

@Composable
private fun LifeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun LifeSmallCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(142.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
