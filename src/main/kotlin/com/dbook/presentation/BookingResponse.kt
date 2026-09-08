package com.dbook.presentation

import com.dbook.domain.Booking
import com.dbook.domain.BookingStatus

data class BookingResponse(
	val id: Long?,
	val bookableId: Long?,
	val customerId: Long,
	val status: BookingStatus,
) {
	companion object {
		fun from(booking: Booking) = BookingResponse(
			id = booking.id,
			bookableId = booking.bookable.id,
			customerId = booking.customerId,
			status = booking.status,
		)
	}
}
