package com.dbook.application

import com.dbook.domain.BookingNotFoundException
import com.dbook.domain.BookingRepository
import com.dbook.domain.BookingStatus
import com.dbook.domain.NotBookingOwnerException
import com.dbook.domain.Review
import com.dbook.domain.ReviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateReviewCommand(
    val bookingId: Long,
    val customerId: Long,
    val rating: Int,
    val comment: String,
)

/**
 * Rates a [com.dbook.domain.Booking] — only the customer who made it can review it, only
 * once it's CONFIRMED (no reviewing a booking that never went through), and only once
 * per booking.
 */

@Service
class CreateReviewUseCase(
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository,
) {
    @Transactional
    fun execute(command: CreateReviewCommand): Review {
        val booking =
            bookingRepository.findById(command.bookingId)
                ?: throw BookingNotFoundException(command.bookingId)
        if (booking.customerId != command.customerId) {
            throw NotBookingOwnerException(command.bookingId)
        }

        check(booking.status == BookingStatus.CONFIRMED) {
            "Only a CONFIRMED booking can be reviewed"
        }

        check(reviewRepository.findByBookingId(command.bookingId) == null) {
            "Booking ${command.bookingId} was already reviewed"
        }

        val review =
            reviewRepository.save(
                Review(
                    customerId = command.customerId,
                    bookingId = command.bookingId,
                    rating = command.rating,
                    comment = command.comment,
                ),
            )

        return review
    }
}
