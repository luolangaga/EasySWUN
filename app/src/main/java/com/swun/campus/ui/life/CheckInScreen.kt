package com.swun.campus.ui.life

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.common.RetryRow
import com.swun.campus.ui.web.CampusWebView
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 生活页的「晚归定位打卡」。
 *
 * 原生页只负责定位、时段提示和进入官方页面；真正提交发生在学校
 * 学生管理平台的官方打卡页内，不在本地伪造打卡记录。
 */

@Composable
fun CheckInScreen(vm: AppViewModel) {
    val context = LocalContext.current

    var location by remember { mutableStateOf<Location?>(null) }
    var address by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var locatingError by remember { mutableStateOf<String?>(null) }
    var showOfficialPage by remember { mutableStateOf(false) }
    var officialWebView by remember { mutableStateOf<WebView?>(null) }
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    var permissionDenied by remember { mutableStateOf(false) }

    val startLocate: () -> Unit = {
        if (!hasLocationPermission(context)) {
            permissionGranted = false
            permissionDenied = true
            locatingError = "没有定位权限，请先授权"
        } else {
            permissionGranted = true
            permissionDenied = false
            locating = true
            locatingError = null
            LocationHelper(context).locate { loc ->
                locating = false
                if (loc == null) {
                    locatingError = "暂时拿不到定位，请到空旷处或开启 GPS 后重试"
                } else {
                    location = loc
                    address = "定位成功 · 校内"
                    locatingError = null
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionGranted = granted
        permissionDenied = !granted
        if (granted) startLocate()
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            startLocate()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val distance = remember(location) {
        val loc = location ?: return@remember null
        val out = FloatArray(1)
        Location.distanceBetween(
            loc.latitude,
            loc.longitude,
            CampusUrls.CAMPUS_LAT,
            CampusUrls.CAMPUS_LNG,
            out
        )
        out[0]
    }

    val now = LocalTime.now()
    val inWindow = !now.isBefore(LocalTime.of(21, 30)) && !now.isAfter(LocalTime.of(23, 25))
    val timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val displayName = vm.profile?.name?.takeIf { it.isNotBlank() } ?: vm.savedName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "晚归打卡",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "生活服务 · 学生管理平台",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = if (inWindow) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = if (inWindow) "打卡时段" else "未到时段",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inWindow) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (displayName.isNotBlank()) "$displayName · 当前位置" else "当前位置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = when {
                                locating -> "正在定位…"
                                location != null -> "定位成功"
                                else -> "尚未获取定位"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (location != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (locating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        TextButton(onClick = startLocate) { Text("重新定位") }
                    }
                }

                if (location != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            distance == null -> ""
                            distance < 2000 -> "距校区中心约 ${distance.toInt()} 米 · 在校园范围内"
                            else -> "距校区中心约 ${String.format(Locale.CHINA, "%.1f", distance / 1000)} 公里 · 离校区较远"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (locatingError != null && !locating) {
                    Spacer(Modifier.height(10.dp))
                    RetryRow(
                        message = locatingError.orEmpty(),
                        onRetry = startLocate
                    )
                }

                if (!permissionGranted) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(if (permissionDenied) "去开启定位权限" else "开启定位权限")
                    }
                    if (permissionDenied) {
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("已拒绝？到系统设置里授权", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "当前 $timeText · 参考 21:30–23:25",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "实际时段以学院/辅导员通知为准；提交结果以学校官方页面为准。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { if (location != null) showOfficialPage = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = location != null
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("打开官方打卡", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (location == null) "定位成功后才能打开官方打卡页" else "在应用内进入学校官方打卡页，不保存本地记录",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 18.dp)
        )
    }

    if (showOfficialPage) {
        Dialog(
            onDismissRequest = { showOfficialPage = false },
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
                                text = "晚归打卡",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "学校官方页面 · 应用内",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { officialWebView?.reload() }
                        ) { Text("刷新") }
                        TextButton(onClick = { showOfficialPage = false }) { Text("关闭") }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CampusWebView(
                            url = CampusUrls.CHECKIN_PAGE,
                            cookies = vm.sessionCookies(),
                            cookieHosts = listOf(
                                "authserver.swun.edu.cn",
                                "zxtjfx.swun.edu.cn",
                                "gyglxt.swun.edu.cn"
                            ),
                            clearCookieHosts = emptyList(),
                            mobileLayout = true,
                            onWebViewReady = { officialWebView = it }
                        )
                    }
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

/**
 * 优先用最后一次已知位置；没有就请求一次单次定位，8 秒超时退回已知位置。
 */
private class LocationHelper(private val context: Context) {

    private val manager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())

    fun locate(onResult: (Location?) -> Unit) {
        if (!hasLocationPermission(context)) {
            onResult(null)
            return
        }

        var best: Location? = null
        listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).forEach { provider ->
            try {
                if (provider == LocationManager.PASSIVE_PROVIDER || manager.isProviderEnabled(provider)) {
                    val last = manager.getLastKnownLocation(provider)
                    if (last != null && (best == null || last.time > best!!.time)) best = last
                }
            } catch (_: Exception) {
            }
        }

        if (best != null && System.currentTimeMillis() - best!!.time < 5 * 60_000L) {
            onResult(best)
            return
        }

        var finished = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (finished) return
                finished = true
                manager.removeUpdates(this)
                handler.removeCallbacksAndMessages(null)
                onResult(location)
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: Exception) {
            onResult(best)
            return
        }

        handler.postDelayed({
            if (!finished) {
                finished = true
                runCatching { manager.removeUpdates(listener) }
                onResult(best)
            }
        }, 8_000L)
    }
}
