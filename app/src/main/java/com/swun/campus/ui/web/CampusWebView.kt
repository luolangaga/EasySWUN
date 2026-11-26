package com.swun.campus.ui.web

import android.annotation.SuppressLint
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.swun.campus.ui.common.ErrorState
import kotlinx.coroutines.delay
import okhttp3.Cookie

/**
 * 学校 ykt 系列页面用 `$.myAlert` 弹校验提示，但 function.do 这类页面里
 * 这个助手并不可靠：一调用就抛 “TypeError: $.myAlert is not a function”，
 * 表现在界面上就是点「立即充值」「龙支付」完全没反应。
 *
 * 这层兜底不依赖 jQuery 或 layer，缺什么补什么，用页面内浮层把文字显示出来，
 * 保证任何一次点击至少能看到“它在说什么”，也顺手把后续回调跑完。
 */
private const val PAGE_DIALOG_PATCH_JS = """
(function () {
  if (window.__swunDialogPatch) return;
  window.__swunDialogPatch = true;
  function toast(msg) {
    try {
      var d = document.createElement('div');
      d.style.cssText = 'position:fixed;left:50%;top:46%;transform:translate(-50%,-50%);' +
        'max-width:78%;background:rgba(20,20,20,.86);color:#fff;padding:12px 16px;' +
        'border-radius:12px;font-size:15px;line-height:1.5;z-index:2147483647;' +
        'box-shadow:0 8px 28px rgba(0,0,0,.28);text-align:center;word-break:break-all';
      d.textContent = String(msg === null || msg === undefined ? '' : msg);
      document.body.appendChild(d);
      setTimeout(function () { if (d.parentNode) d.parentNode.removeChild(d); }, 2800);
    } catch (e) {}
  }
  window.myAlert = window.myAlert || function (m, cb) {
    toast(m);
    if (typeof cb === 'function') { try { cb(); } catch (e) {} }
  };
  window.myToast = window.myToast || toast;
  if (window.jQuery) {
    jQuery.myAlert = jQuery.myAlert || window.myAlert;
    jQuery.myToast = jQuery.myToast || toast;
    jQuery.myConfirm = jQuery.myConfirm || function (m, ok) {
      if (typeof m === 'function') { try { m(true); } catch (e) {} return; }
      toast(m);
      if (typeof ok === 'function') { try { ok(); } catch (e) {} }
    };
    jQuery.alert = jQuery.alert || jQuery.myAlert;
  }
})();
"""

/**
 * 把 OkHttp 里的 CAS / 教务 Cookie 同步进 WebView。
 *
 * 学校的一卡通、学工都在瑞数 WAF 后面，原生 HTTP 会拿到 412，只有 WebView 里
 * 真正执行 JS 才能过盾。这里把 authserver 的 CASTGC 等票据复制进 WebView，
 * 再打开 CAS 的 service 地址，就能免密落到一卡通/学工页面。
 */
object WebCookieSync {

    fun sync(cookies: List<Cookie>, hosts: List<String>, clearHosts: List<String> = listOf("authserver.swun.edu.cn")) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)

        // 先清掉 authserver 的旧票，否则旧 CASTGC 和新 CASTGC 会同时存在，
        // CAS 可能取到旧值导致 SSO 失败。
        clearHosts.forEach { host ->
            // Cookie 的 path 不一定是根路径，尤其是 ykt H5 会挂在 /menu 下；
            // 只清根路径会留下旧的 JSESSIONID，和新的官方会话混在一起。
            val paths = (cookies.map { it.path.ifBlank { "/" } } +
                listOf("/", "/menu", "/app", "/authserver")).distinct()
            paths.forEach { path ->
                val url = "https://$host$path"
                cm.getCookie(url)?.split(";")?.forEach { pair ->
                    val name = pair.substringBefore("=").trim()
                    if (name.isNotBlank()) {
                        cm.setCookie(url, "$name=; Max-Age=0; path=$path")
                    }
                }
            }
            // 再清一遍根路径下的 Cookie（一卡通/学工的 JSESSIONID 一般都在 / 下）。
            val root = "https://$host/"
            cm.getCookie(root)?.split(";")?.forEach { pair ->
                val name = pair.substringBefore("=").trim()
                if (name.isNotBlank()) {
                    cm.setCookie(root, "$name=; Max-Age=0; path=/")
                }
            }
        }

        val now = System.currentTimeMillis()
        hosts.forEach { host ->
            cookies.forEach { c ->
                val domain = c.domain.removePrefix(".")
                val matches = domain.equals(host, true) || host.endsWith(".$domain", true)
                if (!matches || c.expiresAt < now) return@forEach
                val path = c.path.ifBlank { "/" }
                val value = buildString {
                    append(c.name).append('=').append(c.value)
                    append("; path=").append(path)
                    if (c.secure) append("; secure")
                    if (c.httpOnly) append("; httponly")
                }
                // 必须用 cookie 自己的 path 作为 URL，否则 CookieManager 会因
                // “cookie path 不是 URL path 的前缀”直接丢弃。
                val setUrl = "https://$host$path"
                cm.setCookie(setUrl, value)
            }
        }
        cm.flush()
    }
}

/**
 * 通用校园 WebView：
 * - 支持注入 CAS Cookie；
 * - 支持 JS Bridge（页面把余额/标题回传给原生）；
 * - 支持进度、错误、标题、URL 回调；
 * - 返回键优先交给网页历史。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CampusWebView(
    url: String,
    modifier: Modifier = Modifier,
    cookies: List<Cookie> = emptyList(),
    cookieHosts: List<String> = listOf(
        "authserver.swun.edu.cn",
        "card.swun.edu.cn",
        "szjy.swun.edu.cn",
        "ehall.swun.edu.cn"
    ),
    clearCookieHosts: List<String> = listOf("authserver.swun.edu.cn"),
    bridgeName: String? = null,
    bridge: Any? = null,
    injectedScript: String? = null,
    /**
     * 一卡通圈存页在模拟器 GPU 合成下会白屏，单独走软件图层。
     * 二维码页必须保持硬件渲染，否则二维码不出来。
     */
    softwareLayer: Boolean = false,
    /** 学工移动端按手机宽度排版，避免桌面版视口把表单压成 PC 页面。 */
    mobileLayout: Boolean = false,
    /**
     * 瑞数 WAF 会先返回 412/403 的挑战页，页面 JS 过关后才会加载真正的业务页。
     * 这些状态码不能当致命错误盖住页面。
     */
    ignoreHttpCodes: Set<Int> = emptySet(),
    /**
     * 在页面脚本执行前注入。用于一卡通 H5 的 MCK 桥：
     * 官方页面在 onload 里立即调用 MCK.auth / sdk.h5.call("getUserInfo")，
     * 等到 onPageFinished 再注入就来不及了。
     */
    preloadScript: String? = null,
    onWebViewReady: ((WebView) -> Unit)? = null,
    onPageFinished: ((WebView, String) -> Unit)? = null,
    onUrlChanged: ((String) -> Unit)? = null,
    onTitleChanged: ((String) -> Unit)? = null,
    onProgressChanged: ((Int) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.WHITE)
            // 部分学校 H5（一卡通圈存页）在模拟器 GPU 合成下会渲染成白屏，
            // 改走软件图层后 DOM 与表单可正常显示。
            if (softwareLayer) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setGeolocationEnabled(true)
                loadsImagesAutomatically = true
                useWideViewPort = !mobileLayout
                loadWithOverviewMode = !mobileLayout
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                // 去掉 Android WebView 默认 UA 里的 “; wv”，部分瑞数 WAF 会对 wv 直接拦截
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                // 微信 / 支付宝收银台常用 window.open、target=_blank 开新窗口，
                // 不打开多窗口支持，支付按钮会“点了没反应”。
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
            }
            CookieManager.getInstance().let { cm ->
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)
            }

            // 非 http(s) 链接（weixin://、alipays://、intent://…）交给系统处理；
            // 打不开时给出明确提示，而不是静默失败。
            fun openTarget(view: WebView, target: String): Boolean {
                if (target.isBlank() || target.startsWith("about:")) return true
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    view.loadUrl(target)
                    return true
                }
                val launched = try {
                    val intent = Intent.parseUri(target, Intent.URI_INTENT_SCHEME)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                } catch (_: Exception) {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(target))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                if (!launched) {
                    val scheme = target.substringBefore(':')
                    val appName = when {
                        scheme.startsWith("weixin") -> "微信"
                        scheme.startsWith("alipay") -> "支付宝"
                        else -> "对应的 App"
                    }
                    Toast.makeText(
                        context,
                        "没有能打开 $scheme:// 的应用。装好${appName}再试，或改用二维码扫码支付。",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (target.startsWith("weixin") || target.startsWith("alipay")) {
                    // 支付跳转不能静默：用户需要知道刚才那一下去了哪里、付完怎么回来。
                    val appName = if (target.startsWith("weixin")) "微信" else "支付宝"
                    Toast.makeText(
                        context,
                        "正在打开${appName}完成支付，付完返回本页刷新即可",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    error = null
                    progress = 0
                    onUrlChanged?.invoke(url.orEmpty())
                    // 必须早于页面 inline script；官方一卡通 H5 在 DOM 解析阶段就会
                    // 调 MCK.auth 和 getUserInfo，晚一步就只剩白屏。
                    preloadScript?.let { view.evaluateJavascript(it, null) }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    progress = 100
                    onUrlChanged?.invoke(url.orEmpty())
                    view.evaluateJavascript(PAGE_DIALOG_PATCH_JS, null)
                    onPageFinished?.invoke(view, url.orEmpty())
                    injectedScript?.let { view.evaluateJavascript(it, null) }
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    onUrlChanged?.invoke(url.orEmpty())
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    webError: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        val raw = webError.description?.toString()?.takeIf { it.isNotBlank() }
                            ?: "页面加载失败（网络不可用）"
                        // 部分学校老站点（如 ehall）握手带 TLS 重协商，Android WebView
                        // 会直接报 ERR_SSL_PROTOCOL_ERROR；把原始 chromium 错误翻成
                        // 用户能理解的话，并提示改用系统浏览器兼容打开。
                        error = if (raw.contains("ERR_SSL_PROTOCOL_ERROR")) {
                            "该站点与 Android WebView 的安全连接不兼容（ERR_SSL_PROTOCOL_ERROR），请点右上角「浏览器打开」重试"
                        } else {
                            raw
                        }
                        progress = 100
                        onError?.invoke(error.orEmpty())
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse
                ) {
                    if (request.isForMainFrame) {
                        val code = errorResponse.statusCode
                        if (code >= 400 && code !in ignoreHttpCodes) {
                            error = "页面返回 HTTP $code，可能是登录态失效或服务暂不可用"
                            progress = 100
                            onError?.invoke(error.orEmpty())
                        }
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest?
                ): Boolean {
                    val target = request?.url?.toString().orEmpty()
                    if (target.startsWith("http://") || target.startsWith("https://")) return false
                    return openTarget(view, target)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress
                    onProgressChanged?.invoke(newProgress)
                }

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message
                ): Boolean {
                    // window.open / target=_blank：把新窗口的地址接回当前 WebView，
                    // 微信支付的收银台就是走这条路，不接管就会“点了没反应”。
                    val temp = WebView(view.context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                v: WebView,
                                r: WebResourceRequest?
                            ): Boolean {
                                openTarget(view, r?.url?.toString().orEmpty())
                                return true
                            }
                        }
                    }
                    val transport = resultMsg.obj as WebView.WebViewTransport
                    transport.webView = temp
                    resultMsg.sendToTarget()
                    return true
                }

                override fun onReceivedTitle(view: WebView, title: String) {
                    onTitleChanged?.invoke(title)
                }

                // WebView 默认吞掉 alert()；学校的 H5 大量用 alert/$.myAlert 做校验，
                // 不接管就是“点了没反应”。
                override fun onJsAlert(
                    view: WebView,
                    url: String?,
                    message: String?,
                    result: JsResult
                ): Boolean {
                    if (message.isNullOrBlank()) {
                        result.confirm()
                        return true
                    }
                    AlertDialog.Builder(view.context)
                        .setMessage(message)
                        .setPositiveButton("知道了") { _, _ -> result.confirm() }
                        .setOnCancelListener { result.cancel() }
                        .show()
                    return true
                }

                override fun onJsConfirm(
                    view: WebView,
                    url: String?,
                    message: String?,
                    result: JsResult
                ): Boolean {
                    AlertDialog.Builder(view.context)
                        .setMessage(message.orEmpty())
                        .setPositiveButton("确定") { _, _ -> result.confirm() }
                        .setNegativeButton("取消") { _, _ -> result.cancel() }
                        .setOnCancelListener { result.cancel() }
                        .show()
                    return true
                }

                override fun onJsPrompt(
                    view: WebView,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult
                ): Boolean {
                    val input = android.widget.EditText(view.context).apply {
                        setText(defaultValue.orEmpty())
                    }
                    AlertDialog.Builder(view.context)
                        .setMessage(message.orEmpty())
                        .setView(input)
                        .setPositiveButton("确定") { _, _ -> result.confirm(input.text.toString()) }
                        .setNegativeButton("取消") { _, _ -> result.cancel() }
                        .setOnCancelListener { result.cancel() }
                        .show()
                    return true
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String,
                    callback: GeolocationPermissions.Callback
                ) {
                    // 只有 Android 已授予定位权限才向网页放行，避免地图页拿到空坐标。
                    val fine = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    callback.invoke(origin, fine || coarse, false)
                }
            }
        }
    }

    LaunchedEffect(url, reloadKey) {
        error = null
        progress = 0
        WebCookieSync.sync(cookies, cookieHosts, clearCookieHosts)
        if (bridgeName != null && bridge != null) {
            webView.addJavascriptInterface(bridge, bridgeName)
        }
        onWebViewReady?.invoke(webView)
        webView.loadUrl(url)
    }

    LaunchedEffect(url, reloadKey, progress) {
        if (error == null && progress < 100) {
            delay(30_000)
            if (error == null && progress < 100) {
                error = "页面加载超时，请检查网络或登录态后重试"
                onError?.invoke(error.orEmpty())
            }
        }
    }

    BackHandler(enabled = webView.canGoBack()) {
        webView.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        if (progress in 1..99) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        if (error != null) {
            ErrorState(
                title = "网页加载失败",
                message = error.orEmpty(),
                onRetry = {
                    error = null
                    progress = 0
                    reloadKey++
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (progress in 0..0 && error == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
