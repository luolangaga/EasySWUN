package com.swun.campus.ui.life

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swun.campus.AppViewModel
import com.swun.campus.ui.web.CampusWebView

/**
 * 电费、淋浴、账单这类学校官方 H5。
 *
 * 不重画表单，也不伪造提交流程：外层保留原生标题/刷新，内容完整嵌入官方页，
 * 支付与账单结果以学校 ykt 系统为准。ykt 的 H5 需要先经过官方 function.do，
 * 这里复用一卡通那套学生信息跳转脚本，避免白屏。
 */
@Composable
fun UtilityScreen(
    vm: AppViewModel,
    title: String,
    subtitle: String,
    url: String
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    // 记录当前页面地址，供「在浏览器打开」兜底（微信支付在系统浏览器里能正常拉起微信）。
    var currentUrl by remember { mutableStateOf(url) }
    val context = LocalContext.current

    val studentNo = (vm.profile?.studentNo ?: vm.savedAccount).filter { it.isDigit() }
    val redirectScript = remember(studentNo) { yktMenuRedirectScript(studentNo) }
    val authCookies = remember(vm.loggedIn) {
        vm.sessionCookies().filter { cookie ->
            val domain = cookie.domain.removePrefix(".").lowercase()
            domain == "authserver.swun.edu.cn" || domain == "swun.edu.cn"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        val target = currentUrl.ifBlank { url }
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(target))
                            )
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.OpenInBrowser,
                        contentDescription = "在浏览器打开",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { webView?.reload() }) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "刷新",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            CampusWebView(
                url = url,
                cookies = authCookies,
                cookieHosts = listOf(
                    "authserver.swun.edu.cn",
                    "ykth5.swun.edu.cn"
                ),
                clearCookieHosts = emptyList(),
                ignoreHttpCodes = setOf(403, 412),
                mobileLayout = true,
                softwareLayer = false,
                // 与付款码/充值一致：不要 preload 提前跳转。
                // 官方页的 window.timestamp 是页面加载过程中才建立的，
                // onPageStarted 就 location.replace 会停在空白页。
                injectedScript = redirectScript,
                onWebViewReady = { webView = it },
                onUrlChanged = { currentUrl = it }
            )
        }
    }
}
