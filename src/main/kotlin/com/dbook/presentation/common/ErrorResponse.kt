package com.dbook.presentation.common

data class ErrorResponse(
    val error: String,
    val code: ErrorCode,
)
