package com.dbook.infrastructure.persistence

import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import org.springframework.stereotype.Repository

@Repository
class BookingRepositoryAdapter(
	private val bookingJpaRepository: BookingJpaRepository,
	private val bookableJpaRepository: BookableJpaRepository,
) : BookingRepository {

	override fun findById(id: Long): Booking? =
		bookingJpaRepository.findById(id).orElse(null)?.toDomain()

	override fun save(booking: Booking): Booking {
		val bookableRef = bookableJpaRepository.getReferenceById(booking.bookable.id!!)
		val saved = bookingJpaRepository.save(booking.toJpaEntity(bookableRef))
		// same care as FlightRepositoryAdapter: bookableRef is a proxy with only the id,
		// so we reuse the domain Bookable the caller already had.
		return Booking(
			id = saved.id,
			bookable = booking.bookable,
			customerId = saved.customerId,
			status = saved.status,
		)
	}
}
