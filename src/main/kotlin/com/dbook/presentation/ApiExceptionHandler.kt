package com.dbook.presentation

import com.dbook.domain.AirportNotFoundException
import com.dbook.domain.BookableNotFoundException
import com.dbook.domain.BookingNotFoundException
import com.dbook.domain.InvalidCredentialsException
import com.dbook.domain.InvalidTokenException
import com.dbook.domain.NoAvailabilityException
import com.dbook.domain.NotBookingOwnerException
import com.dbook.domain.UserAlreadyExistsException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(
        AirportNotFoundException::class,
        BookableNotFoundException::class,
        BookingNotFoundException::class,
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): Map<String, String> = mapOf("error" to (ex.message ?: "Not found"))

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): Map<String, String> =
        mapOf("error" to (ex.message ?: "Invalid request"))

    @ExceptionHandler(
        NoAvailabilityException::class,
        OptimisticLockingFailureException::class,
        IllegalStateException::class,
        UserAlreadyExistsException::class,
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: Exception): Map<String, String> = mapOf("error" to (ex.message ?: "Conflict"))

    @ExceptionHandler(InvalidCredentialsException::class, InvalidTokenException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(ex: RuntimeException): Map<String, String> = mapOf("error" to (ex.message ?: "Unauthorized"))

    @ExceptionHandler(NotBookingOwnerException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(ex: NotBookingOwnerException): Map<String, String> =
        mapOf("error" to (ex.message ?: "Forbidden"))
}
