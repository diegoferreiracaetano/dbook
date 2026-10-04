package com.dbook.application.identity.logoutusecase

import kotlin.test.Test

class IgnoresAnUnknownTokenTest : LogoutUseCaseFixture() {
    @Test
    fun `given a token that does not exist when logging out then nothing happens`() {
        useCase.execute("never-issued")
    }
}
