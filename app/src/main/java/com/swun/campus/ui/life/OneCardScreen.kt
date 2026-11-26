package com.swun.campus.ui.life

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.Routes
import com.swun.campus.ui.common.RetryRow
import com.swun.campus.ui.web.CampusWebView
import kotlinx.coroutines.delay

/**
 * 一卡通页面里的余额抓取。
 *
 * 一卡通后台是 Struts 老页面：
 * - 登录后的首页（casLogin.action）不直接显示余额；
 * - 余额在 /queryUserBalances.action 里，格式是“校园卡余额：32.89元”。
 * 因此这里在登录页里用同源 fetch 主动取一次余额，再回传给原生。
 */
private const val CARD_SCAN_JS = """
(function () {
  try {
    var body = (document.body && document.body.innerText) || '';
    // 只认页面里真正的“校园卡余额：xx.xx 元”，避免把版权年份 2019 当成余额。
    var direct = body.match(/校园卡余额[：:\s]*([0-9]+(?:\.[0-9]+)?)/);
    if (direct) { AndroidCard.postBalance(direct[1]); }
    var path = (location.pathname || '');
    if (path.indexOf('casLogin') >= 0 || path === '/' || path.indexOf('systemInit') >= 0 || path.indexOf('queryUserInfo') >= 0) {
      if (!window.__cardBalanceFetch) {
        window.__cardBalanceFetch = true;
        var attempt = 0;
        var tryFetch = function () {
          fetch('/queryUserBalances.action', {credentials: 'include'})
            .then(function (r) { return r.text(); })
            .then(function (html) {
              var text = html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ');
              var m2 = text.match(/校园卡余额[：:\s]*([0-9]+(?:\.[0-9]+)?)/);
              if (m2) {
                AndroidCard.postBalance(m2[1]);
              } else if (attempt < 2) {
                attempt++;
                setTimeout(tryFetch, 1500);
              } else if (path.indexOf('queryUserBalances') < 0) {
                // fetch 拿到的是 WAF 挑战页时，直接落到余额页，让页面自己完成校验。
                window.location.href = '/queryUserBalances.action';
              } else {
                AndroidCard.postError('余额接口返回异常，可点重试');
              }
            })
            .catch(function (e) {
              if (attempt < 2) {
                attempt++;
                setTimeout(tryFetch, 1500);
              } else if (path.indexOf('queryUserBalances') < 0) {
                window.location.href = '/queryUserBalances.action';
              } else {
                AndroidCard.postError('余额读取失败，请重试');
              }
            });
        };
        tryFetch();
      }
    }
    AndroidCard.postInfo(body.substring(0, 220));
  } catch (e) {
    AndroidCard.postError('页面脚本异常，请重试');
  }
})();
"""

/**
 * ykt H5 的官方页面在 onload 里用 MCK 原生桥拿 getUserInfo；
 * 独立 WebView 没有这个桥会白屏。这里在页面加载完后，
 * 用当前登录学生的信息走官方页面自己的 function.do 跳转。
 *
 * 只在 /menu/menu.do 上执行，避免 function.do 页面再次跳转。
 */
private const val YKT_MENU_REDIRECT_JS_TEMPLATE = """
(function () {
  try {
    if ((location.pathname || '').indexOf('/menu/menu.do') < 0) return;
    var tries = 0;
    function go() {
      if ((location.pathname || '').indexOf('/menu/menu.do') < 0) return;
      var ts = window.timestamp;
      if (!ts && tries < 20) {
        tries++;
        setTimeout(go, 300);
        return;
      }
      if (!ts) return;
      var m = (location.search.match(/[?&]menu=([^&]+)/) || [])[1] || window.menu || 'qrcode';
      var stu = '__STU__';
      if (!stu) return;
      location.replace('/menu/function.do?expire=' + encodeURIComponent(ts) +
        '&stu_code=' + encodeURIComponent(stu) +
        '&acco_id=' + encodeURIComponent(stu) +
        '&school_id=swun&menu=' + encodeURIComponent(m) +
        '&client_id=swun-ykt-2' +
        '&redirect_uri=' + encodeURIComponent('http://211.83.240.32/menu/enter.do'));
    }
    go();
  } catch (e) {}
})();
"""

private class CardJsBridge(
    private val onBalance: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onInfo: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postBalance(value: String) {
        main.post { onBalance(value) }
    }

    @JavascriptInterface
    fun postError(value: String) {
        main.post { onError(value) }
    }

    @JavascriptInterface
    fun postInfo(value: String) {
        main.post { onInfo(value) }
    }
}

/** 从一卡通付款码页的正文里取动态码，交给原生二维码渲染。 */
private const val PAY_QR_SCAN_JS = """
(function () {
  function scan() {
    try {
      var text = (document.body && document.body.innerText) || '';
      text = text.replace(/\r/g, '');
      var lines = text.split('\n');
      for (var i = 0; i < lines.length; i++) {
        var s = lines[i].trim();
        if (/^[0-9A-Fa-f]{12,64}$/.test(s)) {
          AndroidPay.postQr(s);
          return;
        }
      }
      var m = text.match(/([0-9A-Fa-f]{12,64})/);
      if (m) { AndroidPay.postQr(m[1]); return; }
    } catch (e) {}
  }
  if (window.__swunPayQrTimer) clearInterval(window.__swunPayQrTimer);
  setTimeout(scan, 600);
  setTimeout(scan, 1800);
  setTimeout(scan, 4000);
  window.__swunPayQrTimer = setInterval(scan, 5000);
})()
"""

/**
 * 付款码页只有真正画出二维码后才撤掉加载遮罩。
 * 不能一 onPageFinished 就当成功：官方 H5 会先经过 CAS、menu.do、
 * function.do 几个中间页，任何一页 finished 时都可能还没有二维码。
 */
private const val QR_READY_PROBE_JS = """
(function () {
  function hasBodyCode() {
    try {
      var text = (document.body && document.body.innerText) || '';
      return /(^|[^0-9A-Fa-f])[0-9A-Fa-f]{12,64}([^0-9A-Fa-f]|$)/.test(text);
    } catch (e) { return false; }
  }
  function hasQrGraphic() {
    try {
      var imgs = document.images || [];
      for (var i = 0; i < imgs.length; i++) {
        var src = imgs[i].src || '';
        if (imgs[i].width >= 80 && imgs[i].height >= 80 &&
            (/qrcode|qr|code|pay/i.test(src) || imgs[i].width >= 140)) {
          return true;
        }
      }
      var canvases = document.querySelectorAll('canvas');
      for (var j = 0; j < canvases.length; j++) {
        if (canvases[j].width >= 120 && canvases[j].height >= 120) return true;
      }
      var svgs = document.querySelectorAll('svg');
      for (var k = 0; k < svgs.length; k++) {
        if (svgs[k].clientWidth >= 120 && svgs[k].clientHeight >= 120) return true;
      }
    } catch (e) {}
    return false;
  }
  function notify() {
    try { AndroidQr.postReady(); } catch (e) {}
  }
  if (hasBodyCode() || hasQrGraphic()) { notify(); return; }
  var tries = 0;
  var timer = setInterval(function () {
    tries++;
    if (hasBodyCode() || hasQrGraphic()) {
      clearInterval(timer);
      notify();
    } else if (tries >= 120) {
      clearInterval(timer);
    }
  }, 500);
})()
"""

private class QrReadyBridge(private val onReady: () -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postReady() {
        main.post { onReady() }
    }
}

private class PayCodeBridge(
    private val onQr: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postQr(value: String) {
        main.post { onQr(value) }
    }

    @JavascriptInterface
    fun postError(value: String) {
        main.post { onError(value) }
    }
}


private fun qrBitmap(text: String, sizePx: Int = 720): android.graphics.Bitmap? = runCatching {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            pixels[y * sizePx + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
}.getOrNull()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 付款码弹窗期间把屏幕亮度拉满并保持常亮。
 * 很多 POS 扫不出码不是二维码画错了，而是系统自动亮度把屏幕压得太暗，
 * 摄像头对焦/曝光不足时就会一直识别失败。
 */
@Composable
private fun PaymentDialogBrightness(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        if (!enabled) return@DisposableEffect onDispose { }

        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val activityWindow = view.context.findActivity()?.window
        val windows = listOfNotNull(dialogWindow, activityWindow).distinct()
        val originals = windows.map { it to it.attributes.screenBrightness }

        windows.forEach { window ->
            val attrs = window.attributes
            attrs.screenBrightness = 1.0f
            window.attributes = attrs
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            originals.forEach { (window, original) ->
                val attrs = window.attributes
                attrs.screenBrightness = original
                window.attributes = attrs
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OneCardScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    var balance by remember { mutableStateOf<String?>(null) }
    var balanceStarted by remember { mutableStateOf(false) }
    var balanceLoading by remember { mutableStateOf(false) }
    var balanceError by remember { mutableStateOf<String?>(null) }
    var pageTitle by remember { mutableStateOf("正在连接一卡通…") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var qrStarted by remember { mutableStateOf(true) }
    var qrLoading by remember { mutableStateOf(true) }
    var qrReady by remember { mutableStateOf(false) }
    var qrError by remember { mutableStateOf<String?>(null) }
    var payWebView by remember { mutableStateOf<WebView?>(null) }

    val qrBridge = remember {
        QrReadyBridge(
            onReady = {
                qrLoading = false
                qrReady = true
                qrError = null
            }
        )
    }

    val retryQr: () -> Unit = {
        qrReady = false
        qrError = null
        qrLoading = true
        payWebView?.clearCache(true)
        payWebView?.loadUrl(CampusUrls.CARD_PAYCODE)
    }

    // 只把 authserver 的 CAS 登录态同步给 WebView；card / ykt 的业务会话各自建立，
    // 避免余额和付款码复用同一个 JSESSIONID / 旧票据。
    val cookies = remember(vm.loggedIn) {
        vm.sessionCookies().filter { cookie ->
            val domain = cookie.domain.removePrefix(".").lowercase()
            domain == "authserver.swun.edu.cn" || domain == "swun.edu.cn"
        }
    }
    val bridge = remember {
        CardJsBridge(
            onBalance = { value ->
                if (value.isNotBlank()) {
                    balance = value
                    balanceLoading = false
                    balanceError = null
                }
            },
            onError = { value ->
                balanceLoading = false
                balanceError = value.ifBlank { "余额读取失败，请重试" }
            },
            onInfo = { value ->
                pageTitle = pageTitle.takeIf { it != "正在连接一卡通…" } ?: "一卡通"
                if (balance == null && value.startsWith("FETCHERR")) {
                    balanceLoading = false
                    balanceError = "余额读取失败，请重试"
                }
            }
        )
    }

    val profile = vm.profile
    val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: vm.savedName
    val displayNo = profile?.studentNo?.takeIf { it.isNotBlank() } ?: vm.savedAccount
    val yktStudentNo = displayNo.filter { it.isDigit() }
    val yktRedirectJs = remember(yktStudentNo) {
        YKT_MENU_REDIRECT_JS_TEMPLATE.replace("__STU__", yktStudentNo)
    }
    val retryBalance: () -> Unit = {
        balanceStarted = true
        balanceLoading = true
        balanceError = null
        webView?.reload()
        Unit
    }

    // 付款码优先：先把官方二维码 WebView 拉起来并画出二维码，
    // 二维码 ready（或失败/超时）后再挂余额 WebView 去读余额。
    LaunchedEffect(qrReady, qrError, qrStarted) {
        if ((qrReady || qrError != null) && !balanceStarted) {
            balanceStarted = true
            balanceLoading = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (balanceStarted) {
                CampusWebView(
                url = CampusUrls.CARD_SSO,
                cookies = cookies,
                cookieHosts = listOf("authserver.swun.edu.cn", "card.swun.edu.cn"),
                // 余额只使用 card 自己的业务会话；每次进入重新建立，避免和
                // 付款码/充值共用旧的 card JSESSIONID。
                clearCookieHosts = listOf("card.swun.edu.cn"),
                ignoreHttpCodes = setOf(403, 412),
                bridgeName = "AndroidCard",
                bridge = bridge,
                injectedScript = CARD_SCAN_JS,
                onWebViewReady = { webView = it },
                onPageFinished = { view, _ ->
                    webView = view
                    view.postDelayed({ view.evaluateJavascript(CARD_SCAN_JS, null) }, 2500)
                    view.postDelayed({ view.evaluateJavascript(CARD_SCAN_JS, null) }, 6000)
                    view.postDelayed({
                        if (balance == null && balanceLoading) {
                            balanceLoading = false
                            balanceError = "已登录但暂未识别到余额，可点重试"
                        }
                    }, 35_000)
                },
                onTitleChanged = { title ->
                    if (title.isNotBlank()) pageTitle = title
                },
                onError = { message ->
                    balanceLoading = false
                    balanceError = message
                }
            )
            }

            // 官方一卡通后台只作为余额数据源，界面完全用原生卡片，不把桌面版页面露出来。
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "校园一卡通",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "付款码 · 余额 · 校园支付",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                // 付款码还在加载时，顶部刷新先重试付款码；
                                // 二维码已经出来后再刷新余额，保持“先码后余额”。
                                if (!balanceStarted) {
                                    retryQr()
                                } else {
                                    retryBalance()
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(19.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "付款码",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (qrReady) {
                                            "学校官方动态码 · 每分钟自动刷新"
                                        } else {
                                            "正在连接学校官方页面…"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = retryQr) {
                                    Icon(
                                        Icons.Outlined.Refresh,
                                        contentDescription = "刷新付款码",
                                        modifier = Modifier.size(19.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(ComposeColor.White)
                            ) {
                                if (qrStarted) {
                                    CampusWebView(
                                        url = CampusUrls.CARD_PAYCODE,
                                        cookies = cookies,
                                        cookieHosts = listOf(
                                            "authserver.swun.edu.cn",
                                            "ykth5.swun.edu.cn"
                                        ),
                                        // 付款码是 ykt H5 的独立会话。不要每次打开都清 ykt
                                        // Cookie：瑞数的校验票和官方 JSESSIONID 被清掉后，
                                        // 真机会反复卡在空白挑战页；需要换会话时用“刷新”。
                                        clearCookieHosts = emptyList(),
                                        ignoreHttpCodes = setOf(403, 412),
                                        mobileLayout = true,
                                        softwareLayer = false,
                                        // 只让“二维码已画出”的探测器在页面开始执行；不要在
                                        // onPageStarted 就跳 function.do，官方页还没生成
                                        // window.timestamp，提前跳只会得到一张白页。
                                        preloadScript = QR_READY_PROBE_JS,
                                        injectedScript = yktRedirectJs + "\n" + QR_READY_PROBE_JS,
                                        bridgeName = "AndroidQr",
                                        bridge = qrBridge,
                                        onWebViewReady = {
                                            payWebView = it
                                            it.clearCache(false)
                                        },
                                        onPageFinished = { view, _ ->
                                            payWebView = view
                                            // 中间页也会触发 onPageFinished，这里只探测二维码是否
                                            // 真的画出来；探测到才撤 loading，不再提前露出白屏。
                                            view.evaluateJavascript(QR_READY_PROBE_JS, null)
                                        },
                                        onError = { message ->
                                            qrLoading = false
                                            qrError = message
                                        }
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "正在准备付款码…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (qrStarted && qrLoading) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "正在打开学校付款码…",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (qrStarted && qrError != null && !qrLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = qrError.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            Button(onClick = retryQr) { Text("重新获取") }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onOpen(Routes.BILL) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("账单查询")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(23.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName.ifBlank { "校园一卡通" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (displayNo.isNotBlank()) {
                                        Text(
                                            text = "学号 $displayNo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "校园卡余额",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            if (!balanceStarted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "付款码显示后同步余额…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (balanceLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "正在同步余额…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = balance?.let { "¥ $it" } ?: "¥ —",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balance != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            if (balanceError != null && !balanceLoading) {
                                Spacer(Modifier.height(8.dp))
                                RetryRow(
                                    message = balanceError.orEmpty(),
                                    onRetry = retryBalance
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "付款码由学校一卡通官方页面生成，每分钟自动刷新；余额与消费以学校系统为准。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    LaunchedEffect(qrStarted, qrLoading) {
        if (qrStarted && qrLoading) {
            delay(25_000)
            if (qrLoading) {
                qrLoading = false
                qrError = "付款码加载超时，请点刷新重试"
            }
        }
    }

}
