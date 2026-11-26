package com.swun.campus.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swun.campus.R
import com.swun.campus.ui.theme.SWUNCampusTheme

class FunctionsWidgetConfigActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ADD_SLOT = "extra_add_slot"
    }

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
        val addingSlot = intent?.getBooleanExtra(EXTRA_ADD_SLOT, false) == true

        val prefs = WidgetPrefs.functions(this)
        val initialKeys = WidgetPrefs.actionKeys(this, appWidgetId)

        setContent {
            SWUNCampusTheme {
                FunctionsConfigScreen(
                    initialKeys = initialKeys,
                    addingSlot = addingSlot,
                    onCancel = { finish() },
                    onSave = { keys ->
                        prefs.edit()
                            .putString(
                                WidgetPrefs.KEY_ACTIONS_PREFIX + appWidgetId,
                                keys.joinToString("|")
                            )
                            .apply()
                        FunctionsWidgetProvider.update(
                            this,
                            AppWidgetManager.getInstance(this),
                            appWidgetId
                        )
                        val result = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(RESULT_OK, result)
                        if (addingSlot) {
                            // 从桌面卡片的“+”进来时，保存后直接回桌面，而不是回到 App 登录页。
                            startActivity(
                                Intent(Intent.ACTION_MAIN)
                                    .addCategory(Intent.CATEGORY_HOME)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionsConfigScreen(
    initialKeys: List<String>,
    addingSlot: Boolean,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val initialCount = if (addingSlot) {
        WidgetActions.MAX_SLOTS
    } else {
        initialKeys.count { WidgetActions.byKey(it) != WidgetActions.unset }
            .coerceIn(1, WidgetActions.MAX_SLOTS)
    }
    var count by remember { mutableIntStateOf(initialCount) }
    var keys by remember {
        mutableStateOf(
            initialKeys + List((initialCount - initialKeys.size).coerceAtLeast(0)) { "" }
        )
    }
    var pickerIndex by remember { mutableIntStateOf(-1) }

    fun selectCount(target: Int) {
        count = target
        pickerIndex = -1
        if (keys.size < target) {
            keys = keys + List(target - keys.size) { "" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("功能卡片小组件") },
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
                text = "选择按钮数量",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "桌面卡片会按数量自动适配：1 个铺满，2 个上下排列，3 个时第 4 格显示“＋”；4×2 大尺寸最多可放 8 个。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            (1..WidgetActions.MAX_SLOTS).chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { number ->
                        FilterChip(
                            selected = count == number,
                            onClick = { selectCount(number) },
                            label = { Text("$number 个") }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(14.dp))

            repeat(count) { index ->
                val key = keys.getOrElse(index) { "" }
                val action = WidgetActions.byKey(key)
                Box {
                    Card(
                        onClick = {
                            pickerIndex = if (pickerIndex == index) -1 else index
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(
                                    if (action.iconRes != 0) action.iconRes else R.drawable.ic_widget_generic
                                ),
                                contentDescription = null,
                                modifier = Modifier.width(26.dp).height(26.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "按钮 ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = "更改",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = pickerIndex == index,
                        onDismissRequest = { pickerIndex = -1 }
                    ) {
                        WidgetActions.all.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (option.iconRes != 0) {
                                            Image(
                                                painter = painterResource(option.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.width(22.dp).height(22.dp)
                                            )
                                        }
                                        Text(option.label)
                                    }
                                },
                                onClick = {
                                    val mutable = keys.toMutableList()
                                    while (mutable.size <= index) mutable.add("")
                                    mutable[index] = option.key
                                    keys = mutable
                                    pickerIndex = -1
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = when {
                    count == 3 -> "第 4 格在桌面会显示为“＋”，点它会回到这里继续添加第 4 个按钮。"
                    count > 4 -> "4×2 大尺寸会排成 4×2 网格；如果当前只拉到 2×2，会先显示前 4 个。"
                    else -> "小组件支持拉伸：矮而宽时自动横排，方形或高一些时用网格。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val saved = keys.take(count) +
                        List((count - keys.size).coerceAtLeast(0)) { "" }
                    onSave(saved)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
