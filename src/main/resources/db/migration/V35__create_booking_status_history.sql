-- When the booking was made: the admin list filters and sorts by it. Bookings that already existed get the moment of
-- this migration (the real time was never recorded), the same as their first history entry below.
ALTER TABLE booking ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

-- Every status a booking went through, written by the booking repository itself whenever the status changes, so the
-- timeline does not depend on any use case remembering to log it. actor_id is null for the system (the expiry job).
CREATE TABLE booking_status_history (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking (id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    actor_id BIGINT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_booking_status_history_booking ON booking_status_history (booking_id, occurred_at, id);

INSERT INTO booking_status_history (booking_id, from_status, to_status, actor_id, occurred_at)
SELECT id, NULL, status, NULL, created_at FROM booking;

-- the admin list sorts newest first and filters by status
CREATE INDEX idx_booking_created ON booking (created_at DESC, id DESC);
