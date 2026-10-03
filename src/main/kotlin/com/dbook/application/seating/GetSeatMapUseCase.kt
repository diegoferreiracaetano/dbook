package com.dbook.application.seating

import com.dbook.domain.catalog.BookableNotFoundException
import com.dbook.domain.catalog.BookableRepository
import com.dbook.domain.seating.Seat
import com.dbook.domain.seating.SeatRepository
import org.springframework.stereotype.Service

/** Reads the full seat map of a [com.dbook.domain.catalog.Bookable] — public, same spirit as flight search. */
@Service
class GetSeatMapUseCase(
    private val bookableRepository: BookableRepository,
    private val seatRepository: SeatRepository,
) {
    fun execute(bookableId: Long): List<Seat> {
        bookableRepository.findById(bookableId) ?: throw BookableNotFoundException(bookableId)
        return seatRepository.findByBookableId(bookableId)
    }
}
