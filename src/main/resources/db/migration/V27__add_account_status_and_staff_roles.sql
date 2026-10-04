ALTER TABLE app_user ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_user ADD COLUMN blocked_reason VARCHAR(500);
ALTER TABLE app_user ADD COLUMN blocked_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_user ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;
-- not mapped yet (the customer CRM reads it); existing users get the migration moment
ALTER TABLE app_user ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE app_user ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE app_user SET role = 'SUPER_ADMIN' WHERE role = 'ADMIN';

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_role CHECK (role IN ('CLIENT', 'SUPPORT', 'CATALOG_MANAGER', 'SUPER_ADMIN')),
    ADD CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'BLOCKED')),
    ADD CONSTRAINT ck_app_user_blocked CHECK (
        (status = 'BLOCKED' AND blocked_reason IS NOT NULL AND blocked_at IS NOT NULL)
        OR (status = 'ACTIVE' AND blocked_reason IS NULL AND blocked_at IS NULL)
    );