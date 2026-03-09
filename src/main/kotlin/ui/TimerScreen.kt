package ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Project
import data.ProjectRepository
import data.Task
import timer.TimerController
import timer.TimerPhase
import timer.TimerState

@Composable
fun TimerScreen(controller: TimerController) {
    val state by controller.uiState.collectAsState()

    var projects by remember { mutableStateOf(ProjectRepository.getAllProjects()) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var projectExpanded by remember { mutableStateOf(false) }
    var taskExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedTask) {
        val task = state.selectedTask ?: return@LaunchedEffect
        if (selectedProject == null) {
            projects = ProjectRepository.getAllProjects()
            val proj = projects.find { p ->
                ProjectRepository.getTasksForProject(p.id).any { it.id == task.id }
            }
            if (proj != null) {
                selectedProject = proj
                tasks = ProjectRepository.getTasksForProject(proj.id)
            }
        }
    }

    val timerColor by animateColorAsState(
        targetValue = when {
            state.phase == TimerPhase.BREAK -> Color(0xFF2196F3)
            state.isOvertime -> Color(0xFFFF5722)
            state.phase == TimerPhase.WORK -> Color(0xFF4CAF50)
            else -> Color(0xFF9E9E9E)
        },
        animationSpec = tween(500)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Card(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(bottom = 36.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Text(
                    "Aktivní task",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9E9E9E)
                )

                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            projects = ProjectRepository.getAllProjects()
                            projectExpanded = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.state != TimerState.RUNNING && state.state != TimerState.OVERTIME
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            selectedProject?.name ?: "Vyber projekt…",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = projectExpanded,
                        onDismissRequest = { projectExpanded = false }
                    ) {
                        if (projects.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Žádné projekty – přidej je v záložce Projekty", color = Color(0xFF9E9E9E)) },
                                onClick = { projectExpanded = false }
                            )
                        }
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .padding(end = 0.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier.fillMaxSize(),
                                                shape = MaterialTheme.shapes.small,
                                                color = hexToColor(project.color)
                                            ) {}
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(project.name)
                                    }
                                },
                                onClick = {
                                    selectedProject = project
                                    tasks = ProjectRepository.getTasksForProject(project.id)
                                    // Reset task pokud je z jiného projektu
                                    if (state.selectedTask != null &&
                                        tasks.none { it.id == state.selectedTask!!.id }) {
                                        controller.clearTask()
                                    }
                                    projectExpanded = false
                                    taskExpanded = true  // rovnou otevři task dropdown
                                }
                            )
                        }
                    }
                }

                if (selectedProject != null) {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                tasks = ProjectRepository.getTasksForProject(selectedProject!!.id)
                                taskExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.state != TimerState.RUNNING && state.state != TimerState.OVERTIME,
                            colors = if (state.selectedTask != null)
                                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                state.selectedTask?.name ?: "Vyber task…",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = taskExpanded,
                            onDismissRequest = { taskExpanded = false }
                        ) {
                            if (tasks.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Žádné tasky v tomto projektu", color = Color(0xFF9E9E9E)) },
                                    onClick = { taskExpanded = false }
                                )
                            }
                            tasks.forEach { task ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckBoxOutlineBlank,
                                                null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFF9E9E9E)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(task.name)
                                        }
                                    },
                                    onClick = {
                                        controller.selectTask(task)
                                        taskExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = when (state.phase) {
                TimerPhase.WORK -> if (state.isOvertime) " PRÁCE – OVERTIME" else " PRÁCE"
                TimerPhase.BREAK -> if (state.isOvertime) " PAUZA – OVERTIME" else " PAUZA"
                TimerPhase.IDLE -> "—"
            },
            color = timerColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = controller.formatSeconds(state.elapsedSeconds),
            fontSize = 80.sp,
            fontWeight = FontWeight.Thin,
            color = timerColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (state.phase != TimerPhase.IDLE) {
            Text(
                text = "Cíl: ${controller.formatSeconds(state.targetSeconds)}",
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        } else {
            Spacer(Modifier.height(46.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { controller.startWork() },
                enabled = state.selectedTask != null && state.state != TimerState.RUNNING,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.height(52.dp).widthIn(min = 140.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Spustit práci", fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = {
                    if (state.state == TimerState.RUNNING || state.state == TimerState.OVERTIME) {
                        controller.stop()
                        controller.startBreak()
                    } else {
                        controller.startBreak()
                    }
                },
                enabled = state.selectedTask != null,
                modifier = Modifier.height(52.dp).widthIn(min = 120.dp)
            ) {
                Icon(Icons.Default.Coffee, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pauza")
            }

            if (state.state == TimerState.RUNNING || state.state == TimerState.OVERTIME) {
                Button(
                    onClick = { controller.stop() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Stop", fontWeight = FontWeight.Medium)
                }
            }
        }

        if (state.phase != TimerPhase.IDLE && !state.isOvertime) {
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = {
                    (state.elapsedSeconds.toFloat() / state.targetSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
                },
                modifier = Modifier.width(400.dp).height(6.dp),
                color = timerColor,
                trackColor = Color(0xFF333333)
            )
        }
    }
}