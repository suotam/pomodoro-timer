package export

import data.ExportRow
import java.io.File

object CsvExporter {

    fun export(rows: List<ExportRow>, file: File) {
        val sb = StringBuilder()

        sb.appendLine("Datum,Projekt,Task,Typ,Trvání,Trvání (sec)")

        val taskGroups = rows.groupBy { it.projectName to it.taskName }

        taskGroups.forEach { (_, taskRows) ->
            for (row in taskRows) {
                val type = if (row.type == "work") "Práce" else "Pauza"
                val duration = formatDuration(row.durationSeconds)
                sb.appendLine(
                    listOf(
                        row.date,
                        "\"${row.projectName}\"",
                        "\"${row.taskName}\"",
                        type,
                        duration,
                        row.durationSeconds.toString()
                    ).joinToString(",")
                )
            }

            val taskWork = taskRows.filter { it.type == "work" }.sumOf { it.durationSeconds }
            val taskName = taskRows.first().taskName
            sb.appendLine("\"Součet (práce)\",\"\",\"$taskName\",,${formatDuration(taskWork)},$taskWork")
        }

        if (rows.isNotEmpty()) {
            val totalWork = rows.filter { it.type == "work" }.sumOf { it.durationSeconds }
            sb.appendLine()
            sb.appendLine("CELKEM (práce),,,,${formatDuration(totalWork)},$totalWork")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}