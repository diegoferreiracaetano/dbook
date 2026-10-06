package com.dbook.presentation.common

import com.dbook.domain.common.StaleVersionException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

// The errors every module can raise: a rule broken (400), a state in the way (409), a stale record, a bad request.
// What belongs to one module is answered by that module's own handler (the `XxxExceptionHandler` of its package).
@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): ErrorResponse =
        ErrorResponse(ex.message ?: "Invalid request", ErrorCode.VALIDATION_FAILED)

    // Without this, Spring falls back to response.sendError(400), which triggers a
    // container-level forward to /error — a path that isn't in SecurityConfig's
    // permitAll(), so it gets rejected as 401 before ever reaching this class. Handling
    // it here keeps the response inside the original request, with our own error body.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMalformedRequest(): ErrorResponse = ErrorResponse("Malformed request body", ErrorCode.MALFORMED_REQUEST)

    // A query or path parameter of the wrong kind (an unknown enum value, text where a number goes) is a 400. Same
    // sendError(400) trap as above, so it is answered here.
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleParameterMismatch(ex: MethodArgumentTypeMismatchException): ErrorResponse =
        ErrorResponse("Invalid value for '${ex.name}'", ErrorCode.VALIDATION_FAILED)

    // A query parameter that cannot be read (an unknown enum value, a malformed date) fails the binding of the
    // request object; same sendError(400) trap as above, so it is answered here.
    @ExceptionHandler(BindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBindingFailure(ex: BindException): ErrorResponse =
        ErrorResponse(
            ex.bindingResult.fieldErrors.firstOrNull()?.let { "Invalid value for '${it.field}'" } ?: "Invalid request",
            ErrorCode.VALIDATION_FAILED,
        )

    // Same trap as the malformed body above: left alone, Spring answers with sendError(400),
    // which forwards to /error and gets rejected as 401 before reaching the client.
    @ExceptionHandler(MissingRequestHeaderException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMissingHeader(ex: MissingRequestHeaderException): ErrorResponse =
        ErrorResponse("Missing request header: ${ex.headerName}", ErrorCode.MISSING_HEADER)

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: IllegalStateException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)

    @ExceptionHandler(OptimisticLockingFailureException::class, StaleVersionException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleStaleVersion(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "The record was changed by someone else", ErrorCode.STALE_VERSION)

    @ExceptionHandler(ForbiddenOriginException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenOrigin(ex: ForbiddenOriginException): ErrorResponse =
        ErrorResponse(ex.message ?: "Forbidden", ErrorCode.FORBIDDEN)
}
