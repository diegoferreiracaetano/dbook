package com.dbook.domain

interface BookableRepository {
    fun findById(id: Long): Bookable?

    fun decrementAvailability(bookableId: Long): Bookable

    fun incrementAvailability(bookableId: Long): Bookable
}
