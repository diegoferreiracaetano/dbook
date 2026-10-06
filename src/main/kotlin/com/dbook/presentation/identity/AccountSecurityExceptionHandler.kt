package com.dbook.presentation.identity

import com.dbook.domain.identity.EmailNotVerifiedException
import com.dbook.domain.identity.InvalidAccountTokenException
import com.dbook.domain.identity.InvalidTwoFactorCodeException
import com.dbook.domain.identity.TwoFactorRequiredException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/** The answers of the second factor and of the account recovery links: split from the sign-in errors by subject. */
@RestControllerAdvice
class AccountSecurityExceptionHandler {
    @ExceptionHandler(InvalidAccountTokenException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidAccountToken(ex: InvalidAccountTokenException): ErrorResponse =
        ErrorResponse(ex.message ?: "Invalid link", ErrorCode.INVALID_ACCOUNT_TOKEN)

    @ExceptionHandler(EmailNotVerifiedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleEmailNotVerified(ex: EmailNotVerifiedException): ErrorResponse =
        ErrorResponse(ex.message ?: "E-mail not confirmed", ErrorCode.EMAIL_NOT_VERIFIED)

    // the code is wrong, not the session: a 400, so that a client does not take it for an expired login and retry
    @ExceptionHandler(InvalidTwoFactorCodeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidTwoFactorCode(ex: InvalidTwoFactorCodeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Invalid code", ErrorCode.INVALID_TWO_FACTOR_CODE)

    @ExceptionHandler(TwoFactorRequiredException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleTwoFactorRequired(ex: TwoFactorRequiredException): ErrorResponse =
        ErrorResponse(ex.message ?: "Second factor required", ErrorCode.TWO_FACTOR_REQUIRED)
}
