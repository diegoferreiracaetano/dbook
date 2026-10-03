package com.dbook.application.review

import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateReviewCommand(
    val bookingId: Long,
    val customerId: Long,
    val rating: Int,
    val comment: String,
)

/**
 * Rates a [com.dbook.domain.booking.Booking] — only the customer who made it can review it, only
 * once it's CONFIRMED (no reviewing a booking that never went through), and only once
 * per booking.
 */

@Observed(name = "dbook.usecase")
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
