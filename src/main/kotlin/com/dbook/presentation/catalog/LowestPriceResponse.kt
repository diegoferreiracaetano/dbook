package com.dbook.presentation.catalog

import java.math.BigDecimal

data class LowestPriceResponse(
    val destination: String,
    val lowestPrice: BigDecimal,
)
