CREATE TABLE customer_note (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES app_user (id),
    author_id BIGINT NOT NULL REFERENCES app_user (id),
    body VARCHAR(2000) NOT NULL CHECK (length(trim(body)) > 0),
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

-- the notes of a customer as the portal lists them: pinned first, newest first, deleted ones out
CREATE INDEX idx_customer_note_customer
    ON customer_note (customer_id, pinned DESC, created_at DESC, id DESC) WHERE deleted_at IS NULL;
