package com.swun.campus.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swun.campus.ui.theme.SWUNCampusTheme

/**
 * 课表小组件的单实例设置页。
 *
 * 每张桌面课表 widget 都有自己的 appWidgetId，所以显示范围和背景都按 ID
 * 存储，不会出现“设置了一张，桌面上的全变了”的情况。
 */
class ScheduleWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_CANCELED)

        val initialHideEnded = ScheduleWidgetPrefs.hideEnded(this, appWidgetId)
        val initialShowTomorrow = ScheduleWidgetPrefs.showTomorrow(this, appWidgetId)
        val initialHasBackground = ScheduleWidgetPrefs.hasBackground(this, appWidgetId)

        setContent {
            SWUNCampusTheme {
                ScheduleConfigScreen(
                    initialHideEnded = initialHideEnded,
                    initialShowTomorrow = initialShowTomorrow,
                    initialHasBackground = initialHasBackground,
                    onCancel = { finish() },
                    onSave = { hideEnded, showTomorrow, pickedUri, removeBackground ->
                        ScheduleWidgetPrefs.setHideEnded(this, appWidgetId, hideEnded)
                        ScheduleWidgetPrefs.setShowTomorrow(this, appWidgetId, showTomorrow)
                        if (removeBackground) {
                            ScheduleWidgetPrefs.clearBackground(this, appWidgetId)
                        }
                        if (pickedUri != null) {
                            val saved = ScheduleWidgetPrefs.saveBackground(
                                this,
                                appWidgetId,
                                pickedUri
                            )
                            if (!saved) {
                                Toast.makeText(
                                    this,
                                    "图片保存失败，已保留原来的背景",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        ScheduleWidgetProvider.update(
                            this,
                            AppWidgetManager.getInstance(this),
                            appWidgetId
                        )
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleConfigScreen(
    initialHideEnded: Boolean,
    initialShowTomorrow: Boolean,
    initialHasBackground: Boolean,
    onCancel: () -> Unit,
    onSave: (Boolean, Boolean, Uri?, Boolean) -> Unit
) {
    var hideEnded by remember { mutableStateOf(initialHideEnded) }
    var showTomorrow by remember { mutableStateOf(initialShowTomorrow) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var removeBackground by remember { mutableStateOf(false) }
    var hasBackground by remember { mutableStateOf(initialHasBackground) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            removeBackground = false
            hasBackground = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课表小组件") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "取消"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "这些设置只影响桌面上的这一张课表小组件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "隐藏已结束的课",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "开启后，今天已经下课的课程不会出现在小组件里",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = hideEnded, onCheckedChange = { hideEnded = it })
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "显示范围",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RangeOption(
                            title = "今天和明天",
                            subtitle = "宽版双列显示",
                            selected = showTomorrow,
                            modifier = Modifier.weight(1f),
                            onClick = { showTomorrow = true }
                        )
                        RangeOption(
                            title = "只显示今天",
                            subtitle = "宽版也只占今天",
                            selected = !showTomorrow,
                            modifier = Modifier.weight(1f),
                            onClick = { showTomorrow = false }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "窄版（小尺寸）始终只显示今天。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "自定义图片背景",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasBackground) "已设置，图片会铺在卡片内" else "当前使用默认白色卡片",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                            Text(if (hasBackground) "更换图片" else "选择图片")
                        }
                        if (hasBackground) {
                            TextButton(
                                onClick = {
                                    pickedUri = null
                                    removeBackground = true
                                    hasBackground = false
                                }
                            ) {
                                Text("恢复默认")
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "图片会按卡片比例裁剪；为了文字清楚，会叠一层浅色蒙版。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(hideEnded, showTomorrow, pickedUri, removeBackground) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存并更新小组件")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RangeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
