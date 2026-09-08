package com.dbook.domain

import java.math.BigDecimal

abstract class Bookable(
	open val id: Long? = null,
	open val title: String,
	open val price: BigDecimal,
	open val totalCapacity: Int,
	open val availableCapacity: Int,
	open val active: Boolean = true,
)
