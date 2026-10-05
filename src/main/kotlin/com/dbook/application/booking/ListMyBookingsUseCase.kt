package com.dbook.application.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewRepository
import com.dbook.domain.seating.Seat
import com.dbook.domain.seating.SeatRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

/**
 * A [Booking] paired with its [Seat] — [Booking.bookable] already carries the full
 * [com.dbook.domain.catalog.Flight].
 */
data class BookingWithDetails(val booking: Booking, val seat: Seat?, val review: Review?)

/**
 * Lists every booking made by the authenticated user — "my trips"
 * ([GET /bookings][com.dbook.presentation.booking.BookingController.list]).
 * Never returns another customer's bookings: always filtered by [customerId].
 */
@Observed(name = "dbook.usecase")
@Service
class ListMyBookingsUseCase(
    private val bookingRepository: BookingRepository,
    private val seatRepository: SeatRepository,
    private val reviewRepository: ReviewRepository,
) {
    fun execute(customerId: Long): List<BookingWithDetails> =
        bookingRepository.findByCustomerId(customerId).map { booking ->
            val seat = booking.seatId?.let { requireNotNull(seatRepository.findById(it)) { "Seat $it not found" } }
            val review = booking.id?.let { reviewRepository.findByBookingId(it) }
            BookingWithDetails(booking, seat, review)
        }
}
