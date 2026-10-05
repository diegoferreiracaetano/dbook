-- "bookings paid in this period" reads the history by status and time
CREATE INDEX idx_booking_history_status_time ON booking_status_history (to_status, occurred_at);

-- "refunded in this period": only completed refunds count, and only by when they completed
CREATE INDEX idx_refund_completed ON refund (completed_at) WHERE status = 'COMPLETED';
