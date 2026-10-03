package com.dbook.domain

class IdempotencyKeyReusedException :
    RuntimeException("Idempotency-Key was already used with a different request")
