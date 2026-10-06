package com.dbook.application.accommodation

import com.dbook.domain.accommodation.AccommodationNotFoundException
import com.dbook.domain.accommodation.AccommodationRepository
import com.dbook.domain.accommodation.RoomInventory
import com.dbook.domain.accommodation.RoomTypeNotFoundException
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingEvents
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.Stay
import com.dbook.domain.messaging.OutboxWriter
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

data class RegisterStayBookingCommand(
    val accommodationId: Long,
    val roomTypeId: Long,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guests: Int,
    val customerId: Long,
)

/**
 * Books a room for some nights. The booking is the same [Booking] a flight has (PENDING, paid by the same payment,
 * expired by the same event, refunded by the same saga), with a [Stay] where the flight has a seat, and a price of
 * nights x the nightly rate **frozen now**. The nights are taken, one conditional statement each, in this transaction:
 * if any is full the whole stay is refused (409) and nothing is kept.
 */
@Observed(name = "dbook.usecase")
@Service
class RegisterStayBookingUseCase(
    private val accommodations: AccommodationRepository,
    private val roomInventory: RoomInventory,
    private val bookingRepository: BookingRepository,
    private val outboxWriter: OutboxWriter,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: RegisterStayBookingCommand): Booking {
        val hotel =
            accommodations.findById(command.accommodationId)
                ?: throw AccommodationNotFoundException(command.accommodationId)
        check(hotel.active) { "This accommodation is no longer on offer" }
        val room =
            hotel.roomTypes.find { it.id == command.roomTypeId && it.active }
                ?: throw RoomTypeNotFoundException(command.roomTypeId)
        require(command.guests <= room.capacity) { "${room.name} takes at most ${room.capacity} guests" }
        require(!command.checkIn.isBefore(LocalDate.now(clock))) { "checkIn must not be in the past" }

        val stay = Stay(requireNotNull(room.id), command.checkIn, command.checkOut, command.guests, room.nightlyRate)
        roomInventory.reserve(stay)
        val saved =
            bookingRepository.save(
                Booking(
                    bookable = hotel,
                    seatId = null,
                    customerId = command.customerId,
                    price = stay.total,
                    stay = stay,
                ),
            )
        // the same hold as a seat: still PENDING 15 minutes from now, the booking expires and the nights return
        outboxWriter.add(
            BookingEvents.expirationRequested(requireNotNull(saved.id), clock.instant().plus(BookingEvents.HOLD)),
        )
        return saved
    }
}
