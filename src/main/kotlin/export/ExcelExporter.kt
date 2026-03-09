package export

import data.ExportRow
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

object ExcelExporter {

    fun export(rows: List<ExportRow>, file: File) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("FocusFlow Export")

        val headerFont = workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            setFillForegroundColor(IndexedColors.DARK_TEAL.index)
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val altRowStyle = workbook.createCellStyle().apply {
            setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.index)
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val headers = listOf("Datum", "Projekt", "Task", "Typ", "Trvání", "Trvání (sec)")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, title ->
            headerRow.createCell(i).apply {
                setCellValue(title)
                cellStyle = headerStyle
            }
        }

        rows.forEachIndexed { rowIndex, row ->
            val dataRow = sheet.createRow(rowIndex + 1)
            val style = if (rowIndex % 2 == 1) altRowStyle else null

            fun cell(col: Int, value: String) = dataRow.createCell(col).apply {
                setCellValue(value)
                if (style != null) cellStyle = style
            }

            cell(0, row.date)
            cell(1, row.projectName)
            cell(2, row.taskName)
            cell(3, if (row.type == "work") "Práce" else "Pauza")
            cell(4, formatDuration(row.durationSeconds))
            dataRow.createCell(5).apply {
                setCellValue(row.durationSeconds.toDouble())
                if (style != null) cellStyle = style
            }
        }

        if (rows.isNotEmpty()) {
            val sumRow = sheet.createRow(rows.size + 1)
            sumRow.createCell(0).setCellValue("CELKEM (práce)")
            val totalWork = rows.filter { it.type == "work" }.sumOf { it.durationSeconds }
            sumRow.createCell(4).setCellValue(formatDuration(totalWork))
            sumRow.createCell(5).setCellValue(totalWork.toDouble())
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        workbook.write(file.outputStream())
        workbook.close()
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}