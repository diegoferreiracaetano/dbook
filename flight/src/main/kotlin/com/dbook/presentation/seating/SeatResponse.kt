package com.dbook.presentation.seating

import com.dbook.domain.seating.Seat
import com.dbook.domain.seating.SeatStatus

data class SeatResponse(
    val id: Long?,
    val label: String,
    val status: SeatStatus,
) {
    companion object {
        fun from(seat: Seat) =
            SeatResponse(
                id = seat.id,
                label = seat.label,
                status = seat.status,
            )
    }
}
