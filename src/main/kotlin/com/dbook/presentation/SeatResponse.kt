package com.dbook.presentation

import com.dbook.domain.Seat
import com.dbook.domain.SeatStatus

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
