package com.dbook.domain

class NoAvailabilityException(bookableId: Long) : RuntimeException("No availability for bookable: $bookableId")
