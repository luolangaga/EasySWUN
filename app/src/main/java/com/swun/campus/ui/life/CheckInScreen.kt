package com.swun.campus.ui.life

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.swun.campus.AppViewModel
import com.swun.campus.data.CampusUrls
import com.swun.campus.ui.web.CampusWebView

/**
 * 晚归打卡不再做原生中转页，进入路由后直接显示学校官方页面。
 *
 * 仍然先走 gyglxt 的 CAS service：App 里的统一认证 Cookie 会自动换票，
 * 再落到官方定位打卡页；直接开业务页在会话过期后会停在 gyglxt 登录页。
 */
internal const val CHECKIN_ENTER_JS = """
(function () {
  if (location.host.indexOf('gyglxt.swun.edu.cn') < 0) return;
  var SSO = 'https://gyglxt.swun.edu.cn/appcas/ssoMobileLogin.jsp';
  var tries = 0;
  function go() {
    if (location.host.indexOf('gyglxt.swun.edu.cn') < 0) return;
    if (tries++ > 40) return;
    if (location.pathname.indexOf('/appcas/') >= 0) {
      setTimeout(go, 700);
      return;
    }
    if (location.pathname.indexOf('/wxweb') < 0) {
      setTimeout(go, 700);
      return;
    }
    var hash = (location.hash || '').toLowerCase();
    if (hash.indexOf('/login') >= 0) {
      if (!sessionStorage.getItem('swun_checkin_sso_retry')) {
        sessionStorage.setItem('swun_checkin_sso_retry', '1');
        location.href = SSO;
      }
      return;
    }
    if (hash.indexOf('/caslogin') >= 0 || hash.indexOf('/authenlogin') >= 0 ||
        hash.indexOf('/loading') >= 0) {
      setTimeout(go, 900);
      return;
    }
    if (hash.indexOf('/positioningclock') >= 0) {
      sessionStorage.removeItem('swun_checkin_sso_retry');
      return;
    }
    location.href = 'https://gyglxt.swun.edu.cn/wxweb/#/PositioningClock';
  }
  setTimeout(go, 500);
})()
"""

@Composable
fun CheckInScreen(vm: AppViewModel) {
    val context = LocalContext.current

    // 官方页的定位打卡依赖 WebView 的地理位置权限；这里只申请系统权限，
    // 不画任何原生中转 UI。用户拒绝时官方页仍会打开，只是拿不到坐标。
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CampusWebView(
            url = CampusUrls.CHECKIN_SSO,
            cookies = vm.sessionCookies(),
            cookieHosts = listOf(
                "authserver.swun.edu.cn",
                "zxtjfx.swun.edu.cn",
                "gyglxt.swun.edu.cn"
            ),
            clearCookieHosts = listOf("authserver.swun.edu.cn"),
            mobileLayout = true,
            injectedScript = CHECKIN_ENTER_JS
        )
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
