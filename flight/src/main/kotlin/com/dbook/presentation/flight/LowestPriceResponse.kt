package com.dbook.presentation.flight

import java.math.BigDecimal

data class LowestPriceResponse(
    val destination: String,
    val lowestPrice: BigDecimal,
)
