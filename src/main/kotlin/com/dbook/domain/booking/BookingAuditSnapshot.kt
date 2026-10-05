package com.dbook.domain.booking

// A stay adds its room type to what a flight booking records
fun Booking.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "bookableId" to bookable.id,
        "seatId" to seatId,
        "customerId" to customerId,
        "status" to status.name,
    ) + (stay?.let { mapOf("roomTypeId" to it.roomTypeId) }.orEmpty())
