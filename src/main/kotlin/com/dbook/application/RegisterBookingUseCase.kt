package com.dbook.application

import com.dbook.domain.AvailabilityBroadcaster
import com.dbook.domain.BookableNotFoundException
import com.dbook.domain.BookableRepository
import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import com.dbook.domain.SeatNotFoundException
import com.dbook.domain.SeatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RegisterBookingCommand(
    val bookableId: Long,
    val seatId: Long,
    val customerId: Long,
)

/**
 * Books a specific [com.dbook.domain.Seat] of a [com.dbook.domain.Bookable] for a customer,
 * under a seat-level optimistic lock.
 */
@Service
class RegisterBookingUseCase(
    private val bookableRepository: BookableRepository,
    private val seatRepository: SeatRepository,
    private val bookingRepository: BookingRepository,
    private val availabilityBroadcaster: AvailabilityBroadcaster,
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
        // shouldn't leave WebSocket subscribers believing a reservation that never happened
        afterCommit {
            availabilityBroadcaster.broadcast(command.bookableId, seatRepository.countAvailable(command.bookableId))
        }
        return saved
    }
}
