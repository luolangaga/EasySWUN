package com.swun.campus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.swun.campus.MainActivity
import com.swun.campus.courier.CourierTarget

object WidgetNavigation {

    const val EXTRA_ROUTE = "com.swun.campus.extra.ROUTE"

    fun routeFrom(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_ROUTE)?.takeIf { it.isNotBlank() }

    fun pendingIntent(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ROUTE, route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 集合小组件的点击模板；必须是 mutable，系统才能把每一步的 fill-in Intent 合并进来。 */
    fun collectionPendingIntent(
        context: Context,
        route: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ROUTE, route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }


    fun courierPendingIntent(
        context: Context,
        target: CourierTarget,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, CourierShortcutActivity::class.java).apply {
            putExtra(CourierShortcutActivity.EXTRA_COURIER_TARGET, target.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun configPendingIntent(
        context: Context,
        appWidgetId: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, FunctionsWidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(FunctionsWidgetConfigActivity.EXTRA_ADD_SLOT, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
