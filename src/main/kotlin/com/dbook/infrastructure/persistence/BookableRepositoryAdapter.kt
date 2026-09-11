package com.dbook.infrastructure.persistence

import com.dbook.domain.Bookable
import com.dbook.domain.BookableRepository
import com.dbook.domain.SeatStatus
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
