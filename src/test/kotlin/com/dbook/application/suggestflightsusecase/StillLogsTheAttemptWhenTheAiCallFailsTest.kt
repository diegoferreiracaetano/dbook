package com.dbook.application.suggestflightsusecase

import com.dbook.application.SuggestFlightsCommand
import com.dbook.domain.AiServiceUnavailableException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StillLogsTheAttemptWhenTheAiCallFailsTest : SuggestFlightsUseCaseFixture() {
    @Test
    fun `given a failing AI call when suggesting then it rethrows and still logs the attempt`() {
        val aiService = FixedAiSuggestionService(Result.failure(AiServiceUnavailableException("Bedrock is down")))
        val command = SuggestFlightsCommand(query = "cheap flights to Rio", requestingUserId = 42)

        assertFailsWith<AiServiceUnavailableException> {
            useCase(aiService).execute(command)
        }

        assertEquals(1, logRepository.saved.size)
        assertEquals("ERROR: Bedrock is down", logRepository.saved.single().rawResponse)
    }
}
