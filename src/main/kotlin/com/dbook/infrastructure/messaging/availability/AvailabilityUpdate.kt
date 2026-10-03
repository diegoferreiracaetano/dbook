package com.dbook.infrastructure.messaging.availability

data class AvailabilityUpdate(
    val bookableId: Long,
    val availableCapacity: Int,
)
