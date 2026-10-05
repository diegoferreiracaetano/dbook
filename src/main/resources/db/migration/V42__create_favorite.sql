-- What a customer saved, kept on the server so it follows them to another phone. target_id is the destination's IATA
-- code or the flight's id, as text: the target is a reference by name, not a foreign key, so a kind of target that
-- is not a table (the destination is an airport) fits the same row.
CREATE TABLE favorite (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    target_type VARCHAR(12) NOT NULL CHECK (target_type IN ('DESTINATION', 'FLIGHT')),
    target_id VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, target_type, target_id)
);

-- the list, newest first (the unique index above already serves "is it a favorite?")
CREATE INDEX idx_favorite_user ON favorite (user_id, created_at DESC, id DESC);
