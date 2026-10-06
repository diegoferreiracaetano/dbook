package com.dbook.application.flight

import com.dbook.application.common.afterCommit
import com.dbook.domain.booking.AvailabilityBroadcaster
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingEvents
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.catalog.BookableNotFoundException
import com.dbook.domain.catalog.BookableRepository
import com.dbook.domain.messaging.OutboxWriter
import com.dbook.domain.seating.SeatNotFoundException
import com.dbook.domain.seating.SeatRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class RegisterBookingCommand(
    val bookableId: Long,
    val seatId: Long,
    val customerId: Long,
)

/**
 * Books a specific [com.dbook.domain.seating.Seat] of a [com.dbook.domain.catalog.Bookable] for a customer,
 * under a seat-level optimistic lock.
 */
@Observed(name = "dbook.usecase")
@Service
class RegisterBookingUseCase(
    private val bookableRepository: BookableRepository,
    private val seatRepository: SeatRepository,
    private val bookingRepository: BookingRepository,
    private val availabilityBroadcaster: AvailabilityBroadcaster,
    private val outboxWriter: OutboxWriter,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: RegisterBookingCommand): Booking {
        val bookable =
            bookableRepository.findById(command.bookableId)
                ?: throw BookableNotFoundException(command.bookableId)
        check(bookable.active) { "This flight is no longer on offer" }
        val seat =
            seatRepository.findById(command.seatId)
                ?: throw SeatNotFoundException(command.seatId)
        require(seat.bookableId == command.bookableId) {
            "Seat ${command.seatId} does not belong to bookable ${command.bookableId}"
        }

        seatRepository.reserve(command.seatId)
        val booking =
            Booking(
                bookable = bookable,
                seatId = command.seatId,
                customerId = command.customerId,
                price = bookable.price,
            )
        val saved = bookingRepository.save(booking)

        // In the SAME transaction as the booking: the expiration is an event in the outbox, due 15 minutes from now.
        // Either both exist or neither does, so a crash can no longer leave a booking that never expires (the old
        // afterCommit call to SQS could); the relay delivers it, and a delivered-twice event is harmless.
        outboxWriter.add(
            BookingEvents.expirationRequested(requireNotNull(saved.id), clock.instant().plus(BookingEvents.HOLD)),
        )

        // after the commit: WebSocket subscribers shouldn't believe a reservation that never happened
        afterCommit {
            availabilityBroadcaster.broadcast(command.bookableId, seatRepository.countAvailable(command.bookableId))
        }

        return saved
    }
}
