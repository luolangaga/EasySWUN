package com.swun.campus.data

import java.time.LocalDate

/** 学生基本信息 */
data class UserProfile(
    val name: String = "",
    val studentNo: String = "",
    val college: String = "",
    val className: String = "",
    val major: String = "",
    val grade: String = ""
) {
    val isValid: Boolean get() = name.isNotBlank()
}

/** 学年学期 */
data class Term(val year: String, val half: String) {
    /** 3 表示第一学期，12 表示第二学期（正方教务的编码方式） */
    val halfIndex: Int get() = if (half == HALF_SECOND) 2 else 1

    val label: String
        get() {
            val y2 = year.toIntOrNull()?.plus(1)?.toString() ?: ""
            return "$year-$y2 学年 · 第${if (halfIndex == 1) "一" else "二"}学期"
        }

    val shortLabel: String get() = "$year-$yearShort 第${if (halfIndex == 1) "一" else "二"}学期"
    private val yearShort: String get() = (year.toIntOrNull()?.plus(1)?.toString() ?: "").takeLast(2)

    companion object {
        const val HALF_FIRST = "3"
        const val HALF_SECOND = "12"

        fun of(year: String, half: String) = Term(year, half.ifBlank { HALF_FIRST })
    }
}

/** 课程（课表条目） */
data class Course(
    val name: String,
    val teacher: String,
    val room: String,
    val campus: String,
    val dayOfWeek: Int,      // 1 = 周一 ... 7 = 周日
    val startSection: Int,
    val endSection: Int,
    val weekRanges: List<IntRange>,
    val parity: Parity,
    val weekRaw: String,
    val credit: String,
    /** ktkq 课程明细接口需要的标识，普通教务课表为空 */
    val jxbid: String = "",
    val kbid: String = "",
    val jxblx: String = ""
) {
    val sectionSpan: Int get() = (endSection - startSection + 1).coerceAtLeast(1)

    fun occursOn(week: Int): Boolean {
        if (weekRanges.isEmpty()) return true
        val inRange = weekRanges.any { week in it }
        if (!inRange) return false
        return when (parity) {
            Parity.ODD -> week % 2 == 1
            Parity.EVEN -> week % 2 == 0
            Parity.ALL -> true
        }
    }

    /** 同一门课用于配色的稳定标识 */
    val colorKey: Int get() = name.hashCode()
}

enum class Parity { ALL, ODD, EVEN }

/** 课表整体数据 */
data class Timetable(
    val term: Term,
    val termLabelRaw: String,
    val courses: List<Course>,
    val profile: UserProfile?
)

/** 成绩条目 */
data class Grade(
    val courseName: String,
    val courseCode: String,
    val credit: Double,
    val score: String,
    val gpa: Double,
    val category: String,
    val termYear: String,
    val termHalfRaw: String,
    val teacher: String,
    val examType: String,
    val college: String,
    val isRetake: Boolean
) {
    val termLabel: String
        get() {
            val y2 = termYear.toIntOrNull()?.plus(1)?.toString() ?: ""
            val half = if (termHalfRaw in listOf("2", "12")) "第2学期" else "第1学期"
            return "$termYear-$y2 $half"
        }
}

/** 学期 + 成绩汇总 */
data class GradeSheet(
    val term: Term,
    val grades: List<Grade>
) {
    val totalCredits: Double get() = grades.sumOf { it.credit }
    val weightedGpa: Double
        get() {
            val credits = grades.filter { it.credit > 0 && it.gpa >= 0 }
            val sumCredit = credits.sumOf { it.credit }
            if (sumCredit <= 0.0) return 0.0
            return credits.sumOf { it.credit * it.gpa } / sumCredit
        }
}

/** 校历事件 */
data class CalendarEvent(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val title: String
)

/** 校历 */
data class SchoolCalendar(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val events: List<CalendarEvent>,
    val classDays: List<LocalDate>,
    val rawTermText: String
) {
    /** 当前是第几个教学周 */
    fun weekOf(date: LocalDate): Int? {
        val start = startDate ?: return null
        if (date.isBefore(start)) return null
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, date)
        return (days / 7).toInt() + 1
    }
}

data class TermOption(val year: String, val half: String)

class SessionExpiredException(message: String = "登录状态已失效，请重新登录") : Exception(message)

class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
