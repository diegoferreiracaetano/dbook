package com.dbook.domain

/**
 * An individual seat belonging to a [Bookable] (e.g. seat "12A" on a [Flight]). Carries its
 * own optimistic lock at the persistence level, independent from the [Bookable]'s — see
 * [SeatRepository.reserve] — so two customers racing for the same seat can't both win it.
 */
class Seat(
    val id: Long? = null,
    val bookableId: Long,
    val label: String,
    val status: SeatStatus = SeatStatus.AVAILABLE,
)
