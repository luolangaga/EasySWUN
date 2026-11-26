package com.swun.campus.widget

import androidx.annotation.DrawableRes
import com.swun.campus.R
import com.swun.campus.courier.CourierTarget
import com.swun.campus.ui.Routes

data class WidgetAction(
    val key: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val courierTarget: CourierTarget? = null,
    val widgetLabel: String = label
)

object WidgetActions {

    val unset = WidgetAction("", "未设置", 0)

    val all: List<WidgetAction> = listOf(
        unset,
        WidgetAction(Routes.SCHEDULE, "课表", R.drawable.ic_widget_schedule),
        WidgetAction(Routes.BUS, "校车", R.drawable.ic_widget_bus),
        WidgetAction(Routes.CARD, "一卡通", R.drawable.ic_widget_card),
        WidgetAction(Routes.CHECK_IN, "晚归打卡", R.drawable.ic_widget_checkin),
        WidgetAction(Routes.LEAVE, "请销假", R.drawable.ic_widget_leave),
        WidgetAction(Routes.GRADE, "成绩", R.drawable.ic_widget_grade),
        WidgetAction(Routes.CALENDAR, "校历", R.drawable.ic_widget_calendar),
        WidgetAction(Routes.ATTENDANCE_WEB, "课堂考勤", R.drawable.ic_widget_attendance),
        WidgetAction(Routes.WATER, "淋浴充值", R.drawable.ic_widget_water),
        WidgetAction(Routes.BILL, "账单", R.drawable.ic_widget_bill),
        WidgetAction(Routes.ACCOUNT, "账号", R.drawable.ic_widget_account),
        WidgetAction(Routes.ACADEMIC, "教务首页", R.drawable.ic_widget_academic),
        WidgetAction(
            "courier_taobao",
            "淘宝身份码",
            R.drawable.ic_widget_taobao,
            CourierTarget.TAOBAO,
            widgetLabel = "淘宝码"
        ),
        WidgetAction(
            "courier_pinduoduo",
            "拼多多身份码",
            R.drawable.ic_widget_pinduoduo,
            CourierTarget.PINDUODUO,
            widgetLabel = "拼多多码"
        )
    )

    val defaultKeys: List<String> = listOf(
        Routes.SCHEDULE,
        Routes.BUS,
        Routes.CARD,
        Routes.CHECK_IN
    )

    const val MAX_SLOTS = 8

    fun byKey(key: String?): WidgetAction =
        all.firstOrNull { it.key == key } ?: unset

    fun keyAt(keys: List<String>, index: Int): String =
        keys.getOrElse(index) { "" }

    fun normalizedKeys(raw: String?): List<String> {
        val parsed = raw.orEmpty().split('|')
        return (0 until MAX_SLOTS).map { index ->
            val key = parsed.getOrElse(index) { "" }
            if (byKey(key) == unset) "" else key
        }
    }
}
