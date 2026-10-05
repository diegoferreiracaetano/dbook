package com.dbook.domain.notification

import com.dbook.domain.booking.BookingEvents
import com.dbook.domain.catalog.FlightEvents
import com.dbook.domain.payment.RefundEvents
import com.dbook.domain.pricing.PriceAlertEvents

/** What the customer can be told about. [eventType] is the outbox event that produces it. */
enum class NotificationType(val eventType: String) {
    BOOKING_CONFIRMED(BookingEvents.CONFIRMED),
    BOOKING_EXPIRED(BookingEvents.EXPIRED),
    BOOKING_CANCELLED_BY_STAFF(BookingEvents.CANCELLED_BY_STAFF),
    REFUND_COMPLETED(RefundEvents.COMPLETED),
    FLIGHT_CHANGED(FlightEvents.CHANGED),
    PRICE_ALERT(PriceAlertEvents.TRIGGERED),
    ;

    companion object {
        fun fromEventType(eventType: String): NotificationType? = entries.find { it.eventType == eventType }
    }
}

enum class NotificationChannel { IN_APP, EMAIL, PUSH }
