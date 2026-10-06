package com.dbook.domain.flight

fun Flight.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "flightNumber" to flightNumber,
        "airline" to airline.iataCode,
        "origin" to origin.iataCode,
        "destination" to destination.iataCode,
        "departureTime" to departureTime.toString(),
        "arrivalTime" to arrivalTime.toString(),
        "seatClass" to seatClass.name,
        "price" to price,
        "totalCapacity" to totalCapacity,
        "aircraftType" to aircraftType,
        "status" to status.name,
    )
