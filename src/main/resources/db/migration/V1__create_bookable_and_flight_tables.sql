CREATE TABLE airport (
    id BIGSERIAL PRIMARY KEY,
    iata_code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL
);

CREATE TABLE bookable (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    total_capacity INTEGER NOT NULL,
    available_capacity INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE flight (
    id BIGINT PRIMARY KEY REFERENCES bookable (id),
    flight_number VARCHAR(20) NOT NULL,
    origin_airport_id BIGINT NOT NULL REFERENCES airport (id),
    destination_airport_id BIGINT NOT NULL REFERENCES airport (id),
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    seat_class VARCHAR(20) NOT NULL
);
