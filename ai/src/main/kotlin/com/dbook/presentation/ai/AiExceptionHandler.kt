package com.dbook.presentation.ai

import com.dbook.domain.ai.AiResponseParsingException
import com.dbook.domain.ai.AiServiceUnavailableException
import com.dbook.presentation.common.ErrorCode
import com.dbook.presentation.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AiExceptionHandler {
    // The AI model is an external dependency we don't control the output of — a
    // malformed/unparseable completion is treated as "we (the gateway) messed up", not
    // the caller's fault, hence 502 rather than 400.
    @ExceptionHandler(AiResponseParsingException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleAiResponseParsing(ex: AiResponseParsingException): ErrorResponse =
        ErrorResponse(ex.message ?: "AI response could not be parsed", ErrorCode.AI_RESPONSE_INVALID)

    @ExceptionHandler(AiServiceUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleAiServiceUnavailable(ex: AiServiceUnavailableException): ErrorResponse =
        ErrorResponse(ex.message ?: "AI service unavailable", ErrorCode.AI_UNAVAILABLE)
}
