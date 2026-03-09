package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import asana.AsanaClient
import data.ProjectRepository
import kotlinx.coroutines.launch
import timer.TimerController

@Composable
fun SettingsScreen(timerController: TimerController) {
    val scope = rememberCoroutineScope()

    var workMin by remember { mutableStateOf(timerController.workDurationMinutes.toString()) }
    var breakMin by remember { mutableStateOf(timerController.breakDurationMinutes.toString()) }

    var asanaToken by remember { mutableStateOf(ProjectRepository.getSetting("asana_token")) }
    var showToken by remember { mutableStateOf(false) }

    var workspaces by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // gid, name
    var selectedWorkspaceGid by remember { mutableStateOf(ProjectRepository.getSetting("asana_workspace_gid")) }
    var selectedWorkspaceName by remember { mutableStateOf("") }

    var isFetchingWorkspaces by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Nastavení", style = MaterialTheme.typography.headlineMedium)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("⏱  Časy", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = workMin,
                        onValueChange = { if (it.length <= 3) workMin = it },
                        label = { Text("Práce (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = breakMin,
                        onValueChange = { if (it.length <= 3) breakMin = it },
                        label = { Text("Pauza (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🔗  Asana integrace", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Personal Access Token najdeš na: app.asana.com → Profile Settings → Apps → Personal access tokens",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )

                OutlinedTextField(
                    value = asanaToken,
                    onValueChange = { asanaToken = it },
                    label = { Text("Asana Personal Access Token") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        scope.launch {
                            isFetchingWorkspaces = true
                            message = null
                            runCatching {
                                val client = AsanaClient(asanaToken.trim())
                                val ws = client.getWorkspaces()
                                client.close()
                                ws.map { it.gid to it.name }
                            }.onSuccess {
                                workspaces = it
                                message = "Načteno ${it.size} workspace(s)"
                                if (it.size == 1) selectedWorkspaceGid = it.first().first
                            }.onFailure {
                                message = "Chyba: ${it.message}"
                            }
                            isFetchingWorkspaces = false
                        }
                    },
                    enabled = asanaToken.isNotBlank() && !isFetchingWorkspaces
                ) {
                    if (isFetchingWorkspaces) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Načíst workspaces")
                }

                if (workspaces.isNotEmpty()) {
                    Text("Workspace:", style = MaterialTheme.typography.bodyMedium)
                    workspaces.forEach { (gid, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedWorkspaceGid == gid,
                                onClick = { selectedWorkspaceGid = gid; selectedWorkspaceName = name }
                            )
                            Text(name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                message?.let {
                    Text(
                        it,
                        color = if (it.startsWith("Chyba")) Color(0xFFF44336) else Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val w = workMin.toIntOrNull() ?: 25
                    val b = breakMin.toIntOrNull() ?: 5
                    timerController.saveSettings(w, b)
                    ProjectRepository.setSetting("asana_token", asanaToken.trim())
                    if (selectedWorkspaceGid.isNotBlank()) {
                        ProjectRepository.setSetting("asana_workspace_gid", selectedWorkspaceGid)
                    }
                    saved = true
                }
            ) {
                Text("Uložit nastavení")
            }

            if (saved) {
                Text("✓ Uloženo", color = Color(0xFF4CAF50))
            }
        }
    }
}