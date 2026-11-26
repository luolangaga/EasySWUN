package com.swun.campus.ui

import android.app.Activity
import android.webkit.CookieManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.swun.campus.AppViewModel
import com.swun.campus.ui.web.CampusWebView
import okhttp3.Cookie
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Composable
fun LoginScreen(vm: AppViewModel) {
    var username by remember { mutableStateOf(vm.savedAccount) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var webLoginOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "西南民族大学",
                    style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "校园服务总览 · 统一身份认证",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                Text(
                    text = "登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "使用学号与教务系统密码登录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("学号") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.PersonOutline, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (username.isNotBlank() && password.isNotBlank()) webLoginOpen = true
                    }),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )

                val error = vm.loginError
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        webLoginOpen = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !vm.loginLoading && username.isNotBlank() && password.isNotBlank(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (vm.loginLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("正在登录…")
                    } else {
                        Text("登 录", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "账号密码仅用于向学校统一身份认证发起登录，\n不会上传到任何第三方服务器。",
            modifier = Modifier.padding(horizontal = 32.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(40.dp))
    }

    if (webLoginOpen) {
        WebLoginDialog(
            username = username,
            password = password,
            onDismiss = { webLoginOpen = false },
            onCookies = { cookies ->
                webLoginOpen = false
                vm.loginWithWebSession(username, cookies)
            }
        )
    }
}

private const val CAS_WEB_LOGIN_URL =
    "https://authserver.swun.edu.cn/authserver/login" +
        "?service=http%3A%2F%2Fjwxt.swun.edu.cn%2Fsso%2Fjziotlogin"

/**
 * 校内统一认证网页登录。登录页本身是官方页面，验证码也在页面内完成；
 * 成功后从 WebView 的 CookieManager 取回 CAS/教务 Cookie，再交给原生接口。
 */
@Composable
private fun WebLoginDialog(
    username: String,
    password: String,
    onDismiss: () -> Unit,
    onCookies: (List<Cookie>) -> Unit
) {
    var completed by remember { mutableStateOf(false) }
    var webError by remember { mutableStateOf<String?>(null) }
    val autoFillScript = remember(username, password) {
        casAutoFillScript(username, password)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 18.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "统一身份认证",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "登录成功后自动返回",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭")
                    }
                }
                HorizontalDivider()

                webError?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    CampusWebView(
                        url = CAS_WEB_LOGIN_URL,
                        modifier = Modifier.fillMaxSize(),
                        cookies = emptyList(),
                        cookieHosts = listOf(
                            "authserver.swun.edu.cn",
                            "jwxt.swun.edu.cn",
                            "ktkq.swun.edu.cn"
                        ),
                        clearCookieHosts = emptyList(),
                        injectedScript = autoFillScript,
                        mobileLayout = true,
                        ignoreHttpCodes = setOf(403, 412),
                        onPageFinished = { view, pageUrl ->
                            if (!completed && isJwxtFinal(pageUrl)) {
                                completed = true
                                // 等 Set-Cookie 与重定向链全部落盘后再取，避免拿到半套会话。
                                view.postDelayed({
                                    val cookies = captureLoginCookies()
                                    if (cookies.isEmpty()) {
                                        completed = false
                                        webError = "登录成功但未取到会话，请重试"
                                    } else {
                                        onCookies(cookies)
                                    }
                                }, 900)
                            }
                        },
                        onError = { message ->
                            webError = message
                        }
                    )
                }
            }
        }
    }
}

/** 只把登录页可见的用户名/密码填入官方表单；验证码留给官方页面处理。 */
private fun casAutoFillScript(username: String, password: String): String {
    val u = JSONObject.quote(username)
    val p = JSONObject.quote(password)
    return """
        (function () {
          try {
            var user = document.querySelector('#username');
            var pwd = document.querySelector('#password');
            function fill(el, value) {
              if (!el) return;
              if (el.value !== value) {
                el.value = value;
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
              }
            }
            fill(user, $u);
            fill(pwd, $p);

            var captcha = document.querySelector('#captcha');
            var captchaVisible = !!(captcha && captcha.offsetParent !== null);
            var error = document.querySelector('#showErrorTip, .login-error, .alert-danger');
            var submit = document.querySelector('#loginFromId #login_submit')
              || document.querySelector('#login_submit');
            if (!captchaVisible && !error && submit && !sessionStorage.getItem('swun_auto_submit')) {
              sessionStorage.setItem('swun_auto_submit', '1');
              setTimeout(function () { submit.click(); }, 350);
            }
          } catch (e) {}
        })();
    """
}

private fun isJwxtFinal(url: String): Boolean {
    return url.startsWith("https://jwxt.swun.edu.cn/") &&
        url.contains("/jwglxt/") &&
        !url.contains("/sso/")
}

/**
 * 从 WebView CookieManager 取回登录后的会话 Cookie。
 * CookieManager 只返回 name=value，因此按主机补回 domain/path 后再交给 OkHttp。
 */
private fun captureLoginCookies(): List<Cookie> {
    val now = System.currentTimeMillis()
    val hosts = listOf(
        "https://authserver.swun.edu.cn/authserver/" to "authserver.swun.edu.cn",
        "https://jwxt.swun.edu.cn/jwglxt/" to "jwxt.swun.edu.cn",
        "https://jwxt.swun.edu.cn/sso/" to "jwxt.swun.edu.cn",
        "https://ktkq.swun.edu.cn/" to "ktkq.swun.edu.cn"
    )
    val out = ArrayList<Cookie>()
    hosts.forEach { (url, domain) ->
        CookieManager.getInstance().getCookie(url)
            .orEmpty()
            .split(';')
            .forEach { part ->
                val pair = part.trim()
                val name = pair.substringBefore('=', "").trim()
                val value = pair.substringAfter('=', "").trim()
                if (name.isNotBlank() && value.isNotBlank()) {
                    runCatching {
                        out.add(
                            Cookie.Builder()
                                .domain(domain)
                                .path("/")
                                .name(name)
                                .value(value)
                                .expiresAt(now + TimeUnit.DAYS.toMillis(30))
                                .build()
                        )
                    }
                }
            }
    }
    return out
}
