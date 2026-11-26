package com.swun.campus.widget

import android.app.Activity
import android.os.Bundle
import com.swun.campus.courier.CourierLauncher
import com.swun.campus.courier.CourierTarget

/**
 * 桌面卡片的快递按钮落到这里。它没有可见界面，只负责判断是否安装，
 * 然后直接拉起淘宝/拼多多，行为和生活页的按钮一致。
 */
class CourierShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val raw = intent?.getStringExtra(EXTRA_COURIER_TARGET)
        val target = CourierTarget.entries.firstOrNull { it.name == raw }
        if (target != null) {
            CourierLauncher.open(this, target)
        }
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_COURIER_TARGET = "com.swun.campus.extra.COURIER_TARGET"
    }
}
