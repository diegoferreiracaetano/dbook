CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() is only STABLE and an index expression must be IMMUTABLE: this wrapper pins the dictionary
CREATE FUNCTION immutable_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT public.unaccent('public.unaccent', $1) $$;

CREATE INDEX idx_app_user_name_trgm
    ON app_user USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);

CREATE INDEX idx_app_user_email_trgm
    ON app_user USING gin (immutable_unaccent(lower(email)) gin_trgm_ops);

-- the default listing: no search text, newest customers first
CREATE INDEX idx_app_user_client_created
    ON app_user (created_at DESC, id DESC) WHERE role = 'CLIENT';

-- "has bookings" and the booking count per customer would otherwise scan the whole table
CREATE INDEX idx_booking_customer ON booking (customer_id);
