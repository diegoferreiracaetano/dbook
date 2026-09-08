package com.dbook.domain

class BookingNotFoundException(id: Long) : RuntimeException("Booking not found: $id")
