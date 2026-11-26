package com.swun.campus.ui.academic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swun.campus.AppViewModel
import com.swun.campus.data.Course
import com.swun.campus.ui.common.ErrorState

private val weekDayNames = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

/**
 * 课堂考勤原生页。
 *
 * 数据来自 ktkq 官方接口（隐藏 WebView 里取 JSON，Compose 原生渲染），
 * 不再把整个官方 H5 铺在页面上；真正由老师发起的课堂签到仍走官方入口。
 */
@Composable
fun AttendanceScreen(vm: AppViewModel, onOpenWeb: () -> Unit) {
    var selected by remember { mutableStateOf<Course?>(null) }

    LaunchedEffect(Unit) {
        if (vm.ktkqSchedule == null) vm.beginKtkqLoad()
    }

    val schedule = vm.ktkqSchedule
    val weekCourses = schedule?.courses
        ?.filter { it.occursOn(schedule.currentWeek) }
        ?.sortedWith(compareBy({ it.dayOfWeek }, { it.startSection }, { it.name }))
        .orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Header(
            termName = schedule?.termName ?: "正在读取学期…",
            week = schedule?.currentWeek,
            courseCount = weekCourses.size,
            onRefresh = {
                vm.resetKtkq()
                vm.beginKtkqLoad()
            }
        )

        when {
            vm.ktkqLoading && schedule == null -> Loading()

            schedule == null && vm.ktkqError != null -> ErrorState(
                title = "课堂考勤加载失败",
                message = vm.ktkqError.orEmpty(),
                onRetry = { vm.beginKtkqLoad() }
            )

            schedule == null -> Loading()

            weekCourses.isEmpty() -> ErrorState(
                title = "本周没有课程",
                message = "当前周次没有读到课程安排，可以打开官方页面查看完整考勤记录。",
                onRetry = onOpenWeb
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "本周课程",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                items(weekCourses, key = { "${it.name}-${it.dayOfWeek}-${it.startSection}-${it.room}" }) { course ->
                    CourseRow(course) { selected = course }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onOpenWeb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Outlined.FactCheck, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("打开官方签到 / 查看考勤记录", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "签到由任课教师发起，是否签到、迟到或请假以学校系统记录为准。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    selected?.let { course ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(course.name, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    DetailLine("上课时间", "${weekDayNames.getOrElse(course.dayOfWeek - 1) { "" }} · ${com.swun.campus.data.SectionTimes.label(course.startSection, course.endSection)}")
                    DetailLine("上课地点", course.room)
                    DetailLine("上课周次", course.weekRaw)
                    DetailLine("学分", course.credit)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "课堂签到由老师发起，点击下方按钮进入学校官方页面完成。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selected = null
                        onOpenWeb()
                    },
                    shape = MaterialTheme.shapes.small
                ) { Text("去官方签到") }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun Header(
    termName: String,
    week: Int?,
    courseCount: Int,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "课堂考勤",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(termName)
                            if (week != null) append(" · 第 $week 周")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新", modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("刷新", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "原生课表 · 本周 $courseCount 门课",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CourseRow(course: Course, onClick: () -> Unit) {
    val color = colorForCourse(course)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(color.copy(alpha = 0.12f))
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "周${"一二三四五六日"[course.dayOfWeek - 1]}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = "${course.startSection}-${course.endSection}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOf(course.room, course.weekRaw).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "正在读取课堂考勤课表…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
