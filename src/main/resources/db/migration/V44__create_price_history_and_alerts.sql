-- Every price a flight had, from its creation: the first row is the opening price. Written in the transaction that
-- sets the price (creating or editing the flight), so the history can never disagree with the flight.
CREATE TABLE flight_price_history (
    id BIGSERIAL PRIMARY KEY,
    flight_id BIGINT NOT NULL REFERENCES bookable (id),
    price NUMERIC(12, 2) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_flight_price_history ON flight_price_history (flight_id, changed_at, id);

-- "Tell me when a flight from origin to destination on that date costs at most target_price". One per customer, route
-- and date. last_notified_at is what keeps a price that bounces around the target from sending a message each time.
CREATE TABLE price_alert (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    travel_date DATE NOT NULL,
    target_price NUMERIC(12, 2) NOT NULL CHECK (target_price > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_notified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, origin, destination, travel_date),
    CHECK (origin <> destination)
);

-- what the evaluator reads on a price event: the alerts of one route and date that are on
CREATE INDEX idx_price_alert_route ON price_alert (origin, destination, travel_date) WHERE active;
