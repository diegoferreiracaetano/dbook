package com.dbook.presentation

import java.math.BigDecimal

data class LowestPriceResponse(
    val destination: String,
    val lowestPrice: BigDecimal,
)
