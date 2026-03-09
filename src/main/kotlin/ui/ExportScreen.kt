package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.Project
import data.ProjectRepository
import data.SessionRepository
import export.CsvExporter
import export.ExcelExporter
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ExportScreen() {
    val projects = remember { ProjectRepository.getAllProjects() }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(true) }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Export dat", style = MaterialTheme.typography.headlineMedium)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text("Filtrovat data", style = MaterialTheme.typography.titleMedium)

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedProject?.name ?: "Všechny projekty")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Všechny projekty") },
                            onClick = { selectedProject = null; expanded = false }
                        )
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = { selectedProject = project; expanded = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dateFrom,
                        onValueChange = { dateFrom = it },
                        label = { Text("Od (YYYY-MM-DD)") },
                        singleLine = true,
                        placeholder = { Text("2024-01-01") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateTo,
                        onValueChange = { dateTo = it },
                        label = { Text("Do (YYYY-MM-DD)") },
                        singleLine = true,
                        placeholder = { Text(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "Nechej datumy prázdné pro export všech dat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val result = doExport(selectedProject, dateFrom, dateTo, "xlsx")
                    exportMessage = result.first
                    isSuccess = result.second
                },
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportovat XLSX")
            }

            OutlinedButton(
                onClick = {
                    val result = doExport(selectedProject, dateFrom, dateTo, "csv")
                    exportMessage = result.first
                    isSuccess = result.second
                },
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportovat CSV")
            }
        }

        exportMessage?.let {
            Text(
                text = it,
                color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun doExport(
    project: Project?,
    dateFrom: String,
    dateTo: String
    ,format: String
): Pair<String, Boolean> {
    return try {
        val rows = SessionRepository.getSessionsForExport(
            projectId = project?.id,
            dateFrom = dateFrom.ifBlank { null },
            dateTo = dateTo.ifBlank { null }
        )

        if (rows.isEmpty()) {
            return "Žádná data k exportu pro zvolené filtry." to false
        }

        val projectLabel = project?.name?.replace(" ", "_") ?: "vsechny_projekty"
        val dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val filename = "focusflow_${projectLabel}_$dateLabel.$format"

        // Uloží na plochu uživatele
        val desktop = System.getProperty("user.home") + "/Desktop"
        val file = File("$desktop/$filename")

        if (format == "xlsx") ExcelExporter.export(rows, file)
        else CsvExporter.export(rows, file)

        "Exportováno ${rows.size} záznamů → $filename" to true
    } catch (e: Exception) {
        "Chyba při exportu: ${e.message}" to false
    }
}