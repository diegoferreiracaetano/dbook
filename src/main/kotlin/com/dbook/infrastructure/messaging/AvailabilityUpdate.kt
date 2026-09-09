package com.dbook.infrastructure.messaging

data class AvailabilityUpdate(
    val bookableId: Long,
    val availableCapacity: Int,
)
