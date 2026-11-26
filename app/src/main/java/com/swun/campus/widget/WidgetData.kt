package com.swun.campus.widget

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.swun.campus.data.Course
import com.swun.campus.data.KtkqParser
import com.swun.campus.data.SectionTimes
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

data class WidgetCourse(
    val name: String,
    val location: String,
    val time: String,
    val color: Int
)

data class ScheduleSnapshot(
    val date: LocalDate,
    val week: Int,
    val today: List<WidgetCourse>,
    val tomorrow: List<WidgetCourse>
)

object WidgetPrefs {
    const val SCHEDULE = "swun_ktkq_cache"
    const val BUS = "swun_widget_bus"
    const val FUNCTIONS = "swun_widget_functions"

    const val KEY_LINE_PREFIX = "line_"
    const val KEY_REVERSE_PREFIX = "reverse_"
    const val KEY_ACTIONS_PREFIX = "actions_"

    fun bus(context: Context): SharedPreferences =
        context.getSharedPreferences(BUS, Context.MODE_PRIVATE)

    fun functions(context: Context): SharedPreferences =
        context.getSharedPreferences(FUNCTIONS, Context.MODE_PRIVATE)

    fun lineIndex(context: Context, appWidgetId: Int): Int =
        bus(context).getInt(KEY_LINE_PREFIX + appWidgetId, 0)

    fun reverse(context: Context, appWidgetId: Int): Boolean =
        bus(context).getBoolean(KEY_REVERSE_PREFIX + appWidgetId, false)

    fun actionKeys(context: Context, appWidgetId: Int): List<String> {
        val raw = functions(context).getString(KEY_ACTIONS_PREFIX + appWidgetId, null)
            ?: return WidgetActions.defaultKeys
        return WidgetActions.normalizedKeys(raw)
    }
}

object ScheduleWidgetPrefs {
    const val PREFS = "swun_widget_schedule"
    const val KEY_HIDE_ENDED_PREFIX = "hide_ended_"
    const val KEY_SHOW_TOMORROW_PREFIX = "show_tomorrow_"
    private const val BG_DIR = "schedule_widget_bg"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 默认隐藏今天已经结束的课，和之前小组件的行为保持一致。 */
    fun hideEnded(context: Context, appWidgetId: Int): Boolean =
        prefs(context).getBoolean(KEY_HIDE_ENDED_PREFIX + appWidgetId, true)

    fun setHideEnded(context: Context, appWidgetId: Int, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_ENDED_PREFIX + appWidgetId, value).apply()
    }

    /** 默认展示今天和明天；关闭后宽版只展示今天。 */
    fun showTomorrow(context: Context, appWidgetId: Int): Boolean =
        prefs(context).getBoolean(KEY_SHOW_TOMORROW_PREFIX + appWidgetId, true)

    fun setShowTomorrow(context: Context, appWidgetId: Int, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_TOMORROW_PREFIX + appWidgetId, value).apply()
    }

    fun backgroundFile(context: Context, appWidgetId: Int): File =
        File(File(context.filesDir, BG_DIR), "$appWidgetId.jpg")

    fun hasBackground(context: Context, appWidgetId: Int): Boolean {
        val file = backgroundFile(context, appWidgetId)
        return file.isFile && file.length() > 0
    }

    fun clearBackground(context: Context, appWidgetId: Int) {
        runCatching { backgroundFile(context, appWidgetId).delete() }
    }

    /**
     * 把相册图片复制进应用私有目录。桌面进程无权长期读取相册 Uri，
     * 必须落盘成自己的文件，RemoteViews 才能稳定拿到图片。
     */
    fun saveBackground(context: Context, appWidgetId: Int, uri: Uri): Boolean {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return false
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        var sample = 1
        while (bounds.outWidth / sample * (bounds.outHeight / sample) > 480_000) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return false

        val maxSide = 640f
        val scale = min(1f, maxSide / max(decoded.width, decoded.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        val dir = File(context.filesDir, BG_DIR).apply { mkdirs() }
        val target = File(dir, "$appWidgetId.jpg")
        return runCatching {
            FileOutputStream(target).use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 84, output)
            }
            true
        }.getOrDefault(false).also {
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
    }

    /** 解码成 RemoteViews 可以安全传输的小图。 */
    fun loadBackground(context: Context, appWidgetId: Int): Bitmap? {
        val file = backgroundFile(context, appWidgetId)
        if (!file.isFile || file.length() <= 0) return null
        return runCatching {
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )
        }.getOrNull()
    }
}

object WidgetData {

    private val courseColors = listOf(
        0xFFE5484D.toInt(),
        0xFF12B76A.toInt(),
        0xFF2E90FA.toInt(),
        0xFF9E77ED.toInt(),
        0xFFF79009.toInt(),
        0xFF06AED4.toInt()
    )

    fun loadSchedule(context: Context, hideEnded: Boolean = true): ScheduleSnapshot? {
        val prefs = context.getSharedPreferences(WidgetPrefs.SCHEDULE, Context.MODE_PRIVATE)
        val raw = prefs.getString("raw", null) ?: return null
        val fetchedAt = prefs.getLong("updated_at", 0L)
        val schedule = runCatching { KtkqParser.parse(raw, fetchedAt) }.getOrNull() ?: return null

        val today = LocalDate.now()
        val todayWeek = schedule.weekAt(today)
        val tomorrow = today.plusDays(1)
        val tomorrowWeek = schedule.weekAt(tomorrow)

        val todayCourses = coursesFor(
            schedule.courses,
            today.dayOfWeek.value,
            todayWeek,
            excludePastOn = if (hideEnded) today else null
        )
        val tomorrowCourses = coursesFor(schedule.courses, tomorrow.dayOfWeek.value, tomorrowWeek)

        return ScheduleSnapshot(
            date = today,
            week = todayWeek,
            today = todayCourses,
            tomorrow = tomorrowCourses
        )
    }

    private fun coursesFor(
        courses: List<Course>,
        dayOfWeek: Int,
        week: Int,
        excludePastOn: LocalDate? = null
    ): List<WidgetCourse> =
        courses
            .asSequence()
            .filter {
                it.dayOfWeek == dayOfWeek &&
                    it.occursOn(week) &&
                    (excludePastOn == null || !it.hasEndedOn(excludePastOn))
            }
            .sortedBy { it.startSection }
            .map { course ->
                WidgetCourse(
                    name = course.name,
                    location = buildLocation(course),
                    time = SectionTimes.range(course.startSection, course.endSection)
                        .ifBlank { "时间待定" },
                    color = courseColors[(course.colorKey and 0x7fffffff) % courseColors.size]
                )
            }
            .toList()

    private fun buildLocation(course: Course): String {
        val parts = listOfNotNull(
            course.campus.takeIf { it.isNotBlank() },
            course.room.takeIf { it.isNotBlank() }
        )
        return parts.joinToString(" ").ifBlank { "地点待定" }
    }

    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun dayName(date: LocalDate): String =
        dayNames.getOrElse(date.dayOfWeek.value - 1) { "" }
}
