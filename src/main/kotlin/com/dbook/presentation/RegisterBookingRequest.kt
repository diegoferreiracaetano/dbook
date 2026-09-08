package com.dbook.presentation

data class RegisterBookingRequest(
	val bookableId: Long,
	val customerId: Long,
)
