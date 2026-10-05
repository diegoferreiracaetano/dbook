package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.seating.SeatStatus
import com.dbook.infrastructure.persistence.catalog.BookableJpaRepository
import com.dbook.infrastructure.persistence.payment.PaymentJpaRepository
import com.dbook.infrastructure.persistence.seating.SeatJpaRepository
import org.springframework.stereotype.Repository

@Repository
class BookingRepositoryAdapter(
    private val bookingJpaRepository: BookingJpaRepository,
    private val bookableJpaRepository: BookableJpaRepository,
    private val seatJpaRepository: SeatJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val statusHistoryWriter: BookingStatusHistoryWriter,
) : BookingRepository {
    override fun findById(id: Long): Booking? =
        bookingJpaRepository.findById(id).orElse(null)?.let { entity ->
            entity.toDomain(availableCapacityOf(requireNotNull(entity.bookable.id)))
        }

    override fun findByCustomerId(customerId: Long): List<Booking> =
        bookingJpaRepository.findByCustomerId(customerId).map { entity ->
            entity.toDomain(availableCapacityOf(requireNotNull(entity.bookable.id)))
        }

    override fun save(booking: Booking): Booking {
        val bookableId = requireNotNull(booking.bookable.id) { "Booking.bookable must be persisted" }
        val bookableRef = bookableJpaRepository.getReferenceById(bookableId)
        val seatRef = booking.seatId?.let { seatJpaRepository.getReferenceById(it) }
        val paymentRef = booking.paymentId?.let { paymentJpaRepository.getReferenceById(it) }
        // read before the merge below overwrites it: the managed entity is what tells us the status it had
        val previousStatus = booking.id?.let { bookingJpaRepository.findById(it).orElse(null)?.status }
        val saved = bookingJpaRepository.save(booking.toJpaEntity(bookableRef, seatRef, paymentRef))
        if (previousStatus != saved.status) {
            statusHistoryWriter.record(requireNotNull(saved.id), previousStatus, saved.status)
        }
        // same care as FlightRepositoryAdapter: bookableRef is a proxy with only the id,
        // so we reuse the domain Bookable the caller already had.
        return Booking(
            id = saved.id,
            bookable = booking.bookable,
            seatId = booking.seatId,
            stay = booking.stay,
            customerId = saved.customerId,
            status = saved.status,
            paymentId = saved.payment?.id,
            version = saved.version,
            price = saved.price,
            discount = saved.discount,
        )
    }

    override fun countPending(): Long = bookingJpaRepository.countPending()

    private fun availableCapacityOf(bookableId: Long): Int =
        seatJpaRepository.countByBookable_IdAndStatus(bookableId, SeatStatus.AVAILABLE)
}
