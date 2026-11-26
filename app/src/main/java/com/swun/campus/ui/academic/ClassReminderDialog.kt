package com.swun.campus.ui.academic

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swun.campus.reminder.ClassReminderMode
import com.swun.campus.reminder.ClassReminderNotifier
import com.swun.campus.reminder.ClassReminderPrefs
import com.swun.campus.reminder.ClassReminderScheduler
import kotlin.math.roundToInt

@Composable
fun ClassReminderDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val initial = remember { ClassReminderPrefs.load(context) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var leadMinutes by remember { mutableStateOf(initial.leadMinutes) }
    var mode by remember { mutableStateOf(initial.mode) }
    var notificationsGranted by remember {
        mutableStateOf(ClassReminderNotifier.hasNotificationPermission(context))
    }
    var exactGranted by remember {
        mutableStateOf(ClassReminderNotifier.exactAlarmAllowed(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (granted) ClassReminderNotifier.ensureChannels(context)
        if (!granted) {
            Toast.makeText(context, "通知权限未开启，提醒不会显示", Toast.LENGTH_LONG).show()
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开课前提醒") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用提醒", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "按课表里的开课时间，在设定时间前发出提醒。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { next ->
                            enabled = next
                            if (next && !notificationsGranted) {
                                requestNotificationPermission()
                            }
                        }
                    )
                }

                if (enabled) {
                    Spacer(Modifier.height(18.dp))
                    Text("提前时间", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = leadMinutes.toFloat(),
                            onValueChange = {
                                leadMinutes = it.roundToInt()
                                    .coerceIn(
                                        ClassReminderPrefs.MIN_LEAD_MINUTES,
                                        ClassReminderPrefs.MAX_LEAD_MINUTES
                                    )
                            },
                            valueRange = ClassReminderPrefs.MIN_LEAD_MINUTES.toFloat()..
                                ClassReminderPrefs.MAX_LEAD_MINUTES.toFloat(),
                            steps = ClassReminderPrefs.MAX_LEAD_MINUTES -
                                ClassReminderPrefs.MIN_LEAD_MINUTES - 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "$leadMinutes 分钟",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                    Text(
                        "最多提前 ${ClassReminderPrefs.MAX_LEAD_MINUTES} 分钟；0 分钟不在可选范围内。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(18.dp))
                    Text("提醒方式", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    ClassReminderMode.values().forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mode = item }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == item,
                                onClick = { mode = item }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (!notificationsGranted) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (Build.VERSION.SDK_INT >= 33) {
                            "通知权限未开启。请在系统弹窗中允许通知；若已拒绝，可到系统设置里打开。"
                        } else {
                            "系统通知被关闭。请在系统设置里允许民大校园发送通知。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { requestNotificationPermission() }) {
                            Text("开启通知权限")
                        }
                        TextButton(onClick = { openNotificationSettings(context) }) {
                            Text("通知设置")
                        }
                    }
                }

                if (enabled && !exactGranted) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "精确提醒权限未开启，系统可能把提醒延迟几分钟。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            openExactAlarmSettings(context)
                            exactGranted = ClassReminderNotifier.exactAlarmAllowed(context)
                        }) {
                            Text("开启精确提醒")
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "提醒由手机本地调度；强制停止 App、系统清理后台或关机后，需要重新打开 App 才会恢复。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ClassReminderPrefs.save(context, enabled, leadMinutes, mode)
                ClassReminderNotifier.ensureChannels(context)
                ClassReminderScheduler.reschedule(context)
                if (enabled && !notificationsGranted) {
                    Toast.makeText(
                        context,
                        "提醒已保存，但通知权限未开启，暂时不会显示",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        if (enabled) "已保存，将提前 $leadMinutes 分钟提醒" else "已关闭开课前提醒",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            Toast.makeText(context, "无法打开系统通知设置", Toast.LENGTH_SHORT).show()
        }
}

private fun openExactAlarmSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < 31) return
    val intent = Intent(
        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        Uri.parse("package:${context.packageName}")
    )
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
}
