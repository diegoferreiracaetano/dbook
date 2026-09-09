CREATE TABLE ai_suggestion_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    query TEXT NOT NULL,
    raw_response TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
