CREATE TABLE staff_invitation (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPPORT', 'CATALOG_MANAGER', 'SUPER_ADMIN')),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    invited_by BIGINT NOT NULL REFERENCES app_user (id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_staff_invitation_open_email
    ON staff_invitation (email)
    WHERE accepted_at IS NULL AND revoked_at IS NULL;
