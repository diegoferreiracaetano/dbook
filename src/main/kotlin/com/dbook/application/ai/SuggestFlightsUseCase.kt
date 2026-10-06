package com.dbook.application.ai

import com.dbook.domain.ai.AiSuggestionLog
import com.dbook.domain.ai.AiSuggestionLogRepository
import com.dbook.domain.ai.AiSuggestionResult
import com.dbook.domain.ai.AiSuggestionService
import com.dbook.domain.flight.FlightRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

data class SuggestFlightsCommand(
    val query: String,
    val requestingUserId: Long,
)

/**
 * Suggests flights for a natural-language [SuggestFlightsCommand.query], grounded in
 * currently active flights. Never books or decides on its own — see [execute].
 */
@Observed(name = "dbook.usecase")
@Service
class SuggestFlightsUseCase(
    private val flightRepository: FlightRepository,
    private val aiSuggestionService: AiSuggestionService,
    private val aiSuggestionLogRepository: AiSuggestionLogRepository,
) {
    // Deliberately not @Transactional: the AI call is a slow external HTTP request, and
    // holding a DB connection/transaction open for its duration risks exhausting the
    // pool under load. The log save below runs in its own implicit transaction (Spring
    // Data JPA repositories are transactional per-method by default).
    //
    // Logged on both success AND failure ("always audited" is the closed M7 decision) —
    // a failed call (bad model response, Bedrock unavailable) still consumed a request
    // against the user's rate limit and is still worth a trace, not just the happy path.
    fun execute(command: SuggestFlightsCommand): AiSuggestionResult {
        val candidates = flightRepository.findActive()
        val outcome = runCatching { aiSuggestionService.suggest(command.query, candidates) }
        aiSuggestionLogRepository.save(
            AiSuggestionLog(
                userId = command.requestingUserId,
                query = command.query,
                rawResponse = outcome.fold(onSuccess = { it.rawResponse }, onFailure = { "ERROR: ${it.message}" }),
            ),
        )
        return outcome.getOrThrow()
    }
}
