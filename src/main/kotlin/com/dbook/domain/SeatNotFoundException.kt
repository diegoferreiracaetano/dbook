package com.dbook.domain

class SeatNotFoundException(id: Long) : RuntimeException("Seat not found: $id")
