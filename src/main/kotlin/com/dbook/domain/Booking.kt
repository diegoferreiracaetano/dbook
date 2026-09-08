package com.dbook.domain

class Booking(
	val id: Long? = null,
	val bookable: Bookable,
	val customerId: Long,
	val status: BookingStatus = BookingStatus.PENDING,
) {
	fun confirm(): Booking = transitionTo(BookingStatus.CONFIRMED)

	fun cancel(): Booking = transitionTo(BookingStatus.CANCELLED)

	private fun transitionTo(newStatus: BookingStatus): Booking {
		check(status == BookingStatus.PENDING) { "Only a PENDING booking can transition to $newStatus" }
		return Booking(id, bookable, customerId, newStatus)
	}
}
