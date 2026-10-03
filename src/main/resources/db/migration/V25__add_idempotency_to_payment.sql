ALTER TABLE payment ADD COLUMN idempotency_key VARCHAR(64) NULL;
ALTER TABLE payment ADD COLUMN request_fingerprint VARCHAR(64) NULL;

-- one payment per (customer, key); NULL keys (payments made before this) never collide
CREATE UNIQUE INDEX uq_payment_customer_idempotency_key ON payment (customer_id, idempotency_key);