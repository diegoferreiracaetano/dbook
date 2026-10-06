package com.dbook.presentation.promo

import com.dbook.application.promo.CreatePromoCommand
import com.dbook.application.promo.CreatePromoUseCase
import com.dbook.application.promo.ListPromosUseCase
import com.dbook.application.promo.SetPromoActiveUseCase
import com.dbook.application.promo.UpdatePromoCommand
import com.dbook.application.promo.UpdatePromoUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/** `/admin/promo-codes` — the team's CRUD of promotional codes. A code is switched off, never deleted. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/promo-codes")
@Tag(name = "Promo codes (admin)", description = "Creating, changing and switching off promotional codes")
@SecurityRequirement(name = "bearerAuth")
class AdminPromoController(
    private val createPromoUseCase: CreatePromoUseCase,
    private val updatePromoUseCase: UpdatePromoUseCase,
    private val setPromoActiveUseCase: SetPromoActiveUseCase,
    private val listPromosUseCase: ListPromosUseCase,
) {
    @Operation(summary = "Creates a code; 409 if it exists (PROMO_WRITE)")
    @PreAuthorize("hasAuthority('PROMO_WRITE')")
    @PostMapping
    fun create(
        @RequestBody request: CreatePromoRequest,
        authentication: Authentication,
    ): ResponseEntity<PromoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            PromoResponse.from(
                createPromoUseCase.execute(
                    CreatePromoCommand(
                        authentication.currentActor(), request.code, request.type, request.value,
                        request.minAmount ?: BigDecimal.ZERO, request.validFrom, request.validUntil,
                        request.maxRedemptions, request.maxPerUser ?: 1,
                    ),
                ),
            ),
        )

    @Operation(summary = "Lists the codes, newest first, with how many times each was used (PROMO_WRITE)")
    @PreAuthorize("hasAuthority('PROMO_WRITE')")
    @GetMapping
    fun list(
        @RequestParam(required = false) active: Boolean?,
        params: PageParams,
    ): PageResponse<PromoResponse> =
        PageResponse.from(listPromosUseCase.search(active, params.toQuery()), PromoResponse::from)

    @Operation(summary = "One code (PROMO_WRITE)")
    @PreAuthorize("hasAuthority('PROMO_WRITE')")
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): PromoResponse = PromoResponse.from(listPromosUseCase.find(id))

    @Operation(summary = "Who used the code, newest first (PROMO_WRITE)")
    @PreAuthorize("hasAuthority('PROMO_WRITE')")
    @GetMapping("/{id}/redemptions")
    fun redemptions(
        @PathVariable id: Long,
        params: PageParams,
    ): PageResponse<PromoRedemptionResponse> =
        PageResponse.from(listPromosUseCase.redemptions(id, params.toQuery()), PromoRedemptionResponse::from)

    @Operation(summary = "Changes the window, the minimum and the limits; never the code, type or value (PROMO_WRITE)")
    @PreAuthorize("hasAuthority('PROMO_WRITE')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdatePromoRequest,
        authentication: Authentication,
    ): PromoResponse =
        PromoResponse.from(
            updatePromoUseCase.execute(
                UpdatePromoCommand(
                    authentication.currentActor(),
                    id,
                    request.minAmount ?: BigDecimal.ZERO,
                    request.validFrom,
                    request.validUntil,
                    request.maxRedemptions,
                    request.maxPerUser ?: 1,
                ),
            ),
        )

    @Operation(summary = "Switches a code off (PROMO_WRITE)")
    @PreAuthorize("hasAuthority('PROMO_WRITE')")
    @PostMapping("/{id}/deactivate")
    fun deactivate(
        @PathVariable id: Long,
        authentication: Authentication,
    ): PromoResponse =
        PromoResponse.from(setPromoActiveUseCase.execute(authentication.currentActor(), id, active = false))

    @Operation(summary = "Switches a code back on (PROMO_WRITE)")
    @PreAuthorize("hasAuthority('PROMO_WRITE')")
    @PostMapping("/{id}/activate")
    fun activate(
        @PathVariable id: Long,
        authentication: Authentication,
    ): PromoResponse =
        PromoResponse.from(setPromoActiveUseCase.execute(authentication.currentActor(), id, active = true))
}
