package com.swun.campus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.swun.campus.R
import com.swun.campus.ui.Routes

class FunctionsWidgetProvider : AppWidgetProvider() {

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
        private const val SMALL_CAPACITY = 4
        private const val LARGE_CAPACITY = 8

        private val cellIds = intArrayOf(
            R.id.widget_cell_1,
            R.id.widget_cell_2,
            R.id.widget_cell_3,
            R.id.widget_cell_4,
            R.id.widget_cell_5,
            R.id.widget_cell_6,
            R.id.widget_cell_7,
            R.id.widget_cell_8
        )
        private val iconIds = intArrayOf(
            R.id.widget_icon_1,
            R.id.widget_icon_2,
            R.id.widget_icon_3,
            R.id.widget_icon_4,
            R.id.widget_icon_5,
            R.id.widget_icon_6,
            R.id.widget_icon_7,
            R.id.widget_icon_8
        )
        private val titleIds = intArrayOf(
            R.id.widget_title_1,
            R.id.widget_title_2,
            R.id.widget_title_3,
            R.id.widget_title_4,
            R.id.widget_title_5,
            R.id.widget_title_6,
            R.id.widget_title_7,
            R.id.widget_title_8
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, FunctionsWidgetProvider::class.java)
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

            // 4×1 矮而宽时横排；2×2/4×2 用方块网格。
            val wideAndShort = width >= 280 && height in 1..90
            // 4×2 大约 250×110dp；2×2 约 110×110dp，不能用宽度阈值区分。
            val large = width >= 240 && height >= 100

            val keys = WidgetPrefs.actionKeys(context, appWidgetId)
                .filter { WidgetActions.byKey(it).key.isNotBlank() }
            val count = keys.size.coerceIn(0, WidgetActions.MAX_SLOTS)

            // 4×2 大尺寸最多放 8 个；配置了 4 个时也留出后 4 个“添加”位，
            // 免得用户还得回 App 才能继续加满。
            val useEight = large && count >= 4
            val capacity = if (useEight) LARGE_CAPACITY else SMALL_CAPACITY

            val layout = when {
                useEight -> R.layout.widget_functions_4x2
                count == 0 || count == 1 ->
                    if (wideAndShort) R.layout.widget_functions_row_one
                    else R.layout.widget_functions_one
                count == 2 ->
                    if (wideAndShort) R.layout.widget_functions_row_two
                    else R.layout.widget_functions_two
                count == 3 ->
                    if (wideAndShort) R.layout.widget_functions_row_three
                    else R.layout.widget_functions_three
                else ->
                    if (wideAndShort) R.layout.widget_functions_4x1
                    else R.layout.widget_functions_2x2
            }

            val views = RemoteViews(context.packageName, layout)
            views.setOnClickPendingIntent(
                R.id.widget_functions_root,
                WidgetNavigation.pendingIntent(
                    context,
                    Routes.LIFE,
                    requestCode = 14000 + appWidgetId
                )
            )

            val bindCount = minOf(count, capacity)
            repeat(bindCount) { index ->
                bindAction(context, views, index, appWidgetId, keys[index])
            }

            if (useEight) {
                // 前 4 个已配置时，后 4 格显示“添加”；配置到 8 个后全部是功能卡。
                for (index in bindCount until LARGE_CAPACITY) {
                    bindAdd(context, views, index, appWidgetId)
                }
            } else {
                // 小尺寸保持原来的行为：3 个时第 4 格显示“+”；空组件显示配置入口。
                when (count) {
                    0 -> bindAdd(context, views, 0, appWidgetId, title = "配置快捷按钮")
                    3 -> bindAdd(context, views, 3, appWidgetId)
                }
            }

            return views
        }

        private fun bindAction(
            context: Context,
            views: RemoteViews,
            index: Int,
            appWidgetId: Int,
            key: String
        ) {
            val action = WidgetActions.byKey(key)
            views.setTextViewText(titleIds[index], action.widgetLabel)
            views.setImageViewResource(iconIds[index], action.iconRes)

            val requestCode = 15000 + appWidgetId * 10 + index
            val pendingIntent = action.courierTarget?.let { target ->
                WidgetNavigation.courierPendingIntent(context, target, requestCode)
            } ?: WidgetNavigation.pendingIntent(
                context,
                action.key,
                requestCode
            )
            views.setOnClickPendingIntent(cellIds[index], pendingIntent)
        }

        private fun bindAdd(
            context: Context,
            views: RemoteViews,
            index: Int,
            appWidgetId: Int,
            title: String = "添加"
        ) {
            views.setTextViewText(titleIds[index], title)
            views.setImageViewResource(iconIds[index], R.drawable.ic_widget_add)
            views.setOnClickPendingIntent(
                cellIds[index],
                WidgetNavigation.configPendingIntent(
                    context,
                    appWidgetId,
                    requestCode = 16000 + appWidgetId * 10 + index
                )
            )
        }
    }
}
