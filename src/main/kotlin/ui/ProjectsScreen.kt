package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import asana.syncFromAsana
import data.Project
import data.ProjectRepository
import data.SessionRepository
import data.Task
import kotlinx.coroutines.launch
import timer.TimerController

@Composable
fun ProjectsScreen(timerController: TimerController) {
    val scope = rememberCoroutineScope()

    var projects by remember { mutableStateOf(ProjectRepository.getAllProjects()) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }

    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        projects = ProjectRepository.getAllProjects()
        selectedProject?.let { tasks = ProjectRepository.getTasksForProject(it.id) }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {

        // ── Levý panel: Projekty ────────────────────────────────────────────
        Column(Modifier.width(280.dp).fillMaxHeight()) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Projekty", style = MaterialTheme.typography.headlineSmall)
                Row {
                    // Asana sync
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                syncMessage = null
                                val token = ProjectRepository.getSetting("asana_token")
                                val workspace = ProjectRepository.getSetting("asana_workspace_gid")
                                if (token.isBlank() || workspace.isBlank()) {
                                    syncMessage = "Nejprve nastav Asana token v Nastavení"
                                } else {
                                    syncFromAsana(token, workspace)
                                        .onSuccess { count ->
                                            syncMessage = "Synchronizováno $count tasků"
                                            refresh()
                                        }
                                        .onFailure { syncMessage = "Chyba: ${it.message}" }
                                }
                                isSyncing = false
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Sync, "Sync Asana", tint = Color(0xFF2196F3))
                    }
                    IconButton(onClick = { showAddProjectDialog = true }) {
                        Icon(Icons.Default.Add, "Přidat projekt", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            syncMessage?.let {
                Text(it, color = if (it.startsWith("Chyba")) Color(0xFFF44336) else Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(projects) { project ->
                    ProjectItem(
                        project = project,
                        isSelected = selectedProject?.id == project.id,
                        totalSeconds = SessionRepository.getTotalSecondsForProject(project.id),
                        onClick = {
                            selectedProject = project
                            tasks = ProjectRepository.getTasksForProject(project.id)
                        },
                        onDelete = {
                            ProjectRepository.deleteProject(project.id)
                            if (selectedProject?.id == project.id) {
                                selectedProject = null
                                tasks = emptyList()
                            }
                            refresh()
                        }
                    )
                }
            }
        }

        VerticalDivider()

        // ── Pravý panel: Tasky ───────────────────────────────────────────────
        Column(Modifier.fillMaxSize()) {
            if (selectedProject == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Vyber projekt vlevo", color = Color(0xFF9E9E9E))
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedProject!!.name, style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.Add, "Přidat task", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(tasks) { task ->
                        TaskItem(
                            task = task,
                            isActive = timerController.uiState.value.selectedTask?.id == task.id,
                            totalSeconds = SessionRepository.getTotalSecondsForTask(task.id),
                            onClick = { timerController.selectTask(task) },
                            onDelete = {
                                ProjectRepository.deleteTask(task.id)
                                refresh()
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Dialogy ──────────────────────────────────────────────────────────────

    if (showAddProjectDialog) {
        AddNameDialog(
            title = "Nový projekt",
            placeholder = "Název projektu",
            onConfirm = { name ->
                ProjectRepository.insertProject(name)
                refresh()
                showAddProjectDialog = false
            },
            onDismiss = { showAddProjectDialog = false }
        )
    }

    if (showAddTaskDialog && selectedProject != null) {
        AddNameDialog(
            title = "Nový task – ${selectedProject!!.name}",
            placeholder = "Název tasku",
            onConfirm = { name ->
                ProjectRepository.insertTask(selectedProject!!.id, name)
                refresh()
                showAddTaskDialog = false
            },
            onDismiss = { showAddTaskDialog = false }
        )
    }
}

@Composable
private fun ProjectItem(
    project: Project,
    isSelected: Boolean,
    totalSeconds: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(12.dp).clip(CircleShape)
                    .background(hexToColor(project.color))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.bodyMedium)
                if (totalSeconds > 0) {
                    Text(formatHours(totalSeconds), style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    isActive: Boolean,
    totalSeconds: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckBoxOutlineBlank,
                null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (totalSeconds > 0) {
                    Text(formatHours(totalSeconds), style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E))
                }
            }
            if (isActive) {
                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AddNameDialog(
    title: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("Přidat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}

private fun formatHours(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m odpracováno" else "${m}m odpracováno"
}

fun hexToColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val r = clean.substring(0, 2).toInt(16)
    val g = clean.substring(2, 4).toInt(16)
    val b = clean.substring(4, 6).toInt(16)
    return Color(r, g, b)
}