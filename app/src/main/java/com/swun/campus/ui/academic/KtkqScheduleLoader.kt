package com.swun.campus.ui.academic

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.web.WebCookieSync
import kotlinx.coroutines.delay

/**
 * 一个只负责取数的隐藏 WebView，不复用会拦截返回键、画进度/错误层的 CampusWebView。
 *
 * ktkq 的接口带瑞数 WAF 动态票据，原生 OkHttp 很容易 412；页面上下文里的
 * 相对 fetch 会自动带票据，所以这里只加载一次官方移动端页面，拿到 JSON 就销毁。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KtkqScheduleLoader(vm: AppViewModel) {
    val context = LocalContext.current
    val bridge = remember {
        object {
            @JavascriptInterface
            fun onSchedule(json: String) {
                vm.acceptKtkqSchedule(json)
            }

            @JavascriptInterface
            fun onError(message: String) {
                // 页面还在 about:blank / 早期文档时注入会报无法解析相对 URL，
                // 这不是最终失败，等真正的 ktkq 文档加载完成后会再次注入。
                if (message.contains("about:blank") ||
                    message.contains("Failed to parse URL") ||
                    message.contains("not_ktyet") ||
                    message.contains("HTTP 412")) {
                    return
                }
                val friendly = when {
                    message.contains("超时") -> message
                    message.contains("HTTP 401") || message.contains("HTTP 403") ->
                        "登录状态已失效，请重新登录后再试"
                    message.contains("HTTP") -> "课堂考勤暂时无法连接，请检查网络后重试"
                    else -> "课堂考勤课表加载失败，请稍后重试"
                }
                vm.failKtkq(friendly)
            }
        }
    }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.WHITE)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = false
                useWideViewPort = false
                loadWithOverviewMode = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // 去掉默认 UA 里的 “; wv”，瑞数 WAF 对 wv 会直接拦截
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            }
            CookieManager.getInstance().let {
                it.setAcceptCookie(true)
                it.setAcceptThirdPartyCookies(this, true)
            }
        }
    }

    LaunchedEffect(vm.ktkqAttempt) {
        vm.beginKtkqLoad()
        WebCookieSync.sync(
            cookies = vm.sessionCookies(),
            hosts = listOf(
                "authserver.swun.edu.cn",
                "ktkq.swun.edu.cn",
                "card.swun.edu.cn",
                "szjy.swun.edu.cn",
                "ehall.swun.edu.cn"
            ),
            clearHosts = emptyList()
        )
        webView.addJavascriptInterface(bridge, "KtkqBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                // 不在这里清错误：页面可能先经过 CAS 跳转
            }

            override fun onPageFinished(view: WebView, url: String?) {
                val pageUrl = view.url.orEmpty()
                if (pageUrl.startsWith("https://ktkq.swun.edu.cn")) {
                    // 官方页面是 SPA，onPageFinished 会先于 WAF 运行时/懒加载业务代码完成；
                    // 等页面稳定后再注入，避免在 about:blank 或半成品文档上 fetch。
                    view.postDelayed({
                        val current = view.url.orEmpty()
                        if (current.startsWith("https://ktkq.swun.edu.cn")) {
                            view.evaluateJavascript(KTKQ_FETCH_SCRIPT, null)
                        }
                    }, 2_500)
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    vm.failKtkq("课堂考勤页面加载失败：${error.description}")
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                response: WebResourceResponse
            ) {
                // 412 是瑞数 WAF 的挑战页，不是最终失败；等页面 JS 完成挑战后会再次加载。
                if (request.isForMainFrame && response.statusCode >= 500) {
                    vm.failKtkq("课堂考勤页面 HTTP ${response.statusCode}，登录态可能已失效")
                }
            }
        }
        webView.loadUrl(CampusUrls.ATTENDANCE_CAS)
    }

    // 官方页面偶尔会卡在登录/WAF 挑战，给用户一个明确失败而不是无限转圈。
    LaunchedEffect(vm.ktkqAttempt) {
        delay(75_000)
        if (vm.ktkqSchedule == null && vm.ktkqLoading) {
            vm.failKtkq("课堂考勤加载超时，请稍后重试")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    // 只需要一个 1dp 的真实 WebView 来执行 JS；不展示官方 H5。
    AndroidView(
        factory = { webView },
        modifier = Modifier.size(1.dp)
    )
}

/**
 * 官方页面自带 WAF 运行时；这里只使用相对路径，不手工拼 l2t2q0Jo。
 * 先取校时拿到当前周次，再拉课程，避免用错周次。
 */
private const val KTKQ_FETCH_SCRIPT = """
(async function () {
  const base = (location.origin && location.origin !== 'null')
    ? location.origin
    : 'https://ktkq.swun.edu.cn';

  async function run(attempt) {
    try {
      if (location.href.indexOf('ktkq.swun.edu.cn') < 0) {
        throw new Error('not_ktyet:' + location.href);
      }

      const timeResp = await fetch(base + '/jwmobile/biz/v410/schedule/school/time', {
        credentials: 'include', headers: { 'Accept': 'application/json' }
      });
      if (!timeResp.ok) throw new Error('校时接口 HTTP ' + timeResp.status);
      const timeText = await timeResp.text();
      const time = JSON.parse(timeText);
      if (!time || !time.data) throw new Error('校时接口没有返回数据');

      const infoResp = await fetch(base + '/jwmobile/biz/user/info', {
        credentials: 'include', headers: { 'Accept': 'application/json' }
      });
      const info = await infoResp.json();

      const body = JSON.stringify({
        xnxqdm: time.data.xnxqdm,
        skzc: time.data.todayWeekNum
      });
      const courseResp = await fetch(base + '/jwmobile/biz/v410/schedule/queryCourseInfo', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: body
      });
      if (!courseResp.ok) throw new Error('课表接口 HTTP ' + courseResp.status);
      const courses = await courseResp.json();
      if (!courses || courses.code !== 200) throw new Error(courses && courses.msg ? courses.msg : '课表接口返回异常');

      KtkqBridge.onSchedule(JSON.stringify({ time: time, info: info, courses: courses }));
    } catch (e) {
      if (attempt < 2 && location.href.indexOf('ktkq.swun.edu.cn') >= 0) {
        await new Promise(function (resolve) { setTimeout(resolve, 1600); });
        return run(attempt + 1);
      }
      KtkqBridge.onError(String(e && e.message ? e.message : e) + ' @ ' + location.href);
    }
  }

  run(0);
})();
"""
