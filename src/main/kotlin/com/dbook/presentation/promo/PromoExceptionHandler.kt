package com.dbook.presentation.promo

import com.dbook.domain.promo.DuplicatePromoCodeException
import com.dbook.domain.promo.PromoNotFoundException
import com.dbook.domain.promo.PromoRejectedException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PromoExceptionHandler {
    @ExceptionHandler(PromoNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: PromoNotFoundException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(DuplicatePromoCodeException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDuplicate(ex: DuplicatePromoCodeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)

    // 422: the request is well formed, the code is what cannot be used
    @ExceptionHandler(PromoRejectedException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleRejected(ex: PromoRejectedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Promo code rejected", ErrorCode.PROMO_REJECTED)
}
