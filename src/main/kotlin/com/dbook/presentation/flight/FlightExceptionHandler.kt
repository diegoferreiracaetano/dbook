package com.dbook.presentation.flight

import com.dbook.domain.flight.AirlineNotFoundException
import com.dbook.domain.flight.FlightHasActiveBookingsException
import com.dbook.domain.flight.FlightNotFoundException
import com.dbook.domain.seating.FlightSeatConflictException
import com.dbook.domain.seating.SeatNotFoundException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class FlightExceptionHandler {
    @ExceptionHandler(
        AirlineNotFoundException::class,
        FlightNotFoundException::class,
        SeatNotFoundException::class,
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(FlightHasActiveBookingsException::class, FlightSeatConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)
}
