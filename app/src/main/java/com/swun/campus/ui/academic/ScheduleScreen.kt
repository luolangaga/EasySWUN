package com.swun.campus.ui.academic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swun.campus.AppViewModel
import com.swun.campus.data.Course
import com.swun.campus.data.Term
import com.swun.campus.ui.common.ErrorState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
private val dayFullNames = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

private val coursePalette = listOf(
    Color(0xFF1A6FB5), Color(0xFF2E7D5B), Color(0xFFB26A00), Color(0xFF6F52C4),
    Color(0xFF00737F), Color(0xFFB03060), Color(0xFF3D4EA8), Color(0xFF7A6A00)
)

fun colorForCourse(course: Course): Color =
    coursePalette[Math.floorMod(course.colorKey, coursePalette.size)]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: AppViewModel) {
    var week by remember { mutableStateOf(1) }
    var gridMode by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Course?>(null) }
    var termMenu by remember { mutableStateOf(false) }
    var startDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 原正方教务课表不再作为主课表；主课表来自 ktkq 课堂考勤移动端。
        if (vm.ktkqSchedule == null) vm.beginKtkqLoad()
        if (vm.calendar == null) vm.loadCalendar()
    }

    val schedule = vm.ktkqSchedule
    val timetable = vm.timetable
    val activeTerm = timetable?.term ?: vm.currentTerm
    val currentWeek = schedule?.currentWeek ?: vm.weekNumber(activeTerm)
    val weeksTotal = schedule?.maxWeek ?: weekRangeMax(vm)

    // 开学日或学期变化时自动跳到对应周次
    LaunchedEffect(activeTerm, currentWeek) {
        week = (currentWeek ?: 1).coerceIn(1, weeksTotal)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { termMenu = true },
                    label = {
                        Text(
                            schedule?.termName ?: activeTerm.shortLabel,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = "切换学期",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                DropdownMenu(expanded = termMenu, onDismissRequest = { termMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(schedule?.termName ?: activeTerm.label) },
                        onClick = {
                            termMenu = false
                            if (vm.ktkqSchedule == null) vm.beginKtkqLoad()
                        }
                    )
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = { vm.beginKtkqLoad() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "刷新课表",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(2.dp))
                IconButton(
                    onClick = { gridMode = !gridMode },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (gridMode) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                        contentDescription = if (gridMode) "切换到列表" else "切换到网格",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { week = (week - 1).coerceAtLeast(1) },
                    enabled = week > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "上一周",
                        modifier = Modifier.size(20.dp),
                        tint = if (week > 1) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "第 $week 周",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = buildString {
                            if (currentWeek == week) {
                                append("本周 · 共 $weeksTotal 周")
                            } else {
                                append("共 $weeksTotal 周")
                                currentWeek?.let { append(" · 本周第 $it 周") }
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { week++ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "下一周",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        when {
            vm.ktkqLoading && schedule == null -> LoadingBox("正在读取课堂考勤课表…")

            schedule == null && vm.ktkqError != null -> ErrorState(
                title = "课表加载失败",
                message = vm.ktkqError.orEmpty(),
                onRetry = { vm.beginKtkqLoad() }
            )

            schedule == null -> LoadingBox("正在连接课堂考勤…")

            else -> {
                val visible = schedule.courses.filter { it.occursOn(week) }
                if (gridMode) {
                    TimetableGridView(visible, week, onSelect = { selected = it })
                } else {
                    TimetableListView(visible, week, onSelect = { selected = it })
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
                    DetailLine("上课时间", "${dayFullNames.getOrElse(course.dayOfWeek - 1) { "" }} · ${course.startSection}-${course.endSection} 节")
                    DetailLine("上课地点", "${course.campus} ${course.room}".trim())
                    DetailLine("任课教师", course.teacher)
                    DetailLine("上课周次", course.weekRaw)
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("知道了") } }
        )
    }

    if (startDialog) {
        SemesterStartDialog(
            initial = vm.semesterStart,
            onDismiss = { startDialog = false },
            onConfirm = { date ->
                vm.setSemesterStart(date)
                week = (vm.weekNumber(activeTerm) ?: 1).coerceAtLeast(1)
                startDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterStartDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toUtcMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.selectedDateMillis?.toLocalDateUtc()) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) { Text("跟随校历") }
        }
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
            title = {
                Text(
                    text = "选择开学日",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun weekRangeMax(vm: AppViewModel): Int {
    val maxSchedule = vm.ktkqSchedule?.maxWeek ?: 0
    val courses = vm.timetable?.courses.orEmpty()
    val max = maxSchedule.takeIf { it > 0 }
        ?: courses.flatMap { it.weekRanges }.maxOfOrNull { it.last }
        ?: 16
    return max.coerceAtLeast(16)
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(72.dp),
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

// ------------------------------------------------------------------ 网格视图

@Composable
private fun TimetableGridView(courses: List<Course>, week: Int, onSelect: (Course) -> Unit) {
    val rowHeight = 52.dp
    val sectionCount = 12
    val todayIndex = LocalDate.now().dayOfWeek.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(22.dp))
            dayNames.forEachIndexed { index, day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (index + 1 == todayIndex) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index + 1 == todayIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.width(22.dp)) {
                repeat(sectionCount) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            repeat(7) { dayIndex ->
                val dayCourses = courses.filter { it.dayOfWeek == dayIndex + 1 }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(rowHeight * sectionCount)
                ) {
                    Column {
                        repeat(sectionCount) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                            Spacer(Modifier.height(rowHeight - 1.dp))
                        }
                    }

                    dayCourses.forEach { course ->
                        val color = colorForCourse(course)
                        Box(
                            modifier = Modifier
                                .offset(y = rowHeight * (course.startSection - 1))
                                .fillMaxWidth()
                                .height(rowHeight * course.sectionSpan)
                                .padding(horizontal = 1.dp, vertical = 1.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(color.copy(alpha = 0.12f))
                                .clickable { onSelect(course) }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = color,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = course.room,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        lineHeight = 10.sp
                                    ),
                                    color = color.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ------------------------------------------------------------------ 列表视图

@Composable
private fun TimetableListView(courses: List<Course>, week: Int, onSelect: (Course) -> Unit) {
    val today = LocalDate.now().dayOfWeek.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (day in 1..7) {
            val dayCourses = courses
                .filter { it.dayOfWeek == day }
                .sortedBy { it.startSection }

            item(key = "header-$day") {
                Row(
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayFullNames[day - 1],
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (day == today) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground
                    )
                    if (day == today) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "今天",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (dayCourses.isEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "无课",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            items(dayCourses, key = { "$day-${it.name}-${it.startSection}-${it.room}" }) { course ->
                CourseCard(course) { onSelect(course) }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    val color = colorForCourse(course)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .width(52.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(color.copy(alpha = 0.12f))
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${course.startSection}-${course.endSection}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = "节",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = color.copy(alpha = 0.7f)
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
                    text = listOfNotNull(
                        course.room.takeIf { it.isNotBlank() },
                        course.teacher.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.weekRaw,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
