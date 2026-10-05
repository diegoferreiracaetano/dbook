package com.dbook.presentation.customerprivacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AnonymizingErasesWhatItShouldAndKeepsWhatItMustTest : CustomerPrivacyFixture() {
    @Test
    fun `given a customer with a booking, a note and an AI query when anonymized then only what must stay stays`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email, name = "Maria Silva")
        val refresh = loginRefreshToken(email, "s3cret-password")
        val id = userIdOf(email)
        val (booking) = bookSeats(token, 1)
        pay(token, booking)
        addNote(staffToken(), id, "called about a refund")
        jdbcTemplate.update(
            "INSERT INTO ai_suggestion_log (user_id, query, raw_response, created_at) " +
                "VALUES (?, 'beach in july', '{}', now())",
            id,
        )
        val admin = superAdminToken()

        val result = anonymize(admin, id)

        assertEquals(200, result.response.status)
        val profile = bodyOf(profile(admin, id))
        assertEquals("Anonymous customer", profile["name"].asText())
        assertEquals("BLOCKED", profile["status"].asText())
        assertNotNull(profile["anonymizedAt"].takeIf { !it.isNull })
        assertEquals(1, profile["bookings"]["total"].asInt())
        assertEquals(1, profile["payments"]["count"].asInt())
        assertEquals(0, countOf("customer_note", "customer_id", id))
        assertEquals(0, countOf("ai_suggestion_log", "user_id", id))
        assertEquals(
            "ANONYMIZED",
            jdbcTemplate.queryForObject(
                "SELECT cardholder_name FROM payment WHERE customer_id = ?",
                String::class.java,
                id,
            ),
        )
        assertEquals(401, loginStatus(email))
        assertEquals(401, refreshStatus(refresh))
        assertEquals(409, registerStatus(email))
        assertEquals(409, registerStatus(email.uppercase()))
        assertFalse(bodyOf(profile(admin, id)).toString().contains(email))
    }
}
