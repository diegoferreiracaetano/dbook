package com.dbook.application

import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import com.dbook.domain.Seat
import com.dbook.domain.SeatRepository
import org.springframework.stereotype.Service

/** A [Booking] paired with its [Seat] — [Booking.bookable] already carries the full [com.dbook.domain.Flight]. */
data class BookingWithDetails(val booking: Booking, val seat: Seat)

/**
 * Lists every booking made by the authenticated user — "my trips"
 * ([GET /bookings][com.dbook.presentation.BookingController.list]).
 * Never returns another customer's bookings: always filtered by [customerId].
 */
@Service
class ListMyBookingsUseCase(
    private val bookingRepository: BookingRepository,
    private val seatRepository: SeatRepository,
) {
    fun execute(customerId: Long): List<BookingWithDetails> =
        bookingRepository.findByCustomerId(customerId).map { booking ->
            val seat = requireNotNull(seatRepository.findById(booking.seatId)) { "Seat ${booking.seatId} not found" }
            BookingWithDetails(booking, seat)
        }
}
