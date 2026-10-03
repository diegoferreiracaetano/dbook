package com.dbook.domain.booking

class BookingNotFoundException(id: Long) : RuntimeException("Booking not found: $id")
