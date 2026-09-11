CREATE TABLE seat (
    id BIGSERIAL PRIMARY KEY,
    bookable_id BIGINT NOT NULL REFERENCES bookable (id),
    label VARCHAR(4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (bookable_id, label)
);
