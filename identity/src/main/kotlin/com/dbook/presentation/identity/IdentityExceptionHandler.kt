package com.dbook.presentation.identity

import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.DuplicateOpenInvitationException
import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.InvalidInvitationException
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.domain.identity.InvitationNotFoundException
import com.dbook.domain.identity.SessionNotFoundException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import com.dbook.domain.identity.UserAlreadyExistsException
import com.dbook.domain.identity.UserNotFoundException
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
    @ExceptionHandler(
        InvitationNotFoundException::class,
        UserNotFoundException::class,
        SessionNotFoundException::class,
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(DuplicateOpenInvitationException::class, UserAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)

    @ExceptionHandler(InvalidCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ErrorResponse =
        ErrorResponse(ex.message ?: "Unauthorized", ErrorCode.INVALID_CREDENTIALS)

    @ExceptionHandler(InvalidTokenException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidToken(ex: InvalidTokenException): ErrorResponse =
        ErrorResponse(ex.message ?: "Unauthorized", ErrorCode.INVALID_TOKEN)

    @ExceptionHandler(InvalidInvitationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidInvitation(ex: InvalidInvitationException): ErrorResponse =
        ErrorResponse(ex.message ?: "Invalid invitation", ErrorCode.INVALID_INVITATION)

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
