package com.dbook.infrastructure.persistence.accommodation

import com.dbook.domain.catalog.Bookable
import com.dbook.infrastructure.persistence.catalog.BookableEntityMapper
import com.dbook.infrastructure.persistence.catalog.BookableJpaEntity
import org.springframework.stereotype.Component

/** A hotel as a booking needs it: the light form, with no room types (a booking only knows which hotel it is at). */
@Component
class AccommodationBookableMapper : BookableEntityMapper {
    override fun supports(entity: BookableJpaEntity): Boolean = entity is AccommodationJpaEntity

    override fun toDomain(entities: List<BookableJpaEntity>): List<Bookable> =
        entities.map { (it as AccommodationJpaEntity).toDomain(emptyList()) }
}
