package com.dbook.presentation.review

import com.dbook.domain.review.NotReviewOwnerException
import com.dbook.domain.review.ReviewNotFoundException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ReviewExceptionHandler {
    @ExceptionHandler(ReviewNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: ReviewNotFoundException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(NotReviewOwnerException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleNotOwner(ex: NotReviewOwnerException): ErrorResponse =
        ErrorResponse(ex.message ?: "Forbidden", ErrorCode.FORBIDDEN)
}
