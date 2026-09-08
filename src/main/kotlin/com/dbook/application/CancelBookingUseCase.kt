package com.dbook.application

import com.dbook.domain.BookableRepository
import com.dbook.domain.Booking
import com.dbook.domain.BookingNotFoundException
import com.dbook.domain.BookingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CancelBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val bookableRepository: BookableRepository,
) {
    @Transactional
    fun execute(bookingId: Long): Booking {
        val booking =
            bookingRepository.findById(bookingId)
                ?: throw BookingNotFoundException(bookingId)
        val cancelled = booking.cancel()
        bookableRepository.incrementAvailability(booking.bookable.id!!)
        return bookingRepository.save(cancelled)
    }
}
