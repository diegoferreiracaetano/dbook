package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Bookable
import com.dbook.domain.catalog.BookableRepository
import com.dbook.domain.seating.SeatAvailability
import org.springframework.stereotype.Repository

@Repository
class BookableRepositoryAdapter(
    private val bookableJpaRepository: BookableJpaRepository,
    private val seatAvailability: SeatAvailability,
) : BookableRepository {
    override fun findById(id: Long): Bookable? =
        bookableJpaRepository.findById(id).orElse(null)?.toDomain(availableCapacityOf(id))

    private fun availableCapacityOf(bookableId: Long): Int = seatAvailability.availableSeats(bookableId)
}
