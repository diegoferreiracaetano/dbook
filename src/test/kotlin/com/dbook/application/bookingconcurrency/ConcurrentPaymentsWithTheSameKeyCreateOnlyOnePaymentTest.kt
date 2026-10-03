package com.dbook.application.bookingconcurrency

import com.dbook.domain.BookingStatus
import com.dbook.domain.DuplicateIdempotencyKeyException
import org.springframework.dao.OptimisticLockingFailureException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Two identical requests (same Idempotency-Key) arriving together, e.g. a client retrying
// before the first response came back. Whichever way the race lands — the second one
// replaying the finished payment, or losing to the unique index / the booking's version —
// there must be exactly one payment and never an unexpected error (anything outside the
// 409 family would reach the client as a 500). Many rounds because a race only
// misbehaves some of the time.
class ConcurrentPaymentsWithTheSameKeyCreateOnlyOnePaymentTest : BookingConcurrencyFixture() {
    @Test
    fun `given two identical payment requests racing then exactly one payment exists and none fails unexpectedly`() {
        val userId = registerUser()
        val bookableId = registerFlight(totalCapacity = ROUNDS)

        seatIdsOf(bookableId).forEach { seatId ->
            val bookingId = bookSeat(bookableId, seatId, userId)
            val key = UUID.randomUUID().toString()

            val outcomes = raceOutcomes({ pay(bookingId, userId, key) }, { pay(bookingId, userId, key) })

            assertTrue(outcomes.any { it == null }, "at least one of the identical requests must succeed")
            val losers = outcomes.filterNotNull()
            assertTrue(losers.all(::isConflict), "a losing request must be a conflict (409), got $losers")
            assertEquals(1L, paymentsWithKey(key))
            assertEquals(BookingStatus.CONFIRMED, bookingRepository.findById(bookingId)?.status)
        }
    }

    // the family ApiExceptionHandler answers with 409
    private fun isConflict(error: Throwable) =
        error is DuplicateIdempotencyKeyException ||
            error is OptimisticLockingFailureException ||
            error is IllegalStateException

    private fun paymentsWithKey(key: String): Long? =
        jdbcTemplate.queryForObject("SELECT count(*) FROM payment WHERE idempotency_key = ?", Long::class.java, key)

    private companion object {
        const val ROUNDS = 10
    }
}
