package com.dbook.domain.catalog

class FlightHasActiveBookingsException(val activeBookings: Long) :
    RuntimeException("The flight has $activeBookings active bookings: refund or cancel them before cancelling it")

class CatalogEntryInUseException(what: String, val flights: Long) :
    RuntimeException("$what is used by $flights flights and cannot be removed")

class DuplicateIataCodeException(iataCode: String) : RuntimeException("The IATA code $iataCode is already in use")

class FlightNotFoundException(id: Long) : RuntimeException("Flight not found: $id")

class CatalogEntryNotFoundException(kind: String, id: Long) : RuntimeException("$kind not found: $id")
