package com.dbook.domain

class Booking(
	val id: Long? = null,
	val bookable: Bookable,
	val customerId: Long,
	val status: BookingStatus = BookingStatus.PENDING,
) {
	fun confirm(): Booking {
		check(status == BookingStatus.PENDING) { "Only a PENDING booking can be confirmed" }
		return Booking(id, bookable, customerId, BookingStatus.CONFIRMED)
	}

	fun cancel(): Booking {
		check(status == BookingStatus.PENDING) { "Only a PENDING booking can be cancelled" }
		return Booking(id, bookable, customerId, BookingStatus.CANCELLED)
	}
}
