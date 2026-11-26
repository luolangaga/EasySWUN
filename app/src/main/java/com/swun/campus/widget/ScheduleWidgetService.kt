package com.swun.campus.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.swun.campus.R
import com.swun.campus.ui.Routes

/**
 * 课表小组件的可滚动列表。
 *
 * RemoteViews 里 ScrollView 不是受支持控件，课表超出卡片高度后要滚动，
 * 只能走 ListView + RemoteViewsService 的集合视图。每一行左右两列分别
 * 是今天/明天，宽布局两列都显示，窄布局只显示今天列。
 */
class ScheduleWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ScheduleFactory(
            context = applicationContext,
            wide = intent.getBooleanExtra(EXTRA_WIDE, false),
            showTomorrow = intent.getBooleanExtra(EXTRA_SHOW_TOMORROW, true),
            hideEnded = intent.getBooleanExtra(EXTRA_HIDE_ENDED, true)
        )

    companion object {
        const val EXTRA_WIDE = "com.swun.campus.extra.SCHEDULE_WIDE"
        const val EXTRA_SHOW_TOMORROW = "com.swun.campus.extra.SCHEDULE_SHOW_TOMORROW"
        const val EXTRA_HIDE_ENDED = "com.swun.campus.extra.SCHEDULE_HIDE_ENDED"
    }
}

private class ScheduleFactory(
    private val context: Context,
    private val wide: Boolean,
    private val showTomorrow: Boolean,
    private val hideEnded: Boolean
) : RemoteViewsService.RemoteViewsFactory {

    private var snapshot: ScheduleSnapshot? = null
    private var rowCount = 0

    override fun onCreate() {
        load()
    }

    override fun onDataSetChanged() {
        load()
    }

    override fun onDestroy() {
        snapshot = null
        rowCount = 0
    }

    override fun getCount(): Int = rowCount

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in 0 until rowCount) return null
        val snap = snapshot
        val today = snap?.today?.getOrNull(position)
        val tomorrow = snap?.tomorrow?.getOrNull(position)
        val views = RemoteViews(context.packageName, R.layout.widget_schedule_list_row)

        bindToday(views, today)
        if (wide && showTomorrow) {
            bindTomorrow(views, tomorrow)
            views.setViewVisibility(R.id.widget_row_divider, View.VISIBLE)
            views.setViewVisibility(R.id.widget_row_tomorrow, if (tomorrow == null) View.INVISIBLE else View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_row_divider, View.GONE)
            views.setViewVisibility(R.id.widget_row_tomorrow, View.GONE)
        }

        // 给 ListView 的每一项挂 fill-in Intent，配合 Provider 的模板打开课表页。
        views.setOnClickFillInIntent(
            R.id.widget_schedule_item,
            Intent().putExtra(WidgetNavigation.EXTRA_ROUTE, Routes.SCHEDULE)
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun hasStableIds(): Boolean = true

    override fun getItemId(position: Int): Long = position.toLong()

    private fun load() {
        snapshot = WidgetData.loadSchedule(context, hideEnded)
        rowCount = maxOf(
            snapshot?.today?.size ?: 0,
            if (wide && showTomorrow) snapshot?.tomorrow?.size ?: 0 else 0
        )
    }

    private fun bindToday(views: RemoteViews, course: WidgetCourse?) {
        if (course == null) {
            views.setViewVisibility(R.id.widget_row_today, View.INVISIBLE)
            return
        }
        views.setViewVisibility(R.id.widget_row_today, View.VISIBLE)
        views.setTextViewText(R.id.widget_row_today_name, course.name)
        views.setTextViewText(R.id.widget_row_today_room, course.location)
        views.setTextViewText(R.id.widget_row_today_time, course.time)
        views.setInt(R.id.widget_row_today_bar, "setBackgroundColor", course.color)
    }

    private fun bindTomorrow(views: RemoteViews, course: WidgetCourse?) {
        if (course == null) {
            views.setViewVisibility(R.id.widget_row_tomorrow, View.INVISIBLE)
            return
        }
        views.setViewVisibility(R.id.widget_row_tomorrow, View.VISIBLE)
        views.setTextViewText(R.id.widget_row_tomorrow_name, course.name)
        views.setTextViewText(R.id.widget_row_tomorrow_room, course.location)
        views.setTextViewText(R.id.widget_row_tomorrow_time, course.time)
        views.setInt(R.id.widget_row_tomorrow_bar, "setBackgroundColor", course.color)
    }
}
