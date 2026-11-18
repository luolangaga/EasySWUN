package com.swun.campus.ui.life

import android.webkit.WebView
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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.web.CampusWebView

/** 进入学生管理平台后，自动点开移动端的「请销假」。 */
private const val LEAVE_ENTER_JS = """
(function () {
  if (location.host.indexOf('zxtjfx.swun.edu.cn') < 0) return;
  if (location.hash.indexOf('/leave') >= 0) return;
  var tries = 0;
  function openLeave() {
    if (location.hash.indexOf('/leave') >= 0) return;
    var best = null, bestArea = 1e9;
    var nodes = document.querySelectorAll('div,span,p,li,button,a');
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      var t = (el.textContent || '').trim();
      if (t === '请销假' && el.offsetParent !== null) {
        var r = el.getBoundingClientRect();
        var area = r.width * r.height;
        if (area > 0 && area < bestArea) { best = el; bestArea = area; }
      }
    }
    if (best) { best.click(); return; }
    if (tries++ < 16) setTimeout(openLeave, 350);
  }
  setTimeout(openLeave, 900);
})()
"""

/**
 * 民大学工 · 请销假。
 *
 * 首页是原生入口，点「去请假」后才在应用内打开学生管理平台的移动端页面。
 * 不跳官方 App，不要求第二次登录；提交和审批结果以学校页面为准。
 */
@Composable
fun LeaveScreen(vm: AppViewModel) {
    var showPage by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("请假申请") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val cookies = remember(vm.loggedIn) { vm.sessionCookies() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = "请销假",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "民大学工 · 学生管理平台",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "请假申请",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "事假、病假、离校报备",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "填写起止时间与事由，提交后由辅导员审批。审批进度和返校销假都在同一页处理。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        pageTitle = "请假申请"
                        showPage = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("去请假", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    pageTitle = "请假记录"
                    showPage = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("请假记录")
            }
            OutlinedButton(
                onClick = {
                    pageTitle = "返校销假"
                    showPage = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("返校销假")
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "办理说明",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "· 沿用 App 的统一身份认证，进入学工不再单独登录。\n" +
                        "· 请假、审批进度和返校销假都在同一页处理。\n" +
                        "· 提交与审批结果在学校系统内完成，本应用不保存表单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPage) {
        Dialog(
            onDismissRequest = { showPage = false },
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
                                text = pageTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "学生管理平台 · 移动端",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { webView?.reload() }) { Text("刷新") }
                        TextButton(onClick = { showPage = false }) { Text("关闭") }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CampusWebView(
                            url = CampusUrls.STUDENT_PLATFORM_SSO,
                            cookies = cookies,
                            cookieHosts = listOf(
                                "authserver.swun.edu.cn",
                                "zxtjfx.swun.edu.cn",
                                "gyglxt.swun.edu.cn"
                            ),
                            clearCookieHosts = emptyList(),
                            mobileLayout = true,
                            injectedScript = LEAVE_ENTER_JS,
                            onWebViewReady = { webView = it },
                            onPageFinished = { view, url ->
                                webView = view
                                if (url.contains("swun-stu-manage-platform-mobile") &&
                                    !url.contains("/leave")
                                ) {
                                    view.evaluateJavascript(LEAVE_ENTER_JS, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
