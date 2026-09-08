package com.dbook.infrastructure.persistence

import com.dbook.domain.Bookable
import com.dbook.domain.BookableNotFoundException
import com.dbook.domain.BookableRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class BookableRepositoryAdapter(
    private val bookableJpaRepository: BookableJpaRepository,
) : BookableRepository {
    override fun findById(id: Long): Bookable? = bookableJpaRepository.findById(id).orElse(null)?.toDomain()

    @Transactional
    override fun decrementAvailability(bookableId: Long): Bookable {
        val entity = findEntityOrThrow(bookableId)
        check(entity.availableCapacity > 0) { "No availability for bookable: $bookableId" }
        entity.availableCapacity -= 1
        return bookableJpaRepository.save(entity).toDomain()
    }

    @Transactional
    override fun incrementAvailability(bookableId: Long): Bookable {
        val entity = findEntityOrThrow(bookableId)
        entity.availableCapacity += 1
        return bookableJpaRepository.save(entity).toDomain()
    }

    private fun findEntityOrThrow(bookableId: Long): BookableJpaEntity =
        bookableJpaRepository.findById(bookableId)
            .orElseThrow { BookableNotFoundException(bookableId) }
}
