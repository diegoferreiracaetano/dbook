package com.dbook.domain.audit

enum class AuditAction(val target: String) {
    FLIGHT_CREATED("FLIGHT"),
    BOOKING_CANCELLED_BY_STAFF("BOOKING"),
    ACCESS_DENIED("ENDPOINT"),
}
