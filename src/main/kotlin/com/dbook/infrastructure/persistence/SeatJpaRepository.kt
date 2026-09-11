package com.dbook.infrastructure.persistence

import com.dbook.domain.SeatStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SeatJpaRepository : JpaRepository<SeatJpaEntity, Long> {
    @Suppress("FunctionName")
    fun findByBookable_IdOrderByLabel(bookableId: Long): List<SeatJpaEntity>

    @Suppress("FunctionName")
    fun countByBookable_IdAndStatus(
        bookableId: Long,
        status: SeatStatus,
    ): Int
}
