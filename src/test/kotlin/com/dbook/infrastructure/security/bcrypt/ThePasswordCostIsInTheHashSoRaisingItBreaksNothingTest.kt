package com.dbook.infrastructure.security.bcrypt

import com.dbook.infrastructure.security.BCryptPasswordHasher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThePasswordCostIsInTheHashSoRaisingItBreaksNothingTest {
    @Test
    fun `given the production default when a password is hashed then the cost is 12`() {
        val hash = BCryptPasswordHasher(12).hash("s3cret-password")

        assertTrue(hash.startsWith("\$2a\$12\$"), hash.take(7))
    }

    @Test
    fun `given a hash made at an older cost when verified by a hasher at a higher cost then it still verifies`() {
        val old = BCryptPasswordHasher(10).hash("s3cret-password")
        val current = BCryptPasswordHasher(12)

        assertTrue(current.matches("s3cret-password", old))
        assertFalse(current.matches("another-password", old))
        assertEquals(old.take(7), "\$2a\$10\$")
    }
}
