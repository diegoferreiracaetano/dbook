package com.dbook.domain.booking

fun Booking.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "bookableId" to bookable.id,
        "seatId" to seatId,
        "customerId" to customerId,
        "status" to status.name,
    )
