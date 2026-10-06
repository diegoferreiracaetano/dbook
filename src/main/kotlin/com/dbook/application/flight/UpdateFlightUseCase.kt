package com.dbook.application.flight

import com.dbook.application.pricing.FlightPriceRecorder
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.booking.BookingOccupancy
import com.dbook.domain.flight.AdminFlightDetail
import com.dbook.domain.flight.AdminFlightReader
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.FlightEdit
import com.dbook.domain.flight.FlightEvents
import com.dbook.domain.flight.FlightNotFoundException
import com.dbook.domain.flight.FlightRepository
import com.dbook.domain.flight.SeatClass
import com.dbook.domain.flight.toAuditSnapshot
import com.dbook.domain.identity.Actor
import com.dbook.domain.messaging.OutboxWriter
import com.dbook.domain.seating.SeatMapTarget
import com.dbook.domain.seating.SeatRepository
import com.dbook.domain.seating.planSeatChange
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime

/** [version] is the one the editor last read: if the flight moved on since, the edit is refused (409). */
data class UpdateFlightCommand(
    val actor: Actor,
    val flightId: Long,
    val version: Long,
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
 * Edits a flight and keeps its seat map in step. The flight and its seats change in one transaction, so a refused
 * seat change (capacity below the seats already booked, a layout change with bookings) leaves the flight untouched.
 * The price can change at any time: bookings already made keep the price they were made at.
 */
@Observed(name = "dbook.usecase")
@Service
class UpdateFlightUseCase(
    private val flightRepository: FlightRepository,
    private val seatRepository: SeatRepository,
    private val bookingOccupancy: BookingOccupancy,
    private val catalogLookup: CatalogLookup,
    private val adminFlightReader: AdminFlightReader,
    private val auditLog: AuditLog,
    private val outboxWriter: OutboxWriter,
    private val priceRecorder: FlightPriceRecorder,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: UpdateFlightCommand): AdminFlightDetail {
        val flight = flightRepository.findById(command.flightId) ?: throw FlightNotFoundException(command.flightId)
        val edit =
            FlightEdit(
                flightNumber = command.flightNumber,
                airline = catalogLookup.airline(command.airlineIataCode),
                origin = catalogLookup.airport(command.originIataCode),
                destination = catalogLookup.airport(command.destinationIataCode),
                departureTime = command.departureTime,
                arrivalTime = command.arrivalTime,
                seatClass = command.seatClass,
                price = command.price,
                totalCapacity = command.totalCapacity,
                aircraftType = command.aircraftType,
            )
        flightRepository.update(flight.edit(edit), command.version)

        val change =
            planSeatChange(
                bookableId = command.flightId,
                current = seatRepository.findByBookableId(command.flightId),
                oldAircraft = flight.aircraftType,
                target = SeatMapTarget(edit.aircraftType, edit.totalCapacity),
                seatsWithBookings = bookingOccupancy.bookedSeatIds(command.flightId),
            )
        if (change.toRemove.isNotEmpty()) {
            seatRepository.deleteAll(change.toRemove.map { requireNotNull(it.id) })
        }
        if (change.toAdd.isNotEmpty()) {
            seatRepository.saveAll(change.toAdd)
        }

        val reloaded = requireNotNull(flightRepository.findById(command.flightId))
        announceTravelChanges(flight, reloaded)
        priceRecorder.record(reloaded, previous = flight.price)
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.FLIGHT_UPDATED,
                targetId = command.flightId.toString(),
                before = flight.toAuditSnapshot(),
                after = reloaded.toAuditSnapshot(),
            ),
        )
        return requireNotNull(adminFlightReader.find(command.flightId))
    }

    // A change to the schedule, the route or the number matters to everyone holding a booking on the flight (a price or
    // a capacity change does not): each of them gets an event, in the transaction of the edit.
    private fun announceTravelChanges(
        before: Flight,
        after: Flight,
    ) {
        val changes = after.travelChangesFrom(before)
        if (changes.isEmpty()) {
            return
        }
        bookingOccupancy.activeBookingOwners(requireNotNull(after.id)).forEach {
            outboxWriter.add(FlightEvents.changed(after, it.bookingId, it.customerId, changes, clock.instant()))
        }
    }
}
