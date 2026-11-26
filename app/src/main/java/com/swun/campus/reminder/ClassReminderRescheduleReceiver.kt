package com.swun.campus.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 重启、改时间、改时区或覆盖安装后，AlarmManager 里的闹钟会被清空，需要重排。 */
class ClassReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> ClassReminderScheduler.reschedule(context)
        }
    }
}
