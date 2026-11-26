package com.swun.campus.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClassReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val course = intent.getStringExtra(EXTRA_COURSE).orEmpty()
        val room = intent.getStringExtra(EXTRA_ROOM).orEmpty()
        val start = intent.getStringExtra(EXTRA_START).orEmpty()
        val lead = intent.getIntExtra(EXTRA_LEAD, 15)
        val mode = ClassReminderMode.fromStorage(intent.getStringExtra(EXTRA_MODE))
        if (course.isBlank()) return
        ClassReminderNotifier.show(
            context = context,
            key = key,
            courseName = course,
            room = room,
            startText = start,
            leadMinutes = lead,
            mode = mode
        )
    }

    companion object {
        const val EXTRA_KEY = "com.swun.campus.extra.REMINDER_KEY"
        const val EXTRA_COURSE = "com.swun.campus.extra.REMINDER_COURSE"
        const val EXTRA_ROOM = "com.swun.campus.extra.REMINDER_ROOM"
        const val EXTRA_START = "com.swun.campus.extra.REMINDER_START"
        const val EXTRA_LEAD = "com.swun.campus.extra.REMINDER_LEAD"
        const val EXTRA_MODE = "com.swun.campus.extra.REMINDER_MODE"
    }
}
