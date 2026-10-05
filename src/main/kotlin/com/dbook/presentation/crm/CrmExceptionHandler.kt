package com.dbook.presentation.crm

import com.dbook.domain.crm.CustomerNoteNotFoundException
import com.dbook.domain.crm.NoteAccessDeniedException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class CrmExceptionHandler {
    @ExceptionHandler(CustomerNoteNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoteNotFound(ex: CustomerNoteNotFoundException): ErrorResponse =
        ErrorResponse(ex.message ?: "Not found", ErrorCode.NOT_FOUND)

    @ExceptionHandler(NoteAccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleNoteAccessDenied(ex: NoteAccessDeniedException): ErrorResponse =
        ErrorResponse(ex.message ?: "Forbidden", ErrorCode.FORBIDDEN)
}
