package com.dbook.infrastructure.persistence.seating

import com.dbook.domain.seating.SeatAvailability
import com.dbook.domain.seating.SeatStatus
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class SeatAvailabilityAdapter(
    private val seatJpaRepository: SeatJpaRepository,
) : SeatAvailability {
    @Transactional(readOnly = true)
    override fun availableSeats(bookableId: Long): Int =
        seatJpaRepository.countByBookable_IdAndStatus(bookableId, SeatStatus.AVAILABLE)

    @Transactional(readOnly = true)
    override fun availableSeatsOf(bookableIds: Collection<Long>): Map<Long, Int> =
        if (bookableIds.isEmpty()) {
            emptyMap()
        } else {
            seatJpaRepository.countByBookableIds(bookableIds, SeatStatus.AVAILABLE)
                .associate { (it[0] as Long) to (it[1] as Long).toInt() }
        }
}
