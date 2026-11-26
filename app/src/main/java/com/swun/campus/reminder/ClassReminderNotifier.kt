package com.swun.campus.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.swun.campus.R
import com.swun.campus.ui.Routes
import com.swun.campus.widget.WidgetNavigation

object ClassReminderNotifier {

    private const val CHANNEL_CHIME = "class_reminder_chime"
    private const val CHANNEL_CHIME_VIBRATE = "class_reminder_chime_vibrate"
    private const val CHANNEL_STRONG = "class_reminder_strong"
    private const val CHANNEL_LIGHT = "class_reminder_light"
    private const val CHANNEL_SILENT = "class_reminder_silent"

    private val mediumVibration = longArrayOf(0, 250, 180, 250)
    private val strongVibration = longArrayOf(0, 600, 280, 600, 280, 900)
    private val lightVibration = longArrayOf(0, 120, 160, 120)

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val soundUri = chimeUri(context)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        fun channel(id: String, name: String, description: String): NotificationChannel =
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

        channel(CHANNEL_CHIME, "上课提醒 · 专属提示音", "播放内置上课铃声")
            .apply {
                setSound(soundUri, audioAttributes)
                enableVibration(false)
            }.also(manager::createNotificationChannel)

        channel(CHANNEL_CHIME_VIBRATE, "上课提醒 · 提示音和震动", "播放内置铃声并中等震动")
            .apply {
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = mediumVibration
            }.also(manager::createNotificationChannel)

        channel(CHANNEL_STRONG, "上课提醒 · 强震动", "静音，长震动提醒")
            .apply {
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = strongVibration
            }.also(manager::createNotificationChannel)

        channel(CHANNEL_LIGHT, "上课提醒 · 轻震动", "静音，短震动提醒")
            .apply {
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = lightVibration
            }.also(manager::createNotificationChannel)

        channel(CHANNEL_SILENT, "上课提醒 · 仅通知", "只在通知栏显示")
            .apply {
                setSound(null, null)
                enableVibration(false)
            }.also(manager::createNotificationChannel)
    }

    fun show(
        context: Context,
        key: String,
        courseName: String,
        room: String,
        startText: String,
        leadMinutes: Int,
        mode: ClassReminderMode
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val channelId = channelIdFor(mode)
        val whenText = if (leadMinutes > 0) {
            "《$courseName》将在 $leadMinutes 分钟后开始"
        } else {
            "《$courseName》现在开始上课"
        }
        val detail = listOf(startText, room)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_class)
            .setContentTitle("上课提醒")
            .setContentText(whenText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (detail.isBlank()) whenText else "$whenText\n$detail"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(
                WidgetNavigation.pendingIntent(
                    context,
                    Routes.SCHEDULE,
                    requestCode = 15000 + notificationId(key)
                )
            )

        if (Build.VERSION.SDK_INT < 26) {
            when (mode) {
                ClassReminderMode.CHIME -> builder.setSound(chimeUri(context))
                ClassReminderMode.CHIME_VIBRATE -> builder
                    .setSound(chimeUri(context))
                    .setVibrate(mediumVibration)
                ClassReminderMode.STRONG_VIBRATE -> builder.setVibrate(strongVibration)
                ClassReminderMode.LIGHT_VIBRATE -> builder.setVibrate(lightVibration)
                ClassReminderMode.SILENT -> builder.setDefaults(0)
            }
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId(key), builder.build())
        }
    }

    private fun channelIdFor(mode: ClassReminderMode): String = when (mode) {
        ClassReminderMode.CHIME -> CHANNEL_CHIME
        ClassReminderMode.CHIME_VIBRATE -> CHANNEL_CHIME_VIBRATE
        ClassReminderMode.STRONG_VIBRATE -> CHANNEL_STRONG
        ClassReminderMode.LIGHT_VIBRATE -> CHANNEL_LIGHT
        ClassReminderMode.SILENT -> CHANNEL_SILENT
    }

    private fun notificationId(key: String): Int =
        (key.hashCode() and 0x7fffffff).coerceAtLeast(1)

    private fun chimeUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/class_chime")

    fun exactAlarmAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val manager = context.getSystemService(android.app.AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }
}
