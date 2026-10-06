package com.dbook.presentation.pricing

import com.dbook.domain.pricing.DuplicatePriceAlertException
import com.dbook.domain.pricing.PriceAlertNotFoundException
import com.dbook.domain.pricing.PriceAlertsLimitReachedException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PricingExceptionHandler {
    @ExceptionHandler(PriceAlertNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: PriceAlertNotFoundException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(DuplicatePriceAlertException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDuplicate(ex: DuplicatePriceAlertException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)

    @ExceptionHandler(PriceAlertsLimitReachedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleLimit(ex: PriceAlertsLimitReachedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Limit reached", ErrorCode.PRICE_ALERTS_LIMIT)
}
