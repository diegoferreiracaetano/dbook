package com.dbook.domain.identity

class TooManyLoginAttemptsException(val retryAfterSeconds: Long) :
    RuntimeException("Too many failed login attempts, try again in $retryAfterSeconds seconds")
