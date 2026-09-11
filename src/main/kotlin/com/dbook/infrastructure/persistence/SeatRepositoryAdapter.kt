package com.dbook.infrastructure.persistence

import com.dbook.domain.Seat
import com.dbook.domain.SeatNotFoundException
import com.dbook.domain.SeatRepository
import com.dbook.domain.SeatStatus
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class SeatRepositoryAdapter(
    private val seatJpaRepository: SeatJpaRepository,
    private val bookableJpaRepository: BookableJpaRepository,
) : SeatRepository {
    override fun findById(id: Long): Seat? = seatJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByBookableId(bookableId: Long): List<Seat> =
        seatJpaRepository.findByBookable_IdOrderByLabel(bookableId).map { it.toDomain() }

    override fun countAvailable(bookableId: Long): Int =
        seatJpaRepository.countByBookable_IdAndStatus(bookableId, SeatStatus.AVAILABLE)

    override fun saveAll(seats: List<Seat>): List<Seat> {
        val entities =
            seats.map { seat ->
                val bookableRef = bookableJpaRepository.getReferenceById(seat.bookableId)
                seat.toJpaEntity(bookableRef)
            }
        return seatJpaRepository.saveAll(entities).map { it.toDomain() }
    }

    @Transactional
    override fun reserve(seatId: Long): Seat {
        val entity = findEntityOrThrow(seatId)
        check(entity.status == SeatStatus.AVAILABLE) { "Seat is not available: $seatId" }
        entity.status = SeatStatus.RESERVED
        return seatJpaRepository.save(entity).toDomain()
    }

    @Transactional
    override fun release(seatId: Long): Seat {
        val entity = findEntityOrThrow(seatId)
        entity.status = SeatStatus.AVAILABLE
        return seatJpaRepository.save(entity).toDomain()
    }

    private fun findEntityOrThrow(seatId: Long): SeatJpaEntity =
        seatJpaRepository.findById(seatId).orElseThrow { SeatNotFoundException(seatId) }
}
