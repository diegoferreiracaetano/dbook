-- "how many active bookings does this flight have" and "which of its seats were ever booked": without an index on
-- booking.bookable_id both read the whole table
CREATE INDEX idx_booking_bookable ON booking (bookable_id);

-- the admin flight list: by departure, with a range
CREATE INDEX idx_flight_departure ON flight (departure_time, id);
