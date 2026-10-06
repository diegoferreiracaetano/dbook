package com.dbook.application.flight

import com.dbook.application.pricing.FlightPriceRecorder
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.FlightRepository
import com.dbook.domain.flight.SeatClass
import com.dbook.domain.flight.toAuditSnapshot
import com.dbook.domain.identity.Actor
import com.dbook.domain.seating.SeatRepository
import com.dbook.domain.seating.generateSeats
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

data class RegisterFlightCommand(
    val actor: Actor,
    val flightNumber: String,
    val airlineIataCode: String,
    val originIataCode: String,
    val destinationIataCode: String,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
    val price: BigDecimal,
    val totalCapacity: Int,
    val aircraftType: String,
)

/**
 * Registers a new [Flight], resolving origin/destination by IATA code, and generates its
 * seat map — seats per row and column letters come from [seatLayoutFor], based on
 * [RegisterFlightCommand.aircraftType], so different aircraft really do get different
 * layouts (2+2, 3+3, 3+4+3...). Requires ADMIN.
 */
@Observed(name = "dbook.usecase")
@Service
class RegisterFlightUseCase(
    private val flightRepository: FlightRepository,
    private val catalogLookup: CatalogLookup,
    private val seatRepository: SeatRepository,
    private val auditLog: AuditLog,
    private val priceRecorder: FlightPriceRecorder,
) {
    @Transactional
    fun execute(command: RegisterFlightCommand): Flight {
        val airline = catalogLookup.airline(command.airlineIataCode)
        val origin = catalogLookup.airport(command.originIataCode)
        val destination = catalogLookup.airport(command.destinationIataCode)

        val flight =
            Flight(
                title = "${command.flightNumber} ${origin.iataCode}-${destination.iataCode}",
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
        val saved = flightRepository.save(flight)
        val bookableId = requireNotNull(saved.id) { "A saved Flight must have an id" }
        seatRepository.saveAll(generateSeats(bookableId, command.aircraftType, 0, command.totalCapacity))

        // availableCapacity is derived from the seats just generated above, not from `saved`
        val registered =
            requireNotNull(flightRepository.findById(bookableId)) {
                "Flight $bookableId was just saved but could not be reloaded"
            }
        priceRecorder.record(registered, previous = null)
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.FLIGHT_CREATED,
                targetId = bookableId.toString(),
                after = registered.toAuditSnapshot(),
            ),
        )
        return registered
    }
}
