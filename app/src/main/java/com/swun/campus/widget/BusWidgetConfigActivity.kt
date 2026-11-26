package com.swun.campus.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swun.campus.data.BusSchedule
import com.swun.campus.ui.theme.SWUNCampusTheme

class BusWidgetConfigActivity : ComponentActivity() {

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

        val prefs = WidgetPrefs.bus(this)
        val initialLine = prefs.getInt(WidgetPrefs.KEY_LINE_PREFIX + appWidgetId, 0)
            .coerceIn(0, BusSchedule.lines.lastIndex)
        val initialReverse = prefs.getBoolean(WidgetPrefs.KEY_REVERSE_PREFIX + appWidgetId, false)

        setContent {
            SWUNCampusTheme {
                BusConfigScreen(
                    initialLine = initialLine,
                    initialReverse = initialReverse,
                    onCancel = { finish() },
                    onSave = { line, reverse ->
                        prefs.edit()
                            .putInt(WidgetPrefs.KEY_LINE_PREFIX + appWidgetId, line)
                            .putBoolean(WidgetPrefs.KEY_REVERSE_PREFIX + appWidgetId, reverse)
                            .apply()
                        BusWidgetProvider.update(
                            this,
                            AppWidgetManager.getInstance(this),
                            appWidgetId
                        )
                        val result = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(RESULT_OK, result)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusConfigScreen(
    initialLine: Int,
    initialReverse: Boolean,
    onCancel: () -> Unit,
    onSave: (Int, Boolean) -> Unit
) {
    var lineIndex by remember { mutableIntStateOf(initialLine) }
    var reverse by remember { mutableStateOf(initialReverse) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("校车小组件") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "取消")
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
                text = "选择这条小组件要显示的线路",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "小组件会自动显示当前日期的下一班，点击可进入完整时刻表。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            BusSchedule.lines.forEachIndexed { index, line ->
                Card(
                    onClick = { lineIndex = index },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (index == lineIndex) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = index == lineIndex, onClick = { lineIndex = index })
                        Column(Modifier.weight(1f)) {
                            Text(line.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${line.fromName} → ${line.toName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("方向", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val line = BusSchedule.lines[lineIndex.coerceIn(0, BusSchedule.lines.lastIndex)]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DirectionCard(
                    title = "去程",
                    subtitle = "${line.fromName} → ${line.toName}",
                    selected = !reverse,
                    modifier = Modifier.weight(1f),
                    onClick = { reverse = false }
                )
                DirectionCard(
                    title = "回程",
                    subtitle = "${line.toName} → ${line.fromName}",
                    selected = reverse,
                    modifier = Modifier.weight(1f),
                    onClick = { reverse = true }
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(lineIndex, reverse) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存并添加到桌面")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DirectionCard(
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
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
