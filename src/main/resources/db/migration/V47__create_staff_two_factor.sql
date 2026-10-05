-- The second factor of a staff member (TOTP, RFC 6238). The secret is stored encrypted (AES-GCM, key from the
-- environment), never in the clear. A row with confirmed_at NULL is an enrollment started and not finished: it does
-- not protect the account yet. last_used_step is the time step of the last accepted code, so that a code (seen over
-- a shoulder, or on the wire) cannot be used twice.
CREATE TABLE staff_totp (
    user_id          BIGINT      PRIMARY KEY REFERENCES app_user (id),
    secret_encrypted TEXT        NOT NULL,
    confirmed_at     TIMESTAMPTZ,
    last_used_step   BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Recovery codes: only a hash is kept. A code is spent once, by an UPDATE that only matches an unspent one.
CREATE TABLE staff_recovery_code (
    id        BIGSERIAL   PRIMARY KEY,
    user_id   BIGINT      NOT NULL REFERENCES app_user (id),
    code_hash TEXT        NOT NULL,
    used_at   TIMESTAMPTZ,
    UNIQUE (user_id, code_hash)
);
