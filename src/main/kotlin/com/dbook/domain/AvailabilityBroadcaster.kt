package com.dbook.domain

interface AvailabilityBroadcaster {
    fun broadcast(
        bookableId: Long,
        availableCapacity: Int,
    )
}
