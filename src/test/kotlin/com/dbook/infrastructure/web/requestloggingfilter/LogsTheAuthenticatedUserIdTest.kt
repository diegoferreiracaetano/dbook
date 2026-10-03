package com.dbook.infrastructure.web.requestloggingfilter

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LogsTheAuthenticatedUserIdTest : RequestLoggingFixture() {
    @Test
    fun `given a logged in user when a request is made then the access line says who, and anonymous ones do not`() {
        val email = "log${(1..999_999_999).random()}@example.com"
        postToApi("/auth/register", """{"email":"$email","password":"s3cret-password","name":"Log"}""")
        val login = postToApi("/auth/login", """{"email":"$email","password":"s3cret-password"}""")
        val token = ObjectMapper().readTree(login.body())["accessToken"].asText()

        getFromApi("/bookings", mapOf("Authorization" to "Bearer $token"))
        getFromApi("/health")

        val authenticated = accessLinesFor("/bookings").single()
        assertEquals(200, authenticated.formattedMessage.substringAfter("-> ").substringBefore(" ").toInt())
        assertNotNull(authenticated.mdcPropertyMap["userId"]?.toLongOrNull(), "no userId in the MDC")
        assertNull(accessLinesFor("/health").single().mdcPropertyMap["userId"])
    }
}
