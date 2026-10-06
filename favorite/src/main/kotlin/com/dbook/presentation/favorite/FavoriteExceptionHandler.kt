package com.dbook.presentation.favorite

import com.dbook.domain.favorite.FavoritesLimitReachedException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class FavoriteExceptionHandler {
    @ExceptionHandler(FavoritesLimitReachedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleLimit(ex: FavoritesLimitReachedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Favorites limit reached", ErrorCode.FAVORITES_LIMIT)
}
