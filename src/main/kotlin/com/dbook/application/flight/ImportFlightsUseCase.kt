package com.dbook.application.flight

import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.flight.AdminFlightReader
import com.dbook.domain.flight.AirlineNotFoundException
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.SeatClass
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/** One data line of the file, with the number of the line it came from and its cells by column name. */
data class FlightImportRow(
    val line: Int,
    val cells: Map<String, String>,
)

data class FlightImportCommand(
    val actor: Actor,
    val rows: List<FlightImportRow>,
    val dryRun: Boolean,
)

data class RowError(
    val line: Int,
    val message: String,
)

/** [created] is 0 on a dry run, and on any run that had errors (nothing is written then). */
data class FlightImportResult(
    val dryRun: Boolean,
    val totalRows: Int,
    val toCreate: Int,
    val alreadyExisting: Int,
    val created: Int,
    val errors: List<RowError>,
)

/**
 * Imports flights from a file, all or nothing. It always validates every line first and reports **every** error with
 * its line (not just the first), so the file can be fixed in one go. Only a run that is not a dry run **and** has no
 * errors writes anything, in one transaction. Repeatable: a line whose flight number and departure already exist is
 * counted as such and skipped, so sending the same file twice creates nothing the second time.
 */
@Observed(name = "dbook.usecase")
@Service
class ImportFlightsUseCase(
    private val catalogLookup: CatalogLookup,
    private val adminFlightReader: AdminFlightReader,
    private val registerFlightUseCase: RegisterFlightUseCase,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: FlightImportCommand): FlightImportResult {
        require(command.rows.size <= MAX_ROWS) { "a file has at most $MAX_ROWS lines" }
        val errors = mutableListOf<RowError>()
        val toCreate = mutableListOf<RegisterFlightCommand>()
        val seen = mutableMapOf<Pair<String, LocalDateTime>, Int>()

        var existing = 0
        command.rows.forEach { row ->
            when (val outcome = classify(row, command.actor, seen)) {
                is RowOutcome.Invalid -> errors += RowError(row.line, outcome.message)
                RowOutcome.Existing -> existing++
                is RowOutcome.New -> toCreate += outcome.command
            }
        }

        val created = if (command.dryRun || errors.isNotEmpty()) 0 else write(command.actor, toCreate, existing)
        return FlightImportResult(command.dryRun, command.rows.size, toCreate.size, existing, created, errors)
    }

    // A line's problems are all one kind of thing here: a value that is not valid, or a code that is not known.
    private fun classify(
        row: FlightImportRow,
        actor: Actor,
        seen: MutableMap<Pair<String, LocalDateTime>, Int>,
    ): RowOutcome =
        try {
            val flight = parse(row, actor)
            val firstLine = seen.putIfAbsent(flight.flightNumber to flight.departureTime, row.line)
            when {
                firstLine != null -> RowOutcome.Invalid("the same flight and departure as line $firstLine")
                adminFlightReader.exists(flight.flightNumber, flight.departureTime) -> RowOutcome.Existing
                else -> RowOutcome.New(flight)
            }
        } catch (ex: IllegalArgumentException) {
            RowOutcome.Invalid(ex.message ?: "invalid line")
        } catch (ex: AirlineNotFoundException) {
            RowOutcome.Invalid(ex.message ?: "unknown airline")
        } catch (ex: AirportNotFoundException) {
            RowOutcome.Invalid(ex.message ?: "unknown airport")
        }

    private sealed interface RowOutcome {
        data class Invalid(val message: String) : RowOutcome

        data object Existing : RowOutcome

        data class New(val command: RegisterFlightCommand) : RowOutcome
    }

    private fun write(
        actor: Actor,
        flights: List<RegisterFlightCommand>,
        existing: Int,
    ): Int {
        flights.forEach { registerFlightUseCase.execute(it) }
        auditLog.record(
            AuditEvent(
                actor = actor,
                action = AuditAction.FLIGHTS_IMPORTED,
                targetId = "import",
                after = mapOf("created" to flights.size, "alreadyExisting" to existing),
            ),
        )
        return flights.size
    }

    // Builds the real Flight too, only to run its rules (arrival after departure, different airports) on every line.
    private fun parse(
        row: FlightImportRow,
        actor: Actor,
    ): RegisterFlightCommand {
        val command =
            RegisterFlightCommand(
                actor = actor,
                flightNumber = cell(row, "flightNumber"),
                airlineIataCode = cell(row, "airlineIataCode").uppercase(),
                originIataCode = cell(row, "originIataCode").uppercase(),
                destinationIataCode = cell(row, "destinationIataCode").uppercase(),
                departureTime = parseTime(row, "departureTime"),
                arrivalTime = parseTime(row, "arrivalTime"),
                seatClass = parseSeatClass(cell(row, "seatClass")),
                price = parsePrice(cell(row, "price")),
                totalCapacity = parseCapacity(cell(row, "totalCapacity")),
                aircraftType = cell(row, "aircraftType"),
            )
        validate(command)
        return command
    }

    private fun validate(command: RegisterFlightCommand) {
        val airline = catalogLookup.airline(command.airlineIataCode)
        val origin = catalogLookup.airport(command.originIataCode)
        val destination = catalogLookup.airport(command.destinationIataCode)
        Flight(
            title = command.flightNumber,
            price = command.price,
            totalCapacity = command.totalCapacity,
            availableCapacity = command.totalCapacity,
            flightNumber = command.flightNumber,
            airline = airline,
            origin = origin,
            destination = destination,
            departureTime = command.departureTime,
            arrivalTime = command.arrivalTime,
            seatClass = command.seatClass,
            aircraftType = command.aircraftType,
        )
    }

    private fun cell(
        row: FlightImportRow,
        column: String,
    ): String =
        row.cells[column]?.trim()?.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("$column is empty")

    private fun parseTime(
        row: FlightImportRow,
        column: String,
    ): LocalDateTime =
        try {
            LocalDateTime.parse(cell(row, column))
        } catch (ex: java.time.format.DateTimeParseException) {
            throw IllegalArgumentException("$column must look like 2026-10-01T08:00:00", ex)
        }

    private fun parseSeatClass(value: String): SeatClass =
        SeatClass.entries.firstOrNull { it.name == value.uppercase() }
            ?: throw IllegalArgumentException("seatClass must be one of ${SeatClass.entries.joinToString()}")

    private fun parsePrice(value: String): BigDecimal =
        requireNotNull(value.toBigDecimalOrNull()) { "price must be a number like 450.00" }

    private fun parseCapacity(value: String): Int =
        value.toIntOrNull()?.takeIf {
            it > 0
        } ?: throw IllegalArgumentException("totalCapacity must be a positive whole number")

    companion object {
        const val MAX_ROWS = 5000

        val COLUMNS =
            listOf(
                "flightNumber", "airlineIataCode", "originIataCode", "destinationIataCode", "departureTime",
                "arrivalTime", "seatClass", "price", "totalCapacity", "aircraftType",
            )
    }
}
