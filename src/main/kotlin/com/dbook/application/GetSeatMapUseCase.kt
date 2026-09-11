package com.dbook.application

import com.dbook.domain.BookableNotFoundException
import com.dbook.domain.BookableRepository
import com.dbook.domain.Seat
import com.dbook.domain.SeatRepository
import org.springframework.stereotype.Service

/** Reads the full seat map of a [com.dbook.domain.Bookable] — public, same spirit as flight search. */
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
