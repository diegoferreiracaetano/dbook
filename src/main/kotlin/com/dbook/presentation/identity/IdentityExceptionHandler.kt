package com.dbook.presentation.identity

import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class IdentityExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ErrorResponse =
        ErrorResponse(ex.message ?: "Unauthorized", ErrorCode.INVALID_CREDENTIALS)

    @ExceptionHandler(InvalidTokenException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidToken(ex: InvalidTokenException): ErrorResponse =
        ErrorResponse(ex.message ?: "Unauthorized", ErrorCode.INVALID_TOKEN)

    @ExceptionHandler(AccountBlockedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccountBlocked(ex: AccountBlockedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Account blocked", ErrorCode.ACCOUNT_BLOCKED)

    @ExceptionHandler(TooManyLoginAttemptsException::class)
    fun handleTooManyLoginAttempts(ex: TooManyLoginAttemptsException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, ex.retryAfterSeconds.toString())
            .body(ErrorResponse(ex.message ?: "Too many attempts", ErrorCode.TOO_MANY_ATTEMPTS))
}
