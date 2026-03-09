package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.DayStats
import data.ProjectRepository
import data.SessionRepository

@Composable
fun StatsScreen() {
    var filterDays by remember { mutableStateOf(14) }
    var dayStats by remember { mutableStateOf(SessionRepository.getDayStats(filterDays)) }

    val projects = remember { ProjectRepository.getAllProjects() }
    val projectStats = remember(projects) {
        projects.map { p ->
            p to SessionRepository.getTotalSecondsForProject(p.id)
        }.filter { it.second > 0 }.sortedByDescending { it.second }
    }

    Column(Modifier.fillMaxSize()) {

        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Statistiky", style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7 dní", 14 to "14 dní", 30 to "30 dní").forEach { (days, label) ->
                    FilterChip(
                        selected = filterDays == days,
                        onClick = {
                            filterDays = days
                            dayStats = SessionRepository.getDayStats(days)
                        },
                        label = { Text(label) }
                    )
                }
            }
        }

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {

            Column(Modifier.weight(1.4f)) {

                Text(
                    "Odpracované hodiny po dnech",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (dayStats.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Zatím žádná data", color = Color(0xFF9E9E9E))
                        }
                    } else {
                        BarChart(
                            data = dayStats,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                val totalWork = dayStats.sumOf { it.workSeconds }
                val activeDays = dayStats.count { it.workSeconds > 0 }
                val maxDay = dayStats.maxByOrNull { it.workSeconds }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniStatCard("Celkem", formatHours(totalWork), Color(0xFF4CAF50), Modifier.weight(1f))
                    MiniStatCard("Aktivní dny", "$activeDays / $filterDays", Color(0xFF2196F3), Modifier.weight(1f))
                    MiniStatCard(
                        "Nejlepší den",
                        if (maxDay != null) "${formatDate(maxDay.date)}\n${formatHours(maxDay.workSeconds)}" else "–",
                        Color(0xFFFF9800),
                        Modifier.weight(1f)
                    )
                }
            }

            Column(Modifier.weight(1f)) {

                Text(
                    "Čas podle projektů",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (projectStats.isEmpty()) {
                            Text("Zatím žádná data", color = Color(0xFF9E9E9E))
                        } else {
                            val maxSeconds = projectStats.first().second

                            projectStats.forEach { (project, seconds) ->
                                ProjectStatRow(
                                    name = project.name,
                                    seconds = seconds,
                                    maxSeconds = maxSeconds,
                                    color = hexToColor(project.color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarChart(data: List<DayStats>, modifier: Modifier = Modifier) {
    val maxSeconds = data.maxOfOrNull { it.workSeconds }?.coerceAtLeast(1) ?: 1
    val barColor = Color(0xFF4CAF50)
    val breakColor = Color(0xFF2196F3)
    val gridColor = Color(0xFF2A2A2A)
    val labelColor = Color(0xFF9E9E9E)

    Canvas(modifier) {
        val barAreaWidth = size.width
        val barAreaHeight = size.height - 30.dp.toPx()  // prostor pro popisky
        val barCount = data.size
        val totalBarWidth = barAreaWidth / barCount
        val barWidth = (totalBarWidth * 0.6f).coerceAtLeast(8f)
        val breakBarWidth = barWidth * 0.4f

        for (i in 1..4) {
            val y = barAreaHeight * (1f - i / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(barAreaWidth, y), strokeWidth = 1f)
        }

        data.forEachIndexed { i, day ->
            val centerX = totalBarWidth * i + totalBarWidth / 2f

            val workHeight = (day.workSeconds.toFloat() / maxSeconds * barAreaHeight).coerceAtLeast(0f)
            if (workHeight > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, barAreaHeight - workHeight),
                    size = Size(barWidth, workHeight),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }

            val breakHeight = (day.breakSeconds.toFloat() / maxSeconds * barAreaHeight * 0.6f).coerceAtLeast(0f)
            if (breakHeight > 0) {
                drawRoundRect(
                    color = breakColor.copy(alpha = 0.6f),
                    topLeft = Offset(centerX + barWidth / 2f + 2f, barAreaHeight - breakHeight),
                    size = Size(breakBarWidth, breakHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
private fun ProjectStatRow(name: String, seconds: Long, maxSeconds: Long, color: Color) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                formatHours(seconds),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            val fraction = (seconds.toFloat() / maxSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatHours(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatDate(date: String): String {
    return try {
        val parts = date.split("-")
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        "$day. $month."
    } catch (e: Exception) { date }
}