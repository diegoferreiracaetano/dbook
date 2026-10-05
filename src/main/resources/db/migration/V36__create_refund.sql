CREATE TABLE refund (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payment (id),
    booking_id BIGINT NOT NULL REFERENCES booking (id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('CUSTOMER_REQUEST', 'FLIGHT_CANCELLED', 'DUPLICATE', 'OTHER')),
    note VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED')),
    idempotency_key VARCHAR(64) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    requested_by BIGINT NOT NULL REFERENCES app_user (id),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Two attendants refunding the same booking at once: the second insert hits this index and gets a 409. A FAILED
-- refund does not hold the slot, so the booking can be refunded again (or the failed one retried).
CREATE UNIQUE INDEX uq_refund_active_booking ON refund (booking_id) WHERE status <> 'FAILED';

-- one refund per (attendant, key): the same request sent twice is the same refund
CREATE UNIQUE INDEX uq_refund_requester_key ON refund (requested_by, idempotency_key);

CREATE INDEX idx_refund_status ON refund (status, id DESC);
