package com.swun.campus.reminder

import android.content.Context

/** 提醒方式：声音、震动都由系统通知渠道承载，避免 App 被杀后还要自己响铃。 */
enum class ClassReminderMode(
    val storageKey: String,
    val label: String,
    val description: String
) {
    CHIME("chime", "专属提示音", "播放内置的上课铃声，不震动"),
    CHIME_VIBRATE("chime_vibrate", "提示音 + 震动", "内置铃声配合一次中等震动"),
    STRONG_VIBRATE("strong", "强震动", "静音，长震动，适合手机放在包里"),
    LIGHT_VIBRATE("light", "轻震动", "静音，短震动，适合课堂内提醒"),
    SILENT("silent", "仅通知", "只在通知栏显示，不响铃不震动");

    companion object {
        fun fromStorage(value: String?): ClassReminderMode =
            values().firstOrNull { it.storageKey == value } ?: CHIME_VIBRATE
    }
}

data class ClassReminderSettings(
    val enabled: Boolean = false,
    val leadMinutes: Int = 15,
    val mode: ClassReminderMode = ClassReminderMode.CHIME_VIBRATE
)

object ClassReminderPrefs {

    const val MAX_LEAD_MINUTES = 60
    const val MIN_LEAD_MINUTES = 1

    private const val PREFS = "swun_class_reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LEAD = "lead_minutes"
    private const val KEY_MODE = "mode"
    private const val KEY_SCHEDULED = "scheduled_keys"

    fun load(context: Context): ClassReminderSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ClassReminderSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            leadMinutes = prefs.getInt(KEY_LEAD, 15)
                .coerceIn(MIN_LEAD_MINUTES, MAX_LEAD_MINUTES),
            mode = ClassReminderMode.fromStorage(prefs.getString(KEY_MODE, null))
        )
    }

    fun save(
        context: Context,
        enabled: Boolean,
        leadMinutes: Int,
        mode: ClassReminderMode
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_LEAD, leadMinutes.coerceIn(MIN_LEAD_MINUTES, MAX_LEAD_MINUTES))
            .putString(KEY_MODE, mode.storageKey)
            .apply()
    }

    internal fun scheduledKeys(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SCHEDULED, emptySet())
            .orEmpty()
            .toSet()

    internal fun saveScheduledKeys(context: Context, keys: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_SCHEDULED, keys)
            .apply()
    }

    internal fun clearScheduledKeys(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_SCHEDULED)
            .apply()
    }
}
