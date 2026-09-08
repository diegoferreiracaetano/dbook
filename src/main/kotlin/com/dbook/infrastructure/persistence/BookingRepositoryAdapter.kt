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
		val entity = BookingJpaEntity(
			id = booking.id,
			bookable = bookableRef,
			customerId = booking.customerId,
			status = booking.status,
		)
		val saved = bookingJpaRepository.save(entity)
		// mesmo cuidado do FlightRepositoryAdapter: bookableRef é um proxy só com o id,
		// reaproveitamos o Bookable de domínio que o chamador já tinha em mãos.
		return Booking(
			id = saved.id,
			bookable = booking.bookable,
			customerId = saved.customerId,
			status = saved.status,
		)
	}
}
