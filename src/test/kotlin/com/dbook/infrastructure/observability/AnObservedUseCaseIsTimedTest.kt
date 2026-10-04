package com.dbook.infrastructure.observability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnObservedUseCaseIsTimedTest : ObservabilityFixture() {
    @Test
    fun `given an observed use case when it runs through the api then prometheus reports its timer`() {
        val email = "obs${(1..999_999_999).random()}@example.com"
        val registration =
            postToApi("/v1/auth/register", """{"email":"$email","password":"s3cret-password","name":"Obs"}""")
        assertEquals(201, registration.statusCode())

        val scrape = getFromManagement("/actuator/prometheus").body()

        val timerLine =
            scrape.lines().firstOrNull {
                it.startsWith("dbook_usecase_seconds_count") && "RegisterUserUseCase" in it
            }
        assertTrue(timerLine != null, "no dbook_usecase timer for RegisterUserUseCase in the scrape")
    }
}
