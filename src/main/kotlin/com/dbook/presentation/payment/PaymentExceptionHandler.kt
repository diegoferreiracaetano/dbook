package com.dbook.presentation.payment

import com.dbook.domain.payment.DuplicateIdempotencyKeyException
import com.dbook.domain.payment.IdempotencyKeyReusedException
import com.dbook.domain.payment.RefundAlreadyRequestedException
import com.dbook.domain.payment.RefundNotFoundException
import com.dbook.domain.payment.RefundOverrideNotAllowedException
import com.dbook.domain.payment.RefundWindowClosedException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PaymentExceptionHandler {
    // 422, not 409: the request itself is wrong (a key can only ever mean one request), as
    // opposed to a conflict with the current state of a resource.
    @ExceptionHandler(IdempotencyKeyReusedException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleIdempotencyKeyReused(ex: IdempotencyKeyReusedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Idempotency-Key reused", ErrorCode.IDEMPOTENCY_KEY_REUSED)

    @ExceptionHandler(DuplicateIdempotencyKeyException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDuplicateIdempotencyKey(ex: DuplicateIdempotencyKeyException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)

    @ExceptionHandler(RefundNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleRefundNotFound(ex: RefundNotFoundException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(RefundAlreadyRequestedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleRefundAlreadyRequested(ex: RefundAlreadyRequestedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)

    @ExceptionHandler(RefundWindowClosedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleRefundWindowClosed(ex: RefundWindowClosedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.REFUND_WINDOW_CLOSED)

    @ExceptionHandler(RefundOverrideNotAllowedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleOverrideNotAllowed(ex: RefundOverrideNotAllowedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Forbidden", ErrorCode.FORBIDDEN)
}
