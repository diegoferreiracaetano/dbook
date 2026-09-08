package com.dbook.domain

class InvalidTokenException(message: String = "Invalid or expired token") : RuntimeException(message)
