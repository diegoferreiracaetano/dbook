package com.dbook.presentation

import com.dbook.domain.AiResponseParsingException
import com.dbook.domain.AiServiceUnavailableException
import com.dbook.domain.AirlineNotFoundException
import com.dbook.domain.AirportNotFoundException
import com.dbook.domain.BookableNotFoundException
import com.dbook.domain.BookingNotFoundException
import com.dbook.domain.InvalidCredentialsException
import com.dbook.domain.InvalidTokenException
import com.dbook.domain.NotBookingOwnerException
import com.dbook.domain.SeatNotFoundException
import com.dbook.domain.UserAlreadyExistsException
import com.dbook.domain.UserNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(
        AirlineNotFoundException::class,
        AirportNotFoundException::class,
        BookableNotFoundException::class,
        BookingNotFoundException::class,
        SeatNotFoundException::class,
        UserNotFoundException::class,
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): Map<String, String> = mapOf("error" to (ex.message ?: "Not found"))

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): Map<String, String> =
        mapOf("error" to (ex.message ?: "Invalid request"))

    // Without this, Spring falls back to response.sendError(400), which triggers a
    // container-level forward to /error — a path that isn't in SecurityConfig's
    // permitAll(), so it gets rejected as 401 before ever reaching this class. Handling
    // it here keeps the response inside the original request, with our own error body.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMalformedRequest(): Map<String, String> = mapOf("error" to "Malformed request body")

    @ExceptionHandler(
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

    // The AI model is an external dependency we don't control the output of — a
    // malformed/unparseable completion is treated as "we (the gateway) messed up", not
    // the caller's fault, hence 502 rather than 400.
    @ExceptionHandler(AiResponseParsingException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleAiResponseParsing(ex: AiResponseParsingException): Map<String, String> =
        mapOf("error" to (ex.message ?: "AI response could not be parsed"))

    @ExceptionHandler(AiServiceUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleAiServiceUnavailable(ex: AiServiceUnavailableException): Map<String, String> =
        mapOf("error" to (ex.message ?: "AI service unavailable"))
}
