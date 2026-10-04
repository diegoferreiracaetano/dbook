ALTER TABLE booking ADD COLUMN price NUMERIC(12, 2);

UPDATE booking SET price = bookable.price FROM bookable WHERE bookable.id = booking.bookable_id;

ALTER TABLE booking
    ALTER COLUMN price SET NOT NULL,
    ADD CONSTRAINT ck_booking_price CHECK (price >= 0);
