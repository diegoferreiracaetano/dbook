CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    bookable_id BIGINT NOT NULL REFERENCES bookable (id),
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL
);
