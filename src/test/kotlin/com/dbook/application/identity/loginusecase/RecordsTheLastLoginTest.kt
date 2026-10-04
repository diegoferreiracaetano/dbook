package com.dbook.application.identity.loginusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordsTheLastLoginTest : LoginUseCaseFixture() {
    @Test
    fun `given a correct login when it succeeds then the last-login moment is the clock's now`() {
        login()

        assertEquals(listOf(1L to now), userRepository.loginsRecorded)
    }
}
