package com.swun.campus.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swun.campus.AppViewModel
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

private data class BusTimetable(
    val label: String,
    val fromName: String,
    val toName: String,
    val weekdayOut: List<String>,
    val weekdayBack: List<String>,
    val weekendOut: List<String>,
    val weekendBack: List<String>,
    val holidayOut: List<String>,
    val holidayBack: List<String>,
    val note: String
)

private val busTimetables = listOf(
    BusTimetable(
        label = "武侯 ↔ 航空港",
        fromName = "武侯校区",
        toName = "航空港校区",
        weekdayOut = listOf("7:40", "9:40", "11:20", "13:10", "15:10", "17:20", "18:10", "21:40"),
        weekdayBack = listOf("7:40", "8:30", "10:30", "12:20", "13:10", "15:10", "16:00", "17:20", "17:50", "20:50", "21:40", "22:30"),
        weekendOut = listOf("7:40", "9:40", "11:20", "13:10", "15:10", "17:20", "18:10", "21:40"),
        weekendBack = listOf("7:40", "8:30", "10:30", "12:20", "13:10", "15:10", "16:00", "17:20", "17:50", "20:50", "21:40", "22:30"),
        holidayOut = listOf("8:30", "9:40", "14:30", "17:10", "19:00", "21:30"),
        holidayBack = listOf("9:00", "10:30", "15:30", "18:00", "20:00", "22:30"),
        note = "武侯→航空港 7:40 在武侯校区发车；航空港→武侯 7:40 班次在图书馆廊桥旁候车。"
    ),
    BusTimetable(
        label = "太平园 ↔ 航空港",
        fromName = "太平园校区",
        toName = "航空港校区",
        weekdayOut = listOf("7:40", "9:40", "10:30", "12:20", "13:10", "16:00", "17:50", "20:50"),
        weekdayBack = listOf("7:40", "9:40", "10:30", "13:10", "15:10", "16:00", "17:50", "20:50"),
        weekendOut = listOf("7:40", "9:40", "13:10", "17:00", "20:00"),
        weekendBack = listOf("8:30", "12:20", "16:00", "17:50", "20:50"),
        holidayOut = listOf("9:00", "13:10", "20:00"),
        holidayBack = listOf("9:40", "14:00", "20:50"),
        note = "周末与法定节假日班次不同，节假日按表内「法定节假日」一栏执行。"
    ),
    BusTimetable(
        label = "武侯 ↔ 太平园",
        fromName = "武侯校区",
        toName = "太平园校区",
        weekdayOut = listOf("8:00", "9:50", "13:30", "15:20", "18:20"),
        weekdayBack = listOf("8:30", "10:20", "12:10", "15:50", "17:45", "20:45"),
        weekendOut = listOf("8:30", "16:30"),
        weekendBack = listOf("9:00", "17:00"),
        holidayOut = listOf("8:30", "16:30"),
        holidayBack = listOf("9:00", "17:00"),
        note = "武侯校区乘车点在教工宿舍（一教楼）与站台之间的转弯处；周末与节假日班次较少。"
    )
)

@Composable
fun BusScreen(vm: AppViewModel) {
    var lineIndex by remember { mutableIntStateOf(0) }
    var reverse by remember { mutableStateOf(false) }
    var dayMode by remember {
        val weekend = LocalDate.now().dayOfWeek == DayOfWeek.SATURDAY ||
            LocalDate.now().dayOfWeek == DayOfWeek.SUNDAY
        mutableIntStateOf(if (weekend) 1 else 0)
    }
    var now by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }

    val line = busTimetables[lineIndex]
    val times = when (dayMode) {
        2 -> if (!reverse) line.holidayOut else line.holidayBack
        1 -> if (!reverse) line.weekendOut else line.weekendBack
        else -> if (!reverse) line.weekdayOut else line.weekdayBack
    }.map { raw ->
        raw.replace('：', ':').split(':').joinToString(":") { part -> part.padStart(2, '0') }
    }
    val next = times.firstOrNull { LocalTime.parse(it) >= now }
    val fromName = if (!reverse) line.fromName else line.toName
    val toName = if (!reverse) line.toName else line.fromName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            text = "校车时刻",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "后勤处通勤车 · 三校区",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DirectionsBus,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (next != null) "下一班 $next" else "今日已收班",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$fromName → $toName · ${dayModeLabel(dayMode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
                if (next != null) {
                    val minutes = Duration.between(now, LocalTime.parse(next)).toMinutes()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "距发车约 ${minutes} 分钟",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "线路",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            busTimetables.forEachIndexed { index, item ->
                FilterChip(
                    selected = lineIndex == index,
                    onClick = {
                        lineIndex = index
                        reverse = false
                    },
                    label = { Text(item.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "方向",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DirectionChip(
                text = "${line.fromName} → ${line.toName}",
                selected = !reverse,
                onClick = { reverse = false },
                modifier = Modifier.weight(1f)
            )
            DirectionChip(
                text = "${line.toName} → ${line.fromName}",
                selected = reverse,
                onClick = { reverse = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "日期",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("工作日", "周末", "法定节假日").forEachIndexed { index, label ->
                FilterChip(
                    selected = dayMode == index,
                    onClick = { dayMode = index },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "发车时刻",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${times.size} 班",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                times.chunked(4).forEach { rowTimes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTimes.forEach { time ->
                            val isNext = time == next
                            val isPast = LocalTime.parse(time) < now
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(11.dp),
                                color = when {
                                    isNext -> MaterialTheme.colorScheme.primary
                                    isPast -> MaterialTheme.colorScheme.surfaceContainerLow
                                    else -> MaterialTheme.colorScheme.surfaceContainer
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isNext -> MaterialTheme.colorScheme.onPrimary
                                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                        repeat(4 - rowTimes.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = line.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "通勤车主要保障教职工通勤，学生乘车请听从现场调度。咨询 85523689，应急 18981829767。时刻以学校后勤处最新通知为准。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

private fun dayModeLabel(mode: Int): String = when (mode) {
    2 -> "法定节假日"
    1 -> "周末"
    else -> "工作日"
}

@Composable
private fun DirectionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
