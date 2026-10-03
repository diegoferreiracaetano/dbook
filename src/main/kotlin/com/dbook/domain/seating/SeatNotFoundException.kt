package com.dbook.domain.seating

class SeatNotFoundException(id: Long) : RuntimeException("Seat not found: $id")
