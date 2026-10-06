package com.dbook.presentation.flight

/** One record of a CSV file and the number of the line it started on (the header is line 1). */
data class CsvRecord(
    val line: Int,
    val cells: List<String>,
)

/**
 * A small RFC 4180 reader: commas, quoted cells with commas, line breaks and doubled quotes inside them, CRLF or LF,
 * and an optional byte order mark. Blank lines are skipped.
 */
object CsvParser {
    fun parse(text: String): List<CsvRecord> = Reader(text.removePrefix("\uFEFF")).read()

    private class Reader(private val input: String) {
        private val records = mutableListOf<CsvRecord>()
        private val cells = mutableListOf<String>()
        private val cell = StringBuilder()
        private var inQuotes = false
        private var line = 1
        private var startLine = 1
        private var position = 0

        fun read(): List<CsvRecord> {
            while (position < input.length) {
                consume(input[position])
                position++
            }
            require(!inQuotes) { "the file ends inside a quoted cell" }
            endRecord()
            return records
        }

        private fun consume(c: Char) {
            when {
                inQuotes && c == '"' && input.getOrNull(position + 1) == '"' -> {
                    cell.append('"')
                    position++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> endCell()
                !inQuotes && (c == '\n' || c == '\r') -> lineBreak(c)
                else -> {
                    if (c == '\n') line++
                    cell.append(c)
                }
            }
        }

        private fun lineBreak(c: Char) {
            if (c == '\r' && input.getOrNull(position + 1) == '\n') position++
            line++
            endRecord()
        }

        private fun endCell() {
            cells += cell.toString()
            cell.clear()
        }

        private fun endRecord() {
            endCell()
            if (cells.any { it.isNotBlank() }) {
                records += CsvRecord(startLine, cells.toList())
            }
            cells.clear()
            startLine = line
        }
    }
}
