package com.dbook.application.flight

import com.dbook.domain.booking.BookingOccupancy
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.flight.AdminFlightDetail
import com.dbook.domain.flight.AdminFlightReader
import com.dbook.domain.flight.FlightHasActiveBookingsException
import com.dbook.domain.flight.FlightNotFoundException
import com.dbook.domain.flight.FlightRepository
import com.dbook.domain.flight.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Takes a flight off sale: it stops showing in the public search and cannot be booked. **Phase 1:** a flight that
 * still has active bookings (pending or paid) is refused, with how many, so the team refunds or cancels them first.
 * Cancelling and refunding each booking by itself, with a notice to the customer, waits for the outbox and the
 * notifications.
 */
@Observed(name = "dbook.usecase")
@Service
class CancelFlightUseCase(
    private val flightRepository: FlightRepository,
    private val bookingOccupancy: BookingOccupancy,
    private val adminFlightReader: AdminFlightReader,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        flightId: Long,
    ): AdminFlightDetail {
        val flight = flightRepository.findById(flightId) ?: throw FlightNotFoundException(flightId)
        val active = bookingOccupancy.activeBookings(flightId)
        if (active > 0) {
            throw FlightHasActiveBookingsException(active)
        }
        val cancelled = flightRepository.update(flight.cancel())
        auditLog.record(
            AuditEvent(
                actor = actor,
                action = AuditAction.FLIGHT_CANCELLED,
                targetId = flightId.toString(),
                before = flight.toAuditSnapshot(),
                after = cancelled.toAuditSnapshot(),
            ),
        )
        return requireNotNull(adminFlightReader.find(flightId))
    }
}
