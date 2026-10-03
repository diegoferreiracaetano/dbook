package com.dbook.domain

/**
 * Raised when two requests with the same Idempotency-Key reach the database together and
 * this one lost the race. A domain exception on purpose: a plain IllegalStateException
 * thrown out of the @Repository adapter would be rewritten by Spring's persistence
 * exception translation into an InvalidDataAccessApiUsageException, which nobody maps.
 */
class DuplicateIdempotencyKeyException(cause: Throwable) :
    RuntimeException("A payment with this Idempotency-Key is already being processed", cause)
