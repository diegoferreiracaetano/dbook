package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.infrastructure.persistence.catalog.BookableJpaRepository
import com.dbook.infrastructure.persistence.catalog.BookableMappers
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class BookingRepositoryAdapter(
    private val bookingJpaRepository: BookingJpaRepository,
    private val bookableJpaRepository: BookableJpaRepository,
    private val statusHistoryWriter: BookingStatusHistoryWriter,
    private val bookableMappers: BookableMappers,
) : BookingRepository {
    override fun findById(id: Long): Booking? =
        bookingJpaRepository.findById(id).orElse(null)?.let { it.toDomain(bookableMappers.toDomain(it.bookable)) }

    // one session for the whole list: a flight's airline and airports are loaded once however many bookings share them
    @Transactional(readOnly = true)
    override fun findByCustomerId(customerId: Long): List<Booking> {
        val entities = bookingJpaRepository.findByCustomerId(customerId)
        val bookables = bookableMappers.toDomain(entities.map { it.bookable })
        return entities.mapIndexed { i, entity -> entity.toDomain(bookables[i]) }
    }

    override fun save(booking: Booking): Booking {
        val bookableId = requireNotNull(booking.bookable.id) { "Booking.bookable must be persisted" }
        val bookableRef = bookableJpaRepository.getReferenceById(bookableId)
        // read before the merge below overwrites it: the managed entity is what tells us the status it had
        val previousStatus = booking.id?.let { bookingJpaRepository.findById(it).orElse(null)?.status }
        val saved = bookingJpaRepository.save(booking.toJpaEntity(bookableRef))
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
            paymentId = saved.paymentId,
            version = saved.version,
            price = saved.price,
            discount = saved.discount,
        )
    }

    override fun countPending(): Long = bookingJpaRepository.countPending()
}
