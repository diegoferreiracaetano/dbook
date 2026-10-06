package com.dbook.presentation.accommodation

import com.dbook.domain.accommodation.AccommodationNotFoundException
import com.dbook.domain.accommodation.RoomTypeNotFoundException
import com.dbook.domain.accommodation.RoomUnavailableException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AccommodationExceptionHandler {
    @ExceptionHandler(AccommodationNotFoundException::class, RoomTypeNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    // a night is full: another guest took the last room of the type (or it was already taken)
    @ExceptionHandler(RoomUnavailableException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleUnavailable(ex: RoomUnavailableException): ErrorResponse =
        ErrorResponse(ex.message ?: "No room available", ErrorCode.ROOM_UNAVAILABLE)
}
