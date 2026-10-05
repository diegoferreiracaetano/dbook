-- Hotels. An accommodation is a bookable like a flight (it shares the bookable table), served by an airport: that is its
-- destination, so "hotels at a destination" and the destination's reviews work the same way for flights and hotels.
CREATE TABLE accommodation (
    id BIGINT PRIMARY KEY REFERENCES bookable (id),
    destination_airport_id BIGINT NOT NULL REFERENCES airport (id),
    address VARCHAR(255) NOT NULL,
    stars INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
    description TEXT,
    photo_url VARCHAR(500),
    -- comma separated, lower case: wifi,pool,parking
    amenities VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE INDEX idx_accommodation_destination ON accommodation (destination_airport_id);

-- A kind of room the hotel has, with its nightly rate and how many of them there are.
CREATE TABLE room_type (
    id BIGSERIAL PRIMARY KEY,
    accommodation_id BIGINT NOT NULL REFERENCES accommodation (id),
    name VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    nightly_rate NUMERIC(12, 2) NOT NULL CHECK (nightly_rate > 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (accommodation_id, name)
);

-- The inventory is kept per night: how many rooms of a type are booked on that night. A night with no row has none.
-- Booking a stay raises the count of each of its nights with one conditional statement apiece (see RoomInventory), so
-- two guests can never both take the last room of a night, and a stay that overlaps another only partly is decided
-- night by night.
CREATE TABLE room_night (
    room_type_id BIGINT NOT NULL REFERENCES room_type (id),
    night DATE NOT NULL,
    booked INTEGER NOT NULL DEFAULT 0 CHECK (booked >= 0),
    PRIMARY KEY (room_type_id, night)
);

-- A booking is now either a seat on a flight or a stay in a room. The stay's columns live on the booking itself.
ALTER TABLE booking ALTER COLUMN seat_id DROP NOT NULL;
ALTER TABLE booking
    ADD COLUMN room_type_id BIGINT REFERENCES room_type (id),
    ADD COLUMN check_in DATE,
    ADD COLUMN check_out DATE,
    ADD COLUMN guests INTEGER,
    ADD COLUMN nightly_rate NUMERIC(12, 2);
ALTER TABLE booking ADD CONSTRAINT booking_seat_or_stay CHECK (
    (seat_id IS NOT NULL AND room_type_id IS NULL AND check_in IS NULL)
    OR (seat_id IS NULL AND room_type_id IS NOT NULL AND check_in IS NOT NULL AND check_out > check_in AND guests > 0)
);

CREATE INDEX idx_booking_room_type ON booking (room_type_id) WHERE room_type_id IS NOT NULL;
