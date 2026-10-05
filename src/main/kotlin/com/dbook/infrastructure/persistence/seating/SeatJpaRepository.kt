package com.dbook.infrastructure.persistence.seating

import com.dbook.domain.seating.SeatStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SeatJpaRepository : JpaRepository<SeatJpaEntity, Long> {
    @Suppress("FunctionName")
    fun findByBookable_IdOrderByLabel(bookableId: Long): List<SeatJpaEntity>

    // the seats and their flights (or hotels) in one query: the seat points to its bookable, and left alone that
    // is one more query for each distinct one
    @Query("SELECT s FROM SeatJpaEntity s JOIN FETCH s.bookable WHERE s.id IN :ids")
    fun findAllWithBookable(
        @Param("ids") ids: Collection<Long>,
    ): List<SeatJpaEntity>

    // one row per bookable: [bookableId, how many seats in that status]
    @Query(
        "SELECT s.bookable.id, COUNT(s) FROM SeatJpaEntity s " +
            "WHERE s.bookable.id IN :ids AND s.status = :status GROUP BY s.bookable.id",
    )
    fun countByBookableIds(
        @Param("ids") ids: Collection<Long>,
        @Param("status") status: SeatStatus,
    ): List<Array<Any>>

    @Suppress("FunctionName")
    fun countByBookable_IdAndStatus(
        bookableId: Long,
        status: SeatStatus,
    ): Int
}
