package com.dbook.application

import com.dbook.domain.BookableNotFoundException
import com.dbook.domain.BookableRepository
import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import com.dbook.domain.NoAvailabilityException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RegisterBookingCommand(
    val bookableId: Long,
    val customerId: Long,
)

@Service
class RegisterBookingUseCase(
    private val bookableRepository: BookableRepository,
    private val bookingRepository: BookingRepository,
) {
    @Transactional
    fun execute(command: RegisterBookingCommand): Booking {
        val bookable =
            bookableRepository.findById(command.bookableId)
                ?: throw BookableNotFoundException(command.bookableId)
        if (bookable.availableCapacity <= 0) {
            throw NoAvailabilityException(command.bookableId)
        }

        val updatedBookable = bookableRepository.decrementAvailability(command.bookableId)
        val booking =
            Booking(
                bookable = updatedBookable,
                customerId = command.customerId,
            )
        return bookingRepository.save(booking)
    }
}
