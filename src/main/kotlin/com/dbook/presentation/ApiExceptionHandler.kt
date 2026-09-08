package com.dbook.presentation

import com.dbook.domain.AirportNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(AirportNotFoundException::class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	fun handleAirportNotFound(ex: AirportNotFoundException): Map<String, String> =
		mapOf("error" to (ex.message ?: "Airport not found"))

	@ExceptionHandler(IllegalArgumentException::class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	fun handleIllegalArgument(ex: IllegalArgumentException): Map<String, String> =
		mapOf("error" to (ex.message ?: "Invalid request"))
}
