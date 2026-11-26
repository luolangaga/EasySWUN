package com.swun.campus.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** 后勤处通勤车的一条线路。 */
data class BusLine(
    val label: String,
    val fromName: String,
    val toName: String,
    val weekdayOut: List<String>,
    val weekdayBack: List<String>,
    val weekendOut: List<String>,
    val weekendBack: List<String>,
    val holidayOut: List<String>,
    val holidayBack: List<String>,
    val note: String
)

enum class BusDayType(val label: String) {
    WORKDAY("工作日"),
    WEEKEND("周末"),
    HOLIDAY("法定节假日")
}

/**
 * 校车时刻的单一数据源。
 *
 * 原生校车页、桌面小组件和小组件配置页都从这里取数据，避免三处时刻表
 * 各写一份后逐渐对不上。
 */
object BusSchedule {

    private val holidayRanges = listOf(
        LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 1, 3),
        LocalDate.of(2026, 5, 1)..LocalDate.of(2026, 5, 5),
        LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 7),
        LocalDate.of(2027, 1, 1)..LocalDate.of(2027, 1, 3),
        LocalDate.of(2027, 5, 1)..LocalDate.of(2027, 5, 5),
        LocalDate.of(2027, 10, 1)..LocalDate.of(2027, 10, 7)
    )

    val lines: List<BusLine> = listOf(
        BusLine(
            label = "武侯 ↔ 航空港",
            fromName = "武侯校区",
            toName = "航空港校区",
            weekdayOut = listOf("7:40", "9:40", "11:20", "13:10", "15:10", "17:20", "18:10", "21:40"),
            weekdayBack = listOf("7:40", "8:30", "10:30", "12:20", "13:10", "15:10", "16:00", "17:20", "17:50", "20:50", "21:40", "22:30"),
            weekendOut = listOf("7:40", "9:40", "11:20", "13:10", "15:10", "17:20", "18:10", "21:40"),
            weekendBack = listOf("7:40", "8:30", "10:30", "12:20", "13:10", "15:10", "16:00", "17:20", "17:50", "20:50", "21:40", "22:30"),
            holidayOut = listOf("8:30", "9:40", "14:30", "17:10", "19:00", "21:30"),
            holidayBack = listOf("9:00", "10:30", "15:30", "18:00", "20:00", "22:30"),
            note = "武侯→航空港 7:40 在武侯校区发车；航空港→武侯 7:40 班次在图书馆廊桥旁候车。"
        ),
        BusLine(
            label = "太平园 ↔ 航空港",
            fromName = "太平园校区",
            toName = "航空港校区",
            weekdayOut = listOf("7:40", "9:40", "10:30", "12:20", "13:10", "16:00", "17:50", "20:50"),
            weekdayBack = listOf("7:40", "9:40", "10:30", "13:10", "15:10", "16:00", "17:50", "20:50"),
            weekendOut = listOf("7:40", "9:40", "13:10", "17:00", "20:00"),
            weekendBack = listOf("8:30", "12:20", "16:00", "17:50", "20:50"),
            holidayOut = listOf("9:00", "13:10", "20:00"),
            holidayBack = listOf("9:40", "14:00", "20:50"),
            note = "周末与法定节假日班次不同，节假日按表内「法定节假日」一栏执行。"
        ),
        BusLine(
            label = "武侯 ↔ 太平园",
            fromName = "武侯校区",
            toName = "太平园校区",
            weekdayOut = listOf("8:00", "9:50", "13:30", "15:20", "18:20"),
            weekdayBack = listOf("8:30", "10:20", "12:10", "15:50", "17:45", "20:45"),
            weekendOut = listOf("8:30", "16:30"),
            weekendBack = listOf("9:00", "17:00"),
            holidayOut = listOf("8:30", "16:30"),
            holidayBack = listOf("9:00", "17:00"),
            note = "武侯校区乘车点在教工宿舍（一教楼）与站台之间的转弯处；周末与节假日班次较少。"
        )
    )

    fun dayType(date: LocalDate): BusDayType {
        if (holidayRanges.any { date in it }) return BusDayType.HOLIDAY
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> BusDayType.WEEKEND
            else -> BusDayType.WORKDAY
        }
    }

    /** 某一线路、某一方向在指定日期的班次（统一为 HH:mm）。 */
    fun times(line: BusLine, reverse: Boolean, date: LocalDate): List<String> {
        val raw = when (dayType(date)) {
            BusDayType.HOLIDAY -> if (reverse) line.holidayBack else line.holidayOut
            BusDayType.WEEKEND -> if (reverse) line.weekendBack else line.weekendOut
            BusDayType.WORKDAY -> if (reverse) line.weekdayBack else line.weekdayOut
        }
        return raw.map { normalizeTime(it) }
    }

    fun nextTime(times: List<String>, now: LocalTime = LocalTime.now()): String? =
        times.firstOrNull { LocalTime.parse(it) >= now }

    fun normalizeTime(raw: String): String =
        raw.replace('：', ':')
            .split(':')
            .joinToString(":") { part -> part.padStart(2, '0') }

    fun directionText(line: BusLine, reverse: Boolean): String =
        if (reverse) "${line.toName} → ${line.fromName}" else "${line.fromName} → ${line.toName}"
}
