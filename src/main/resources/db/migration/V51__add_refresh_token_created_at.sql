-- when the token was issued (each rotation issues a new one): the customer's "active sessions" list shows it as the
-- last time the session was used
ALTER TABLE refresh_token ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
