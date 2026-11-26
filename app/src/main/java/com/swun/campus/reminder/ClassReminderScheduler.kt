package com.swun.campus.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.swun.campus.data.KtkqParser
import com.swun.campus.data.SectionTimes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ClassReminderScheduler {

    private const val CACHE_PREFS = "swun_ktkq_cache"
    private const val HORIZON_DAYS = 14L
    private const val URI_PREFIX = "swun://class-reminder/"

    fun reschedule(context: Context) {
        cancelAll(context)
        val settings = ClassReminderPrefs.load(context)
        if (!settings.enabled) return

        val prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("raw", null) ?: return
        val fetchedAt = prefs.getLong("updated_at", 0L)
        val schedule = runCatching { KtkqParser.parse(raw, fetchedAt) }.getOrNull() ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        val now = LocalDateTime.now()
        val latest = now.plusDays(HORIZON_DAYS + 1)
        val scheduled = mutableSetOf<String>()

        for (offset in 0..HORIZON_DAYS) {
            val date = LocalDate.now().plusDays(offset)
            val week = schedule.weekAt(date)
            schedule.courses.forEach { course ->
                if (course.dayOfWeek != date.dayOfWeek.value || !course.occursOn(week)) return@forEach
                val startText = SectionTimes.start(course.startSection)
                val startTime = runCatching { LocalTime.parse(startText) }.getOrNull()
                    ?: return@forEach
                val trigger = LocalDateTime.of(date, startTime)
                    .minusMinutes(settings.leadMinutes.toLong())
                if (!trigger.isAfter(now) || trigger.isAfter(latest)) return@forEach

                val key = buildKey(date, course.name, course.startSection, course.room)
                val room = listOf(course.campus, course.room)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val intent = Intent(context, ClassReminderReceiver::class.java).apply {
                    data = reminderUri(key)
                    putExtra(ClassReminderReceiver.EXTRA_KEY, key)
                    putExtra(ClassReminderReceiver.EXTRA_COURSE, course.name)
                    putExtra(ClassReminderReceiver.EXTRA_ROOM, room)
                    putExtra(ClassReminderReceiver.EXTRA_START, startText)
                    putExtra(ClassReminderReceiver.EXTRA_LEAD, settings.leadMinutes)
                    putExtra(ClassReminderReceiver.EXTRA_MODE, settings.mode.storageKey)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode(key),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val exact = if (Build.VERSION.SDK_INT >= 31) {
                    runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
                } else {
                    true
                }
                val scheduledOk = runCatching {
                    if (exact) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                }.isSuccess
                if (scheduledOk) scheduled += key
            }
        }
        ClassReminderPrefs.saveScheduledKeys(context, scheduled)
    }

    private fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        ClassReminderPrefs.scheduledKeys(context).forEach { key ->
            val intent = Intent(context, ClassReminderReceiver::class.java).apply {
                data = reminderUri(key)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(key),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            runCatching { alarmManager?.cancel(pendingIntent) }
            pendingIntent.cancel()
        }
        ClassReminderPrefs.clearScheduledKeys(context)
    }

    private fun buildKey(
        date: LocalDate,
        courseName: String,
        startSection: Int,
        room: String
    ): String = "$date|$startSection|$courseName|$room"

    private fun requestCode(key: String): Int = key.hashCode() and 0x7fffffff

    private fun reminderUri(key: String): Uri = Uri.parse(URI_PREFIX + Uri.encode(key))
}
