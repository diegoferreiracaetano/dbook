package com.dbook.presentation.catalog

import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.catalog.BookableNotFoundException
import com.dbook.domain.catalog.CatalogEntryInUseException
import com.dbook.domain.catalog.CatalogEntryNotFoundException
import com.dbook.domain.catalog.DuplicateIataCodeException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class CatalogExceptionHandler {
    @ExceptionHandler(
        AirportNotFoundException::class,
        BookableNotFoundException::class,
        CatalogEntryNotFoundException::class,
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(CatalogEntryInUseException::class, DuplicateIataCodeException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: RuntimeException): ErrorResponse =
        ErrorResponse(ex.message ?: "Conflict", ErrorCode.CONFLICT)
}
