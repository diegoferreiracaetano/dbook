package com.dbook.presentation

import com.dbook.application.SuggestFlightsCommand
import com.dbook.application.SuggestFlightsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ai")
@Tag(
    name = "AI",
    description = "Natural-language flight suggestions — advisory only, never books or decides on its own",
)
@SecurityRequirement(name = "bearerAuth")
class AiSuggestionController(
    private val suggestFlightsUseCase: SuggestFlightsUseCase,
) {
    @Operation(summary = "Suggests flights matching a natural-language request, out of currently active flights")
    @PostMapping("/suggestions")
    fun suggest(
        @RequestBody request: SuggestFlightsRequest,
        authentication: Authentication,
    ): ResponseEntity<SuggestFlightsResponse> {
        val result =
            suggestFlightsUseCase.execute(
                SuggestFlightsCommand(query = request.query, requestingUserId = authentication.currentUserId()),
            )
        return ResponseEntity.ok(SuggestFlightsResponse.from(result))
    }
}
