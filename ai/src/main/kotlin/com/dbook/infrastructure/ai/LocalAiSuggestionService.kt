package com.dbook.infrastructure.ai

import com.dbook.domain.ai.AiSuggestion
import com.dbook.domain.ai.AiSuggestionResult
import com.dbook.domain.ai.AiSuggestionService
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.SeatClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.text.Normalizer

private const val MAX_SUGGESTIONS = 3
private const val DESTINATION_WEIGHT = 3
private const val ORIGIN_WEIGHT = 2
private const val CLASS_WEIGHT = 2
private const val MIN_NAME_LENGTH = 3
private val CHEAP_WORDS = listOf("barato", "baratos", "barata", "menor preco", "mais em conta", "economico", "cheap")

/**
 * Answers with plain rules over the same real [candidates] the model would see: no network, no credentials. For
 * development and demos where Bedrock is out of reach (`ai.provider=local`); it matches the request against the
 * cities, airports and cabin class of each flight, and ranks by price when the request asks for something cheap.
 * Like the model, it only ever suggests a flight from the list and never one with no seat left.
 */
@Component
@ConditionalOnProperty(name = ["ai.provider"], havingValue = "local")
class LocalAiSuggestionService : AiSuggestionService {
    override fun suggest(
        query: String,
        candidates: List<Flight>,
    ): AiSuggestionResult {
        val text = normalize(query)
        val cheap = CHEAP_WORDS.any { text.contains(it) }
        val available = candidates.filter { it.availableCapacity > 0 }

        val scored =
            available.map { flight -> flight to score(flight, text) }
        val matching = scored.filter { it.second > 0 }
        val pool = matching.ifEmpty { scored }
        val ordered =
            pool.sortedWith(
                compareByDescending<Pair<Flight, Int>> { if (matching.isEmpty()) 0 else it.second }
                    .thenBy { if (cheap || matching.isEmpty()) it.first.price else null }
                    .thenBy { it.first.departureTime },
            )

        val suggestions =
            ordered.take(MAX_SUGGESTIONS).map { (flight, _) ->
                AiSuggestion(requireNotNull(flight.id), reasonFor(flight, text, cheap, matching.isEmpty()))
            }
        return AiSuggestionResult(suggestions, "local-rules")
    }

    private fun score(
        flight: Flight,
        text: String,
    ): Int {
        var points = 0
        if (mentions(text, flight.destination.city, flight.destination.iataCode, flight.destination.name)) {
            points += DESTINATION_WEIGHT
        }
        if (mentions(text, flight.origin.city, flight.origin.iataCode, flight.origin.name)) points += ORIGIN_WEIGHT
        if (classWords(flight.seatClass).any { text.contains(it) }) points += CLASS_WEIGHT
        return points
    }

    private fun mentions(
        text: String,
        vararg names: String,
    ) = names.any { name ->
        val normalized = normalize(name)
        normalized.length >= MIN_NAME_LENGTH && text.contains(normalized)
    }

    private fun classWords(seatClass: SeatClass): List<String> =
        when (seatClass) {
            SeatClass.ECONOMY -> listOf("economica", "economy")
            SeatClass.PREMIUM_ECONOMY -> listOf("premium")
            SeatClass.BUSINESS -> listOf("executiva", "business")
            SeatClass.FIRST -> listOf("primeira", "first")
        }

    private fun reasonFor(
        flight: Flight,
        text: String,
        cheap: Boolean,
        noMatch: Boolean,
    ): String {
        val route = "${flight.origin.city} → ${flight.destination.city}"
        return when {
            noMatch -> "Não achei o destino no pedido; é uma das opções mais em conta: $route por ${flight.price}."
            cheap -> "Combina com o que você pediu e está entre os mais baratos: $route por ${flight.price}."
            mentions(text, flight.destination.city, flight.destination.iataCode) ->
                "Vai para ${flight.destination.city}, como você pediu: $route por ${flight.price}."
            else -> "Combina com o seu pedido: $route por ${flight.price}."
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}
