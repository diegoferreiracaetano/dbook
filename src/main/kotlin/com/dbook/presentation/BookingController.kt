package com.dbook.presentation

import com.dbook.application.CancelBookingUseCase
import com.dbook.application.RegisterBookingCommand
import com.dbook.application.RegisterBookingUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bookings")
@Tag(name = "Bookings", description = "Booking and cancellation of bookable items (flights, and hotels in the future)")
class BookingController(
	private val registerBookingUseCase: RegisterBookingUseCase,
	private val cancelBookingUseCase: CancelBookingUseCase,
) {
	@Operation(summary = "Creates a booking (PENDING) for a Bookable, decrementing its availability")
	@PostMapping
	fun register(@RequestBody request: RegisterBookingRequest): ResponseEntity<BookingResponse> {
		val booking = registerBookingUseCase.execute(
			RegisterBookingCommand(bookableId = request.bookableId, customerId = request.customerId),
		)
		return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking))
	}

	@Operation(summary = "Cancels a PENDING booking, returning the availability")
	@PostMapping("/{id}/cancel")
	fun cancel(@Parameter(example = "1") @PathVariable id: Long): ResponseEntity<BookingResponse> {
		val booking = cancelBookingUseCase.execute(id)
		return ResponseEntity.ok(BookingResponse.from(booking))
	}
}
