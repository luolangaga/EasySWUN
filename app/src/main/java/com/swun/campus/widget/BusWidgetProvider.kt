package com.swun.campus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.swun.campus.R
import com.swun.campus.data.BusDayType
import com.swun.campus.data.BusSchedule
import com.swun.campus.ui.Routes
import java.time.LocalDate
import java.time.LocalTime

class BusWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, BusWidgetProvider::class.java)
            )
            ids.forEach { update(context, manager, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            manager.updateAppWidget(
                appWidgetId,
                build(context, appWidgetId, manager.getAppWidgetOptions(appWidgetId))
            )
        }

        private fun build(
            context: Context,
            appWidgetId: Int,
            options: Bundle
        ): RemoteViews {
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val wide = width >= 240
            // 2×1 / 3×1 这类窄而矮的卡片单独用 short 布局，不再落到竖排 compact。
            val short = !wide && height <= 90
            val layout = when {
                wide -> R.layout.widget_bus_wide
                short -> R.layout.widget_bus_short
                else -> R.layout.widget_bus_compact
            }
            val views = RemoteViews(context.packageName, layout)

            val lineIndex = WidgetPrefs.lineIndex(context, appWidgetId)
                .coerceIn(0, BusSchedule.lines.lastIndex)
            val reverse = WidgetPrefs.reverse(context, appWidgetId)
            val line = BusSchedule.lines[lineIndex]
            val today = LocalDate.now()
            val dayType = BusSchedule.dayType(today)
            val times = BusSchedule.times(line, reverse, today)
            val next = BusSchedule.nextTime(times, LocalTime.now())
            val dateLabel = "%02d.%02d".format(today.monthValue, today.dayOfMonth)

            views.setOnClickPendingIntent(
                R.id.widget_bus_root,
                WidgetNavigation.pendingIntent(
                    context,
                    Routes.BUS,
                    requestCode = 13000 + appWidgetId
                )
            )

            views.setTextViewText(R.id.widget_bus_line, line.label)
            views.setTextViewText(
                R.id.widget_bus_direction,
                BusSchedule.directionText(line, reverse)
            )

            if (wide) {
                views.setTextViewText(
                    R.id.widget_bus_daytype,
                    "${dayType.label} · $dateLabel"
                )
            } else if (short) {
                views.setTextViewText(
                    R.id.widget_bus_daytype,
                    if (dayType == BusDayType.HOLIDAY) "节假日" else dayType.label
                )
            } else {
                views.setTextViewText(R.id.widget_bus_daytype, "下一班 · ${dayType.label}")
            }

            if (next != null) {
                views.setViewVisibility(R.id.widget_bus_next, View.VISIBLE)
                views.setViewVisibility(R.id.widget_bus_empty, View.GONE)
                views.setTextViewText(R.id.widget_bus_next, next)
                val remaining = times.dropWhile { it != next }.drop(1).take(2)
                val following = when {
                    remaining.isNotEmpty() -> "之后 " + remaining.joinToString(" · ")
                    times.isNotEmpty() -> "共 ${times.size} 班 · 末班 ${times.last()}"
                    else -> ""
                }
                views.setTextViewText(
                    R.id.widget_bus_following,
                    following
                )
            } else {
                views.setViewVisibility(R.id.widget_bus_next, View.GONE)
                views.setViewVisibility(R.id.widget_bus_empty, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_bus_empty,
                    when {
                        times.isEmpty() && short -> "无班次"
                        times.isEmpty() -> "今天没有班次"
                        short -> "已收班"
                        else -> "今日已收班"
                    }
                )
                views.setTextViewText(
                    R.id.widget_bus_following,
                    when {
                        times.isEmpty() -> ""
                        short -> "首班 ${times.first()}"
                        else -> "首班 ${times.first()} · 共 ${times.size} 班"
                    }
                )
            }

            if (!wide) {
                views.setTextViewText(
                    R.id.widget_bus_hint,
                    if (short) "点击查看完整时刻" else "$dateLabel · 点击查看完整时刻"
                )
            }
            return views
        }
    }
}
