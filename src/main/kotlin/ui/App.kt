package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import timer.TimerController

enum class Screen { TIMER, PROJECTS, HISTORY, STATS, EXPORT, SETTINGS }

data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

@Composable
fun App(timerController: TimerController) {
    var currentScreen by remember { mutableStateOf(Screen.TIMER) }

    val navItems = listOf(
        NavItem(Screen.TIMER,    Icons.Default.Timer,          "Timer"),
        NavItem(Screen.PROJECTS, Icons.Default.Folder,         "Projekty"),
        NavItem(Screen.HISTORY,  Icons.Default.History,        "Historie"),
        NavItem(Screen.STATS,    Icons.Default.BarChart,       "Statistiky"),
        NavItem(Screen.EXPORT,   Icons.Default.Download,       "Export"),
        NavItem(Screen.SETTINGS, Icons.Default.Settings,       "Nastavení")
    )

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4CAF50),
            secondary = Color(0xFF2196F3),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(Modifier.height(16.dp))
                navItems.forEach { item ->
                    NavigationRailItem(
                        selected = currentScreen == item.screen,
                        onClick = { currentScreen = item.screen },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
            ) {
                when (currentScreen) {
                    Screen.TIMER    -> TimerScreen(timerController)
                    Screen.PROJECTS -> ProjectsScreen(timerController)
                    Screen.HISTORY  -> HistoryScreen()
                    Screen.STATS    -> StatsScreen()
                    Screen.EXPORT   -> ExportScreen()
                    Screen.SETTINGS -> SettingsScreen(timerController)
                }
            }
        }
    }
}