package com.swun.campus.ui.life

import org.json.JSONObject

/**
 * ykt H5 的官方页面在 onload 里用 MCK 原生桥拿 getUserInfo；
 * 独立 WebView 没有这个桥会白屏。这里在页面脚本执行前放一个最小原生桥，
 * 并在页面加载完后用当前登录学生的信息走官方 function.do 跳转。
 */
internal fun yktMenuRedirectScript(studentNo: String): String {
    val stu = JSONObject.quote(studentNo.filter { it.isDigit() })
    return """
    (function () {
      try {
        if (typeof window.MCK === 'undefined') {
          window.MCK = {
            auth: function (cb) { if (typeof cb === 'function') cb(window.MCK); },
            ready: function (fn) { if (typeof fn === 'function') setTimeout(function () { fn(window.MCK); }, 0); },
            h5: {
              call: function (api, info, cb) {
                var data = { content: {}, ERRCODE: 'SUCCESS' };
                if (typeof cb === 'function') setTimeout(function () { cb(data); }, 0);
              },
              watch: function () { return window.MCK; },
              watchOnce: function () { return window.MCK; },
              unWatch: function () { return window.MCK; },
              getApi: function () { return ['getUserInfo']; }
            },
            conflict: function () { return window.MCK; }
          };
        }
        if ((location.pathname || '').indexOf('/menu/menu.do') < 0) return;
        var tries = 0;
        function go() {
          if ((location.pathname || '').indexOf('/menu/menu.do') < 0) return;
          var ts = window.timestamp || window.timestampStr;
          if (!ts && tries < 20) {
            tries++;
            setTimeout(go, 300);
            return;
          }
          if (!ts) ts = String(Date.now());
          var m = (location.search.match(/[?&]menu=([^&]+)/) || [])[1] || window.menu || '';
          var stu = $stu;
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
}

/**
 * 付款码页只有真正画出二维码后才撤掉加载提示。
 * 不能一 onPageFinished 就当成功：官方 H5 会先经过 CAS、menu.do、
 * function.do 几个中间页，任何一页 finished 时都可能还没有二维码。
 */
internal const val YKT_QR_READY_PROBE_JS = """
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
        if (imgs[i].width >= 70 && imgs[i].height >= 70) return true;
      }
      var canvases = document.querySelectorAll('canvas');
      for (var j = 0; j < canvases.length; j++) {
        if (canvases[j].width >= 100 && canvases[j].height >= 100) return true;
      }
      var svgs = document.querySelectorAll('svg');
      for (var k = 0; k < svgs.length; k++) {
        if (svgs[k].clientWidth >= 100 && svgs[k].clientHeight >= 100) return true;
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
