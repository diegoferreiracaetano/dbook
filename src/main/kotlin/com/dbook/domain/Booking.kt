package com.dbook.domain

class Booking(
	val id: Long? = null,
	val bookable: Bookable,
	val customerId: Long,
	val status: BookingStatus = BookingStatus.PENDING,
)
