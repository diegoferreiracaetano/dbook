-- Account recovery: confirming the e-mail address and choosing a new password both go through a link that is mailed
-- to the address. Only the hash of the token is stored (as the staff invitations do), so a copy of the database
-- holds no usable link.
ALTER TABLE app_user ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Accounts that exist today are taken as confirmed: asking every current customer to confirm an address that has
-- been working would lock people out of booking. From here on, a new account starts unconfirmed.
UPDATE app_user SET email_verified_at = now();

CREATE TABLE account_token (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id),
    purpose    VARCHAR(30) NOT NULL,
    token_hash TEXT        NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ
);

-- "the open links of this user for this purpose": what issuing a new one has to close
CREATE INDEX idx_account_token_open ON account_token (user_id, purpose) WHERE used_at IS NULL;
