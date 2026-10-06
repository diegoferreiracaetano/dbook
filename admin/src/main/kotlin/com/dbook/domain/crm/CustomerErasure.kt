package com.dbook.domain.crm

// What leaves with an anonymized customer, outside the account itself: notes written about them, what they typed
// into the AI search, and the cardholder name on their payments. Bookings and payments stay (fiscal duty).
interface CustomerErasure {
    fun erasePersonalData(customerId: Long)
}
