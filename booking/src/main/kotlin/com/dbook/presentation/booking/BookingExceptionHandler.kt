package com.dbook.presentation.booking

import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class BookingExceptionHandler {
    @ExceptionHandler(BookingNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: BookingNotFoundException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(NotBookingOwnerException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleNotOwner(ex: NotBookingOwnerException): ErrorResponse =
        ErrorResponse(ex.message ?: "Forbidden", ErrorCode.FORBIDDEN)
}
