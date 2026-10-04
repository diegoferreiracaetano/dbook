-- The "pending bookings" gauge counts these on every Prometheus scrape. Almost every booking
-- leaves PENDING quickly (paid or expired), so an index over only the pending ones stays tiny
-- however large the table grows, and the count never scans it.
CREATE INDEX idx_booking_pending ON booking (id) WHERE status = 'PENDING';
