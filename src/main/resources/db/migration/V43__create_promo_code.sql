-- Promotional codes. The code is stored in capitals, so it is unique whatever case the customer types.
CREATE TABLE promo_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE CHECK (code = upper(code)),
    type VARCHAR(10) NOT NULL CHECK (type IN ('PERCENT', 'FIXED')),
    value NUMERIC(10, 2) NOT NULL CHECK (value > 0),
    min_amount NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (min_amount >= 0),
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    -- null = no limit
    max_redemptions INTEGER CHECK (max_redemptions > 0),
    max_per_user INTEGER NOT NULL DEFAULT 1 CHECK (max_per_user > 0),
    redeemed INTEGER NOT NULL DEFAULT 0 CHECK (redeemed >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES app_user (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (valid_until > valid_from),
    CHECK (type <> 'PERCENT' OR value < 100)
);

-- One row per use. payment_id is unique: a payment redeems at most one code, and a code is redeemed once per payment.
CREATE TABLE promo_redemption (
    id BIGSERIAL PRIMARY KEY,
    promo_id BIGINT NOT NULL REFERENCES promo_code (id),
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    payment_id BIGINT NOT NULL UNIQUE REFERENCES payment (id),
    discount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_promo_redemption_user ON promo_redemption (promo_id, user_id);

-- A payment keeps what it was worth before the discount, the discount and the code it used. amount stays what was
-- actually charged: subtotal - discount.
ALTER TABLE payment
    ADD COLUMN subtotal NUMERIC(10, 2),
    ADD COLUMN discount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN promo_code_id BIGINT REFERENCES promo_code (id),
    ADD COLUMN promo_code VARCHAR(32);
UPDATE payment SET subtotal = amount;
ALTER TABLE payment ALTER COLUMN subtotal SET NOT NULL;

-- The share of the payment's discount that belongs to each booking, so a refund gives back what was really paid for it.
ALTER TABLE booking ADD COLUMN discount NUMERIC(12, 2) NOT NULL DEFAULT 0;
