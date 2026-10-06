package com.dbook.domain.payment

class IdempotencyKeyReusedException :
    RuntimeException("Idempotency-Key was already used with a different request")
