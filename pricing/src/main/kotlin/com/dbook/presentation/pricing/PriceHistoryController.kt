package com.dbook.presentation.pricing

import com.dbook.application.pricing.GetPriceHistoryUseCase
import com.dbook.presentation.common.ApiPaths
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `GET /flights/{id}/price-history` — public, like the search that shows the price. */
@RestController
@RequestMapping("${ApiPaths.V1}/flights")
@Tag(name = "Price history (public)", description = "How a flight's price moved")
class PriceHistoryController(
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase,
) {
    @Operation(summary = "Every price the flight had, oldest first, with the lowest and highest it ever had")
    @GetMapping("/{id}/price-history")
    fun history(
        @PathVariable id: Long,
    ): PriceHistoryResponse = PriceHistoryResponse.from(getPriceHistoryUseCase.execute(id))
}
