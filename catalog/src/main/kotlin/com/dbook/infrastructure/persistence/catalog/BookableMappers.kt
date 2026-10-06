package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Bookable
import org.springframework.stereotype.Component

/**
 * How one kind of `Bookable` (a flight, a hotel) is read from its table. Each kind owns its mapper and registers it
 * here by being a bean: the catalog does not know which kinds exist, and a new kind (a car, a tour) is one more mapper.
 */
interface BookableEntityMapper {
    fun supports(entity: BookableJpaEntity): Boolean

    /**
     * The domain objects of [entities] (all of them supported), in the same order. A list, not one at a time: what a
     * kind needs to look up (the free seats of a flight) is asked for the whole list in one query.
     */
    fun toDomain(entities: List<BookableJpaEntity>): List<Bookable>
}

/** The `Bookable` of any entity, whatever its kind: asks the mapper that supports it. */
@Component
class BookableMappers(
    private val mappers: List<BookableEntityMapper>,
) {
    fun toDomain(entity: BookableJpaEntity): Bookable = toDomain(listOf(entity)).single()

    fun toDomain(entities: List<BookableJpaEntity>): List<Bookable> {
        val mapped = arrayOfNulls<Bookable>(entities.size)
        mappers.forEach { mapper ->
            val positions = entities.indices.filter { mapper.supports(entities[it]) }
            if (positions.isNotEmpty()) {
                mapper.toDomain(positions.map { entities[it] }).forEachIndexed {
                        i,
                        bookable,
                    ->
                    mapped[positions[i]] = bookable
                }
            }
        }
        return entities.indices.map { mapped[it] ?: error("Unknown Bookable subtype: ${entities[it]::class}") }
    }
}
