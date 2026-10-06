package com.dbook.domain.flight

class FlightHasActiveBookingsException(val activeBookings: Long) :
    RuntimeException("The flight has $activeBookings active bookings: refund or cancel them before cancelling it")

class FlightNotFoundException(id: Long) : RuntimeException("Flight not found: $id")
