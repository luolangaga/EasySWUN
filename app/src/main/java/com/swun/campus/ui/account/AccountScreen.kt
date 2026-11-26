package com.swun.campus.ui.account

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swun.campus.AppViewModel
import com.swun.campus.BuildConfig
import com.swun.campus.R
import com.swun.campus.ui.common.CampusTabBarContentPadding
import com.swun.campus.widget.BusWidgetConfigActivity
import com.swun.campus.widget.BusWidgetProvider
import com.swun.campus.widget.FunctionsWidgetConfigActivity
import com.swun.campus.widget.FunctionsWidgetProvider
import com.swun.campus.widget.ScheduleWidgetConfigActivity
import com.swun.campus.widget.ScheduleWidgetProvider
import kotlinx.coroutines.delay

private const val GITHUB_URL = "https://github.com/luolangaga/EasySWUN"

@Composable
fun AccountScreen(vm: AppViewModel) {
    val profile = vm.profile
    var showConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showWidgets by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile?.name?.take(1) ?: "学",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = profile?.name?.ifBlank { "未获取到姓名" } ?: "未获取到姓名",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = profile?.studentNo?.ifBlank { vm.savedAccount } ?: vm.savedAccount,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                InfoRow(Icons.Outlined.School, "学院", profile?.college.orEmpty())
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                InfoRow(Icons.Outlined.AccountBalance, "专业", profile?.major.orEmpty())
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                InfoRow(Icons.Outlined.Class, "班级", profile?.className.orEmpty())
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                InfoRow(Icons.Outlined.Badge, "年级", profile?.grade.orEmpty())
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { showConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = MaterialTheme.shapes.small,
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            )
        ) {
            Icon(
                Icons.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            Text("退出登录", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showAbout = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text("关于民大校园", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showWidgets = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                Icons.Outlined.Widgets,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text("桌面小组件", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "民大校园 v${BuildConfig.VERSION_NAME} · 非官方校园客户端",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(Modifier.height(CampusTabBarContentPadding))
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("退出登录") },
            text = { Text("退出后需要重新输入学号和密码才能查看教务数据，是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    vm.logout()
                }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (showWidgets) {
        WidgetsDialog(onDismiss = { showWidgets = false })
    }
}

@Composable
private fun WidgetsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val scheduleProvider = remember {
        ComponentName(context, ScheduleWidgetProvider::class.java)
    }
    val busProvider = remember {
        ComponentName(context, BusWidgetProvider::class.java)
    }
    val functionsProvider = remember {
        ComponentName(context, FunctionsWidgetProvider::class.java)
    }
    // 系统添加小组件的确认页关闭后，当前页面可能不会自动重组；
    // 对话框打开期间轻量轮询一次 ID，让“已有 N 个 / 设置”及时更新。
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            revision++
        }
    }
    val scheduleIds = remember(revision) { manager.getAppWidgetIds(scheduleProvider) }
    val busIds = remember(revision) { manager.getAppWidgetIds(busProvider) }
    val functionsIds = remember(revision) { manager.getAppWidgetIds(functionsProvider) }
    var pinFailureName by remember { mutableStateOf<String?>(null) }

    val addWidget: (ComponentName, String) -> Unit = { provider, name ->
        val requested = runCatching {
            manager.requestPinAppWidget(provider, null, null)
        }.getOrDefault(false)
        if (requested) {
            Toast.makeText(
                context,
                "已向桌面发送添加请求，请在系统提示中确认；若没有提示，请长按桌面从小组件列表添加",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // 部分国产桌面（如 MIUI/HyperOS）不支持 requestPinAppWidget，
            // 只弹一个 Toast 用户会以为按钮没反应。这里给出明确的兜底步骤。
            pinFailureName = name
        }
    }
    val openScheduleConfig: (Int) -> Unit = { id ->
        context.startActivity(
            Intent(context, ScheduleWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
    val openBusConfig: (Int) -> Unit = { id ->
        context.startActivity(
            Intent(context, BusWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
    val openFunctionsConfig: (Int) -> Unit = { id ->
        context.startActivity(
            Intent(context, FunctionsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("桌面小组件") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "把课表、校车和常用功能放到桌面。课表可以设置是否隐藏已结束课程、显示范围和图片背景；校车和功能卡片可设置线路/按钮。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                WidgetEntry(
                    title = "课表",
                    subtitle = "今天 / 明天 · 可设置隐藏与背景",
                    icon = Icons.Outlined.School,
                    count = scheduleIds.size,
                    onAdd = { addWidget(scheduleProvider, "课表") },
                    onConfigure = scheduleIds.firstOrNull()?.let { id ->
                        { openScheduleConfig(id) }
                    }
                )
                Spacer(Modifier.height(10.dp))
                WidgetEntry(
                    title = "校车",
                    subtitle = "线路 / 方向 / 下一班",
                    icon = Icons.Outlined.DirectionsBus,
                    count = busIds.size,
                    onAdd = { addWidget(busProvider, "校车") },
                    onConfigure = busIds.firstOrNull()?.let { id ->
                        { openBusConfig(id) }
                    }
                )
                Spacer(Modifier.height(10.dp))
                WidgetEntry(
                    title = "功能卡片",
                    subtitle = "1–4 个快捷按钮，按数量自适应",
                    icon = Icons.Outlined.Widgets,
                    count = functionsIds.size,
                    onAdd = { addWidget(functionsProvider, "功能卡片") },
                    onConfigure = functionsIds.firstOrNull()?.let { id ->
                        { openFunctionsConfig(id) }
                    }
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "如果桌面没有弹出添加提示，可长按桌面 → 小组件 → 民大校园，从列表添加。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )

    val failedName = pinFailureName
    if (failedName != null) {
        AlertDialog(
            onDismissRequest = { pinFailureName = null },
            title = { Text("添加到桌面") },
            text = {
                Text(
                    "当前桌面没有接受一键添加请求。\n\n" +
                        "请长按桌面空白处 → 小组件 → 民大校园 → 选择「$failedName」拖到桌面。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pinFailureName = null
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_HOME)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("去桌面") }
            },
            dismissButton = {
                TextButton(onClick = { pinFailureName = null }) { Text("知道了") }
            }
        )
    }
}

@Composable
private fun WidgetEntry(
    title: String,
    subtitle: String,
    icon: ImageVector,
    count: Int,
    onAdd: () -> Unit,
    onConfigure: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (count > 0) "桌面已有 $count 个" else "尚未添加",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (count > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onConfigure != null) {
                    TextButton(onClick = onConfigure) { Text("设置") }
                }
                OutlinedButton(onClick = onAdd) { Text("添加到桌面") }
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val openGithub: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
        }
        Unit
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_handdrawn),
                    contentDescription = "手绘版西南民族大学校徽",
                    modifier = Modifier.size(92.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "民大校园",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME} · 非官方校园客户端",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(6.dp))
                AboutLine("开源仓库", "github.com/luolangaga/EasySWUN")
                AboutLine("开发者", "luolangaga")
                AboutLine("数据来源", "学校教务 / 一卡通 / 学工官方系统")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "本项目用于个人学习与校园生活，所有数据来自学校官方系统；与学校官方无关，请勿用于商业用途。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = openGithub) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
    }
}
