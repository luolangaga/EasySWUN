package com.swun.campus.data

/**
 * 西南民族大学教学运行时间表（教务处公开版）。
 *
 * 学校页面只列到第 11 节；课表网格保留了 12 节的位置，第 12 节按晚间顺延，
 * 避免第 12 节课程在界面上显示成没有时间的空白。
 */
object SectionTimes {

    private val slots: List<Pair<String, String>> = listOf(
        "08:30" to "09:15",
        "09:20" to "10:05",
        "10:25" to "11:10",
        "11:15" to "12:00",
        "14:00" to "14:45",
        "14:50" to "15:35",
        "15:55" to "16:40",
        "16:45" to "17:30",
        "19:00" to "19:45",
        "19:50" to "20:35",
        "20:40" to "21:25",
        "21:30" to "22:15"
    )

    fun start(section: Int): String =
        slots.getOrNull(section - 1)?.first.orEmpty()

    fun end(section: Int): String =
        slots.getOrNull(section - 1)?.second.orEmpty()

    /** 例如 08:30–09:15，覆盖跨多节的课程。 */
    fun range(startSection: Int, endSection: Int): String {
        val start = start(section = startSection)
        val end = end(section = endSection.coerceAtLeast(startSection))
        if (start.isBlank()) return ""
        return "$start–${end.ifBlank { start }}"
    }

    fun label(startSection: Int, endSection: Int): String {
        val sectionText = if (endSection > startSection) {
            "$startSection-$endSection 节"
        } else {
            "$startSection 节"
        }
        return "$sectionText · ${range(startSection, endSection)}"
    }

    fun all(): List<Pair<String, String>> = slots
}
