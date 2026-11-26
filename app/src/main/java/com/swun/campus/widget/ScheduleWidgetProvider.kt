package com.swun.campus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.swun.campus.R
import com.swun.campus.ui.Routes

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ScheduleWidgetProvider::class.java)
            )
            ids.forEach { update(context, manager, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val options = manager.getAppWidgetOptions(appWidgetId)
            manager.updateAppWidget(appWidgetId, build(context, appWidgetId, options))
            // ListView 的 adapter 是 RemoteViewsService 提供的，更新 RemoteViews 后
            // 主动通知一次，课程同步或尺寸变化时列表才会重新取数据。
            runCatching {
                manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_schedule_list)
            }
        }

        private fun build(
            context: Context,
            appWidgetId: Int,
            options: Bundle
        ): RemoteViews {
            val wide = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) >= 240
            val layout = if (wide) {
                R.layout.widget_schedule_wide
            } else {
                R.layout.widget_schedule_compact
            }
            val views = RemoteViews(context.packageName, layout)
            val hideEnded = ScheduleWidgetPrefs.hideEnded(context, appWidgetId)
            val showTomorrow = wide && ScheduleWidgetPrefs.showTomorrow(context, appWidgetId)
            val snapshot = WidgetData.loadSchedule(context, hideEnded)

            views.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetNavigation.pendingIntent(
                    context,
                    Routes.SCHEDULE,
                    requestCode = 12000 + appWidgetId
                )
            )

            applyBackground(context, appWidgetId, views)

            val date = snapshot?.date ?: java.time.LocalDate.now()
            val day = WidgetData.dayName(date)
            val week = snapshot?.week ?: 0
            val weekText = if (week > 0) "第 $week 周 · $day" else "课表未同步 · $day"

            views.setTextViewText(R.id.widget_school, "西南民族大学")
            views.setTextViewText(R.id.widget_week, weekText)

            if (wide) {
                views.setTextViewText(
                    R.id.widget_date,
                    "%02d.%02d".format(date.monthValue, date.dayOfMonth)
                )
                views.setTextViewText(R.id.widget_today_title, "今天")
                views.setViewVisibility(
                    R.id.widget_tomorrow_title,
                    if (showTomorrow) View.VISIBLE else View.GONE
                )
                views.setViewVisibility(
                    R.id.widget_header_divider,
                    if (showTomorrow) View.VISIBLE else View.GONE
                )
                if (showTomorrow) {
                    views.setTextViewText(
                        R.id.widget_tomorrow_title,
                        "明天 · ${WidgetData.dayName(date.plusDays(1))}"
                    )
                }
            } else {
                views.setTextViewText(
                    R.id.widget_date,
                    "%02d.%02d · 今天".format(date.monthValue, date.dayOfMonth)
                )
            }

            val adapter = Intent(context, ScheduleWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(ScheduleWidgetService.EXTRA_WIDE, wide)
                putExtra(ScheduleWidgetService.EXTRA_SHOW_TOMORROW, showTomorrow)
                putExtra(ScheduleWidgetService.EXTRA_HIDE_ENDED, hideEnded)
                // settings 变化时让 RemoteViewsService 重新取数据。
                data = Uri.parse("swun://widget/schedule/$appWidgetId/$hideEnded/$showTomorrow")
            }
            views.setRemoteAdapter(R.id.widget_schedule_list, adapter)
            views.setPendingIntentTemplate(
                R.id.widget_schedule_list,
                WidgetNavigation.collectionPendingIntent(
                    context,
                    Routes.SCHEDULE,
                    requestCode = 12100 + appWidgetId
                )
            )

            val today = snapshot?.today.orEmpty()
            val tomorrow = if (showTomorrow) snapshot?.tomorrow.orEmpty() else emptyList()
            val hasRows = today.isNotEmpty() || tomorrow.isNotEmpty()
            views.setViewVisibility(
                R.id.widget_schedule_empty,
                if (hasRows) View.GONE else View.VISIBLE
            )
            views.setTextViewText(
                R.id.widget_schedule_empty,
                when {
                    snapshot == null -> "打开 App 同步课表"
                    showTomorrow && today.isEmpty() && tomorrow.isEmpty() -> "今天和明天都没有课"
                    showTomorrow && today.isEmpty() -> "今天没有课"
                    showTomorrow -> "明天没有课"
                    else -> "今天没有课"
                }
            )

            return views
        }

        private fun applyBackground(
            context: Context,
            appWidgetId: Int,
            views: RemoteViews
        ) {
            val bitmap = ScheduleWidgetPrefs.loadBackground(context, appWidgetId)
            if (bitmap == null) {
                views.setViewVisibility(R.id.widget_bg_image, View.GONE)
                views.setViewVisibility(R.id.widget_bg_scrim, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_bg_image, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
                views.setViewVisibility(R.id.widget_bg_scrim, View.VISIBLE)
            }
        }
    }
}
