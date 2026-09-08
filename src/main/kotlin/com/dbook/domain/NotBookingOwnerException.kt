package com.dbook.domain

class NotBookingOwnerException(bookingId: Long) : RuntimeException("User is not the owner of booking: $bookingId")
