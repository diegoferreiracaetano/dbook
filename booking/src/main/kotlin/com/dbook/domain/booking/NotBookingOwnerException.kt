package com.dbook.domain.booking

class NotBookingOwnerException(bookingId: Long) : RuntimeException("User is not the owner of booking: $bookingId")
