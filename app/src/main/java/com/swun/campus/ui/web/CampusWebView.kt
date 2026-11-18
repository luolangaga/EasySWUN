package com.swun.campus.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.swun.campus.ui.common.ErrorState
import kotlinx.coroutines.delay
import okhttp3.Cookie

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
                listOf("/", "/menu", "/app")).distinct()
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
            }
            CookieManager.getInstance().let { cm ->
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)
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
                        error = webError.description?.toString()?.takeIf { it.isNotBlank() }
                            ?: "页面加载失败（网络不可用）"
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
                    return try {
                        val intent = Intent.parseUri(target, Intent.URI_INTENT_SCHEME)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        true
                    } catch (_: Exception) {
                        true
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress
                    onProgressChanged?.invoke(newProgress)
                }

                override fun onReceivedTitle(view: WebView, title: String) {
                    onTitleChanged?.invoke(title)
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String,
                    callback: GeolocationPermissions.Callback
                ) {
                    callback.invoke(origin, true, false)
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
