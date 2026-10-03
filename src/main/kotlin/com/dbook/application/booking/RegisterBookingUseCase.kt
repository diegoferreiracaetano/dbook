package com.dbook.application.booking

import com.dbook.application.common.afterCommit
import com.dbook.domain.booking.AvailabilityBroadcaster
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingExpirationScheduler
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.catalog.BookableNotFoundException
import com.dbook.domain.catalog.BookableRepository
import com.dbook.domain.seating.SeatNotFoundException
import com.dbook.domain.seating.SeatRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

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
    private val bookingExpirationScheduler: BookingExpirationScheduler,
) {
    @Transactional
    fun execute(command: RegisterBookingCommand): Booking {
        val bookable =
            bookableRepository.findById(command.bookableId)
                ?: throw BookableNotFoundException(command.bookableId)
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
            )
        val saved = bookingRepository.save(booking)

        // only after the transaction actually commits — a rollback past this point
        // must not leave an expiration scheduled for a booking that never existed.
        // First on purpose: an exception in one afterCommit callback skips the next ones,
        // and the broadcast is best-effort while the expiration is what frees the seat.
        afterCommit {
            bookingExpirationScheduler.scheduleExpiration(requireNotNull(saved.id), BOOKING_EXPIRATION)
        }
        // same reason for the broadcast: WebSocket subscribers shouldn't believe a
        // reservation that never happened
        afterCommit {
            availabilityBroadcaster.broadcast(command.bookableId, seatRepository.countAvailable(command.bookableId))
        }

        return saved
    }

    private companion object {
        // how long a PENDING booking holds its seat; 15 min is also the most SQS can delay
        val BOOKING_EXPIRATION: Duration = Duration.ofMinutes(15)
    }
}
