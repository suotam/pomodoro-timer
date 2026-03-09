package notification

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.Color
import java.awt.RenderingHints

object NotificationService {

    private var trayIcon: TrayIcon? = null

    fun initialize() {
        if (!SystemTray.isSupported()) return
        try {
            val tray = SystemTray.getSystemTray()
            val image = createTrayImage()
            trayIcon = TrayIcon(image, "FocusFlow").apply {
                isImageAutoSize = true
            }
            tray.add(trayIcon)
        } catch (e: Exception) {
            // Tray není dostupný – tiché selhání
        }
    }

    fun notifyWorkDone(taskName: String, minutes: Int) {
        show(
            title = "Čas vypršel! ($minutes min)",
            message = "Task: $taskName\nTimer běží dál – zastav ho až budeš připraven.",
            type = TrayIcon.MessageType.INFO
        )
        beep()
    }

    fun notifyBreakDone(minutes: Int) {
        show(
            title = "Pauza skončila! ($minutes min)",
            message = "Čas se vrátit k práci.\nTimer běží dál – zastav ho až budeš připraven.",
            type = TrayIcon.MessageType.INFO
        )
        beep()
    }

    fun notifySessionSaved(taskName: String, durationSeconds: Long) {
        val h = durationSeconds / 3600
        val m = (durationSeconds % 3600) / 60
        val s = durationSeconds % 60
        val timeStr = if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
        show(
            title = "Session uložena",
            message = "Task: $taskName\nOdpracováno: $timeStr",
            type = TrayIcon.MessageType.NONE
        )
    }

    private fun show(title: String, message: String, type: TrayIcon.MessageType) {
        try {
            trayIcon?.displayMessage(title, message, type)
        } catch (e: Exception) {
        }
    }

    private fun beep() {
        Toolkit.getDefaultToolkit().beep()
    }

    private fun createTrayImage(): Image {
        val size = 16
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(76, 175, 80) // Material green
        g.fillOval(1, 1, size - 2, size - 2)
        g.color = Color(255, 255, 255)
        g.fillOval(4, 4, size - 8, size - 8)
        g.dispose()
        return img
    }

    fun dispose() {
        try {
            trayIcon?.let { SystemTray.getSystemTray().remove(it) }
        } catch (e: Exception) {}
    }
}