package com.dbook.domain.identity

/** An [EmailSender] could not hand the message over; whoever asked for it decides whether that is fatal. */
class EmailDeliveryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
