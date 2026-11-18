package com.swun.campus.ui.academic

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swun.campus.AppViewModel
import com.swun.campus.data.Grade
import com.swun.campus.data.Term
import com.swun.campus.ui.common.ErrorState

@Composable
fun GradeScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) {
        if (vm.grades == null) vm.loadGrades(vm.gradeTerm)
    }

    val grades = vm.grades
    var termMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 学期选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssistChip(
                    onClick = { termMenu = true },
                    label = {
                        Text(
                            text = vm.gradeTerm.shortLabel,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = "切换学期",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                DropdownMenu(expanded = termMenu, onDismissRequest = { termMenu = false }) {
                    vm.termOptions.forEach { term ->
                        DropdownMenuItem(
                            text = { Text(term.shortLabel) },
                            onClick = {
                                termMenu = false
                                vm.selectGradeTerm(term)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            grades?.let {
                Text(
                    text = "共 ${it.size} 门",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            vm.gradesLoading -> LoadingBox("正在读取成绩…")

            grades == null && vm.gradesError != null -> ErrorState(
                title = "成绩加载失败",
                message = vm.gradesError.orEmpty(),
                onRetry = { vm.loadGrades(vm.gradeTerm) }
            )

            grades == null -> ErrorState(
                title = "没有读取到成绩",
                message = "可能是登录态失效或服务暂不可用",
                onRetry = { vm.loadGrades(vm.gradeTerm) }
            )

            grades.isEmpty() -> EmptyBox(
                title = "该学期暂无成绩",
                subtitle = "${vm.gradeTerm.label}\n成绩可能尚未录入，换一个学期看看"
            )

            else -> LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SummaryCard(vm.gradeTerm, grades) }
                item { Spacer(Modifier.height(2.dp)) }
                items(grades, key = { it.courseCode + it.courseName }) { GradeRow(it) }
            }
        }
    }
}

@Composable
private fun SummaryCard(term: Term, grades: List<Grade>) {
    val credits = grades.sumOf { it.credit }
    val weighted = grades.filter { it.gpa >= 0 && it.credit > 0 }
    val avgGpa = if (weighted.isEmpty()) 0.0
    else weighted.sumOf { it.credit * it.gpa } / weighted.sumOf { it.credit }
    val passCount = grades.count { scoreValue(it.score) >= 60.0 }
    val failed = grades.size - passCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = term.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Metric("平均绩点", String.format("%.2f", avgGpa), Modifier.weight(1f))
                Metric("修读学分", String.format("%.1f", credits), Modifier.weight(1f))
                Metric(
                    "课程 / 挂科",
                    "${grades.size} / $failed",
                    Modifier.weight(1f),
                    highlight = failed > 0
                )
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GradeRow(grade: Grade) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = grade.courseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (grade.isRetake) {
                        Spacer(Modifier.width(6.dp))
                        Tag("重修", MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(grade.category.ifBlank { "课程" })
                        append(" · ")
                        append(String.format("%.1f", grade.credit))
                        append(" 学分")
                        if (grade.gpa >= 0) {
                            append(" · 绩点 ")
                            append(String.format("%.2f", grade.gpa))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            ScoreBadge(grade.score)
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ScoreBadge(score: String) {
    val value = scoreValue(score)
    val color = scoreColor(value)
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.ifBlank { "—" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun scoreValue(score: String): Double =
    score.toDoubleOrNull() ?: when (score) {
        "优秀" -> 95.0
        "良好" -> 85.0
        "中等" -> 75.0
        "及格" -> 65.0
        "不及格" -> 50.0
        else -> 0.0
    }

private fun scoreColor(value: Double): Color = when {
    value >= 90 -> Color(0xFF2E7D4F)
    value >= 80 -> Color(0xFF1B6CA8)
    value >= 70 -> Color(0xFF9A6B00)
    value >= 60 -> Color(0xFFB4661A)
    else -> Color(0xFFB3261E)
}

@Composable
fun LoadingBox(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyBox(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.6
            )
        }
    }
}
