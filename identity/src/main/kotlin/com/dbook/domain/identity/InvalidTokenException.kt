package com.dbook.domain.identity

class InvalidTokenException(message: String = "Invalid or expired token") : RuntimeException(message)
