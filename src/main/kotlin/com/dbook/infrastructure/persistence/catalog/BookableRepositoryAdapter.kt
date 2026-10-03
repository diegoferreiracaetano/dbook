package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Bookable
import com.dbook.domain.catalog.BookableRepository
import com.dbook.domain.seating.SeatStatus
import com.dbook.infrastructure.persistence.seating.SeatJpaRepository
import org.springframework.stereotype.Repository

@Repository
class BookableRepositoryAdapter(
    private val bookableJpaRepository: BookableJpaRepository,
    private val seatJpaRepository: SeatJpaRepository,
) : BookableRepository {
    override fun findById(id: Long): Bookable? =
        bookableJpaRepository.findById(id).orElse(null)?.toDomain(availableCapacityOf(id))

    private fun availableCapacityOf(bookableId: Long): Int =
        seatJpaRepository.countByBookable_IdAndStatus(bookableId, SeatStatus.AVAILABLE)
}
