package com.swun.campus.courier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.swun.campus.data.CampusUrls

/** 快递身份码目标。生活页和桌面小组件共用同一套跳转逻辑。 */
enum class CourierTarget(
    val label: String,
    val packageName: String,
    val uri: String,
    val activityName: String?,
    val missingMessage: String,
    val failureMessage: String
) {
    TAOBAO(
        label = "淘宝身份码",
        packageName = CampusUrls.TAOBAO_PACKAGE,
        uri = CampusUrls.TAOBAO_IDENTITY,
        activityName = CampusUrls.TAOBAO_BROWSER_ACTIVITY,
        missingMessage = "未安装淘宝，无法打开身份码",
        failureMessage = "淘宝打开失败，请更新淘宝后重试"
    ),
    PINDUODUO(
        label = "拼多多身份码",
        packageName = CampusUrls.PINDUODUO_PACKAGE,
        uri = CampusUrls.PINDUODUO_IDENTITY,
        activityName = null,
        missingMessage = "未安装拼多多，无法打开身份码",
        failureMessage = "拼多多打开失败，请更新拼多多后重试"
    )
}

object CourierLauncher {

    fun isInstalled(context: Context, target: CourierTarget): Boolean =
        context.packageManager.getLaunchIntentForPackage(target.packageName) != null

    /**
     * 打开官方身份码。淘宝优先指定它自己的 BrowserActivity，保留淘宝登录态；
     * 如果版本改了 Activity 名，再退回普通 VIEW 由 App Links 兜底。
     */
    fun open(context: Context, target: CourierTarget): Boolean {
        if (!isInstalled(context, target)) {
            Toast.makeText(context, target.missingMessage, Toast.LENGTH_LONG).show()
            return false
        }

        val uri = Uri.parse(target.uri)
        if (target.activityName != null) {
            val explicit = Intent(Intent.ACTION_VIEW, uri).apply {
                setClassName(target.packageName, target.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(explicit) }.isSuccess) {
                return true
            }
        }

        val generic = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (runCatching { context.startActivity(generic) }.isSuccess) {
            true
        } else {
            Toast.makeText(context, target.failureMessage, Toast.LENGTH_LONG).show()
            false
        }
    }
}
