package com.swun.campus.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** ktkq 移动端课表（课堂考勤）数据 */
data class KtkqSchedule(
    val termCode: String,
    val termName: String,
    val currentWeek: Int,
    val maxWeek: Int,
    val weekCalendar: List<String>,
    val studentName: String,
    val courses: List<Course>,
    /** 抓取时刻；用于 App 隔几天再打开时把周次顺延到真实当前周。 */
    val fetchedAt: Long = 0L
) {
    val currentWeekLabel: String
        get() = if (weekCalendar.size >= currentWeek) weekCalendar[currentWeek - 1] else ""

    /** 按抓取时间来推算今天的教学周，避免缓存课表一直停在抓取时的那一周。 */
    fun weekAt(today: LocalDate = LocalDate.now()): Int {
        if (fetchedAt <= 0L) {
            // 旧缓存没有 updated_at，用 weekCalendar 第一条（抓取当周的周一）反推，
            // 这样即便几天没打开 App，首页和小组件也不会一直显示旧周次。
            val anchor = weekCalendar.firstOrNull()
                ?.substringBefore(':')
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (anchor != null) {
                val elapsedWeeks = (ChronoUnit.DAYS.between(anchor, today).toInt() / 7)
                return (currentWeek + elapsedWeeks).coerceIn(1, maxWeek)
            }
            return currentWeek.coerceIn(1, maxWeek)
        }
        val fetchedDate = Instant.ofEpochMilli(fetchedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val elapsedWeeks = ChronoUnit.DAYS.between(fetchedDate, today).toInt() / 7
        return (currentWeek + elapsedWeeks).coerceIn(1, maxWeek)
    }
}

object KtkqParser {

    /** 解析隐藏 WebView 里组合好的 time/info/courses 三段 JSON */
    fun parse(raw: String, fetchedAt: Long = 0L): KtkqSchedule {
        val root = JSONObject(raw)
        val time = root.getJSONObject("time").getJSONObject("data")
        val info = root.optJSONObject("info")?.optJSONObject("data")
        val coursesRoot = root.getJSONObject("courses")
        val termCode = time.optString("xnxqdm")
        val termName = time.optString("xnxqmc")
        val currentWeek = time.optInt("todayWeekNum", 1)
        val maxWeek = time.optInt("maxWeekNum", 16).coerceAtLeast(1)
        val weekCalendar = buildList {
            val arr = time.optJSONArray("weekCalendar") ?: return@buildList
            for (i in 0 until arr.length()) add(arr.optString(i))
        }
        val courses = mutableListOf<Course>()
        val arr = coursesRoot.optJSONArray("data") ?: return emptySchedule(
            termCode, termName, currentWeek, maxWeek, weekCalendar, info?.optString("xm").orEmpty(), fetchedAt
        )
        for (i in 0 until arr.length()) {
            val group = arr.optJSONObject(i) ?: continue
            val list = group.optJSONArray("list") ?: continue
            for (j in 0 until list.length()) {
                val item = list.optJSONObject(j) ?: continue
                val name = item.optString("kcm").ifBlank { group.optString("kcm") }
                val day = item.optInt("skxq", 0)
                if (day !in 1..7) continue
                val start = item.optInt("ksjc", 1).coerceAtLeast(1)
                val end = item.optInt("jsjc", start).coerceAtLeast(start)
                val ranges = parseWeeks(item.optString("skzc"))
                val credit = when {
                    !item.isNull("xf") -> item.optDouble("xf").toString()
                    !group.isNull("xf") -> group.optDouble("xf").toString()
                    else -> ""
                }
                courses += Course(
                    name = name,
                    teacher = "",
                    room = item.optString("jasmc"),
                    campus = "",
                    dayOfWeek = day,
                    startSection = start,
                    endSection = end,
                    weekRanges = ranges,
                    parity = Parity.ALL,
                    weekRaw = item.optString("zcmc"),
                    credit = credit,
                    jxbid = item.optString("jxbid"),
                    kbid = item.optString("kbid"),
                    jxblx = item.optString("jxblx")
                )
            }
        }
        return KtkqSchedule(
            termCode = termCode,
            termName = termName.ifBlank { "当前学期" },
            currentWeek = currentWeek.coerceIn(1, maxWeek),
            maxWeek = maxWeek,
            weekCalendar = weekCalendar,
            studentName = info?.optString("xm").orEmpty(),
            courses = courses.sortedWith(compareBy({ it.dayOfWeek }, { it.startSection }, { it.name })),
            fetchedAt = fetchedAt
        )
    }

    private fun emptySchedule(
        termCode: String,
        termName: String,
        currentWeek: Int,
        maxWeek: Int,
        weekCalendar: List<String>,
        studentName: String,
        fetchedAt: Long
    ) = KtkqSchedule(
        termCode = termCode,
        termName = termName.ifBlank { "当前学期" },
        currentWeek = currentWeek.coerceIn(1, maxWeek),
        maxWeek = maxWeek,
        weekCalendar = weekCalendar,
        studentName = studentName,
        courses = emptyList(),
        fetchedAt = fetchedAt
    )

    /** skzc 是 20/32 位的周次位图，例如 111111111111111100000000... */
    private fun parseWeeks(bits: String): List<IntRange> {
        if (bits.isBlank()) return listOf(1..16)
        val ranges = mutableListOf<IntRange>()
        var start = -1
        for (index in bits.indices) {
            val enabled = bits[index] == '1'
            if (enabled && start < 0) start = index + 1
            if (!enabled && start >= 0) {
                ranges += start..index
                start = -1
            }
        }
        if (start >= 0) ranges += start..bits.length
        return ranges.ifEmpty { listOf(1..bits.length) }
    }
}
