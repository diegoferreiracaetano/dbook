package com.dbook.infrastructure.security.twofactor

import com.dbook.infrastructure.security.SecureRandomRecoveryCodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecoveryCodesAreTypedByPeopleAndGuessedByNobodyTest {
    private val generator = SecureRandomRecoveryCodes()

    @Test
    fun `given many codes when generated then each is two groups of five without look-alike characters`() {
        val codes = List(500) { generator.generate() }

        assertTrue(codes.all { Regex("^[A-HJ-NP-Z2-9]{5}-[A-HJ-NP-Z2-9]{5}$").matches(it) }, codes.first())
        assertEquals(codes.size, codes.toSet().size, "no two alike in 500")
    }
}
