CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    card_last4 VARCHAR(4) NOT NULL,
    cardholder_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE booking ADD COLUMN payment_id BIGINT NULL REFERENCES payment (id);
