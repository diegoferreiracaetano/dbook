package com.dbook.presentation.pricing

import com.dbook.application.pricing.CreatePriceAlertCommand
import com.dbook.application.pricing.CreatePriceAlertUseCase
import com.dbook.application.pricing.DeletePriceAlertUseCase
import com.dbook.application.pricing.ListPriceAlertsUseCase
import com.dbook.application.pricing.UpdatePriceAlertCommand
import com.dbook.application.pricing.UpdatePriceAlertUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/price-alerts` — the caller's own alerts: "tell me when a flight on this route and date costs at most this". */
@RestController
@RequestMapping("${ApiPaths.V1}/price-alerts")
@Tag(name = "Price alerts", description = "The authenticated user's price alerts")
@SecurityRequirement(name = "bearerAuth")
class PriceAlertController(
    private val createPriceAlertUseCase: CreatePriceAlertUseCase,
    private val listPriceAlertsUseCase: ListPriceAlertsUseCase,
    private val updatePriceAlertUseCase: UpdatePriceAlertUseCase,
    private val deletePriceAlertUseCase: DeletePriceAlertUseCase,
) {
    @Operation(
        summary = "Creates an alert for a route and a date",
        description =
            "404 if an airport does not exist, 400 for a past date, 409 for a second alert on the same route and " +
                "date and 409 PRICE_ALERTS_LIMIT after 20 active ones.",
    )
    @PostMapping
    fun create(
        @RequestBody request: CreatePriceAlertRequest,
        authentication: Authentication,
    ): ResponseEntity<PriceAlertResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            PriceAlertResponse.from(
                createPriceAlertUseCase.execute(
                    CreatePriceAlertCommand(
                        authentication.currentUserId(),
                        request.origin,
                        request.destination,
                        request.date,
                        request.targetPrice,
                    ),
                ),
            ),
        )

    @Operation(summary = "The caller's alerts, newest first")
    @GetMapping
    fun list(
        authentication: Authentication,
        params: PageParams,
    ): PageResponse<PriceAlertResponse> =
        PageResponse.from(
            listPriceAlertsUseCase.execute(authentication.currentUserId(), params.toQuery()),
            PriceAlertResponse::from,
        )

    @Operation(summary = "Changes the target, switches the alert off or on (404 if it is not yours)")
    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdatePriceAlertRequest,
        authentication: Authentication,
    ): PriceAlertResponse =
        PriceAlertResponse.from(
            updatePriceAlertUseCase.execute(
                UpdatePriceAlertCommand(authentication.currentUserId(), id, request.targetPrice, request.active),
            ),
        )

    @Operation(summary = "Deletes the alert (404 if it is not yours)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
        authentication: Authentication,
    ) = deletePriceAlertUseCase.execute(authentication.currentUserId(), id)
}
