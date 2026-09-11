package com.dbook.domain

/**
 * Persistence port for [Bookable] itself — deliberately generic (not flight-specific),
 * since any concrete specialization (flights today, accommodations later) shares the
 * same availability-tracking behavior.
 */
interface BookableRepository {
    fun findById(id: Long): Bookable?
}
