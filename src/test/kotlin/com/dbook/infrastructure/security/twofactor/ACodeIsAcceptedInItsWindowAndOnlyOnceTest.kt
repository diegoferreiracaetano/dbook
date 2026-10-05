package com.dbook.infrastructure.security.twofactor

import com.dbook.infrastructure.security.Rfc6238TotpService
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ACodeIsAcceptedInItsWindowAndOnlyOnceTest {
    private val secret = "12345678901234567890".toByteArray()
    private val service = Rfc6238TotpService("DBook")
    private val now = Instant.ofEpochSecond(1_111_111_111L) // step 37037037
    private val step = now.epochSecond / 30

    private fun match(
        at: Long,
        afterStep: Long = 0,
    ) = service.matchingStep(secret, service.codeAt(secret, at), now, afterStep)

    @Test
    fun `given the code of the current step when matched then the step is returned`() {
        assertEquals(step, match(step))
    }

    @Test
    fun `given codes one step either side when matched then they are accepted for the drift of a clock`() {
        assertEquals(step - 1, match(step - 1))
        assertEquals(step + 1, match(step + 1))
    }

    @Test
    fun `given codes two steps away when matched then they are refused`() {
        assertNull(match(step - 2))
        assertNull(match(step + 2))
    }

    @Test
    fun `given the step already used when the same code comes again then it is refused, and so is any older one`() {
        assertNull(match(step, afterStep = step))
        assertNull(match(step - 1, afterStep = step))
    }

    @Test
    fun `given a used step when a newer code comes then it is accepted`() {
        assertEquals(step + 1, match(step + 1, afterStep = step))
    }

    @Test
    fun `given a code with the wrong length or letters when matched then it is refused`() {
        assertNull(service.matchingStep(secret, "12345", now, 0))
        assertNull(service.matchingStep(secret, "abcdef", now, 0))
        assertNull(service.matchingStep(secret, "", now, 0))
    }
}
