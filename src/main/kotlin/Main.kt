import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import data.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import notification.NotificationService
import timer.TimerController
import ui.App

fun main() = application {
    Database.initialize()
    NotificationService.initialize()

    val timerScope = CoroutineScope(Dispatchers.Default)
    val timerController = TimerController(timerScope)

    val windowState = rememberWindowState(size = DpSize(1000.dp, 680.dp))

    Window(
        onCloseRequest = {
            timerController.stop()
            NotificationService.dispose()
            exitApplication()
        },
        title = "FocusFlow ",
        state = windowState
    ) {
        App(timerController)
    }
}