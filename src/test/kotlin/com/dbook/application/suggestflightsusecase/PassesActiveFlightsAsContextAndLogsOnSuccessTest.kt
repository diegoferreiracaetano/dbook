package com.dbook.application.suggestflightsusecase

import com.dbook.application.SuggestFlightsCommand
import com.dbook.domain.AiSuggestion
import com.dbook.domain.AiSuggestionResult
import kotlin.test.Test
import kotlin.test.assertEquals

class PassesActiveFlightsAsContextAndLogsOnSuccessTest : SuggestFlightsUseCaseFixture() {
    @Test
    fun `given a successful AI call when suggesting then active flights are passed and the call is logged`() {
        val aiResult =
            AiSuggestionResult(
                suggestions = listOf(AiSuggestion(flightId = 1, reason = "cheapest match")),
                rawResponse = "{}",
            )
        val aiService = FixedAiSuggestionService(Result.success(aiResult))
        val command = SuggestFlightsCommand(query = "cheap flights to Rio", requestingUserId = 42)

        val result = useCase(aiService).execute(command)

        assertEquals(aiResult, result)
        assertEquals(listOf(flight), aiService.lastCandidates)
        assertEquals(1, logRepository.saved.size)
        assertEquals("{}", logRepository.saved.single().rawResponse)
        assertEquals(42, logRepository.saved.single().userId)
    }
}
