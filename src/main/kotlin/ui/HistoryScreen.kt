package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Session
import data.SessionRepository

@Composable
fun HistoryScreen() {
    var sessions by remember { mutableStateOf(SessionRepository.getRecentSessions(7)) }
    var filterDays by remember { mutableStateOf(7) }

    val grouped = sessions.groupBy { it.startedAt.take(10) }
        .toSortedMap(reverseOrder())

    Column(Modifier.fillMaxSize()) {

        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Historie sessions", style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Dnes", 7 to "7 dní", 14 to "14 dní", 30 to "30 dní").forEach { (days, label) ->
                    FilterChip(
                        selected = filterDays == days,
                        onClick = {
                            filterDays = days
                            sessions = SessionRepository.getRecentSessions(days)
                        },
                        label = { Text(label) }
                    )
                }
            }
        }

        val totalWork = sessions.filter { it.type == "work" }.sumOf { it.durationSeconds }
        val totalSessions = sessions.count { it.type == "work" }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                label = "Celkem práce",
                value = formatDuration(totalWork),
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Počet sessions",
                value = "$totalSessions",
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Průměr / den",
                value = if (filterDays > 0) formatDuration(totalWork / filterDays) else "–",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        if (grouped.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF444444)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Žádné sessions za zvolené období", color = Color(0xFF9E9E9E))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                grouped.forEach { (date, daySessions) ->
                    item {
                        DayGroup(
                            date = date,
                            sessions = daySessions,
                            onDelete = { id ->
                                SessionRepository.deleteSession(id)
                                sessions = SessionRepository.getRecentSessions(filterDays)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DayGroup(date: String, sessions: List<Session>, onDelete: (Long) -> Unit) {
    val workSeconds = sessions.filter { it.type == "work" }.sumOf { it.durationSeconds }

    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        formatDate(date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${sessions.count { it.type == "work" }} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            Text(
                formatDuration(workSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )
        }

        sessions.sortedByDescending { it.startedAt }.forEach { session ->
            SessionRow(session = session, onDelete = { onDelete(session.id) })
        }
    }
}

@Composable
private fun SessionRow(session: Session, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.type == "work")
                MaterialTheme.colorScheme.surface
            else
                Color(0xFF1A2A3A)
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (session.type == "work") Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )
            )
            Spacer(Modifier.width(12.dp))

            Text(
                session.startedAt.takeLast(8).take(5),  // HH:MM
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.width(40.dp)
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    session.taskName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    session.projectName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (session.type == "work")
                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                else
                    Color(0xFF2196F3).copy(alpha = 0.15f)
            ) {
                Text(
                    if (session.type == "work") "Práce" else "Pauza",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (session.type == "work") Color(0xFF4CAF50) else Color(0xFF2196F3)
                )
            }
            Spacer(Modifier.width(12.dp))

            Text(
                formatDuration(session.durationSeconds),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(60.dp)
            )

            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFF555555), modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Smazat session?") },
            text = { Text("Tato akce je nevratná.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Smazat") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Zrušit") }
            }
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%dh %02dm" .format(h, m)
    else if (m > 0) "%dm %02ds".format(m, s)
    else "${s}s"
}

private fun formatDate(date: String): String {
    return try {
        val parts = date.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        val cal = java.util.Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        val dayName = arrayOf("Ne", "Po", "Út", "St", "Čt", "Pá", "So")[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        "$dayName $day. $month."
    } catch (e: Exception) {
        date
    }
}