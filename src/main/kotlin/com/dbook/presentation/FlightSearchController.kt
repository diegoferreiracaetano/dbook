package com.dbook.presentation

import com.dbook.application.SearchFlightsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/flights")
@Tag(name = "Flights (public)", description = "Public flight search")
class FlightSearchController(
	private val searchFlightsUseCase: SearchFlightsUseCase,
) {
	@Operation(summary = "Searches flights by origin, destination and date")
	@GetMapping("/search")
	fun search(
		@Parameter(example = "GRU")
		@RequestParam
		origin: String,
		@Parameter(example = "GIG")
		@RequestParam
		destination: String,
		@Parameter(example = "2026-10-01")
		@RequestParam
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		date: LocalDate,
	): List<FlightResponse> =
		searchFlightsUseCase.execute(origin, destination, date).map { FlightResponse.from(it) }
}
