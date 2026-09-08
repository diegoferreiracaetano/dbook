package com.dbook.presentation

import io.swagger.v3.oas.annotations.media.Schema

data class RegisterBookingRequest(
	@get:Schema(example = "1", description = "id of an existing Bookable (e.g. a flight registered via POST /admin/flights)")
	val bookableId: Long,
	@get:Schema(
		example = "42",
		description = "Placeholder until M3 introduces real authentication (JWT) — " +
			"any Long is accepted for now, no User entity exists yet",
	)
	val customerId: Long,
)
