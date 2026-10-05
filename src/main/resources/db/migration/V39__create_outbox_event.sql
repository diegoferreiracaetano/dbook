-- The transactional outbox: an event is written in the same transaction as the change that caused it, and a relay
-- delivers it afterwards. There is no window in which the change committed and the event was lost (see docs/mensageria.md).
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    -- what must travel with the message, e.g. the W3C traceparent of the request that caused it
    headers JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- the earliest the event may be delivered: a booking's expiration is due 15 minutes after the booking
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- when the relay may try next: the lease while one relay holds the event, or the backoff after a failure
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);

-- what the relay reads: only the not yet published ones, so the index stays small however much history there is
CREATE INDEX idx_outbox_to_publish ON outbox_event (next_attempt_at) WHERE published_at IS NULL;

-- the cleanup of what was published long ago
CREATE INDEX idx_outbox_published ON outbox_event (published_at) WHERE published_at IS NOT NULL;
