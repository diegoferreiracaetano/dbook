package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Bookable
import com.dbook.domain.catalog.BookableRepository
import org.springframework.stereotype.Repository

@Repository
class BookableRepositoryAdapter(
    private val bookableJpaRepository: BookableJpaRepository,
    private val bookableMappers: BookableMappers,
) : BookableRepository {
    override fun findById(id: Long): Bookable? =
        bookableJpaRepository.findById(id).orElse(null)?.let(bookableMappers::toDomain)
}
