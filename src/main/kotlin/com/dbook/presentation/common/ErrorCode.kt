package com.dbook.presentation.common

// Clients decide what to show from the code, never from the message. UNAUTHORIZED and RATE_LIMITED
// are written as plain text by infrastructure (it cannot import this class); a test keeps them in step.
enum class ErrorCode {
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    MISSING_HEADER,
    NOT_FOUND,
    CONFLICT,
    STALE_VERSION,
    IDEMPOTENCY_KEY_REUSED,
    UNAUTHORIZED,
    INVALID_CREDENTIALS,
    INVALID_TOKEN,
    INVALID_INVITATION,
    ACCOUNT_BLOCKED,
    FORBIDDEN,
    REFUND_WINDOW_CLOSED,
    FAVORITES_LIMIT,
    PROMO_REJECTED,
    PRICE_ALERTS_LIMIT,
    ROOM_UNAVAILABLE,
    TOO_MANY_ATTEMPTS,
    RATE_LIMITED,
    AI_RESPONSE_INVALID,
    AI_UNAVAILABLE,
}
