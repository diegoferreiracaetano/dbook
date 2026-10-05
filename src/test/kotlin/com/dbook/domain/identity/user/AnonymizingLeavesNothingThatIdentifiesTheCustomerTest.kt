package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnonymizingLeavesNothingThatIdentifiesTheCustomerTest {
    @Test
    fun `given a customer when anonymizing then name and email are replaced and the account is closed for good`() {
        val now = Instant.parse("2026-10-04T12:00:00Z")
        val customer = User(id = 7, email = "maria@example.com", passwordHash = "real-hash", name = "Maria")

        val anonymized = customer.anonymize(now)

        assertEquals("anonymized-7@anonymous.invalid", anonymized.email)
        assertEquals(User.ANONYMIZED_NAME, anonymized.name)
        assertFalse(anonymized.passwordHash.contains("real-hash"))
        assertEquals(UserStatus.BLOCKED, anonymized.status)
        assertEquals(now, anonymized.anonymizedAt)
        assertTrue(anonymized.isAnonymized)
        assertNull(anonymized.lastLoginAt)
        assertEquals(7L, anonymized.id)
    }
}
