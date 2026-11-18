package com.swun.campus.ui.academic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.web.CampusWebView

/**
 * 课堂考勤官方移动端。
 *
 * 直接加载 ktkq 手机端，并继承 App 当前的 CAS Cookie；不跳转外部浏览器，
 * 也不再从 AIOCP 的 PC 端口进入。加载中转圈、失败可重试由 CampusWebView 统一处理。
 */
@Composable
fun AttendanceWebScreen(vm: AppViewModel) {
    CampusWebView(
        // 统一从 CAS 进入 ktkq，确保 WebView 自己拿到课堂考勤的登录态；
        // 进来后就是官方移动端页面，不再经过原生课表/弹窗这一层。
        url = CampusUrls.ATTENDANCE_CAS,
        modifier = Modifier.fillMaxSize(),
        cookies = vm.sessionCookies(),
        cookieHosts = listOf(
            "authserver.swun.edu.cn",
            "ktkq.swun.edu.cn",
            "jwxt.swun.edu.cn"
        ),
        clearCookieHosts = emptyList(),
        mobileLayout = true,
        softwareLayer = true,
        ignoreHttpCodes = setOf(403, 412)
    )
}
