package com.dbook.presentation.flight

import com.dbook.application.flight.FlightImportResult
import com.dbook.application.flight.FlightImportRow
import com.dbook.application.flight.ImportFlightsUseCase

/** Turns the records of the file into rows keyed by the header; a header without a required column is refused. */
fun List<CsvRecord>.toImportRows(): List<FlightImportRow> {
    require(isNotEmpty()) { "the file is empty: it needs a header line" }
    val header = first().cells.map { it.trim() }
    val missing = ImportFlightsUseCase.COLUMNS.filterNot { it in header }
    require(missing.isEmpty()) { "the header is missing the columns: ${missing.joinToString()}" }
    return drop(1).map { record ->
        FlightImportRow(record.line, header.zip(record.cells).toMap())
    }
}

data class FlightImportResponse(
    val dryRun: Boolean,
    val totalRows: Int,
    val toCreate: Int,
    val alreadyExisting: Int,
    val created: Int,
    val errors: List<RowErrorResponse>,
) {
    data class RowErrorResponse(val line: Int, val message: String)

    companion object {
        fun from(result: FlightImportResult) =
            FlightImportResponse(
                result.dryRun,
                result.totalRows,
                result.toCreate,
                result.alreadyExisting,
                result.created,
                result.errors.map { RowErrorResponse(it.line, it.message) },
            )
    }
}
